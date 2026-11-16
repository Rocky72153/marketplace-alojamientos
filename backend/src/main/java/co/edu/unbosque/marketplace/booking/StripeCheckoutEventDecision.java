package co.edu.unbosque.marketplace.booking;

import java.util.Objects;

/**
 * Classifies a previously signature-verified event against a locked checkout attempt.
 * This is not a webhook handler: callers must persist decisions atomically and retry
 * transient failures. No decision here performs a payment, refund, or reservation change.
 */
final class StripeCheckoutEventDecision {
    enum Action { IGNORE, RETRY, WAIT_FOR_PAYMENT, CONFIRM, EXPIRE, REFUND_LATE, ALREADY_HANDLED }

    private StripeCheckoutEventDecision() { }

    static Action classify(StripeCheckoutAttempt attempt, StripeTestGateway.VerifiedEvent event) {
        Objects.requireNonNull(attempt, "attempt");
        Objects.requireNonNull(event, "event");
        String type = event.type();
        if (!"checkout.session.completed".equals(type)
                && !"checkout.session.async_payment_succeeded".equals(type)
                && !"checkout.session.async_payment_failed".equals(type)
                && !"checkout.session.expired".equals(type)) return Action.IGNORE;

        if (event.eventId() == null || event.eventId().isBlank()
                || !attempt.getId().equals(event.attemptId())
                || event.sessionId() == null || !event.sessionId().startsWith("cs_test_")
                || attempt.getAmountMinor() != event.amountMinor()
                || !attempt.getCurrency().equals(event.currency())) throw invalid();

        // Stripe may deliver an event before the request that created the session
        // records its ID. Retry instead of trusting an unbound session or losing it.
        if (attempt.getSessionId() == null) return Action.RETRY;
        if (!attempt.getSessionId().equals(event.sessionId())) throw invalid();

        boolean succeeded = "checkout.session.completed".equals(type)
                || "checkout.session.async_payment_succeeded".equals(type);
        if (succeeded && "unpaid".equals(event.paymentStatus())
                && "checkout.session.completed".equals(type)) {
            return attempt.getState() == StripeCheckoutAttempt.State.OPEN
                    ? Action.WAIT_FOR_PAYMENT : Action.ALREADY_HANDLED;
        }
        if (succeeded) {
            if (!"paid".equals(event.paymentStatus()) || event.paymentIntentId() == null
                    || !event.paymentIntentId().startsWith("pi_")) throw invalid();
            return switch (attempt.getState()) {
                case OPEN -> Action.CONFIRM;
                case EXPIRED -> Action.REFUND_LATE;
                case PAID, REFUND_PENDING, REFUND_FAILED, REFUND_REVIEW, REFUNDED, CANCELLED -> {
                    if (!event.paymentIntentId().equals(attempt.getPaymentIntentId())) throw invalid();
                    yield Action.ALREADY_HANDLED;
                }
                case PREPARING -> Action.RETRY;
            };
        }
        if (!"unpaid".equals(event.paymentStatus())) throw invalid();
        return switch (attempt.getState()) {
            case OPEN -> Action.EXPIRE;
            case PREPARING -> Action.RETRY;
            case PAID, EXPIRED, REFUND_PENDING, REFUND_FAILED, REFUND_REVIEW, REFUNDED, CANCELLED -> Action.ALREADY_HANDLED;
        };
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("El evento no coincide con el intento de pago de prueba.");
    }
}
