package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unbosque.marketplace.listing.AvailabilityRepository;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.listing.ListingStatus;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "test", "demo" })
@TestPropertySource(properties = "app.demo-seed.password=demo-seed-password")
@DirtiesContext
class DemoDataSeederAcceptanceTest {
    @Autowired MockMvc mockMvc;
    @Autowired ListingRepository listings;
    @Autowired AvailabilityRepository availability;
    @Autowired MarketplaceUserRepository users;

    @Test
    void seedsColombianCatalogAndEveryAccountRoleOnlyForDemo() throws Exception {
        assertEquals(5_000, listings.count());
        assertEquals(5_000, availability.count());
        assertEquals(1_122, listings.findDistinctCitiesByStatus(ListingStatus.ACTIVE).size());
        assertEquals(1_001, users.count());
        assertEquals(500, users.findAll().stream().filter(user -> user.getRole() == AccountRole.GUEST).count());
        assertEquals(500, users.findAll().stream().filter(user -> user.getRole() == AccountRole.HOST).count());
        assertEquals(1, users.findAll().stream().filter(user -> user.getRole() == AccountRole.ADMIN).count());

        mockMvc.perform(get("/api/listings/cities"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10));
        mockMvc.perform(get("/api/listings/cities").param("query", "bogota"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("Bogotá D.C., Bogotá D.C."));
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"guest.001@marketplace.test\",\"password\":\"demo-seed-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("GUEST"));
        mockMvc.perform(get("/api/reservations")).andExpect(status().isUnauthorized());
    }
}
