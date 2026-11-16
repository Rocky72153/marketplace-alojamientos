package co.edu.unbosque.marketplace.booking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.listing.ListingService;
import co.edu.unbosque.marketplace.notification.NotificationService;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StripeCheckoutTransactionsTest {
    private StripeCheckoutAttemptRepository attempts;
    private StripeCheckoutTransactions transactions;
    private StripeCheckoutAttempt attempt;
    private final StripeTestGateway.CheckoutSession session = new StripeTestGateway.CheckoutSession(
            "cs_test_123", "https://checkout.stripe.com/c/pay/test");

    @BeforeEach
    void setUp() {
        attempts = mock(StripeCheckoutAttemptRepository.class);
        transactions = new StripeCheckoutTransactions(attempts, mock(ReservationRepository.class),
                mock(ReservationService.class), mock(ListingService.class), mock(ListingRepository.class),
                mock(MarketplaceUserRepository.class), mock(AuditService.class), mock(NotificationService.class));
        attempt = new StripeCheckoutAttempt(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                80_000L, Instant.parse("2026-12-01T12:00:00Z"));
        when(attempts.lockById(attempt.getId())).thenReturn(Optional.of(attempt));
    }

    @Test
    void repeatedBindingIsIdempotentButDifferentSessionIsRejected() {
        transactions.bind(attempt.getId(), session);
        transactions.bind(attempt.getId(), session);
        assertEquals(StripeCheckoutAttempt.State.OPEN, attempt.getState());
        assertEquals("cs_test_123", attempt.getSessionId());
        assertThrows(DomainException.class, () -> transactions.bind(attempt.getId(),
                new StripeTestGateway.CheckoutSession("cs_test_other", session.url())));
        verify(attempts, times(3)).lockById(attempt.getId());
    }

    @Test
    void expiredAttemptCanRecoverItsSessionWithoutReopening() {
        attempt.expired();
        transactions.bind(attempt.getId(), session);
        assertEquals(StripeCheckoutAttempt.State.EXPIRED, attempt.getState());
        assertEquals("cs_test_123", attempt.getSessionId());
        assertEquals(StripeCheckoutEventDecision.Action.REFUND_LATE,
                StripeCheckoutEventDecision.classify(attempt, new StripeTestGateway.VerifiedEvent("evt_123",
                        "checkout.session.completed", attempt.getId(), "cs_test_123", 80_000L, "cop", "paid", "pi_123")));
    }

    @Test
    void verifiedCompletedSessionMayHaveNoUrlAndNeverReopensExpiredInventory() {
        attempt.expired();
        transactions.bindVerified(attempt.getId(), new StripeTestGateway.CheckoutSession("cs_test_123", null));
        assertEquals(StripeCheckoutAttempt.State.EXPIRED, attempt.getState());
        assertEquals("cs_test_123", attempt.getSessionId());
        assertNull(attempt.getCheckoutUrl());
        assertThrows(DomainException.class, () -> transactions.bindVerified(attempt.getId(),
                new StripeTestGateway.CheckoutSession("cs_test_other", null)));
    }
}
