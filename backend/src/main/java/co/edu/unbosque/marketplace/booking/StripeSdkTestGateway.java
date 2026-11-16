package co.edu.unbosque.marketplace.booking;

import co.edu.unbosque.marketplace.api.DomainException;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "marketplace.payment.mode", havingValue = "STRIPE_TEST")
public class StripeSdkTestGateway implements StripeTestGateway {
    private final StripeClient client;
    private final String webhookSecret;
    private final String frontendBase;
    private final String expectedAccount;

    @Autowired
    public StripeSdkTestGateway(@Value("${marketplace.payment.stripe-test-key:}") String key,
            @Value("${marketplace.payment.stripe-webhook-secret:}") String webhookSecret,
            @Value("${marketplace.payment.stripe-frontend-url:}") String frontendBase,
            @Value("${marketplace.payment.stripe-account-id:}") String expectedAccount,
            @Value("${marketplace.payment.stripe-allow-http-demo:false}") boolean allowHttpDemo) {
        if (!(key.startsWith("rk_test_") || key.startsWith("sk_test_")) || key.length() <= 8 || key.chars().anyMatch(Character::isWhitespace))
            throw new IllegalStateException("Se requiere una clave restringida o secreta de Stripe de prueba.");
        if (!webhookSecret.startsWith("whsec_") || webhookSecret.length() <= 6 || webhookSecret.chars().anyMatch(Character::isWhitespace))
            throw new IllegalStateException("Se requiere la firma de webhook de Stripe de prueba.");
        URI base = URI.create(frontendBase);
        // Public HTTP return URLs are an explicit, test-key-only classroom demo
        // exception. This does not alter Stripe's separate webhook requirements.
        if (base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null
                || !("https".equals(base.getScheme()) || ("http".equals(base.getScheme())
                        && (allowHttpDemo || "localhost".equals(base.getHost()) || "127.0.0.1".equals(base.getHost())))))
            throw new IllegalStateException("La URL de retorno de Stripe no es válida.");
        if (!expectedAccount.startsWith("acct_") || expectedAccount.length() <= 5)
            throw new IllegalStateException("Se requiere el identificador de la cuenta sandbox autorizada.");
        this.client = StripeClient.builder().setApiKey(key).setConnectTimeout(10_000)
                .setReadTimeout(30_000).setMaxNetworkRetries(2).build();
        this.webhookSecret = webhookSecret;
        this.frontendBase = frontendBase.endsWith("/") ? frontendBase : frontendBase + "/";
        this.expectedAccount = expectedAccount;
    }

    @Override
    public CheckoutSession createCheckout(UUID attemptId, long amountMinor, Instant expiresAt) {
        try {
            // Do not allow a key from a different Stripe account to be used silently.
            // Retrieving a named account only proves it is visible to the key, not that the
            // key itself belongs to that account (e.g. a connected account may be visible).
            if (!expectedAccount.equals(client.v1().accounts().retrieveCurrent().getId())) throw unavailable();
            String suffix = letters(attemptId);
            var item = SessionCreateParams.LineItem.builder().setQuantity(1L).setPriceData(
                    SessionCreateParams.LineItem.PriceData.builder().setCurrency("cop").setUnitAmount(amountMinor)
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Reserva de alojamiento · pago de prueba").build()).build()).build();
            var params = SessionCreateParams.builder().setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendBase + "?stripe_checkout=" + attemptId)
                    .setCancelUrl(frontendBase + "?stripe_checkout=" + attemptId + "&stripe_canceled=1")
                    .setExpiresAt(expiresAt.getEpochSecond()).setClientReferenceId(attemptId.toString())
                    .setIntegrationIdentifier("alojamientos_" + suffix).putMetadata("attempt_id", attemptId.toString())
                    .addLineItem(item).build();
            RequestOptions options = RequestOptions.builder().setIdempotencyKey("checkout-" + attemptId).build();
            Session session = client.v1().checkout().sessions().create(params, options);
            if (!Boolean.FALSE.equals(session.getLivemode()) || session.getId() == null || session.getUrl() == null
                    || !"checkout.stripe.com".equals(URI.create(session.getUrl()).getHost())
                    || !"https".equals(URI.create(session.getUrl()).getScheme())
                    || !Long.valueOf(amountMinor).equals(session.getAmountTotal()) || !"cop".equals(session.getCurrency()))
                throw unavailable();
            return new CheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException | IllegalArgumentException failure) { throw unavailable(); }
    }

    @Override
    public CheckoutSession retrieveCheckout(UUID attemptId, String sessionId, long amountMinor) {
        try {
            if (sessionId == null || !sessionId.startsWith("cs_test_") || sessionId.length() > 100
                    || !expectedAccount.equals(client.v1().accounts().retrieveCurrent().getId())) throw unavailable();
            Session session = client.v1().checkout().sessions().retrieve(sessionId);
            if (!sessionId.equals(session.getId()) || !Boolean.FALSE.equals(session.getLivemode())
                    || !attemptId.toString().equals(session.getClientReferenceId())
                    || session.getMetadata() == null || !attemptId.toString().equals(session.getMetadata().get("attempt_id"))
                    || !Long.valueOf(amountMinor).equals(session.getAmountTotal()) || !"cop".equals(session.getCurrency()))
                throw unavailable();
            // A completed Checkout Session can legitimately have no remaining URL.
            return new CheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException | IllegalArgumentException failure) { throw unavailable(); }
    }

    @Override
    public VerifiedEvent verifyWebhook(String payload, String signature) {
        try {
            if (payload == null || payload.isBlank() || signature == null || signature.isBlank()) throw badWebhook();
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            if (!Boolean.FALSE.equals(event.getLivemode()) || event.getId() == null || event.getType() == null) throw badWebhook();
            if (!isHandledCheckoutEvent(event.getType()))
                return new VerifiedEvent(event.getId(), event.getType(), null, null, 0L, null, null, null);
            var deserializer = event.getDataObjectDeserializer();
            Object data = deserializer.getObject().orElse(null);
            if (data == null) {
                // A valid signature authenticates the original payload. Stripe CLI's
                // event version can differ from the SDK's model version: preserve
                // delivery and independently validate every required field below and
                // against the locked local attempt before changing any reservation.
                try { data = deserializer.deserializeUnsafe(); }
                catch (EventDataObjectDeserializationException incompatible) { throw unavailable(); }
            }
            if (!(data instanceof Session session) || !Boolean.FALSE.equals(session.getLivemode()) || session.getId() == null
                    || session.getMetadata() == null || session.getMetadata().get("attempt_id") == null
                    || session.getAmountTotal() == null) throw badWebhook();
            UUID attemptId = UUID.fromString(session.getMetadata().get("attempt_id"));
            return new VerifiedEvent(event.getId(), event.getType(), attemptId, session.getId(),
                    session.getAmountTotal(), session.getCurrency(), session.getPaymentStatus(), session.getPaymentIntent());
        } catch (SignatureVerificationException | IllegalArgumentException failure) { throw badWebhook(); }
    }
    private static boolean isHandledCheckoutEvent(String type) {
        return "checkout.session.completed".equals(type)
                || "checkout.session.async_payment_succeeded".equals(type)
                || "checkout.session.async_payment_failed".equals(type)
                || "checkout.session.expired".equals(type);
    }

    @Override
    public RefundStatus refund(String paymentIntentId, long amountMinor, UUID attemptId) {
        try {
            if (paymentIntentId == null || !paymentIntentId.startsWith("pi_")) throw unavailable();
            var params = RefundCreateParams.builder().setPaymentIntent(paymentIntentId).setAmount(amountMinor).build();
            Refund result = client.v1().refunds().create(params,
                    RequestOptions.builder().setIdempotencyKey("refund-" + attemptId).build());
            // Idempotent create can keep returning its original pending response. Read
            // the current refund state before deciding whether reconciliation is done.
            if (!"succeeded".equals(result.getStatus()) && result.getId() != null)
                result = client.v1().refunds().retrieve(result.getId());
            if (!Long.valueOf(amountMinor).equals(result.getAmount()) || !"cop".equals(result.getCurrency())
                    || !paymentIntentId.equals(result.getPaymentIntent())) throw unavailable();
            return switch (result.getStatus()) {
                case "succeeded" -> RefundStatus.SUCCEEDED;
                case "failed", "canceled" -> RefundStatus.FAILED;
                default -> RefundStatus.PENDING;
            };
        } catch (StripeException failure) { throw unavailable(); }
    }
    private static String letters(UUID id) {
        long bits = id.getLeastSignificantBits(); StringBuilder value = new StringBuilder();
        for (int i = 0; i < 8; i++) { value.append((char) ('a' + (int) (bits & 0xff) % 26)); bits >>>= 8; }
        return value.toString().toLowerCase(Locale.ROOT);
    }
    private DomainException unavailable() { return new DomainException(HttpStatus.SERVICE_UNAVAILABLE, "STRIPE_TEST_UNAVAILABLE", "No fue posible procesar el pago de prueba; intenta nuevamente."); }
    private DomainException badWebhook() { return new DomainException(HttpStatus.BAD_REQUEST, "INVALID_WEBHOOK", "Evento de pago de prueba inválido."); }
}
