package co.edu.unbosque.marketplace.review;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unbosque.marketplace.audit.AuditEvent;
import co.edu.unbosque.marketplace.audit.AuditEventRepository;
import co.edu.unbosque.marketplace.booking.Reservation;
import co.edu.unbosque.marketplace.booking.ReservationRepository;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.listing.ListingData;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.listing.ListingStatus;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReviewAcceptanceTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired MarketplaceUserRepository users;
    @Autowired ListingRepository listings;
    @Autowired ReservationRepository reservations;
    @Autowired AuditEventRepository events;

    @Test
    void onlyTheGuestOfACompletedStayCanReviewTheMatchingListingOnce() throws Exception {
        Account host = register("HOST");
        Account guest = register("GUEST");
        Account other = register("GUEST");
        Listing listing = listing(host.id());
        Listing otherListing = listing(host.id());
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
        Reservation completed = booking(guest.id(), listing, today.minusDays(3), today.minusDays(1), false);
        Reservation checkoutToday = booking(guest.id(), listing, today.minusDays(2), today, false);
        Reservation cancelled = booking(guest.id(), listing, today.minusDays(5), today.minusDays(3), true);

        review(guest.token(), listing.getId(), checkoutToday.getId(), 5, "Aún no termina")
                .andExpect(status().isForbidden());
        review(guest.token(), listing.getId(), cancelled.getId(), 5, "Fue cancelada")
                .andExpect(status().isForbidden());
        review(other.token(), listing.getId(), completed.getId(), 5, "No es mi reserva")
                .andExpect(status().isForbidden());
        review(guest.token(), otherListing.getId(), completed.getId(), 5, "Otro alojamiento")
                .andExpect(status().isForbidden());
        review(guest.token(), listing.getId(), completed.getId(), 0, "Fuera de escala")
                .andExpect(status().isBadRequest());
        review(guest.token(), listing.getId(), completed.getId(), 5, "   ")
                .andExpect(status().isBadRequest());
        review(guest.token(), listing.getId(), completed.getId(), 5, "Estadía excelente")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.bookingId").value(completed.getId().toString()))
                .andExpect(jsonPath("$.content").value("Estadía excelente"));
        review(guest.token(), listing.getId(), completed.getId(), 4, "Otra reseña")
                .andExpect(status().isConflict());
        mvc.perform(get("/api/listings/" + listing.getId() + "/reviews"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rating").value(5));
    }

    @Test
    void onlyAdminCanHideAReviewWithReasonAndAudit() throws Exception {
        Account host = register("HOST");
        Account guest = register("GUEST");
        Account admin = register("ADMIN");
        Listing listing = listing(host.id());
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
        Reservation booking = booking(guest.id(), listing, today.minusDays(3), today.minusDays(1), false);
        mvc.perform(get("/api/reservations/" + booking.getId()).header("Authorization", "Bearer " + guest.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reviewed").value(false));
        String response = review(guest.token(), listing.getId(), booking.getId(), 4, "Contenido de prueba")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/reservations").header("Authorization", "Bearer " + guest.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].reviewed").value(true));
        UUID reviewId = UUID.fromString(mapper.readTree(response).get("id").asText());
        String path = "/api/admin/reviews/" + reviewId + "/hide";
        mvc.perform(post(path).header("Authorization", "Bearer " + guest.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Motivo\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(path).header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Incumple las reglas\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hidden").value(true));
        mvc.perform(get("/api/listings/" + listing.getId() + "/reviews"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        // Moderation must not allow the guest to review the same stay again.
        mvc.perform(get("/api/reservations").header("Authorization", "Bearer " + guest.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].reviewed").value(true));
        mvc.perform(get("/api/reservations/" + booking.getId()).header("Authorization", "Bearer " + guest.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reviewed").value(true));
        review(guest.token(), listing.getId(), booking.getId(), 5, "Intento duplicado")
                .andExpect(status().isConflict());
        mvc.perform(get("/api/admin/reviews").header("Authorization", "Bearer " + guest.token()))
                .andExpect(status().isForbidden());
        AuditEvent event = events.findFirstByEventTypeOrderByOccurredAtDesc("REVIEW_HIDDEN");
        assertEquals(admin.id(), event.getActorId());
        assertEquals(reviewId, event.getEntityId());
        assertEquals("Incumple las reglas", event.getReason());
        assertNotNull(event.getOccurredAt());
    }

    @Test
    void concurrentReviewsOfOneReservationCannotCreateDuplicates() throws Exception {
        Account host = register("HOST");
        Account guest = register("GUEST");
        Listing listing = listing(host.id());
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
        Reservation booking = booking(guest.id(), listing, today.minusDays(3), today.minusDays(1), false);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> attempt = () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("La prueba concurrente no inició.");
            return review(guest.token(), listing.getId(), booking.getId(), 5, "Reseña concurrente")
                    .andReturn().getResponse().getStatus();
        };
        try {
            Future<Integer> first = executor.submit(attempt);
            Future<Integer> second = executor.submit(attempt);
            if (!ready.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Las solicitudes no quedaron listas.");
            start.countDown();
            List<Integer> results = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(code -> code == 201).count());
            assertEquals(1, results.stream().filter(code -> code == 409).count());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private org.springframework.test.web.servlet.ResultActions review(String token, UUID listingId, UUID bookingId,
            int rating, String content) throws Exception {
        String body = mapper.writeValueAsString(Map.of("bookingId", bookingId, "rating", rating, "content", content));
        return mvc.perform(post("/api/listings/" + listingId + "/reviews")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Account register(String role) throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        String body = mapper.writeValueAsString(Map.of("name", role, "email", email,
                "password", "safe-password-123", "role", role));
        String response = mvc.perform(post("/api/auth/register").header("X-Admin-Bootstrap", "bootstrap-key-for-tests")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(response);
        return new Account(UUID.fromString(json.get("user").get("id").asText()), json.get("accessToken").asText());
    }

    private Listing listing(UUID hostId) {
        MarketplaceUser host = users.findById(hostId).orElseThrow();
        Listing result = new Listing(host, new ListingData("Casa de prueba", "Prueba de reseñas", "Bogotá", "Centro",
                "APARTMENT", 4, new BigDecimal("100000.00"), Set.of("WIFI"), "No fumar", Set.of()));
        result.setStatus(ListingStatus.ACTIVE);
        return listings.saveAndFlush(result);
    }

    private Reservation booking(UUID guestId, Listing listing, LocalDate checkIn, LocalDate checkOut, boolean cancelled) {
        MarketplaceUser guest = users.findById(guestId).orElseThrow();
        BigDecimal zero = BigDecimal.ZERO;
        Reservation.ReservationDetails details = new Reservation.ReservationDetails(checkIn, checkOut, 2, 1,
                new BigDecimal("100000.00"), new BigDecimal("200000.00"), zero, zero, zero, zero,
                new BigDecimal("200000.00"), "COP", "TEST-PAYMENT", 7, 2, new BigDecimal("50.00"));
        Reservation result = new Reservation(guest, listing, UUID.randomUUID().toString(), "a".repeat(64), details, List.of());
        if (cancelled) result.cancel("Cancelada", Instant.now(), zero, zero);
        return reservations.saveAndFlush(result);
    }

    private record Account(UUID id, String token) { }
}
