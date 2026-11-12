package co.edu.unbosque.marketplace.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.listing.ListingService;
import co.edu.unbosque.marketplace.notification.NotificationService;
import co.edu.unbosque.marketplace.review.ReviewRepository;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ReservationRestrictionRaceTest {
    @Test
    void accountRestrictedAfterAuthenticationCannotReserveAfterAcquiringLock() {
        MarketplaceUser stalePrincipal = new MarketplaceUser("Huésped", "guest@example.test", "hash", AccountRole.GUEST);
        MarketplaceUser lockedAccount = mock(MarketplaceUser.class);
        when(lockedAccount.isEnabled()).thenReturn(false);
        MarketplaceUserRepository users = mock(MarketplaceUserRepository.class);
        when(users.lockForReservationById(stalePrincipal.getId())).thenReturn(Optional.of(lockedAccount));
        ReservationRepository reservations = mock(ReservationRepository.class);
        ListingService listings = mock(ListingService.class);
        TestPaymentSimulator payments = mock(TestPaymentSimulator.class);
        NotificationService notifications = mock(NotificationService.class);
        ReservationService service = new ReservationService(listings, reservations, mock(BookingPricePolicy.class),
                payments, mock(AuditService.class), users, notifications, mock(ReservationCompletionService.class),
                mock(ReviewRepository.class));
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        var request = new ReservationService.ReservationRequest(UUID.randomUUID(), tomorrow, tomorrow.plusDays(1), 1, List.of());

        assertTrue(stalePrincipal.isEnabled());
        DomainException error = assertThrows(DomainException.class, () -> service.reserve(stalePrincipal, request, "idempotency-key"));
        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
        verifyNoInteractions(reservations, listings, payments, notifications);
    }
}
