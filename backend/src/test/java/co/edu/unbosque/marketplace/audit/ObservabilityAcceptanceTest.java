package co.edu.unbosque.marketplace.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.unbosque.marketplace.booking.Reservation;
import co.edu.unbosque.marketplace.booking.ReservationRepository;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ObservabilityAcceptanceTest.FailureController.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ObservabilityAcceptanceTest {
    private static final String SECRET = "LAB-SECRET-SHOULD-NEVER-APPEAR-73918";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AuditEventRepository events;
    @Autowired MarketplaceUserRepository users;
    @Autowired ListingRepository listings;
    @Autowired ReservationRepository reservations;
    @Autowired AuditService audit;
    @Autowired PlatformTransactionManager transactionManager;
    private Logger structured;
    private Logger rootLogger;
    private ListAppender<ILoggingEvent> appender;
    private ListAppender<ILoggingEvent> allLogs;

    @BeforeEach
    void captureStructuredLog() {
        structured = (Logger) LoggerFactory.getLogger("co.edu.unbosque.marketplace.audit.StructuredEventLogger");
        appender = new ListAppender<>();
        appender.start();
        structured.addAppender(appender);
        rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        allLogs = new ListAppender<>();
        allLogs.start();
        rootLogger.addAppender(allLogs);
    }

    @AfterEach
    void stopCapturing() {
        structured.detachAppender(appender);
        appender.stop();
        rootLogger.detachAppender(allLogs);
        allLogs.stop();
    }

    @Test
    void tracesCriticalBusinessOperationsWithTheirOwnRequestId() throws Exception {
        Account admin = register("ADMIN");
        Account host = register("HOST");
        Account guest = register("GUEST");
        MvcResult listingResponse = mvc.perform(post("/api/listings").header("Authorization", bearer(host))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of(
                        "title", "Casa de prueba", "description", "Ficticia", "city", "Bogotá",
                        "location", "Centro", "type", "APARTMENT", "capacity", 4,
                        "pricePerNight", 100000, "amenityIds", List.of("WIFI")))))
                .andExpect(status().isOk()).andReturn();
        UUID listingId = UUID.fromString(read(listingResponse).get("id").asText());
        assertTrace("LISTING_CREATED", listingId, host.id(), "SUCCESS", correlation(listingResponse));
        mvc.perform(patch("/api/listings/" + listingId + "/state").header("Authorization", bearer(host))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());

        LocalDate checkIn = LocalDate.now(ZoneId.of("America/Bogota")).plusDays(14);
        LocalDate checkOut = checkIn.plusDays(2);
        mvc.perform(put("/api/listings/" + listingId + "/availability").header("Authorization", bearer(host))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("periods", List.of(Map.of(
                        "startDate", checkIn, "endDate", checkOut, "type", "AVAILABLE"))))))
                .andExpect(status().isOk());
        MvcResult booked = mvc.perform(post("/api/reservations").header("Authorization", bearer(guest))
                .header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("listingId", listingId, "checkIn", checkIn, "checkOut", checkOut,
                        "guests", 1, "extras", List.of())))).andExpect(status().isOk()).andReturn();
        UUID bookingId = UUID.fromString(read(booked).get("id").asText());
        assertTrace("RESERVATION_CONFIRMED", bookingId, guest.id(), "SUCCESS", correlation(booked));
        MvcResult cancelled = mvc.perform(post("/api/reservations/" + bookingId + "/cancel")
                .header("Authorization", bearer(guest)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Cambio de planes\"}")).andExpect(status().isOk()).andReturn();
        assertTrace("RESERVATION_CANCELLED", bookingId, guest.id(), "SUCCESS", correlation(cancelled));

        LocalDate past = LocalDate.now(ZoneId.of("America/Bogota")).minusDays(4);
        MarketplaceUser guestUser = users.findById(guest.id()).orElseThrow();
        Listing listing = listings.findById(listingId).orElseThrow();
        BigDecimal zero = BigDecimal.ZERO;
        Reservation.ReservationDetails details = new Reservation.ReservationDetails(past, past.plusDays(2),
                2, 1, new BigDecimal("100000"), new BigDecimal("200000"), zero, zero, zero, zero,
                new BigDecimal("200000"), "COP", "TEST", 7, 2, new BigDecimal("50"));
        Reservation completed = reservations.saveAndFlush(new Reservation(guestUser, listing,
                UUID.randomUUID().toString(), "a".repeat(64), details, List.of()));
        MvcResult review = mvc.perform(post("/api/listings/" + listingId + "/reviews")
                .header("Authorization", bearer(guest)).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("bookingId", completed.getId(), "rating", 5, "content", "Prueba"))))
                .andExpect(status().isCreated()).andReturn();
        UUID reviewId = UUID.fromString(read(review).get("id").asText());
        assertTrace("REVIEW_CREATED", reviewId, guest.id(), "SUCCESS", correlation(review));
        MvcResult hidden = mvc.perform(post("/api/admin/reviews/" + reviewId + "/hide")
                .header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Motivo de prueba\"}")).andExpect(status().isOk()).andReturn();
        assertTrace("REVIEW_HIDDEN", reviewId, admin.id(), "SUCCESS", correlation(hidden));
    }

    @Test
    void errorsAndFailedLoginHaveOneSafeServerGeneratedCorrelationId() throws Exception {
        Account guest = register("GUEST");
        String incoming = "bad-correlation-" + SECRET;
        MvcResult rejected = mvc.perform(post("/api/auth/login").header("X-Correlation-ID", incoming)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", guest.email(), "password", SECRET))))
                .andExpect(status().isUnauthorized()).andReturn();
        String rejectedId = correlation(rejected);
        assertNotEquals(incoming, rejectedId);
        assertEquals(rejectedId, read(rejected).get("correlationId").asText());
        assertTrace("LOGIN", guest.id(), guest.id(), "REJECTED", rejectedId);
        assertLog("HTTP_ERROR", rejectedId);

        MvcResult validation = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest()).andReturn();
        assertEquals(correlation(validation), read(validation).get("correlationId").asText());
        assertLog("HTTP_ERROR", correlation(validation));

        MvcResult denied = mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + SECRET))
                .andExpect(status().isUnauthorized()).andReturn();
        assertEquals(correlation(denied), read(denied).get("correlationId").asText());
        assertLog("HTTP_ERROR", correlation(denied));

        MvcResult forbidden = mvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(guest)))
                .andExpect(status().isForbidden()).andReturn();
        assertEquals(correlation(forbidden), read(forbidden).get("correlationId").asText());
        assertLog("HTTP_ERROR", correlation(forbidden));

        MvcResult missing = mvc.perform(get("/api/test/unknown-route").header("Authorization", bearer(guest)))
                .andExpect(status().isNotFound()).andReturn();
        assertEquals(correlation(missing), read(missing).get("correlationId").asText());
        assertLog("HTTP_ERROR", correlation(missing));

        MvcResult wrongMethod = mvc.perform(get("/api/auth/login").header("Authorization", bearer(guest)))
                .andExpect(status().isMethodNotAllowed()).andReturn();
        assertEquals(correlation(wrongMethod), read(wrongMethod).get("correlationId").asText());
        assertLog("HTTP_ERROR", correlation(wrongMethod));

        MvcResult unexpected = mvc.perform(get("/api/test/observability-failure")
                .header("Authorization", bearer(guest))).andExpect(status().isInternalServerError()).andReturn();
        assertEquals(correlation(unexpected), read(unexpected).get("correlationId").asText());
        assertLog("HTTP_ERROR", correlation(unexpected));
        assertFalse(unexpected.getResponse().getContentAsString().contains(SECRET));
        assertFalse(unexpected.getResponse().getContentAsString().contains("IllegalStateException"));
        for (ILoggingEvent log : allLogs.list) {
            assertFalse(log.getFormattedMessage().contains(SECRET));
            assertFalse(log.getFormattedMessage().contains(guest.token()));
            if (log.getThrowableProxy() != null && log.getThrowableProxy().getMessage() != null) {
                assertFalse(log.getThrowableProxy().getMessage().contains(SECRET));
            }
        }
    }

    @Test
    void onlyCommittedEventsAreLoggedAndFreeTextIsNeverIncluded() throws Exception {
        UUID rolledBackId = UUID.randomUUID();
        UUID committedId = UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            audit.recordEvent("TEST_ROLLED_BACK", null, "TEST", rolledBackId, "SUCCESS", SECRET);
            status.setRollbackOnly();
        });
        assertTrue(events.findAll().stream().noneMatch(event -> rolledBackId.equals(event.getEntityId())));
        assertTrue(appender.list.stream().noneMatch(event -> event.getFormattedMessage().contains("TEST_ROLLED_BACK")));

        tx.executeWithoutResult(status -> audit.recordEvent("TEST_COMMITTED", null, "TEST", committedId, "SUCCESS", SECRET));
        AuditEvent event = events.findAll().stream().filter(item -> committedId.equals(item.getEntityId()))
                .findFirst().orElseThrow();
        assertLog("TEST_COMMITTED", event.getCorrelationId());
        assertTrue(appender.list.stream().noneMatch(item -> item.getFormattedMessage().contains(SECRET)));
    }

    private Account register(String role) throws Exception {
        String email = UUID.randomUUID() + "@example.test";
        MvcResult result = mvc.perform(post("/api/auth/register")
                .header("X-Admin-Bootstrap", "bootstrap-key-for-tests")
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "Prueba", "email", email,
                        "password", "safe-password-123", "role", role)))).andExpect(status().isOk()).andReturn();
        JsonNode body = read(result);
        UUID id = UUID.fromString(body.get("user").get("id").asText());
        assertTrace("ACCOUNT_REGISTERED", id, id, "SUCCESS", correlation(result));
        return new Account(id, email, body.get("accessToken").asText());
    }

    private void assertTrace(String type, UUID entityId, UUID actorId, String result, String correlation) throws Exception {
        AuditEvent event = events.findAll().stream().filter(item -> type.equals(item.getEventType())
                && entityId.equals(item.getEntityId()) && correlation.equals(item.getCorrelationId()))
                .findFirst().orElseThrow();
        assertEquals(actorId, event.getActorId());
        assertEquals(result, event.getResult());
        assertNotNull(event.getOccurredAt());
        assertLog(type, correlation);
    }

    private void assertLog(String type, String correlation) throws Exception {
        boolean found = false;
        for (ILoggingEvent event : appender.list) {
            JsonNode node = mapper.readTree(event.getFormattedMessage());
            if (type.equals(node.path("eventType").asText()) && correlation.equals(node.path("correlationId").asText())) {
                assertTrue(node.hasNonNull("timestamp"));
                assertTrue(node.hasNonNull("result"));
                found = true;
            }
        }
        assertTrue(found, "Falta evento estructurado correlacionado: " + type);
    }

    private String correlation(MvcResult result) {
        String value = result.getResponse().getHeader("X-Correlation-ID");
        assertNotNull(value);
        assertEquals(value, UUID.fromString(value).toString());
        return value;
    }
    private JsonNode read(MvcResult result) throws Exception { return mapper.readTree(result.getResponse().getContentAsString()); }
    private String json(Object value) throws Exception { return mapper.writeValueAsString(value); }
    private String bearer(Account account) { return "Bearer " + account.token(); }
    private record Account(UUID id, String email, String token) { }

    @RestController
    static class FailureController {
        @GetMapping("/api/test/observability-failure")
        void fail() { throw new IllegalStateException(SECRET); }
    }
}
