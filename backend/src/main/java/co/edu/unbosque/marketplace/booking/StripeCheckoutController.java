package co.edu.unbosque.marketplace.booking;

import co.edu.unbosque.marketplace.security.MarketplaceUser;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Sandbox only. A browser redirect is never treated as proof of payment. */
@RestController
@RequestMapping("/api/stripe-test")
@ConditionalOnProperty(name = "marketplace.payment.mode", havingValue = "STRIPE_TEST")
public class StripeCheckoutController {
    private final StripeCheckoutTransactions transactions;
    private final StripeTestGateway gateway;
    private final StripeEventTransactions events;

    public StripeCheckoutController(StripeCheckoutTransactions transactions, StripeTestGateway gateway,
            StripeEventTransactions events) {
        this.transactions = transactions; this.gateway = gateway; this.events = events;
    }

    @PostMapping("/checkouts")
    public CheckoutView checkout(@AuthenticationPrincipal MarketplaceUser guest,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReservationService.ReservationRequest request) {
        StripeCheckoutAttempt attempt = transactions.prepare(guest, request, idempotencyKey);
        if (attempt.getState() == StripeCheckoutAttempt.State.PREPARING) {
            var session = gateway.createCheckout(attempt.getId(), attempt.getAmountMinor(), attempt.getExpiresAt());
            attempt = transactions.bind(attempt.getId(), session);
        }
        return CheckoutView.from(attempt);
    }

    @GetMapping("/checkouts/{id}")
    public CheckoutView get(@AuthenticationPrincipal MarketplaceUser guest, @PathVariable UUID id) {
        return CheckoutView.from(transactions.get(id, guest));
    }

    @PostMapping("/webhook")
    public void webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
        var event = gateway.verifyWebhook(payload, signature);
        if (event.attemptId() != null && event.sessionId() != null) {
            var attempt = transactions.getForVerifiedEvent(event.attemptId());
            if (attempt.getSessionId() == null) {
                // Session creation may have succeeded while the response was lost.
                // Recover by the signed session ID; never recreate an expired session.
                var session = gateway.retrieveCheckout(attempt.getId(), event.sessionId(), attempt.getAmountMinor());
                transactions.bindVerified(attempt.getId(), session);
            }
        }
        events.process(event);
    }

    public record CheckoutView(UUID attemptId, String status, String checkoutUrl, Instant expiresAt) {
        static CheckoutView from(StripeCheckoutAttempt attempt) {
            return new CheckoutView(attempt.getId(), attempt.getState().name(),
                    attempt.getState() == StripeCheckoutAttempt.State.OPEN ? attempt.getCheckoutUrl() : null,
                    attempt.getExpiresAt());
        }
    }
}
