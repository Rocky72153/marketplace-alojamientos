package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unbosque.marketplace.listing.AvailabilityRepository;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.listing.ListingService;
import co.edu.unbosque.marketplace.listing.ListingStatus;
import co.edu.unbosque.marketplace.demo.DemoDataSeeder;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Set;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "test", "demo" })
@TestPropertySource(properties = {"app.demo-seed.password=demo-seed-password", "app.demo-seed.listing-count=5000"})
@DirtiesContext
class DemoDataSeederAcceptanceTest {
    @Autowired MockMvc mockMvc;
    @Autowired ListingRepository listings;
    @Autowired AvailabilityRepository availability;
    @Autowired MarketplaceUserRepository users;
    @Autowired ListingService listingService;
    @Autowired EntityManager entityManager;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired Environment environment;

    @Test
    void seedsColombianCatalogAndEveryAccountRoleOnlyForDemo() throws Exception {
        assertEquals(5_000, listings.count());
        assertEquals(5_000, availability.count());
        assertEquals(1_122, listings.findDistinctCitiesByStatus(ListingStatus.ACTIVE).size());
        assertEquals(1_001, users.count());
        assertEquals(500, users.findAll().stream().filter(user -> user.getRole() == AccountRole.GUEST).count());
        assertEquals(500, users.findAll().stream().filter(user -> user.getRole() == AccountRole.HOST).count());
        assertEquals(1, users.findAll().stream().filter(user -> user.getRole() == AccountRole.ADMIN).count());
        var medellinResults = listingService.search("medellin", null, null, null, null, null, null, Set.of());
        assertEquals(5, medellinResults.size());
        assertTrue(medellinResults.stream().allMatch(listing -> listing.getCity().startsWith("Medellín")));

        mockMvc.perform(get("/api/listings").param("city", "medellin"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(5));
        mockMvc.perform(get("/api/listings/cities"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10));
        mockMvc.perform(get("/api/listings/cities").param("query", "bogota"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("Bogotá D.C., Bogotá D.C."));
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"guest.001@marketplace.test\",\"password\":\"demo-seed-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("GUEST"));
        mockMvc.perform(get("/api/reservations")).andExpect(status().isUnauthorized());

        // Model the previous release: listings exist but the new identity table is empty.
        var originalIds = Set.copyOf(entityManager.createQuery("select l.id from Listing l", java.util.UUID.class).getResultList());
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        var selectedIds = originalIds.iterator();
        var editedId = selectedIds.next();
        var deletedId = selectedIds.next();
        var editedHostId = listings.findById(editedId).orElseThrow().getHost().getId();
        transaction.executeWithoutResult(status -> {
            entityManager.createQuery("delete from DemoSeedEntry").executeUpdate();
            entityManager.createQuery("update Listing l set l.title = :title where l.id = :id")
                    .setParameter("title", "Publicación modificada por su anfitrión").setParameter("id", editedId).executeUpdate();
            entityManager.createQuery("update MarketplaceUser u set u.name = :name where u.id = :id")
                    .setParameter("name", "Nombre actualizado").setParameter("id", editedHostId).executeUpdate();
            availability.deleteAll(availability.findByListing_IdOrderByStartDateAsc(deletedId));
            listings.deleteById(deletedId);
        });
        DemoDataSeeder expanded = new DemoDataSeeder(entityManager, passwordEncoder, objectMapper,
                "demo-seed-password", 10_000, "jdbc:h2:mem:test", environment);
        transaction.executeWithoutResult(status -> expanded.run());
        assertEquals(9_999, listings.count());
        assertEquals("Publicación modificada por su anfitrión", listings.findById(editedId).orElseThrow().getTitle());
        assertEquals("Nombre actualizado", users.findById(editedHostId).orElseThrow().getName());
        assertFalse(listings.existsById(deletedId));
        assertEquals(9_999, availability.count());
        assertEquals(1_001, users.count());
        var migratedIds = Set.copyOf(entityManager.createQuery("select l.id from Listing l", java.util.UUID.class).getResultList());
        assertTrue(originalIds.stream().filter(id -> !id.equals(deletedId)).allMatch(migratedIds::contains));
        transaction.executeWithoutResult(status -> expanded.run());
        assertEquals(9_999, listings.count());
        assertEquals(9_999, availability.count());
        assertFalse(listings.existsById(deletedId));
        assertEquals(1_001, users.count());
    }
}
