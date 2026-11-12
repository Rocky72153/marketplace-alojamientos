package co.edu.unbosque.marketplace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationAcceptanceTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @Test
    void notifiesBothParticipantsAndProtectsEachInbox() throws Exception {
        String host = register("HOST");
        String guest = register("GUEST");
        String outsider = register("GUEST");
        String listingId = createListing(host);

        JsonNode hostAfterPublication = inbox(host);
        assertEquals(1, hostAfterPublication.size());
        JsonNode publication = find(hostAfterPublication, "LISTING_STATE_CHANGED");
        assertEquals(listingId, publication.get("referenceId").asText());
        assertEquals(0, inbox(guest).size());
        assertEquals(0, inbox(outsider).size());

        String publicationId = publication.get("id").asText();
        mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/notifications/" + publicationId)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/notifications/" + publicationId).header("Authorization", bearer(outsider)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/notifications/" + publicationId + "/read").header("Authorization", bearer(outsider)))
                .andExpect(status().isNotFound());
        MvcResult read = mockMvc.perform(patch("/api/notifications/" + publicationId + "/read")
                        .header("Authorization", bearer(host)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.readAt").isNotEmpty()).andReturn();
        String readAt = mapper.readTree(read.getResponse().getContentAsString()).get("readAt").asText();
        mockMvc.perform(patch("/api/notifications/" + publicationId + "/read").header("Authorization", bearer(host)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.readAt").value(readAt));

        LocalDate checkIn = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(15);
        LocalDate checkOut = checkIn.plusDays(2);
        String periods = mapper.writeValueAsString(Map.of("periods", List.of(Map.of(
                "startDate", checkIn.toString(), "endDate", checkOut.toString(), "type", "AVAILABLE"))));
        mockMvc.perform(put("/api/listings/" + listingId + "/availability").header("Authorization", bearer(host))
                        .contentType(MediaType.APPLICATION_JSON).content(periods)).andExpect(status().isOk());
        String body = mapper.writeValueAsString(Map.of("listingId", listingId, "checkIn", checkIn.toString(),
                "checkOut", checkOut.toString(), "guests", 2, "extras", List.of()));
        String key = UUID.randomUUID().toString();
        MvcResult confirmed = mockMvc.perform(post("/api/reservations").header("Authorization", bearer(guest))
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        String reservationId = mapper.readTree(confirmed.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/reservations").header("Authorization", bearer(guest))
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(reservationId));
        mockMvc.perform(post("/api/reservations").header("Authorization", bearer(guest))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());

        JsonNode guestConfirmed = inbox(guest);
        JsonNode hostConfirmed = inbox(host);
        assertEquals(1, guestConfirmed.size());
        assertEquals(2, hostConfirmed.size());
        assertEquals(reservationId, find(guestConfirmed, "RESERVATION_CONFIRMED").get("referenceId").asText());
        assertEquals(reservationId, find(hostConfirmed, "RESERVATION_CONFIRMED").get("referenceId").asText());
        String guestNotificationId = find(guestConfirmed, "RESERVATION_CONFIRMED").get("id").asText();
        mockMvc.perform(get("/api/notifications/" + guestNotificationId).header("Authorization", bearer(host)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/reservations/" + reservationId + "/cancel")
                        .header("Authorization", bearer(guest)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Cambio de planes\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/reservations/" + reservationId + "/cancel")
                        .header("Authorization", bearer(guest)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Segundo intento\"}"))
                .andExpect(status().isConflict());
        JsonNode guestCancelled = inbox(guest);
        JsonNode hostCancelled = inbox(host);
        assertEquals(2, guestCancelled.size());
        assertEquals(3, hostCancelled.size());
        assertEquals(reservationId, find(guestCancelled, "RESERVATION_CANCELLED").get("referenceId").asText());
        assertEquals(reservationId, find(hostCancelled, "RESERVATION_CANCELLED").get("referenceId").asText());
        assertEquals(0, inbox(outsider).size());
    }

    private String register(String role) throws Exception {
        String body = mapper.writeValueAsString(Map.of("name", "Usuario", "email", UUID.randomUUID() + "@example.test",
                "password", "safe-password-123", "role", role));
        MvcResult response = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(response.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String createListing(String token) throws Exception {
        String body = mapper.writeValueAsString(Map.of("title", "Casa de prueba", "description", "Alojamiento de prueba",
                "city", "Bogotá", "location", "Chapinero", "type", "APARTMENT", "capacity", 4,
                "pricePerNight", 100000, "amenityIds", List.of("WIFI")));
        MvcResult response = mockMvc.perform(post("/api/listings").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();
        String listingId = mapper.readTree(response.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(patch("/api/listings/" + listingId + "/state").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        return listingId;
    }

    private JsonNode inbox(String token) throws Exception {
        MvcResult response = mockMvc.perform(get("/api/notifications").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(response.getResponse().getContentAsString());
    }

    private JsonNode find(JsonNode items, String type) {
        for (JsonNode item : items) {
            if (type.equals(item.get("type").asText())) return item;
        }
        return fail("No se encontró una notificación de tipo " + type);
    }

    private String bearer(String token) { return "Bearer " + token; }
}
