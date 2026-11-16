package co.edu.unbosque.marketplace.booking;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StripeCheckoutControllerTest {
    @Test
    void recoversMissingSessionFromProviderBeforeProcessingSignedEvent() {
        var transactions = mock(StripeCheckoutTransactions.class);
        var gateway = mock(StripeTestGateway.class);
        var events = mock(StripeEventTransactions.class);
        var controller = new StripeCheckoutController(transactions, gateway, events);
        var attempt = new StripeCheckoutAttempt(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                80_000L, Instant.now().plusSeconds(3600));
        var event = new StripeTestGateway.VerifiedEvent("evt_test", "checkout.session.completed",
                attempt.getId(), "cs_test_lost", 80_000L, "cop", "paid", "pi_test_123");
        var recovered = new StripeTestGateway.CheckoutSession("cs_test_lost", null);
        when(gateway.verifyWebhook("body", "signature")).thenReturn(event);
        when(transactions.getForVerifiedEvent(attempt.getId())).thenReturn(attempt);
        when(gateway.retrieveCheckout(attempt.getId(), "cs_test_lost", 80_000L)).thenReturn(recovered);
        controller.webhook("body", "signature");
        var order = inOrder(gateway, transactions, events);
        order.verify(gateway).verifyWebhook("body", "signature");
        order.verify(transactions).getForVerifiedEvent(attempt.getId());
        order.verify(gateway).retrieveCheckout(attempt.getId(), "cs_test_lost", 80_000L);
        order.verify(transactions).bindVerified(attempt.getId(), recovered);
        order.verify(events).process(event);
    }

    @Test
    void unverifiableProviderSessionIsNotBoundOrProcessed() {
        var transactions = mock(StripeCheckoutTransactions.class);
        var gateway = mock(StripeTestGateway.class);
        var events = mock(StripeEventTransactions.class);
        var attempt = new StripeCheckoutAttempt(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                80_000L, Instant.now().plusSeconds(3600));
        var event = new StripeTestGateway.VerifiedEvent("evt_test", "checkout.session.completed",
                attempt.getId(), "cs_test_lost", 80_000L, "cop", "paid", "pi_test_123");
        when(gateway.verifyWebhook("body", "signature")).thenReturn(event);
        when(transactions.getForVerifiedEvent(attempt.getId())).thenReturn(attempt);
        when(gateway.retrieveCheckout(attempt.getId(), "cs_test_lost", 80_000L))
                .thenThrow(new IllegalStateException("provider unavailable"));
        assertThrows(IllegalStateException.class,
                () -> new StripeCheckoutController(transactions, gateway, events).webhook("body", "signature"));
        verify(transactions, never()).bindVerified(any(), any());
        verifyNoInteractions(events);
    }
}
