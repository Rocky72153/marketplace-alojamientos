package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReservationAcceptanceTest {
    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @Test
    void quoteAndReservationAreRecalculatedIdempotentAndPrivate() throws Exception {
        String hostToken = register("Host", "HOST").get("accessToken").asText();
        JsonNode listing = createActiveListing(hostToken);
        String listingId = listing.get("id").asText();
        String extraId = listing.get("extraOptions").get(0).get("id").asText();
        DateRange range = range(14);
        setAvailability(hostToken, listingId, range, true);
        String guestToken = register("Guest", "GUEST").get("accessToken").asText();

        String quoteRequest = mapper.writeValueAsString(Map.of("checkIn", range.start().toString(),
                "checkOut", range.end().toString(), "guests", 2,
                "extras", List.of(Map.of("extraId", extraId, "quantity", 2))));
        mockMvc.perform(post("/api/listings/" + listingId + "/quotes").contentType(MediaType.APPLICATION_JSON)
                        .content(quoteRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nights").value(2))
                .andExpect(jsonPath("$.nightlySubtotal").value(200000))
                .andExpect(jsonPath("$.cleaningFee").value(35000))
                .andExpect(jsonPath("$.serviceFee").value(20000))
                .andExpect(jsonPath("$.extrasTotal").value(40000))
                .andExpect(jsonPath("$.discountAmount").value(0))
                .andExpect(jsonPath("$.total").value(295000))
                .andExpect(jsonPath("$.freeRefundDays").value(7))
                .andExpect(jsonPath("$.partialRefundPercentage").value(50));

        String bookingRequest = mapper.writeValueAsString(Map.of("listingId", listingId,
                "checkIn", range.start().toString(), "checkOut", range.end().toString(), "guests", 2,
                "extras", List.of(Map.of("extraId", extraId, "quantity", 2))));
        String key = UUID.randomUUID().toString();
        MvcResult first = mockMvc.perform(post("/api/reservations").header("Authorization", "Bearer " + guestToken)
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(bookingRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("TEST_APPROVED")).andReturn();
        String reservationId = mapper.readTree(first.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/reservations").header("Authorization", "Bearer " + guestToken)
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(bookingRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(reservationId));
        String reusedKeyRequest = mapper.writeValueAsString(Map.of("listingId", listingId,
                "checkIn", range.start().plusDays(1).toString(), "checkOut", range.end().plusDays(1).toString(),
                "guests", 2, "extras", List.of(Map.of("extraId", extraId, "quantity", 2))));
        mockMvc.perform(post("/api/reservations").header("Authorization", "Bearer " + guestToken)
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(reusedKeyRequest))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/reservations").header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/reservations/" + reservationId).header("Authorization", "Bearer " + register("Other", "GUEST").get("accessToken").asText()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/listings/" + listingId + "/availability").param("from", range.start().toString())
                        .param("to", range.end().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("RESERVED"));
        mockMvc.perform(post("/api/reservations").header("Authorization", "Bearer " + guestToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                        .content(bookingRequest))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/listings/" + listingId + "/quotes").contentType(MediaType.APPLICATION_JSON)
                        .content(quoteRequest))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("UNAVAILABLE"));
    }

    @Test
    void cancellationAppliesFullRefundAndReleasesTheReservedDates() throws Exception {
        String hostToken = register("Host", "HOST").get("accessToken").asText();
        JsonNode listing = createActiveListing(hostToken);
        String listingId = listing.get("id").asText();
        DateRange range = range(12);
        setAvailability(hostToken, listingId, range, false);
        String guestToken = register("Guest", "GUEST").get("accessToken").asText();
        String reservationId = reserve(guestToken, listingId, range, List.of()).get("id").asText();

        mockMvc.perform(post("/api/reservations/" + reservationId + "/cancel")
                        .header("Authorization", "Bearer " + guestToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Cambio de planes\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundPercentage").value(100))
                .andExpect(jsonPath("$.refundAmount").value(255000))
                .andExpect(jsonPath("$.paymentStatus").value("TEST_REFUNDED"));
        mockMvc.perform(get("/api/listings/" + listingId + "/availability").param("from", range.start().toString())
                        .param("to", range.end().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("AVAILABLE"));
        mockMvc.perform(post("/api/reservations/" + reservationId + "/cancel")
                        .header("Authorization", "Bearer " + guestToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Segundo intento\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cancellationUsesPartialAndZeroRefundThresholds() throws Exception {
        String hostToken = register("Host", "HOST").get("accessToken").asText();
        JsonNode listing = createActiveListing(hostToken);
        String listingId = listing.get("id").asText();
        String guestToken = register("Guest", "GUEST").get("accessToken").asText();

        DateRange partialRange = range(4);
        setAvailability(hostToken, listingId, partialRange, false);
        String partialId = reserve(guestToken, listingId, partialRange, List.of()).get("id").asText();
        mockMvc.perform(post("/api/reservations/" + partialId + "/cancel")
                        .header("Authorization", "Bearer " + guestToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Prueba reembolso parcial\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.refundPercentage").value(50))
                .andExpect(jsonPath("$.refundAmount").value(127500));

        DateRange noRefundRange = range(2);
        setAvailability(hostToken, listingId, noRefundRange, false);
        String noRefundId = reserve(guestToken, listingId, noRefundRange, List.of()).get("id").asText();
        mockMvc.perform(post("/api/reservations/" + noRefundId + "/cancel")
                        .header("Authorization", "Bearer " + guestToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Prueba sin reembolso\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.refundPercentage").value(0))
                .andExpect(jsonPath("$.refundAmount").value(0)).andExpect(jsonPath("$.paymentStatus").value("TEST_NO_REFUND"));
    }

    @Test
    void concurrentBookingsCannotReserveTheSameDatesTwice() throws Exception {
        String hostToken = register("Host", "HOST").get("accessToken").asText();
        JsonNode listing = createActiveListing(hostToken);
        String listingId = listing.get("id").asText();
        DateRange range = range(18);
        setAvailability(hostToken, listingId, range, false);
        String firstGuest = register("GuestA", "GUEST").get("accessToken").asText();
        String secondGuest = register("GuestB", "GUEST").get("accessToken").asText();

        List<Integer> statuses = runTogether(
                () -> reserveStatus(firstGuest, listingId, range, UUID.randomUUID().toString()),
                () -> reserveStatus(secondGuest, listingId, range, UUID.randomUUID().toString()));

        assertEquals(1, statuses.stream().filter(code -> code == 200).count());
        assertEquals(1, statuses.stream().filter(code -> code == 409).count());
    }

    @Test
    void concurrentReuseOfAnIdempotencyKeyAcrossListingsReturnsConflict() throws Exception {
        String hostToken = register("Host", "HOST").get("accessToken").asText();
        String firstListingId = createActiveListing(hostToken).get("id").asText();
        String secondListingId = createActiveListing(hostToken).get("id").asText();
        DateRange range = range(22);
        setAvailability(hostToken, firstListingId, range, false);
        setAvailability(hostToken, secondListingId, range, false);
        String guestToken = register("Guest", "GUEST").get("accessToken").asText();
        String key = UUID.randomUUID().toString();

        List<Integer> statuses = runTogether(
                () -> reserveStatus(guestToken, firstListingId, range, key),
                () -> reserveStatus(guestToken, secondListingId, range, key));

        assertEquals(1, statuses.stream().filter(code -> code == 200).count());
        assertEquals(1, statuses.stream().filter(code -> code == 409).count());
        mockMvc.perform(get("/api/reservations").header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    private JsonNode register(String name, String role) throws Exception {
        String email = name.toLowerCase() + "-" + UUID.randomUUID() + "@example.test";
        MvcResult result = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", name, "email", email,
                                "password", "safe-password-123", "role", role))))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode createActiveListing(String hostToken) throws Exception {
        Map<String, Object> listingBody = Map.ofEntries(Map.entry("title", "Casa de prueba"),
                Map.entry("description", "Alojamiento para reservas."), Map.entry("city", "Bogotá"),
                Map.entry("location", "Chapinero"), Map.entry("type", "APARTMENT"), Map.entry("capacity", 4),
                Map.entry("pricePerNight", 100000), Map.entry("amenityIds", List.of("WIFI")),
                Map.entry("rules", "No fumar."), Map.entry("imageReferences", List.of()),
                Map.entry("extraOptions", List.of(Map.of("name", "Desayuno", "unitPrice", 20000))));
        MvcResult created = mockMvc.perform(post("/api/listings").header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(listingBody)))
                .andExpect(status().isOk()).andReturn();
        JsonNode listing = mapper.readTree(created.getResponse().getContentAsString());
        String id = listing.get("id").asText();
        mockMvc.perform(patch("/api/listings/" + id + "/state").header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        return listing;
    }

    private void setAvailability(String hostToken, String listingId, DateRange range, boolean adjacentPeriods) throws Exception {
        List<Map<String, Object>> periods = adjacentPeriods
                ? List.of(period(range.start(), range.start().plusDays(1)), period(range.start().plusDays(1), range.end()))
                : List.of(period(range.start(), range.end()));
        mockMvc.perform(put("/api/listings/" + listingId + "/availability").header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("periods", periods))))
                .andExpect(status().isOk());
    }

    private Map<String, Object> period(LocalDate start, LocalDate end) {
        return Map.of("startDate", start.toString(), "endDate", end.toString(), "type", "AVAILABLE");
    }

    private JsonNode reserve(String guestToken, String listingId, DateRange range, List<Map<String, Object>> extras) throws Exception {
        Map<String, Object> body = Map.of("listingId", listingId, "checkIn", range.start().toString(),
                "checkOut", range.end().toString(), "guests", 2, "extras", extras);
        MvcResult result = mockMvc.perform(post("/api/reservations").header("Authorization", "Bearer " + guestToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private int reserveStatus(String guestToken, String listingId, DateRange range, String idempotencyKey) throws Exception {
        Map<String, Object> body = Map.of("listingId", listingId, "checkIn", range.start().toString(),
                "checkOut", range.end().toString(), "guests", 2, "extras", List.of());
        return mockMvc.perform(post("/api/reservations").header("Authorization", "Bearer " + guestToken)
                        .header("Idempotency-Key", idempotencyKey).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andReturn().getResponse().getStatus();
    }

    private List<Integer> runTogether(Callable<Integer> first, Callable<Integer> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> gatedFirst = gated(ready, start, first);
        Callable<Integer> gatedSecond = gated(ready, start, second);
        try {
            Future<Integer> firstResult = executor.submit(gatedFirst);
            Future<Integer> secondResult = executor.submit(gatedSecond);
            if (!ready.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Las solicitudes no quedaron listas a tiempo.");
            start.countDown();
            return List.of(firstResult.get(15, TimeUnit.SECONDS), secondResult.get(15, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private Callable<Integer> gated(CountDownLatch ready, CountDownLatch start, Callable<Integer> request) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("La prueba concurrente no inició.");
            return request.call();
        };
    }

    private DateRange range(int daysFromToday) {
        LocalDate start = LocalDate.now(ZONE).plusDays(daysFromToday);
        return new DateRange(start, start.plusDays(2));
    }

    private record DateRange(LocalDate start, LocalDate end) { }
}
