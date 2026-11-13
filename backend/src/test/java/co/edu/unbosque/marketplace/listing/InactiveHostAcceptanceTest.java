package co.edu.unbosque.marketplace.listing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unbosque.marketplace.booking.Reservation;
import co.edu.unbosque.marketplace.booking.ReservationRepository;
import co.edu.unbosque.marketplace.booking.ReservationStatus;
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

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:inactivehost_${random.uuid};DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InactiveHostAcceptanceTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired MarketplaceUserRepository users;
    @Autowired ListingRepository listings;
    @Autowired ReservationRepository reservations;

    @Test
    void deactivatedHostDisappearsFromPublicFlowsAndKeepsReservations() throws Exception {
        verifyHostUnavailable(false);
    }

    @Test
    void restrictedHostDisappearsFromPublicFlowsAndKeepsReservations() throws Exception {
        verifyHostUnavailable(true);
    }

    private void verifyHostUnavailable(boolean restricted) throws Exception {
        Account host = register("HOST");
        Account guest = register("GUEST");
        Account admin = register("ADMIN");
        String city = "Ciudad" + UUID.randomUUID().toString().replace("-", "");
        Listing listing = new Listing(users.findById(host.id()).orElseThrow(),
                new ListingData("Casa " + city, "Prueba", city, "Centro", "HOUSE", 4,
                        new BigDecimal("100000"), Set.of(), "", Set.of()));
        listing.setStatus(ListingStatus.ACTIVE);
        listing = listings.saveAndFlush(listing);
        LocalDate checkIn = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(10);
        var details = new Reservation.ReservationDetails(checkIn, checkIn.plusDays(2), 2, 1,
                new BigDecimal("100000"), new BigDecimal("200000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("200000"), "COP", "TEST", 7, 2, new BigDecimal("50"));
        Reservation existing = reservations.saveAndFlush(new Reservation(users.findById(guest.id()).orElseThrow(),
                listing, UUID.randomUUID().toString(), "a".repeat(64), details, List.of()));
        String id = listing.getId().toString();
        mvc.perform(get("/api/listings").param("city", city)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/listings/suggestions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '" + id + "')]").isNotEmpty());

        if (restricted) {
            mvc.perform(post("/api/admin/users/" + host.id() + "/restrict")
                    .header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"Verificación de prueba\"}")).andExpect(status().isOk());
        } else {
            mvc.perform(delete("/api/profile/me").header("Authorization", bearer(host))).andExpect(status().isOk());
        }

        mvc.perform(get("/api/listings").param("city", city)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/listings/cities").param("query", city)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/listings/suggestions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '" + id + "')]").isEmpty());
        mvc.perform(get("/api/listings/" + id)).andExpect(status().isNotFound());
        String quote = mapper.writeValueAsString(Map.of("checkIn", checkIn, "checkOut", checkIn.plusDays(2), "guests", 1));
        mvc.perform(post("/api/listings/" + id + "/quotes").contentType(MediaType.APPLICATION_JSON).content(quote))
                .andExpect(status().isNotFound());
        String booking = mapper.writeValueAsString(Map.of("listingId", id, "checkIn", checkIn,
                "checkOut", checkIn.plusDays(2), "guests", 1));
        mvc.perform(post("/api/reservations").header("Authorization", bearer(guest)).header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(booking)).andExpect(status().isNotFound());
        assertEquals(ReservationStatus.CONFIRMED, reservations.findById(existing.getId()).orElseThrow().getStatus());
        assertTrue(reservations.findById(existing.getId()).orElseThrow().isManualReviewRequired());
        mvc.perform(get("/api/reservations/" + existing.getId()).header("Authorization", bearer(guest)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(get("/api/admin/reservations/manual-review").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '" + existing.getId() + "')]").isNotEmpty());
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
