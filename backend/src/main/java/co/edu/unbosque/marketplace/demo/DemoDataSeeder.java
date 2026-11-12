package co.edu.unbosque.marketplace.demo;

import co.edu.unbosque.marketplace.listing.AvailabilityPeriod;
import co.edu.unbosque.marketplace.listing.AvailabilityType;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.listing.ListingData;
import co.edu.unbosque.marketplace.listing.ListingStatus;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates local-only fictional data to make the app easy to demonstrate. */
@Component
@Profile({"local", "demo"})
@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {
    private static final String ADMIN_EMAIL = "demo-admin@marketplace.test";
    private static final int HOST_COUNT = 500;
    private static final int GUEST_COUNT = 500;
    private static final int LISTINGS_PER_HOST = 10;
    private static final int FLUSH_EVERY = 200;
    private static final String DEMO_NOTICE = "Registro ficticio para demostración académica. Los datos son ilustrativos y no corresponden a una oferta real.";
    private static final String[] TYPES = { "APARTMENT", "HOUSE", "ROOM", "CABIN" };
    private static final String[] PLACES = { "Centro histórico", "Zona residencial", "Sector turístico", "Cerca del parque", "Zona campestre" };
    private static final String[] TYPE_NAMES = { "Apartamento", "Casa", "Habitación", "Cabaña" };
    private static final Set<String> LOWERCASE_ARTICLES = Set.of("de", "del", "la", "las", "los", "y", "el");
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final String demoPassword;
    private final int listingCount;
    private final String databaseUrl;
    private final Environment environment;

    public DemoDataSeeder(EntityManager entityManager, PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper, @Value("${app.demo-seed.password:}") String demoPassword,
            @Value("${app.demo-seed.listing-count:10000}") int listingCount,
            @Value("${spring.datasource.url}") String databaseUrl, Environment environment) {
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.demoPassword = demoPassword;
        this.listingCount = listingCount;
        this.databaseUrl = databaseUrl;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (environment.acceptsProfiles(Profiles.of("local"))
                && !databaseUrl.matches("jdbc:postgresql://(?:localhost|127\\.0\\.0\\.1)(?::[0-9]+)?/[^?]+(?:\\?.*)?")) {
            throw new IllegalStateException("El perfil local solo carga datos demo en PostgreSQL local. Configura un perfil de producción para otras bases.");
        }
        if (listingCount < 0 || listingCount > 100_000) {
            throw new IllegalStateException("DEMO_SEED_LISTING_COUNT debe estar entre 0 y 100000.");
        }
        if (demoPassword != null && !demoPassword.isEmpty() && (demoPassword.length() < 8 || demoPassword.length() > 72)) {
            throw new IllegalStateException("Define DEMO_SEED_PASSWORD con una contraseña de 8 a 72 caracteres para habilitar los usuarios demo.");
        }
        List<Destination> destinations = loadDestinations();

        Set<Integer> seeded = new HashSet<>(entityManager.createQuery("select e.seedNumber from DemoSeedEntry e", Integer.class).getResultList());
        Map<String, List<Object[]>> legacy = new HashMap<>();
        entityManager.createQuery("select l.host.email, l.title, l.description, l.id from Listing l "
                        + "where l.host.email like :email and l.title like :title", Object[].class)
                .setParameter("email", "host.%@marketplace.test").setParameter("title", "Alojamiento demo %")
                .getResultList().forEach(row -> legacy.computeIfAbsent(row[0] + "\n" + row[1], key -> new ArrayList<>()).add(row));

        int recognizedLegacy = 0;
        if (seeded.isEmpty()) {
            for (int i = 0; i < 5_000; i++) {
                String hostEmail = String.format(Locale.ROOT, "host.%03d@marketplace.test", (i / LISTINGS_PER_HOST) + 1);
                List<Object[]> matches = legacy.getOrDefault(hostEmail + "\n" + demoTitle(i, destinations.get(i % destinations.size())), List.of());
                if (matches.size() == 1 && DEMO_NOTICE.equals(matches.get(0)[2])) recognizedLegacy++;
            }
        }
        boolean legacyImport = seeded.isEmpty() && recognizedLegacy >= 4_500;
        if (seeded.isEmpty() && !legacyImport && listingCount > 0) {
            long existingDemoHostListings = entityManager.createQuery(
                    "select count(l) from Listing l where l.host.email like :email", Long.class)
                    .setParameter("email", "host.%@marketplace.test").getSingleResult();
            if (existingDemoHostListings > 0) {
                throw new IllegalStateException("No se puede identificar sin ambigüedad el catálogo demo anterior; no se modificaron los datos existentes.");
            }
        }
        boolean knownCatalog = legacyImport || !seeded.isEmpty();

        // Without a configured password, generated accounts cannot be signed into using a predictable secret.
        String passwordHash = passwordEncoder.encode(demoPassword == null || demoPassword.isEmpty()
                ? UUID.randomUUID().toString() + UUID.randomUUID().toString() : demoPassword);
        Map<String, MarketplaceUser> demoUsers = new HashMap<>();
        entityManager.createQuery("select u from MarketplaceUser u where u.email like :email", MarketplaceUser.class)
                .setParameter("email", "%@marketplace.test").getResultList().forEach(user -> demoUsers.put(user.getEmail(), user));
        ensureAccount(demoUsers, "Administración de prueba", ADMIN_EMAIL, AccountRole.ADMIN, passwordHash, knownCatalog);
        List<UUID> hosts = new ArrayList<>(HOST_COUNT);
        for (int i = 1; i <= HOST_COUNT; i++) {
            MarketplaceUser host = ensureAccount(demoUsers, String.format(Locale.ROOT, "Anfitrión de prueba %03d", i),
                    String.format(Locale.ROOT, "host.%03d@marketplace.test", i), AccountRole.HOST, passwordHash, knownCatalog);
            hosts.add(host.getId());
        }
        for (int i = 1; i <= GUEST_COUNT; i++) {
            ensureAccount(demoUsers, String.format(Locale.ROOT, "Huésped de prueba %03d", i),
                    String.format(Locale.ROOT, "guest.%03d@marketplace.test", i), AccountRole.GUEST, passwordHash, knownCatalog);
        }
        entityManager.flush();
        entityManager.clear();

        LocalDate availabilityStart = LocalDate.now(ZoneId.of("America/Bogota"));
        LocalDate availabilityEnd = availabilityStart.plusYears(2);
        for (int i = 0; i < listingCount; i++) {
            if (seeded.contains(i + 1)) continue;
            int hostIndex = (i / LISTINGS_PER_HOST) % HOST_COUNT;
            int destinationIndex = i % destinations.size();
            Destination destination = destinations.get(destinationIndex);
            String city = destination.city() + ", " + destination.department();
            String type = TYPES[i % TYPES.length];
            String title = demoTitle(i, destination);
            String hostEmail = String.format(Locale.ROOT, "host.%03d@marketplace.test", hostIndex + 1);
            List<Object[]> matches = legacy.getOrDefault(hostEmail + "\n" + title, List.of());
            if (legacyImport && i < 5_000 && (matches.size() != 1 || !DEMO_NOTICE.equals(matches.get(0)[2]))) {
                // Missing, deleted, renamed, or otherwise ambiguous legacy entries stay omitted.
                entityManager.persist(new DemoSeedEntry(i + 1, null));
                continue;
            }
            if (!matches.isEmpty()) {
                if (matches.size() != 1 || !DEMO_NOTICE.equals(matches.get(0)[2])) {
                    throw new IllegalStateException("Existe una colisión con un alojamiento demo anterior; no se modificaron los datos existentes.");
                }
                entityManager.persist(new DemoSeedEntry(i + 1, (UUID) matches.get(0)[3]));
                continue;
            }
            String location = PLACES[i % PLACES.length] + ", " + destination.city();
            long nightlyRate = 78_000L + ((Integer.parseInt(destination.code()) * 7_919L) % 180_000L)
                    + ((i / destinations.size()) * 12_500L);
            Set<String> amenities = amenitiesFor(i);
            Listing listing = new Listing(entityManager.getReference(MarketplaceUser.class, hosts.get(hostIndex)),
                    new ListingData(title, DEMO_NOTICE, city, location, type, 1 + (i % 8), BigDecimal.valueOf(nightlyRate),
                            amenities, "Información de muestra; reglas sujetas a configuración del anfitrión.", Set.of()));
            listing.setStatus(ListingStatus.ACTIVE);
            entityManager.persist(listing);
            entityManager.persist(new DemoSeedEntry(i + 1, listing.getId()));
            entityManager.persist(new AvailabilityPeriod(listing, availabilityStart, availabilityEnd, AvailabilityType.AVAILABLE, null));
            if ((i + 1) % FLUSH_EVERY == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
    }

    private String demoTitle(int index, Destination destination) {
        return String.format(Locale.ROOT, "Alojamiento demo %04d · %s · %s", index + 1, TYPE_NAMES[index % TYPES.length], destination.city());
    }

    private MarketplaceUser ensureAccount(Map<String, MarketplaceUser> existing, String name, String email,
            AccountRole role, String passwordHash, boolean knownCatalog) {
        MarketplaceUser user = existing.get(email);
        if (user != null) {
            if (user.getRole() != role || (!knownCatalog && !name.equals(user.getName()))) {
                throw new IllegalStateException("La cuenta " + email + " no coincide con la cuenta demo esperada; no se modificó.");
            }
            return user;
        }
        user = new MarketplaceUser(name, email, passwordHash, role);
        entityManager.persist(user);
        return user;
    }

    private List<Destination> loadDestinations() {
        try (var input = new ClassPathResource("demo/colombian-destinations.json").getInputStream()) {
            List<Destination> rawDestinations = objectMapper.readValue(input, new TypeReference<>() { });
            if (rawDestinations.size() < 1_000) {
                throw new IllegalStateException("El catálogo colombiano debe incluir al menos 1.000 municipios.");
            }
            return rawDestinations.stream().map(destination -> new Destination(destination.code(),
                    toDisplayName(destination.city()), toDisplayName(destination.department()))).toList();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer el catálogo de municipios colombiano.", exception);
        }
    }

    private String toDisplayName(String value) {
        if (value.equalsIgnoreCase("BOGOTÁ, D.C.")) return "Bogotá D.C.";
        String[] words = value.toLowerCase(Locale.ROOT).split(" ");
        for (int i = 0; i < words.length; i++) {
            if (!words[i].isBlank() && (i == 0 || !LOWERCASE_ARTICLES.contains(words[i]))) {
                words[i] = words[i].substring(0, 1).toUpperCase(Locale.ROOT) + words[i].substring(1);
            }
        }
        String displayName = String.join(" ", words);
        for (String article : List.of("El", "La", "Los", "Las")) {
            String suffix = ", " + article;
            if (displayName.endsWith(suffix)) return article + " " + displayName.substring(0, displayName.length() - suffix.length());
        }
        return displayName;
    }

    private Set<String> amenitiesFor(int index) {
        if (index % 4 == 0) return Set.of("WIFI", "PARKING", "POOL");
        if (index % 4 == 1) return Set.of("WIFI", "AIR_CONDITIONING");
        if (index % 4 == 2) return Set.of("WIFI", "PARKING");
        return Set.of("WIFI");
    }

    private record Destination(String code, String city, String department) { }
}
