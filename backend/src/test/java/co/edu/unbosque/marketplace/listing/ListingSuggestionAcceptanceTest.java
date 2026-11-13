package co.edu.unbosque.marketplace.listing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:suggestions_${random.uuid};DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ListingSuggestionAcceptanceTest {
    @Autowired MockMvc mvc;
    @Autowired ListingRepository listings;
    @Autowired MarketplaceUserRepository users;

    @Test
    void returnsAtMostEightActivePublicCardsWithoutPrivateAccountData() throws Exception {
        MarketplaceUser host = users.saveAndFlush(new MarketplaceUser("Anfitrión de prueba",
                "suggestions-host@example.test", "hash-for-test", AccountRole.HOST));
        for (int i = 0; i < 12; i++) {
            Listing listing = new Listing(host, new ListingData("Casa " + i, "Descripción ficticia", "Bogotá",
                    "Centro", "HOUSE", 3, new BigDecimal("100000.00"), Set.of("WIFI"), "", Set.of()));
            listing.setStatus(ListingStatus.ACTIVE);
            listings.save(listing);
        }
        Listing hidden = new Listing(host, new ListingData("No publicar", "Descripción ficticia", "Bogotá",
                "Centro", "HOUSE", 3, new BigDecimal("100000.00"), Set.of(), "", Set.of()));
        hidden.setStatus(ListingStatus.BLOCKED);
        listings.saveAndFlush(hidden);

        mvc.perform(get("/api/listings/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(8))
                .andExpect(jsonPath("$.items[0].id").exists())
                .andExpect(jsonPath("$.items[0].pricePerNight").value(100000.00))
                .andExpect(jsonPath("$.items[0].hostId").doesNotExist())
                .andExpect(jsonPath("$.items[0].hostName").doesNotExist())
                .andExpect(jsonPath("$.items[?(@.title == 'No publicar')]").isEmpty());
    }
}
