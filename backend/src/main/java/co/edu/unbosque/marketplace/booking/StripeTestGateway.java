package co.edu.unbosque.marketplace.booking;

import java.time.Instant;
import java.util.UUID;

/** External operations must always target the configured sandbox. */
public interface StripeTestGateway {
    CheckoutSession createCheckout(UUID attemptId, long amountMinor, Instant expiresAt);
    CheckoutSession retrieveCheckout(UUID attemptId, String sessionId, long amountMinor);
    VerifiedEvent verifyWebhook(String payload, String signature);
    RefundStatus refund(String paymentIntentId, long amountMinor, UUID attemptId);

    enum RefundStatus { SUCCEEDED, PENDING, FAILED }

    record CheckoutSession(String id, String url) { }
    record VerifiedEvent(String eventId, String type, UUID attemptId, String sessionId,
            long amountMinor, String currency, String paymentStatus, String paymentIntentId) { }
}
