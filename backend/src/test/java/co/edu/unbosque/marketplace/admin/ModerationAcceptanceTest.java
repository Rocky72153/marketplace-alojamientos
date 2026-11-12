package co.edu.unbosque.marketplace.admin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unbosque.marketplace.audit.AuditEvent;
import co.edu.unbosque.marketplace.audit.AuditEventRepository;
import co.edu.unbosque.marketplace.booking.Reservation;
import co.edu.unbosque.marketplace.booking.ReservationRepository;
import co.edu.unbosque.marketplace.booking.ReservationStatus;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.listing.ListingData;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.listing.ListingStatus;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModerationAcceptanceTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired MarketplaceUserRepository users;
    @Autowired ListingRepository listings;
    @Autowired ReservationRepository reservations;
    @Autowired AuditEventRepository audit;

    @Test
    void restrictionIsAdminOnlyAuditedAndInvalidatesExistingSession() throws Exception {
        Account admin = register("ADMIN");
        Account guest = register("GUEST");
        Account other = register("HOST");
        String path = "/api/admin/users/" + guest.id() + "/restrict";
        String body = "{\"reason\":\"Incumplimiento comprobado\"}";
        mvc.perform(post(path).header("Authorization", bearer(other)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(guest))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/audit").header("Authorization", bearer(guest))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/listings").header("Authorization", bearer(guest))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/reservations/manual-review").header("Authorization", bearer(guest))).andExpect(status().isForbidden());
        mvc.perform(post(path).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"   \"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/users/" + admin.id() + "/restrict").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        mvc.perform(post(path).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accountState").value("RESTRICTED"));
        mvc.perform(get("/api/profile/me").header("Authorization", bearer(guest))).andExpect(status().isUnauthorized());
        String login = mapper.writeValueAsString(Map.of("email", users.findById(guest.id()).orElseThrow().getEmail(),
                "password", "safe-password-123"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(path).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        AuditEvent event = audit.findFirstByEventTypeOrderByOccurredAtDesc("ACCOUNT_RESTRICTED");
        mvc.perform(get("/api/admin/audit/" + event.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.actorId").value(admin.id().toString()))
                .andExpect(jsonPath("$.entityId").value(guest.id().toString()))
                .andExpect(jsonPath("$.reason").value("Incumplimiento comprobado"))
                .andExpect(jsonPath("$.result").value("SUCCESS")).andExpect(jsonPath("$.occurredAt").exists());
        mvc.perform(get("/api/admin/audit").header("Authorization", bearer(admin)).param("size", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/api/admin/listings").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/api/admin/audit").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void blockingListingPreservesFutureReservationsAndFlagsThemForReview() throws Exception {
        Account admin = register("ADMIN");
        Account host = register("HOST");
        Account guest = register("GUEST");
        MarketplaceUser owner = users.findById(host.id()).orElseThrow();
        Listing listing = new Listing(owner, new ListingData("Casa a moderar", "Prueba", "Bogotá", "Centro",
                "APARTMENT", 4, new BigDecimal("100000"), Set.of(), "", Set.of()));
        listing.setStatus(ListingStatus.ACTIVE);
        listing = listings.saveAndFlush(listing);
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
        Reservation future = reservation(guest.id(), listing, today.plusDays(5));
        Reservation past = reservation(guest.id(), listing, today.minusDays(10));
        String url = "/api/listings/" + listing.getId() + "/state";
        String body = "{\"status\":\"BLOCKED\",\"reason\":\"Revisión administrativa\"}";
        mvc.perform(patch(url).header("Authorization", bearer(host)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(patch(url).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"BLOCKED\",\"reason\":\"\"}")).andExpect(status().isForbidden());
        mvc.perform(patch(url).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
        assertTrue(reservations.findById(future.getId()).orElseThrow().isManualReviewRequired());
        assertEquals(ReservationStatus.CONFIRMED, reservations.findById(future.getId()).orElseThrow().getStatus());
        assertFalse(reservations.findById(past.getId()).orElseThrow().isManualReviewRequired());
        mvc.perform(get("/api/admin/reservations/manual-review").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '" + future.getId() + "')]").isNotEmpty());
        AuditEvent event = audit.findFirstByEventTypeOrderByOccurredAtDesc("LISTING_STATE_CHANGED");
        mvc.perform(get("/api/admin/audit/" + event.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.actorId").value(admin.id().toString()))
                .andExpect(jsonPath("$.entityId").value(listing.getId().toString()))
                .andExpect(jsonPath("$.reason").value("Revisión administrativa"));
        String resolve = "/api/admin/reservations/" + future.getId() + "/resolve-review";
        String resolution = "{\"reason\":\"Contacto verificado\"}";
        mvc.perform(post(resolve).header("Authorization", bearer(guest)).contentType(MediaType.APPLICATION_JSON).content(resolution))
                .andExpect(status().isForbidden());
        mvc.perform(post(resolve).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON).content(resolution))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        assertFalse(reservations.findById(future.getId()).orElseThrow().isManualReviewRequired());
        AuditEvent resolved = audit.findFirstByEventTypeOrderByOccurredAtDesc("RESERVATION_REVIEW_RESOLVED");
        org.junit.jupiter.api.Assertions.assertEquals(admin.id(), resolved.getActorId());
        org.junit.jupiter.api.Assertions.assertEquals("Contacto verificado", resolved.getReason());
    }

    private Reservation reservation(UUID guestId, Listing listing, LocalDate checkIn) {
        BigDecimal zero = BigDecimal.ZERO;
        var details = new Reservation.ReservationDetails(checkIn, checkIn.plusDays(2), 2, 1,
                new BigDecimal("100000"), new BigDecimal("200000"), zero, zero, zero, zero,
                new BigDecimal("200000"), "COP", "TEST", 7, 2, new BigDecimal("50"));
        return reservations.saveAndFlush(new Reservation(users.findById(guestId).orElseThrow(), listing,
                UUID.randomUUID().toString(), "a".repeat(64), details, List.of()));
    }

    private Account register(String role) throws Exception {
        String body = mapper.writeValueAsString(Map.of("name", role, "email", UUID.randomUUID() + "@example.test",
                "password", "safe-password-123", "role", role));
        String response = mvc.perform(post("/api/auth/register").header("X-Admin-Bootstrap", "bootstrap-key-for-tests")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(response);
        return new Account(UUID.fromString(node.get("user").get("id").asText()), node.get("accessToken").asText());
    }

    private String bearer(Account account) { return "Bearer " + account.token(); }
    private record Account(UUID id, String token) { }
}
