package co.edu.unbosque.marketplace.booking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.listing.ListingService;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.notification.NotificationService;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StripeCancellationServiceTest {
    StripeCheckoutAttemptRepository attempts = mock(StripeCheckoutAttemptRepository.class);
    ReservationRepository reservations = mock(ReservationRepository.class);
    MarketplaceUserRepository users = mock(MarketplaceUserRepository.class);
    ListingService inventory = mock(ListingService.class);
    ListingRepository listings = mock(ListingRepository.class);
    AuditService audit = mock(AuditService.class);
    NotificationService notifications = mock(NotificationService.class);
    StripeCancellationService subject = new StripeCancellationService(attempts, reservations, users, inventory, listings, audit, notifications);
    MarketplaceUser guest = mock(MarketplaceUser.class);
    Reservation reservation = mock(Reservation.class, RETURNS_DEEP_STUBS);
    UUID guestId = UUID.randomUUID(), listingId = UUID.randomUUID(), reservationId = UUID.randomUUID();
    StripeCheckoutAttempt attempt;

    @BeforeEach void setup() {
        attempt = new StripeCheckoutAttempt(reservationId, guestId, listingId, 80000, Instant.now().plusSeconds(3600));
        attempt.opened("cs_test_123", "https://checkout.stripe.com/pay/123");
        attempt.paid("pi_123");
        when(attempts.findByReservationId(reservationId)).thenReturn(Optional.of(attempt));
        when(attempts.lockById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(users.lockForReservationById(guestId)).thenReturn(Optional.of(guest));
        when(reservations.lockById(reservationId)).thenReturn(Optional.of(reservation));
        when(listings.lockById(listingId)).thenReturn(Optional.of(mock(Listing.class)));
        when(guest.getRole()).thenReturn(AccountRole.GUEST); when(guest.getId()).thenReturn(guestId);
        when(guest.isEnabled()).thenReturn(true); when(guest.isAccountNonLocked()).thenReturn(true);
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getCheckIn()).thenReturn(LocalDate.now(ZoneId.of("America/Bogota")).plusDays(10));
        when(reservation.getPolicyFreeRefundDays()).thenReturn(7);
        when(reservation.getTotal()).thenReturn(new BigDecimal("800.00"));
    }

    @Test void eligibleCancellationRecordsPendingRefundAndReleasesInventory() {
        subject.cancel(guest, reservationId, new ReservationService.CancelRequest("Cambio de planes"));
        assertEquals(StripeCheckoutAttempt.State.REFUND_PENDING, attempt.getState());
        assertEquals(80000L, attempt.getRefundAmountMinor());
        verify(inventory).releaseReservation(listingId, reservationId);
        verify(reservation).cancelStripeTest(eq("Cambio de planes"), any(), eq(new BigDecimal("100.00")), eq(new BigDecimal("800.00")));
        verify(reservation, never()).stripeTestRefunded();
    }
    @Test void anotherGuestCannotCancelSomeoneElsesCheckout() {
        when(guest.getId()).thenReturn(UUID.randomUUID());
        assertThrows(RuntimeException.class, () -> subject.cancel(guest, reservationId, new ReservationService.CancelRequest("Motivo")));
        verifyNoInteractions(inventory);
        assertEquals(StripeCheckoutAttempt.State.PAID, attempt.getState());
    }
    @Test void cancellationWithoutRefundIsNotShownAsConfirmed() {
        when(reservation.getCheckIn()).thenReturn(LocalDate.now(ZoneId.of("America/Bogota")).plusDays(1));
        when(reservation.getPolicyPartialRefundDays()).thenReturn(2);
        subject.cancel(guest, reservationId, new ReservationService.CancelRequest("Cambio"));
        assertEquals(StripeCheckoutAttempt.State.CANCELLED, attempt.getState());
        assertNull(attempt.getRefundAmountMinor());
    }
}
