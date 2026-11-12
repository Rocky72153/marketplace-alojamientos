package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.edu.unbosque.marketplace.listing.AvailabilityRepository;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles({"test", "demo"})
@TestPropertySource(properties = {"app.demo-seed.password=demo-seed-password", "app.demo-seed.listing-count=10000"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoDataSeederFreshAcceptanceTest {
    @Autowired ListingRepository listings;
    @Autowired AvailabilityRepository availability;
    @Autowired MarketplaceUserRepository users;

    @Test
    void newDatabaseGetsTenThousandDemoListings() {
        assertEquals(10_000, listings.count());
        assertEquals(10_000, availability.count());
        assertEquals(1_001, users.count());
    }
}
