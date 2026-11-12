package co.edu.unbosque.marketplace.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.listing.ListingData;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReservationCompletionServiceTest {
    @Autowired private MarketplaceUserRepository users;
    @Autowired private ListingRepository listings;
    @Autowired private ReservationRepository reservations;
    @Autowired private EntityManager entityManager;

    @Test
    @Transactional
    void completesOnlyConfirmedStaysAfterCheckoutInBogota() {
        MarketplaceUser host = users.saveAndFlush(new MarketplaceUser("Host", "host-" + UUID.randomUUID() + "@example.test",
                "hash", AccountRole.HOST));
        MarketplaceUser guest = users.saveAndFlush(new MarketplaceUser("Guest", "guest-" + UUID.randomUUID() + "@example.test",
                "hash", AccountRole.GUEST));
        Listing listing = listings.saveAndFlush(new Listing(host, new ListingData("Casa", "Descripción", "Bogotá",
                "Centro", "HOUSE", 2, new BigDecimal("100000"), Set.of(), "", Set.of())));

        Reservation past = reservation(guest, listing, LocalDate.of(2026, 11, 9));
        Reservation checkoutToday = reservation(guest, listing, LocalDate.of(2026, 11, 10));
        Reservation future = reservation(guest, listing, LocalDate.of(2026, 11, 11));
        Reservation cancelled = reservation(guest, listing, LocalDate.of(2026, 11, 9));
        cancelled.cancel("Cambio de planes", Instant.parse("2026-11-01T12:00:00Z"), BigDecimal.ZERO, BigDecimal.ZERO);
        reservations.saveAllAndFlush(List.of(past, checkoutToday, future, cancelled));

        // At 02:00 UTC it is still November 10 in Bogotá.
        Clock clock = Clock.fixed(Instant.parse("2026-11-11T02:00:00Z"), ZoneOffset.UTC);
        ReservationCompletionService completion = new ReservationCompletionService(reservations, clock);
        assertEquals(1, completion.completeDueReservations());
        entityManager.clear();
        assertEquals(ReservationStatus.COMPLETED, reservations.findById(past.getId()).orElseThrow().getStatus());
        assertEquals(ReservationStatus.CONFIRMED, reservations.findById(checkoutToday.getId()).orElseThrow().getStatus());
        assertEquals(ReservationStatus.CONFIRMED, reservations.findById(future.getId()).orElseThrow().getStatus());
        assertEquals(ReservationStatus.CANCELLED, reservations.findById(cancelled.getId()).orElseThrow().getStatus());
        assertEquals(0, completion.completeDueReservations());
    }

    private Reservation reservation(MarketplaceUser guest, Listing listing, LocalDate checkout) {
        BigDecimal zero = new BigDecimal("0.00");
        Reservation.ReservationDetails details = new Reservation.ReservationDetails(checkout.minusDays(2), checkout,
                2, 1, new BigDecimal("100000"), new BigDecimal("200000"), new BigDecimal("35000"),
                new BigDecimal("20000"), zero, zero, new BigDecimal("255000"), "COP", "TEST-REFERENCE", 7, 2,
                new BigDecimal("50.00"));
        return new Reservation(guest, listing, UUID.randomUUID().toString(), "test-fingerprint", details, List.of());
    }
}
