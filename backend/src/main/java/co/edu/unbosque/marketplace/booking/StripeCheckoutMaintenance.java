package co.edu.unbosque.marketplace.booking;

import java.time.Instant;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Bounded, retryable work. Stripe calls deliberately occur outside database locks. */
@Component
@ConditionalOnProperty(name = "marketplace.payment.mode", havingValue = "STRIPE_TEST")
public class StripeCheckoutMaintenance {
    private static final Logger LOG = LoggerFactory.getLogger(StripeCheckoutMaintenance.class);
    private final StripeCheckoutAttemptRepository attempts;
    private final StripeCheckoutTransactions checkout;
    private final StripeEventTransactions events;
    private final StripeTestGateway gateway;
    private UUID expireCursor;
    private UUID refundCursor;
    private final Map<StripeCheckoutAttempt.State, UUID> recoveryCursors = new EnumMap<>(StripeCheckoutAttempt.State.class);

    public StripeCheckoutMaintenance(StripeCheckoutAttemptRepository attempts, StripeCheckoutTransactions checkout,
            StripeEventTransactions events, StripeTestGateway gateway) {
        this.attempts = attempts; this.checkout = checkout; this.events = events; this.gateway = gateway;
    }

    @Scheduled(fixedDelayString = "${marketplace.payment.stripe-maintenance-ms:60000}")
    public void reconcile() {
        var states = List.of(StripeCheckoutAttempt.State.PREPARING, StripeCheckoutAttempt.State.OPEN);
        Instant now = Instant.now();
        var due = expireCursor == null ? attempts.findTop10ByStateInAndExpiresAtLessThanEqualOrderByIdAsc(states, now)
                : attempts.findTop10ByStateInAndExpiresAtLessThanEqualAndIdGreaterThanOrderByIdAsc(states, now, expireCursor);
        expireCursor = nextCursor(due);
        for (var attempt : due) {
            try { events.expireIfDue(attempt.getId(), Instant.now()); }
            catch (RuntimeException failure) { LOG.warn("No se pudo vencer un intento Stripe de prueba; se reintentará."); }
        }
        var refundState = StripeCheckoutAttempt.State.REFUND_PENDING;
        var refunds = refundCursor == null ? attempts.findTop10ByStateOrderByIdAsc(refundState)
                : attempts.findTop10ByStateAndIdGreaterThanOrderByIdAsc(refundState, refundCursor);
        refundCursor = nextCursor(refunds);
        for (var attempt : refunds) {
            try {
                // Never issue a potentially duplicate refund after Stripe can
                // discard its idempotency key. Null is treated conservatively.
                if (attempt.getRefundRetryDeadline() == null || !Instant.now().isBefore(attempt.getRefundRetryDeadline())) {
                    events.markRefundUncertain(attempt.getId());
                    continue;
                }
                var result = gateway.refund(attempt.getPaymentIntentId(), attempt.getRefundAmountMinor(), attempt.getId());
                if (result == StripeTestGateway.RefundStatus.SUCCEEDED) events.finishRefund(attempt.getId());
                else if (result == StripeTestGateway.RefundStatus.FAILED) events.failRefund(attempt.getId());
            } catch (RuntimeException failure) { LOG.warn("No se pudo reembolsar un intento Stripe de prueba; se reintentará."); }
        }
        recover(StripeCheckoutAttempt.State.PREPARING);
    }

    private void recover(StripeCheckoutAttempt.State state) {
        UUID cursor = recoveryCursors.get(state);
        var records = cursor == null ? attempts.findTop10ByStateAndSessionIdIsNullOrderByIdAsc(state)
                : attempts.findTop10ByStateAndSessionIdIsNullAndIdGreaterThanOrderByIdAsc(state, cursor);
        recoveryCursors.put(state, nextCursor(records));
        for (var attempt : records) {
            try {
                if (!Instant.now().isBefore(attempt.getExpiresAt())) {
                    events.expireIfDue(attempt.getId(), Instant.now());
                    continue;
                }
                var session = gateway.createCheckout(attempt.getId(), attempt.getAmountMinor(), attempt.getExpiresAt());
                checkout.bind(attempt.getId(), session);
            } catch (RuntimeException failure) { LOG.warn("No se pudo conciliar una sesión Stripe de prueba; se reintentará."); }
        }
    }

    private UUID nextCursor(List<StripeCheckoutAttempt> records) {
        return records.size() == 10 ? records.get(records.size() - 1).getId() : null;
    }
}
