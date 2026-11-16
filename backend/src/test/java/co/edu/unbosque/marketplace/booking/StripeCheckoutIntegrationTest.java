package co.edu.unbosque.marketplace.booking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = { "marketplace.payment.mode=STRIPE_TEST", "marketplace.payment.stripe-maintenance-ms=3600000" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StripeCheckoutIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired StripeCheckoutAttemptRepository attempts;
    @Autowired StripeCheckoutMaintenance maintenance;
    @MockitoBean StripeTestGateway gateway;

    @Test void concurrentGuestsCannotHoldTheSameDates() throws Exception {
        String host = register("HOST"), first = register("GUEST"), second = register("GUEST");
        String listingBody = "{\"title\":\"Casa concurrencia\",\"description\":\"Alojamiento prueba\",\"city\":\"Bogotá\","
                + "\"location\":\"Centro\",\"type\":\"APARTMENT\",\"capacity\":4,\"pricePerNight\":100000,"
                + "\"amenityIds\":[\"WIFI\"],\"rules\":\"No fumar\",\"imageReferences\":[]}";
        String listingJson = mvc.perform(post("/api/listings").header("Authorization", "Bearer " + host)
                .contentType(MediaType.APPLICATION_JSON).content(listingBody)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String listingId = mapper.readTree(listingJson).get("id").asText();
        mvc.perform(patch("/api/listings/" + listingId + "/state").header("Authorization", "Bearer " + host)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}")).andExpect(status().isOk());
        LocalDate from = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(30), to = from.plusDays(2);
        String availability = "{\"periods\":[{\"startDate\":\"" + from + "\",\"endDate\":\"" + to + "\",\"type\":\"AVAILABLE\"}]}";
        mvc.perform(put("/api/listings/" + listingId + "/availability").header("Authorization", "Bearer " + host)
                .contentType(MediaType.APPLICATION_JSON).content(availability)).andExpect(status().isOk());
        when(gateway.createCheckout(any(), anyLong(), any())).thenAnswer(call ->
                new StripeTestGateway.CheckoutSession("cs_test_" + call.getArgument(0), "https://checkout.stripe.com/c/pay/test"));
        String request = "{\"listingId\":\"" + listingId + "\",\"checkIn\":\"" + from + "\",\"checkOut\":\"" + to + "\",\"guests\":2,\"extras\":[]}";
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var responses = List.of(first, second).stream().map(token -> pool.submit(() -> {
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Inicio de concurrencia agotado");
                return mvc.perform(post("/api/stripe-test/checkouts").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(request)).andReturn().getResponse().getStatus();
            })).toList();
            start.countDown();
            var results = responses.stream().map(f -> {
                try { return f.get(30, TimeUnit.SECONDS); }
                catch (Exception ex) { throw new IllegalStateException(ex); }
            }).sorted().toList();
            assertEquals(List.of(200, 409), results);
        }
        verify(gateway, times(1)).createCheckout(any(), anyLong(), any());
    }

    @Test void checkoutIsPendingUntilSignedEventThenRefundIsPendingUntilProviderConfirms() throws Exception {
        String host = register("HOST"), guest = register("GUEST");
        String listingBody = "{\"title\":\"Casa prueba\",\"description\":\"Alojamiento prueba\",\"city\":\"Bogotá\","
                + "\"location\":\"Centro\",\"type\":\"APARTMENT\",\"capacity\":4,\"pricePerNight\":100000,"
                + "\"amenityIds\":[\"WIFI\"],\"rules\":\"No fumar\",\"imageReferences\":[]}";
        String listingJson = mvc.perform(post("/api/listings").header("Authorization", "Bearer " + host)
                .contentType(MediaType.APPLICATION_JSON).content(listingBody)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String listingId = mapper.readTree(listingJson).get("id").asText();
        mvc.perform(patch("/api/listings/" + listingId + "/state").header("Authorization", "Bearer " + host)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}")).andExpect(status().isOk());
        LocalDate from = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(20), to = from.plusDays(2);
        String availability = "{\"periods\":[{\"startDate\":\"" + from + "\",\"endDate\":\"" + to + "\",\"type\":\"AVAILABLE\"}]}";
        mvc.perform(put("/api/listings/" + listingId + "/availability").header("Authorization", "Bearer " + host)
                .contentType(MediaType.APPLICATION_JSON).content(availability)).andExpect(status().isOk());
        when(gateway.createCheckout(any(), anyLong(), any())).thenReturn(
                new StripeTestGateway.CheckoutSession("cs_test_integrated", "https://checkout.stripe.com/c/pay/integration"));
        String request = "{\"listingId\":\"" + listingId + "\",\"checkIn\":\"" + from + "\",\"checkOut\":\"" + to + "\",\"guests\":2,\"extras\":[]}";
        String key = UUID.randomUUID().toString();
        String response = mvc.perform(post("/api/stripe-test/checkouts").header("Authorization", "Bearer " + guest)
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        UUID attemptId = UUID.fromString(mapper.readTree(response).get("attemptId").asText());
        StripeCheckoutAttempt attempt = attempts.findById(attemptId).orElseThrow();
        UUID reservationId = attempt.getReservationId();
        mvc.perform(get("/api/reservations/" + reservationId).header("Authorization", "Bearer " + guest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
        mvc.perform(post("/api/stripe-test/checkouts").header("Authorization", "Bearer " + guest)
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.attemptId").value(attemptId.toString()));
        verify(gateway, times(1)).createCheckout(any(), anyLong(), any());
        var event = new StripeTestGateway.VerifiedEvent("evt_test_1", "checkout.session.completed", attemptId,
                "cs_test_integrated", attempt.getAmountMinor(), "cop", "paid", "pi_test_123");
        when(gateway.verifyWebhook("paid", "valid-signature")).thenReturn(event);
        for (int i = 0; i < 2; i++) mvc.perform(post("/api/stripe-test/webhook").header("Stripe-Signature", "valid-signature")
                .contentType(MediaType.APPLICATION_JSON).content("paid")).andExpect(status().isOk());
        mvc.perform(get("/api/reservations/" + reservationId).header("Authorization", "Bearer " + guest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(post("/api/reservations/" + reservationId + "/cancel").header("Authorization", "Bearer " + guest)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Cambio de planes\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("TEST_REFUND_PENDING"));
        when(gateway.refund(eq("pi_test_123"), anyLong(), eq(attemptId))).thenReturn(StripeTestGateway.RefundStatus.SUCCEEDED);
        maintenance.reconcile();
        assertEquals(StripeCheckoutAttempt.State.REFUNDED, attempts.findById(attemptId).orElseThrow().getState());
        mvc.perform(get("/api/reservations/" + reservationId).header("Authorization", "Bearer " + guest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("TEST_REFUNDED"));
    }

    private String register(String role) throws Exception {
        String body = "{\"name\":\"Usuario Prueba\",\"email\":\"" + UUID.randomUUID() + "@example.test\","
                + "\"password\":\"safe-password-123\",\"role\":\"" + role + "\"}";
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("accessToken").asText();
    }
}
