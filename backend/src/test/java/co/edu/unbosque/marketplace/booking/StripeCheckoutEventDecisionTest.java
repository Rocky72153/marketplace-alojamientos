package co.edu.unbosque.marketplace.booking;

import static co.edu.unbosque.marketplace.booking.StripeCheckoutEventDecision.Action.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StripeCheckoutEventDecisionTest {
    private StripeCheckoutAttempt attempt;

    @BeforeEach
    void setUp() {
        attempt = new StripeCheckoutAttempt(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                8_000_000L, Instant.parse("2026-12-01T12:00:00Z"));
    }

    private StripeTestGateway.VerifiedEvent event(String type, String status, String intent) {
        return new StripeTestGateway.VerifiedEvent("evt_test_1", type, attempt.getId(), "cs_test_123",
                8_000_000L, "cop", status, intent);
    }

    private StripeCheckoutEventDecision.Action decide(String type, String status, String intent) {
        return StripeCheckoutEventDecision.classify(attempt, event(type, status, intent));
    }

    @Test
    void eventBeforeSessionBindingMustBeRetried() {
        assertEquals(RETRY, decide("checkout.session.completed", "paid", "pi_123"));
    }

    @Test
    void completedUnpaidWaitsAndOnlyVerifiedPaidConfirms() {
        attempt.opened("cs_test_123", "https://checkout.stripe.com/test");
        assertEquals(WAIT_FOR_PAYMENT, decide("checkout.session.completed", "unpaid", null));
        assertEquals(CONFIRM, decide("checkout.session.completed", "paid", "pi_123"));
        assertEquals(CONFIRM, decide("checkout.session.async_payment_succeeded", "paid", "pi_123"));
        assertThrows(IllegalArgumentException.class,
                () -> decide("checkout.session.async_payment_succeeded", "unpaid", "pi_123"));
        assertThrows(IllegalArgumentException.class,
                () -> decide("checkout.session.completed", "paid", null));
    }

    @Test
    void failureOrExpirationReleasesOnlyAnOpenAttempt() {
        attempt.opened("cs_test_123", "https://checkout.stripe.com/test");
        assertEquals(EXPIRE, decide("checkout.session.expired", "unpaid", null));
        assertEquals(EXPIRE, decide("checkout.session.async_payment_failed", "unpaid", null));
        attempt.paid("pi_123");
        assertEquals(ALREADY_HANDLED, decide("checkout.session.expired", "unpaid", null));
    }

    @Test
    void latePaidEventIsRefundedAndDuplicateEventsAreNotFulfilledAgain() {
        attempt.opened("cs_test_123", "https://checkout.stripe.com/test");
        attempt.expired();
        assertEquals(REFUND_LATE, decide("checkout.session.async_payment_succeeded", "paid", "pi_123"));
        attempt.requireRefund("pi_123", 8_000_000L);
        assertEquals(ALREADY_HANDLED, decide("checkout.session.async_payment_succeeded", "paid", "pi_123"));
        assertThrows(IllegalArgumentException.class,
                () -> decide("checkout.session.async_payment_succeeded", "paid", "pi_other"));
        attempt.refunded();
        assertEquals(ALREADY_HANDLED, decide("checkout.session.completed", "paid", "pi_123"));
    }

    @Test
    void rejectsWrongAttemptSessionAmountAndCurrency() {
        attempt.opened("cs_test_123", "https://checkout.stripe.com/test");
        assertThrows(IllegalArgumentException.class, () -> StripeCheckoutEventDecision.classify(attempt,
                new StripeTestGateway.VerifiedEvent("evt_1", "checkout.session.completed", UUID.randomUUID(),
                        "cs_test_123", 8_000_000L, "cop", "paid", "pi_123")));
        assertThrows(IllegalArgumentException.class, () -> StripeCheckoutEventDecision.classify(attempt,
                new StripeTestGateway.VerifiedEvent("evt_1", "checkout.session.completed", attempt.getId(),
                        "cs_test_other", 8_000_000L, "cop", "paid", "pi_123")));
        assertThrows(IllegalArgumentException.class, () -> StripeCheckoutEventDecision.classify(attempt,
                new StripeTestGateway.VerifiedEvent("evt_1", "checkout.session.completed", attempt.getId(),
                        "cs_test_123", 1L, "cop", "paid", "pi_123")));
        assertThrows(IllegalArgumentException.class, () -> StripeCheckoutEventDecision.classify(attempt,
                new StripeTestGateway.VerifiedEvent("evt_1", "checkout.session.completed", attempt.getId(),
                        "cs_test_123", 8_000_000L, "usd", "paid", "pi_123")));
    }

    @Test
    void unknownEventIsIgnored() {
        assertEquals(IGNORE, decide("customer.created", null, null));
    }
}
