package co.edu.unbosque.marketplace.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import co.edu.unbosque.marketplace.api.DomainException;
import com.stripe.Stripe;
import com.stripe.StripeClient;
import com.stripe.model.Account;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class StripeSdkTestGatewayTest {
    private static final String TEST_KEY = "rk_test_example_for_unit_test";
    private static final String SIGNING_SECRET = "whsec_example_for_unit_test";
    private static final String EVENT = "{\"id\":\"evt_example\",\"object\":\"event\","
            + "\"type\":\"customer.created\",\"livemode\":false,\"data\":{\"object\":{}}}";

    private StripeSdkTestGateway gateway() {
        return new StripeSdkTestGateway(TEST_KEY, SIGNING_SECRET, "https://example.com", "acct_example", false);
    }

    @Test
    void rejectsLiveKeysAndMissingSigningSecret() {
        assertThrows(IllegalStateException.class, () -> new StripeSdkTestGateway(
                "sk_live_example", SIGNING_SECRET, "https://example.com", "acct_example", false));
        assertThrows(IllegalStateException.class, () -> new StripeSdkTestGateway(
                TEST_KEY, "", "https://example.com", "acct_example", false));
    }

    @Test
    void publicHttpReturnRequiresExplicitTestOnlyDemoConfiguration() {
        String demo = "http://moralesforero.lol";
        assertThrows(IllegalStateException.class, () -> new StripeSdkTestGateway(
                TEST_KEY, SIGNING_SECRET, demo, "acct_example", false));
        new StripeSdkTestGateway(TEST_KEY, SIGNING_SECRET, demo, "acct_example", true);
        new StripeSdkTestGateway(TEST_KEY, SIGNING_SECRET, "http://localhost:4173", "acct_example", false);
        assertThrows(IllegalStateException.class, () -> new StripeSdkTestGateway(
                "sk_live_example", SIGNING_SECRET, demo, "acct_example", true));
        assertThrows(IllegalStateException.class, () -> new StripeSdkTestGateway(
                TEST_KEY, SIGNING_SECRET, "http://example.com/?redirect=example.com", "acct_example", true));
        assertThrows(IllegalStateException.class, () -> new StripeSdkTestGateway(
                TEST_KEY, SIGNING_SECRET, "http://user@example.com", "acct_example", true));
        assertThrows(IllegalStateException.class, () -> new StripeSdkTestGateway(
                TEST_KEY, SIGNING_SECRET, "ftp://example.com", "acct_example", true));
    }

    @Test
    void acceptsOnlyAuthenticatedTestEvents() throws Exception {
        StripeTestGateway.VerifiedEvent event = gateway().verifyWebhook(EVENT, signature(EVENT));
        assertEquals("evt_example", event.eventId());
        assertEquals("customer.created", event.type());
    }

    @Test
    void rejectsTamperedAndLiveEventsEvenWithValidSignature() throws Exception {
        String live = EVENT.replace("false", "true");
        assertEquals("INVALID_WEBHOOK", assertThrows(DomainException.class,
                () -> gateway().verifyWebhook(live, signature(EVENT))).getCode());
        assertEquals("INVALID_WEBHOOK", assertThrows(DomainException.class,
                () -> gateway().verifyWebhook(live, signature(live))).getCode());
    }

    @Test
    void rejectsMissingSignaturePayloadOrType() throws Exception {
        assertThrows(DomainException.class, () -> gateway().verifyWebhook(EVENT, null));
        assertThrows(DomainException.class, () -> gateway().verifyWebhook(null, "anything"));
        String withoutType = EVENT.replace("\"type\":\"customer.created\",", "");
        assertThrows(DomainException.class, () -> gateway().verifyWebhook(withoutType, signature(withoutType)));
    }

    @Test
    void readsSignedCheckoutDataAndRejectsMissingSessionId() throws Exception {
        String id = "5b529eab-08f0-4e8b-94eb-896e4efcd228";
        String session = "{\"id\":\"evt_checkout\",\"object\":\"event\",\"type\":\"checkout.session.completed\","
                + "\"api_version\":\"" + Stripe.API_VERSION + "\",\"livemode\":false,\"data\":{\"object\":{"
                + "\"id\":\"cs_test_123\",\"object\":\"checkout.session\",\"livemode\":false,"
                + "\"amount_total\":12000000,\"currency\":\"cop\",\"payment_status\":\"paid\","
                + "\"payment_intent\":\"pi_test_123\",\"metadata\":{\"attempt_id\":\"" + id + "\"}}}}";
        StripeTestGateway.VerifiedEvent event = gateway().verifyWebhook(session, signature(session));
        assertEquals(id, event.attemptId().toString());
        assertEquals("cs_test_123", event.sessionId());
        assertEquals(12_000_000L, event.amountMinor());
        assertEquals("paid", event.paymentStatus());
        // Signature is verified before the compatibility fallback, including for
        // events created with an API version different from the installed SDK.
        String olderEvent = session.replace(Stripe.API_VERSION, "2020-08-27");
        StripeTestGateway.VerifiedEvent older = gateway().verifyWebhook(olderEvent, signature(olderEvent));
        assertEquals(event, older);
        assertThrows(DomainException.class, () -> gateway().verifyWebhook(olderEvent, signature(session)));
        String unexpectedCheckoutEvent = session.replace("checkout.session.completed", "checkout.session.created")
                .replace("\"id\":\"cs_test_123\",", "");
        assertEquals(null, gateway().verifyWebhook(unexpectedCheckoutEvent,
                signature(unexpectedCheckoutEvent)).attemptId());
        String missingId = session.replace("\"id\":\"cs_test_123\",", "");
        assertThrows(DomainException.class, () -> gateway().verifyWebhook(missingId, signature(missingId)));
    }

    @Test
    void createsOnlyMatchingSandboxCheckoutAndRejectsForeignAccountOrAmount() throws Exception {
        var gateway = gateway();
        StripeClient client = mock(StripeClient.class, RETURNS_DEEP_STUBS);
        ReflectionTestUtils.setField(gateway, "client", client);
        Account account = mock(Account.class);
        Session session = mock(Session.class);
        when(client.v1().accounts().retrieveCurrent()).thenReturn(account);
        when(account.getId()).thenReturn("acct_example");
        when(client.v1().checkout().sessions().create(any(SessionCreateParams.class), any(RequestOptions.class))).thenReturn(session);
        when(session.getLivemode()).thenReturn(false);
        when(session.getId()).thenReturn("cs_test_123");
        when(session.getUrl()).thenReturn("https://checkout.stripe.com/c/pay/example");
        when(session.getAmountTotal()).thenReturn(120000L);
        when(session.getCurrency()).thenReturn("cop");
        UUID attempt = UUID.randomUUID();
        assertEquals("cs_test_123", gateway.createCheckout(attempt, 120000L, Instant.now().plusSeconds(3600)).id());
        when(session.getAmountTotal()).thenReturn(1L);
        assertThrows(DomainException.class, () -> gateway.createCheckout(attempt, 120000L, Instant.now().plusSeconds(3600)));
        when(account.getId()).thenReturn("acct_wrong");
        assertThrows(DomainException.class, () -> gateway.createCheckout(attempt, 120000L, Instant.now().plusSeconds(3600)));
    }

    @Test
    void recoveredCheckoutMustMatchAttemptAndProviderValues() throws Exception {
        var gateway = gateway();
        StripeClient client = mock(StripeClient.class, RETURNS_DEEP_STUBS);
        ReflectionTestUtils.setField(gateway, "client", client);
        Account account = mock(Account.class);
        Session session = mock(Session.class);
        UUID attempt = UUID.randomUUID();
        when(client.v1().accounts().retrieveCurrent()).thenReturn(account);
        when(account.getId()).thenReturn("acct_example");
        when(client.v1().checkout().sessions().retrieve("cs_test_123")).thenReturn(session);
        when(session.getId()).thenReturn("cs_test_123");
        when(session.getLivemode()).thenReturn(false);
        when(session.getClientReferenceId()).thenReturn(attempt.toString());
        when(session.getMetadata()).thenReturn(Map.of("attempt_id", attempt.toString()));
        when(session.getAmountTotal()).thenReturn(120000L);
        when(session.getCurrency()).thenReturn("cop");
        assertEquals("cs_test_123", gateway.retrieveCheckout(attempt, "cs_test_123", 120000L).id());
        when(session.getMetadata()).thenReturn(Map.of("attempt_id", UUID.randomUUID().toString()));
        assertThrows(DomainException.class, () -> gateway.retrieveCheckout(attempt, "cs_test_123", 120000L));
        assertThrows(DomainException.class, () -> gateway.retrieveCheckout(attempt, "cs_live_123", 120000L));
    }

    @Test
    void refundReportsOnlyVerifiedProviderStateAndRejectsWrongAmount() throws Exception {
        var gateway = gateway();
        StripeClient client = mock(StripeClient.class, RETURNS_DEEP_STUBS);
        ReflectionTestUtils.setField(gateway, "client", client);
        Refund refund = mock(Refund.class);
        when(client.v1().refunds().create(any(RefundCreateParams.class), any(RequestOptions.class))).thenReturn(refund);
        when(refund.getAmount()).thenReturn(120000L);
        when(refund.getCurrency()).thenReturn("cop");
        when(refund.getPaymentIntent()).thenReturn("pi_test_123");
        when(refund.getStatus()).thenReturn("succeeded");
        UUID attempt = UUID.randomUUID();
        assertEquals(StripeTestGateway.RefundStatus.SUCCEEDED, gateway.refund("pi_test_123", 120000L, attempt));
        when(refund.getStatus()).thenReturn("failed");
        assertEquals(StripeTestGateway.RefundStatus.FAILED, gateway.refund("pi_test_123", 120000L, attempt));
        when(refund.getStatus()).thenReturn("pending");
        assertEquals(StripeTestGateway.RefundStatus.PENDING, gateway.refund("pi_test_123", 120000L, attempt));
        when(refund.getAmount()).thenReturn(1L);
        assertThrows(DomainException.class, () -> gateway.refund("pi_test_123", 120000L, attempt));
        assertThrows(DomainException.class, () -> gateway.refund("invalid", 120000L, attempt));
    }

    private String signature(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(digest);
    }
}
