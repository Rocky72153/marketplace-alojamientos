package co.edu.unbosque.marketplace.booking;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StripeCheckoutAttemptTest {
    private static final String URL = "https://checkout.stripe.com/c/pay/test";
    private StripeCheckoutAttempt attempt;

    @BeforeEach
    void setUp() {
        attempt = new StripeCheckoutAttempt(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                80_000, Instant.parse("2026-12-01T12:00:00Z"));
    }

    @Test
    void onlyAcceptsTestSessionOnStripeHostedUrl() {
        assertThrows(IllegalStateException.class, () -> attempt.opened("cs_live_123", URL));
        assertThrows(IllegalStateException.class, () -> attempt.opened("cs_test_123", "http://checkout.stripe.com/"));
        assertThrows(IllegalStateException.class, () -> attempt.opened("cs_test_123", "https://checkout.stripe.com.evil.example/"));
        assertThrows(IllegalStateException.class, () -> attempt.opened("cs_test_123", "https://user@checkout.stripe.com/"));
        assertThrows(IllegalStateException.class, () -> attempt.opened("cs_test_123", "https://checkout.stripe.com:8443/"));
        assertEquals(StripeCheckoutAttempt.State.PREPARING, attempt.getState());
        attempt.opened("cs_test_123", URL);
        assertEquals(StripeCheckoutAttempt.State.OPEN, attempt.getState());
    }

    @Test
    void paidAndRefundedRequireProperPriorState() {
        assertThrows(IllegalStateException.class, () -> attempt.paid("pi_123"));
        assertThrows(IllegalStateException.class, attempt::refunded);
        attempt.opened("cs_test_123", URL);
        assertThrows(IllegalStateException.class, () -> attempt.paid("invalid"));
        attempt.paid("pi_123");
        assertThrows(IllegalStateException.class, attempt::expired);
        assertThrows(IllegalStateException.class, () -> attempt.requireRefund("pi_other", 100));
        assertThrows(IllegalStateException.class, () -> attempt.requireRefund("pi_123", 0));
        assertThrows(IllegalStateException.class, () -> attempt.requireRefund("pi_123", 80_001));
        attempt.requireRefund("pi_123", 40_000);
        assertThrows(IllegalStateException.class, () -> attempt.requireRefund("pi_123", 40_000));
        attempt.refunded();
        assertThrows(IllegalStateException.class, attempt::refunded);
        assertEquals(StripeCheckoutAttempt.State.REFUNDED, attempt.getState());
    }

    @Test
    void recoversBoundSessionAfterExpirationWithoutReopeningInventory() {
        attempt.expired();
        assertThrows(IllegalStateException.class, () -> attempt.opened("cs_test_123", URL));
        attempt.bindExpiredSession("cs_test_123", URL);
        assertEquals(StripeCheckoutAttempt.State.EXPIRED, attempt.getState());
        assertEquals("cs_test_123", attempt.getSessionId());
        assertThrows(IllegalStateException.class, () -> attempt.bindExpiredSession("cs_test_other", URL));
        assertEquals(StripeCheckoutEventDecision.Action.REFUND_LATE,
                StripeCheckoutEventDecision.classify(attempt, new StripeTestGateway.VerifiedEvent("evt_123",
                        "checkout.session.completed", attempt.getId(), "cs_test_123", 80_000, "cop", "paid", "pi_123")));
        attempt.requireRefund("pi_123", 80_000);
        assertEquals(StripeCheckoutAttempt.State.REFUND_PENDING, attempt.getState());
    }

    @Test
    void rejectsInvalidStartingAmountsAndMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new StripeCheckoutAttempt(UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), 0, Instant.now()));
        assertThrows(NullPointerException.class, () -> new StripeCheckoutAttempt(null,
                UUID.randomUUID(), UUID.randomUUID(), 50, Instant.now()));
    }
}
