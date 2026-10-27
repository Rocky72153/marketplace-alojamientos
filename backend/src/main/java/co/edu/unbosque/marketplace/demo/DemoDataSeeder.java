package co.edu.unbosque.marketplace.demo;

import co.edu.unbosque.marketplace.listing.AvailabilityPeriod;
import co.edu.unbosque.marketplace.listing.AvailabilityType;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.listing.ListingData;
import co.edu.unbosque.marketplace.listing.ListingStatus;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates local-only fictional data to make the app easy to demonstrate. */
@Component
@Profile("demo")
@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {
    private static final String ADMIN_EMAIL = "demo-admin@marketplace.test";
    private static final int HOST_COUNT = 500;
    private static final int GUEST_COUNT = 500;
    private static final int LISTING_COUNT = 5_000;
    private static final int LISTINGS_PER_HOST = LISTING_COUNT / HOST_COUNT;
    private static final int FLUSH_EVERY = 200;
    private static final String[] TYPES = { "APARTMENT", "HOUSE", "ROOM", "CABIN" };
    private static final String[] PLACES = { "Centro histórico", "Zona residencial", "Sector turístico", "Cerca del parque", "Zona campestre" };
    private static final String[] TYPE_NAMES = { "Apartamento", "Casa", "Habitación", "Cabaña" };
    private final EntityManager entityManager;
    private final MarketplaceUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final String demoPassword;

    public DemoDataSeeder(EntityManager entityManager, MarketplaceUserRepository users, PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper, @Value("${app.demo-seed.password:}") String demoPassword) {
        this.entityManager = entityManager;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.existsByEmail(ADMIN_EMAIL)) return;
        if (demoPassword == null || demoPassword.length() < 8 || demoPassword.length() > 72) {
            throw new IllegalStateException("Define DEMO_SEED_PASSWORD con una contraseña de 8 a 72 caracteres para habilitar los usuarios demo.");
        }
        List<Destination> destinations = loadDestinations();

        String passwordHash = passwordEncoder.encode(demoPassword);
        MarketplaceUser admin = new MarketplaceUser("Administración de prueba", ADMIN_EMAIL, passwordHash, AccountRole.ADMIN);
        entityManager.persist(admin);
        List<MarketplaceUser> hosts = new ArrayList<>(HOST_COUNT);
        for (int i = 1; i <= HOST_COUNT; i++) {
            MarketplaceUser host = new MarketplaceUser(String.format(Locale.ROOT, "Anfitrión de prueba %03d", i),
                    String.format(Locale.ROOT, "host.%03d@marketplace.test", i), passwordHash, AccountRole.HOST);
            entityManager.persist(host);
            hosts.add(host);
        }
        for (int i = 1; i <= GUEST_COUNT; i++) {
            entityManager.persist(new MarketplaceUser(String.format(Locale.ROOT, "Huésped de prueba %03d", i),
                    String.format(Locale.ROOT, "guest.%03d@marketplace.test", i), passwordHash, AccountRole.GUEST));
        }
        entityManager.flush();
        entityManager.clear();

        LocalDate availabilityStart = LocalDate.now();
        LocalDate availabilityEnd = availabilityStart.plusYears(2);
        String demoNotice = "Registro ficticio para demostración académica. Los datos son ilustrativos y no corresponden a una oferta real.";
        for (int i = 0; i < LISTING_COUNT; i++) {
            int hostIndex = i / LISTINGS_PER_HOST;
            int destinationIndex = i % destinations.size();
            Destination destination = destinations.get(destinationIndex);
            String city = destination.city() + ", " + destination.department();
            String type = TYPES[i % TYPES.length];
            String title = String.format(Locale.ROOT, "Alojamiento demo %04d · %s · %s", i + 1, TYPE_NAMES[i % TYPES.length], destination.city());
            String location = PLACES[i % PLACES.length] + ", " + destination.city();
            long nightlyRate = 78_000L + ((Integer.parseInt(destination.code()) * 7_919L) % 180_000L)
                    + ((i / destinations.size()) * 12_500L);
            Set<String> amenities = amenitiesFor(i);
            Listing listing = new Listing(entityManager.getReference(MarketplaceUser.class, hosts.get(hostIndex).getId()),
                    new ListingData(title, demoNotice, city, location, type, 1 + (i % 8), BigDecimal.valueOf(nightlyRate),
                            amenities, "Información de muestra; reglas sujetas a configuración del anfitrión.", Set.of()));
            listing.setStatus(ListingStatus.ACTIVE);
            entityManager.persist(listing);
            entityManager.persist(new AvailabilityPeriod(listing, availabilityStart, availabilityEnd, AvailabilityType.AVAILABLE, null));
            if ((i + 1) % FLUSH_EVERY == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
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
            if (i == 0 || !Set.of("de", "del", "la", "las", "los", "y", "el").contains(words[i])) {
                if (!words[i].isBlank()) words[i] = words[i].substring(0, 1).toUpperCase(Locale.ROOT) + words[i].substring(1);
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
