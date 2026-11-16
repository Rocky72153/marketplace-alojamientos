package co.edu.unbosque.marketplace.booking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.listing.ListingService;
import co.edu.unbosque.marketplace.listing.ListingStatus;
import co.edu.unbosque.marketplace.notification.NotificationService;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StripeEventTransactionsTest {
    StripeCheckoutAttemptRepository attempts = mock(StripeCheckoutAttemptRepository.class);
    StripeProcessedEventRepository processed = mock(StripeProcessedEventRepository.class);
    ReservationRepository reservations = mock(ReservationRepository.class);
    ListingRepository listings = mock(ListingRepository.class);
    ListingService inventory = mock(ListingService.class);
    MarketplaceUserRepository users = mock(MarketplaceUserRepository.class);
    AuditService audit = mock(AuditService.class);
    NotificationService notifications = mock(NotificationService.class);
    StripeEventTransactions subject = new StripeEventTransactions(attempts, processed, reservations, listings,
            inventory, users, audit, notifications);
    MarketplaceUser guest = mock(MarketplaceUser.class), host = mock(MarketplaceUser.class);
    Listing listing = mock(Listing.class);
    Reservation reservation = mock(Reservation.class);
    UUID guestId = UUID.randomUUID(), hostId = UUID.randomUUID(), listingId = UUID.randomUUID(), reservationId = UUID.randomUUID();
    StripeCheckoutAttempt attempt;

    @BeforeEach void setup() {
        attempt = new StripeCheckoutAttempt(reservationId, guestId, listingId, 80000, Instant.now().plusSeconds(3600));
        attempt.opened("cs_test_123", "https://checkout.stripe.com/pay/123");
        when(attempts.lockById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(reservations.lockById(reservationId)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.PENDING_PAYMENT);
        when(users.lockForReservationById(guestId)).thenReturn(Optional.of(guest));
        when(users.lockForReservationById(hostId)).thenReturn(Optional.of(host));
        when(guest.isEnabled()).thenReturn(true); when(guest.isAccountNonLocked()).thenReturn(true);
        when(host.isEnabled()).thenReturn(true); when(host.isAccountNonLocked()).thenReturn(true);
        when(host.getId()).thenReturn(hostId); when(listing.getHost()).thenReturn(host);
        when(listing.getStatus()).thenReturn(ListingStatus.ACTIVE);
        when(listings.lockById(listingId)).thenReturn(Optional.of(listing));
    }
    StripeTestGateway.VerifiedEvent paid(String id) {
        return new StripeTestGateway.VerifiedEvent(id, "checkout.session.completed", attempt.getId(),
                "cs_test_123", 80000, "cop", "paid", "pi_123");
    }
    @Test void confirmsOnlyVerifiedMatchingPaidEvent() {
        subject.process(paid("evt_1"));
        assertEquals(StripeCheckoutAttempt.State.PAID, attempt.getState());
        verify(reservation).confirmStripeTestPayment("pi_123");
        verify(notifications).reservationConfirmed(reservation);
        verify(processed).save(any(StripeProcessedEvent.class));
        verifyNoInteractions(inventory);
    }
    @Test void duplicateEventDoesNotRepeatFulfillment() {
        when(processed.existsById("evt_1")).thenReturn(true);
        subject.process(paid("evt_1"));
        assertEquals(StripeCheckoutAttempt.State.OPEN, attempt.getState());
        verifyNoInteractions(reservations, inventory, notifications);
    }
    @Test void restrictedGuestCausesRefundInsteadOfConfirmation() {
        when(guest.isAccountNonLocked()).thenReturn(false);
        subject.process(paid("evt_2"));
        assertEquals(StripeCheckoutAttempt.State.REFUND_PENDING, attempt.getState());
        assertEquals(80000L, attempt.getRefundAmountMinor());
        verify(reservation).expireStripeTestPayment();
        verify(reservation).stripeTestLateRefundPending("pi_123", 80000L);
        verify(inventory).releaseReservation(listingId, reservationId);
        verify(reservation, never()).confirmStripeTestPayment(anyString());
    }
    @Test void expiryReleasesInventoryAndLatePaymentRequiresRefund() {
        subject.expireIfDue(attempt.getId(), Instant.now().plusSeconds(7200));
        assertEquals(StripeCheckoutAttempt.State.EXPIRED, attempt.getState());
        subject.process(paid("evt_late"));
        assertEquals(StripeCheckoutAttempt.State.REFUND_PENDING, attempt.getState());
        verify(reservation).stripeTestLateRefundPending("pi_123", 80000L);
        verify(inventory, times(1)).releaseReservation(listingId, reservationId);
    }
    @Test void lateRefundUpdatesReservationWithoutClearingManualReview() {
        var late = new Reservation();
        late.awaitStripeTestPayment();
        late.expireStripeTestPayment();
        late.requireManualReview();
        late.stripeTestLateRefundPending("pi_123", 80000L);
        assertEquals(ReservationStatus.CANCELLED, late.getStatus());
        assertEquals("TEST_REFUND_PENDING", late.getPaymentStatus());
        assertEquals("pi_123", late.getPaymentReference());
        assertEquals(new java.math.BigDecimal("800.00"), late.getRefundAmount());
        assertEquals(new java.math.BigDecimal("100.00"), late.getRefundPercentage());
        assertTrue(late.isManualReviewRequired());
    }
    @Test void amountMismatchDoesNotFulfillOrRecordEvent() {
        var wrong = new StripeTestGateway.VerifiedEvent("evt_bad", "checkout.session.completed", attempt.getId(),
                "cs_test_123", 1, "cop", "paid", "pi_123");
        assertThrows(RuntimeException.class, () -> subject.process(wrong));
        verify(processed, never()).save(any());
        verifyNoInteractions(reservations, inventory, notifications);
    }
}
