package co.edu.unbosque.marketplace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import co.edu.unbosque.marketplace.listing.AvailabilityPeriod;
import co.edu.unbosque.marketplace.listing.AvailabilityRepository;
import co.edu.unbosque.marketplace.listing.AvailabilityType;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.listing.ListingService;
import co.edu.unbosque.marketplace.audit.AuditEventRepository;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import co.edu.unbosque.marketplace.audit.AuditEvent;
import java.time.LocalDate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MarketplaceAcceptanceTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired ListingRepository listings;
    @Autowired AvailabilityRepository availability;
    @Autowired ListingService listingService;
    @Autowired AuditEventRepository auditEvents;
    @Autowired MarketplaceUserRepository users;

    @Test
    void registrationLoginRecoveryAndProfileAreSafe() throws Exception {
        String email = "guest-" + UUID.randomUUID() + "@example.test";
        String registration = "{\"name\":\"Guest One\",\"email\":\"" + email + "\",\"password\":\"safe-password-123\",\"role\":\"GUEST\"}";
        MvcResult created = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email)).andExpect(jsonPath("$.user.passwordHash").doesNotExist()).andReturn();
        String token = mapper.readTree(created.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Correo o contraseña inválidos."));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"safe-password-123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("GUEST"));

        long recoveryEventsBefore = auditEvents.countByEventType("RECOVERY_REQUESTED");
        String existing = mockMvc.perform(post("/api/auth/recovery").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String absent = mockMvc.perform(post("/api/auth/recovery").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"absent-" + UUID.randomUUID() + "@example.test\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertEquals(mapper.readTree(existing).get("message").asText(), mapper.readTree(absent).get("message").asText());
        org.junit.jupiter.api.Assertions.assertEquals(recoveryEventsBefore + 2, auditEvents.countByEventType("RECOVERY_REQUESTED"));
        mockMvc.perform(post("/api/auth/recovery").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/profile/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
        mockMvc.perform(get("/api/profile/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/profile/me").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Guest\",\"phone\":\"3001234567\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated Guest"));

        String otherEmail = "profile-other-" + UUID.randomUUID() + "@example.test";
        MvcResult other = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other Guest\",\"email\":\"" + otherEmail + "\",\"password\":\"safe-password-123\"}"))
                .andExpect(status().isOk()).andReturn();
        String otherToken = mapper.readTree(other.getResponse().getContentAsString()).get("accessToken").asText();
        mockMvc.perform(patch("/api/profile/me").header("Authorization", "Bearer " + otherToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other Profile\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(otherEmail));
        mockMvc.perform(get("/api/profile/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated Guest"));
        mockMvc.perform(delete("/api/profile/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/profile/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"safe-password-123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hostCanCreateActivateAndFindOwnedListing() throws Exception {
        String email = "host-" + UUID.randomUUID() + "@example.test";
        String body = "{\"name\":\"Host One\",\"email\":\"" + email + "\",\"password\":\"safe-password-123\",\"role\":\"HOST\"}";
        MvcResult registered = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        String token = mapper.readTree(registered.getResponse().getContentAsString()).get("accessToken").asText();
        String listingBody = "{\"title\":\"Casa central\",\"description\":\"Alojamiento de prueba\",\"city\":\"Bogotá\",\"location\":\"Chapinero\",\"type\":\"APARTMENT\",\"capacity\":4,\"pricePerNight\":180000,\"amenityIds\":[\"WIFI\"],\"rules\":\"No fumar\",\"imageReferences\":[]}";
        mockMvc.perform(post("/api/listings").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(listingBody.replace("\"capacity\":4", "\"capacity\":0")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/listings").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(listingBody.replace("180000", "0")))
                .andExpect(status().isBadRequest());
        MvcResult created = mockMvc.perform(post("/api/listings").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(listingBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT")).andReturn();
        JsonNode listing = mapper.readTree(created.getResponse().getContentAsString());
        String id = listing.get("id").asText();

        mockMvc.perform(get("/api/listings/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));
        String updatedBody = listingBody.replace("Casa central", "Casa central renovada");
        mockMvc.perform(patch("/api/listings/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(updatedBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Casa central renovada"));
        mockMvc.perform(patch("/api/listings/" + id + "/state").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAUSED\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/listings/" + id + "/state").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(put("/api/listings/" + id + "/availability").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"periods\":[{\"startDate\":\"2026-12-01\",\"endDate\":\"2026-12-10\",\"type\":\"AVAILABLE\"}]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("AVAILABLE"));
        mockMvc.perform(get("/api/listings").param("city", "bogotá").param("guests", "2").param("amenityIds", "WIFI"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id));
        mockMvc.perform(get("/api/listings/cities").param("query", "bogota"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("Bogotá"));
        mockMvc.perform(get("/api/listings").param("city", "bogotá").param("checkIn", "2026-12-03")
                        .param("checkOut", "2026-12-05").param("guests", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(get("/api/listings").param("type", "APARTMENT").param("minPrice", "100000")
                        .param("maxPrice", "200000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(get("/api/listings/" + id + "/availability").param("from", "2026-12-03").param("to", "2026-12-05"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("AVAILABLE"));
        long reservationEventsBefore = auditEvents.countByEventType("AVAILABILITY_RESERVED");
        AvailabilityPeriod reserved = listingService.confirmReservation(UUID.fromString(id),
                LocalDate.parse("2026-12-03"), LocalDate.parse("2026-12-05"), "reservation-confirmed-test");
        org.junit.jupiter.api.Assertions.assertEquals(AvailabilityType.RESERVED, reserved.getType());
        org.junit.jupiter.api.Assertions.assertEquals("reservation-confirmed-test", reserved.getReservationReference());
        org.junit.jupiter.api.Assertions.assertEquals(reservationEventsBefore + 1, auditEvents.countByEventType("AVAILABILITY_RESERVED"));
        var periodsAfterReservation = availability.findByListing_IdOrderByStartDateAsc(UUID.fromString(id));
        org.junit.jupiter.api.Assertions.assertEquals(3, periodsAfterReservation.size());
        org.junit.jupiter.api.Assertions.assertEquals(AvailabilityType.AVAILABLE, periodsAfterReservation.get(0).getType());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.parse("2026-12-03"), periodsAfterReservation.get(0).getEndDate());
        org.junit.jupiter.api.Assertions.assertEquals(AvailabilityType.RESERVED, periodsAfterReservation.get(1).getType());
        org.junit.jupiter.api.Assertions.assertEquals(AvailabilityType.AVAILABLE, periodsAfterReservation.get(2).getType());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.parse("2026-12-05"), periodsAfterReservation.get(2).getStartDate());
        mockMvc.perform(get("/api/listings").param("checkIn", "2026-12-03").param("checkOut", "2026-12-05"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(get("/api/listings/" + id + "/availability").param("from", "2026-12-03").param("to", "2026-12-05"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("RESERVED"));
        org.junit.jupiter.api.Assertions.assertThrows(co.edu.unbosque.marketplace.api.DomainException.class,
                () -> listingService.confirmReservation(UUID.fromString(id), LocalDate.parse("2026-12-03"),
                        LocalDate.parse("2026-12-05"), "reservation-duplicate-test"));
        mockMvc.perform(put("/api/listings/" + id + "/availability").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"periods\":[{\"startDate\":\"2026-12-01\",\"endDate\":\"2026-12-01\",\"type\":\"AVAILABLE\"}]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/listings/" + id + "/availability").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"periods\":[{\"startDate\":\"2026-12-01\",\"endDate\":\"2026-12-07\",\"type\":\"AVAILABLE\"},{\"startDate\":\"2026-12-06\",\"endDate\":\"2026-12-10\",\"type\":\"AVAILABLE\"}]}"))
                .andExpect(status().isConflict());

        var persistedListing = listings.findById(UUID.fromString(id)).orElseThrow();
        availability.save(new AvailabilityPeriod(persistedListing, LocalDate.parse("2026-12-12"),
                LocalDate.parse("2026-12-15"), AvailabilityType.RESERVED, "reservation-test"));
        mockMvc.perform(put("/api/listings/" + id + "/availability").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"periods\":[{\"startDate\":\"2026-12-13\",\"endDate\":\"2026-12-16\",\"type\":\"BLOCKED\"}]}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/listings/" + id + "/availability").param("from", "2026-12-12").param("to", "2026-12-15"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("RESERVED"));

        String otherHostEmail = "other-" + UUID.randomUUID() + "@example.test";
        MvcResult otherHost = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other Host\",\"email\":\"" + otherHostEmail + "\",\"password\":\"safe-password-123\",\"role\":\"HOST\"}"))
                .andExpect(status().isOk()).andReturn();
        String otherToken = mapper.readTree(otherHost.getResponse().getContentAsString()).get("accessToken").asText();
        mockMvc.perform(patch("/api/listings/" + id).header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON).content(updatedBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void activeListingDetailIsPublicAndInactiveDirectUrlIsHidden() throws Exception {
        String email = "detail-host-" + UUID.randomUUID() + "@example.test";
        MvcResult registered = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Detail Host\",\"email\":\"" + email + "\",\"password\":\"safe-password-123\",\"role\":\"HOST\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = mapper.readTree(registered.getResponse().getContentAsString()).get("accessToken").asText();
        String body = "{\"title\":\"Casa con terraza\",\"description\":\"Alojamiento amplio y luminoso.\",\"city\":\"Bogotá\",\"location\":\"Chapinero Alto\",\"type\":\"APARTMENT\",\"capacity\":4,\"pricePerNight\":180000,\"amenityIds\":[\"WIFI\",\"PARKING\"],\"rules\":\"No fumar.\",\"imageReferences\":[\"/images/casa-terraza.jpg\"]}";
        MvcResult created = mockMvc.perform(post("/api/listings").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        String id = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/listings/" + id))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/listings/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Alojamiento amplio y luminoso."));

        mockMvc.perform(patch("/api/listings/" + id + "/state").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/listings/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Casa con terraza"))
                .andExpect(jsonPath("$.description").value("Alojamiento amplio y luminoso."))
                .andExpect(jsonPath("$.location").value("Chapinero Alto"))
                .andExpect(jsonPath("$.city").value("Bogotá"))
                .andExpect(jsonPath("$.type").value("APARTMENT"))
                .andExpect(jsonPath("$.capacity").value(4))
                .andExpect(jsonPath("$.pricePerNight").value(180000))
                .andExpect(jsonPath("$.amenityIds").value(org.hamcrest.Matchers.containsInAnyOrder("WIFI", "PARKING")))
                .andExpect(jsonPath("$.rules").value("No fumar."))
                .andExpect(jsonPath("$.imageReferences[0]").value("/images/casa-terraza.jpg"));

        mockMvc.perform(patch("/api/listings/" + id + "/state").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAUSED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/listings/" + id))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/listings/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void publicRegistrationCannotAssignAdminAndGuestCannotCreateListing() throws Exception {
        String adminEmail = "admin-" + UUID.randomUUID() + "@example.test";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No Admin\",\"email\":\"" + adminEmail + "\",\"password\":\"safe-password-123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        String guestEmail = "guest-" + UUID.randomUUID() + "@example.test";
        MvcResult registered = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Guest\",\"email\":\"" + guestEmail + "\",\"password\":\"safe-password-123\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = mapper.readTree(registered.getResponse().getContentAsString()).get("accessToken").asText();
        String validListing = "{\"title\":\"Casa\",\"description\":\"Desc\",\"city\":\"Bogotá\",\"location\":\"Centro\",\"type\":\"ROOM\",\"capacity\":2,\"pricePerNight\":100000}";
        mockMvc.perform(post("/api/listings").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(validListing))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminMustProvideReasonToBlockListingAndBlockedListingIsNotSearchable() throws Exception {
        String hostEmail = "host-" + UUID.randomUUID() + "@example.test";
        MvcResult host = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Host\",\"email\":\"" + hostEmail + "\",\"password\":\"safe-password-123\",\"role\":\"HOST\"}"))
                .andExpect(status().isOk()).andReturn();
        String hostToken = mapper.readTree(host.getResponse().getContentAsString()).get("accessToken").asText();
        String validListing = "{\"title\":\"Casa para bloquear\",\"description\":\"Desc\",\"city\":\"Cali\",\"location\":\"Centro\",\"type\":\"ROOM\",\"capacity\":2,\"pricePerNight\":100000}";
        MvcResult created = mockMvc.perform(post("/api/listings").header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON).content(validListing))
                .andExpect(status().isOk()).andReturn();
        String listingId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        var persistedListing = listings.findById(UUID.fromString(listingId)).orElseThrow();
        availability.save(new AvailabilityPeriod(persistedListing, LocalDate.parse("2027-01-10"),
                LocalDate.parse("2027-01-12"), AvailabilityType.RESERVED, "reservation-review-test"));

        String adminEmail = "admin-" + UUID.randomUUID() + "@example.test";
        MvcResult admin = mockMvc.perform(post("/api/auth/register").header("X-Admin-Bootstrap", "bootstrap-key-for-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Admin\",\"email\":\"" + adminEmail + "\",\"password\":\"safe-password-123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andReturn();
        String adminToken = mapper.readTree(admin.getResponse().getContentAsString()).get("accessToken").asText();
        mockMvc.perform(patch("/api/listings/" + listingId + "/state").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isForbidden());
        long stateEventsBeforeBlock = auditEvents.countByEventType("LISTING_STATE_CHANGED");
        mockMvc.perform(patch("/api/listings/" + listingId + "/state").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BLOCKED\",\"reason\":\"Incumple las reglas\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
        org.junit.jupiter.api.Assertions.assertEquals(stateEventsBeforeBlock + 1, auditEvents.countByEventType("LISTING_STATE_CHANGED"));
        AuditEvent blockEvent = auditEvents.findFirstByEventTypeOrderByOccurredAtDesc("LISTING_STATE_CHANGED");
        org.junit.jupiter.api.Assertions.assertEquals(users.findByEmail(adminEmail).orElseThrow().getId(), blockEvent.getActorId());
        org.junit.jupiter.api.Assertions.assertEquals("LISTING", blockEvent.getEntityType());
        org.junit.jupiter.api.Assertions.assertEquals(UUID.fromString(listingId), blockEvent.getEntityId());
        org.junit.jupiter.api.Assertions.assertEquals("SUCCESS", blockEvent.getResult());
        org.junit.jupiter.api.Assertions.assertEquals("Incumple las reglas", blockEvent.getReason());
        org.junit.jupiter.api.Assertions.assertNotNull(blockEvent.getOccurredAt());
        mockMvc.perform(get("/api/listings/" + listingId + "/availability").header("Authorization", "Bearer " + adminToken)
                        .param("from", "2027-01-10").param("to", "2027-01-12"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("RESERVED"));
        mockMvc.perform(get("/api/listings").param("city", "Cali"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }
}
