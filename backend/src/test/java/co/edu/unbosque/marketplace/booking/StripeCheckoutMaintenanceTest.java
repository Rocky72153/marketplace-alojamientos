package co.edu.unbosque.marketplace.booking;

import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StripeCheckoutMaintenanceTest {
    @Test
    void unknownRefundDeadlineRequiresReviewInsteadOfIssuingAnotherRefund() {
        var attempts = mock(StripeCheckoutAttemptRepository.class);
        var checkout = mock(StripeCheckoutTransactions.class);
        var events = mock(StripeEventTransactions.class);
        var gateway = mock(StripeTestGateway.class);
        var uncertain = mock(StripeCheckoutAttempt.class);
        UUID id = UUID.randomUUID();
        when(uncertain.getId()).thenReturn(id);
        when(uncertain.getRefundRetryDeadline()).thenReturn(null);
        when(attempts.findTop10ByStateOrderByIdAsc(StripeCheckoutAttempt.State.REFUND_PENDING))
                .thenReturn(List.of(uncertain));
        new StripeCheckoutMaintenance(attempts, checkout, events, gateway).reconcile();
        verify(events).markRefundUncertain(id);
        verify(gateway, never()).refund(any(), anyLong(), any());
    }

    @Test
    void expiredUnboundAttemptIsNotRecreatedWithProvider() {
        var attempts = mock(StripeCheckoutAttemptRepository.class);
        var checkout = mock(StripeCheckoutTransactions.class);
        var events = mock(StripeEventTransactions.class);
        var gateway = mock(StripeTestGateway.class);
        var expired = mock(StripeCheckoutAttempt.class);
        UUID id = UUID.randomUUID();
        when(expired.getId()).thenReturn(id);
        when(expired.getExpiresAt()).thenReturn(Instant.now().minusSeconds(30));
        when(attempts.findTop10ByStateAndSessionIdIsNullOrderByIdAsc(StripeCheckoutAttempt.State.PREPARING))
                .thenReturn(List.of(expired));
        new StripeCheckoutMaintenance(attempts, checkout, events, gateway).reconcile();
        verify(events).expireIfDue(eq(id), any());
        verify(gateway, never()).createCheckout(any(), anyLong(), any());
    }
}
