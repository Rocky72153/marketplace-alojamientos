package co.edu.unbosque.marketplace.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.audit.AuditEvent;
import co.edu.unbosque.marketplace.audit.AuditEventRepository;
import co.edu.unbosque.marketplace.booking.Reservation;
import co.edu.unbosque.marketplace.booking.ReservationRepository;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.listing.ListingData;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.listing.ListingStatus;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// This test asserts absolute dashboard totals, so it must not reuse another
// test class's already-populated Spring context and in-memory database.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:reporting_${random.uuid};DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReportingAcceptanceTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired MarketplaceUserRepository users;
    @Autowired ListingRepository listings;
    @Autowired ReservationRepository reservations;
    @Autowired AuditEventRepository events;
    @Autowired JdbcTemplate jdbc;

    @Test
    void dashboardAndExportsApplyTheSameFiltersAndRestrictAccess() throws Exception {
        Account admin = register("ADMIN");
        Account host = register("HOST");
        Account guest = register("GUEST");
        Listing bogota = listing(host.id(), "Bogotá");
        Listing medellin = listing(host.id(), "Medellín");
        booking(guest.id(), bogota, "100000", "CONFIRMED", "2026-03-10T05:00:00Z");
        booking(guest.id(), medellin, "200000", "COMPLETED", "2026-03-11T04:59:59Z");
        booking(guest.id(), bogota, "300000", "CANCELLED", "2026-03-10T12:00:00Z");
        booking(guest.id(), bogota, "400000", "CONFIRMED", "2026-03-10T04:59:59Z");
        String period = "?from=2026-03-10&to=2026-03-10";
        mvc.perform(get("/api/admin/dashboard" + period)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/dashboard" + period).header("Authorization", "Bearer " + guest.token()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/reports?format=PDF").header("Authorization", "Bearer " + host.token()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/dashboard" + period).header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.users").value(3))
                .andExpect(jsonPath("$.activeListings").value(2))
                .andExpect(jsonPath("$.bookingsByStatus.CONFIRMED").value(1))
                .andExpect(jsonPath("$.bookingsByStatus.COMPLETED").value(1))
                .andExpect(jsonPath("$.bookingsByStatus.CANCELLED").value(1))
                .andExpect(jsonPath("$.estimatedRevenue").value(300000))
                .andExpect(jsonPath("$.topCities[0].city").value("Bogotá"))
                .andExpect(jsonPath("$.topCities[0].bookings").value(1))
                .andExpect(jsonPath("$.topCities[1].city").value("Medellín"))
                .andExpect(jsonPath("$.timezone").value("America/Bogota"));

        byte[] pdf = mvc.perform(get("/api/admin/reports?format=PDF&from=2026-03-10&to=2026-03-10")
                .header("Authorization", "Bearer " + admin.token())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("300000"));
            assertTrue(text.contains("Bogotá"));
            assertTrue(text.contains("simulados"));
            assertFalse(text.contains("400000"));
        }
        byte[] xlsx = mvc.perform(get("/api/admin/reports?format=XLSX&from=2026-03-10&to=2026-03-10")
                .header("Authorization", "Bearer " + admin.token())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            var sheet = workbook.getSheetAt(0);
            assertTrue(sheet.getPhysicalNumberOfRows() > 10);
            boolean revenueFound = false;
            for (var row : sheet) {
                if (row.getCell(0).getStringCellValue().startsWith("Ingresos estimados")) {
                    revenueFound = row.getCell(1).getStringCellValue().contains("300000");
                }
            }
            assertTrue(revenueFound);
        }
        assertEquals(2, events.countByEventType("REPORT_EXPORT"));
        AuditEvent success = events.findFirstByEventTypeOrderByOccurredAtDesc("REPORT_EXPORT");
        assertEquals(admin.id(), success.getActorId());
        assertEquals("SUCCESS", success.getResult());
        assertNotNull(success.getOccurredAt());
        assertTrue(success.getReason().contains("2026-03-10"));
        mvc.perform(get("/api/admin/reports?format=DOC").header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isBadRequest());
        assertEquals("FAILURE", events.findFirstByEventTypeOrderByOccurredAtDesc("REPORT_EXPORT").getResult());
        mvc.perform(get("/api/admin/reports?format=PDF&from=2026-03-11&to=2026-03-10")
                .header("Authorization", "Bearer " + admin.token())).andExpect(status().isBadRequest());
        assertEquals("FAILURE", events.findFirstByEventTypeOrderByOccurredAtDesc("REPORT_EXPORT").getResult());
        mvc.perform(get("/api/admin/reports?format=PDF&from=private-invalid-value")
                .header("Authorization", "Bearer " + admin.token())).andExpect(status().isBadRequest());
        AuditEvent invalidDate = events.findFirstByEventTypeOrderByOccurredAtDesc("REPORT_EXPORT");
        assertEquals("FAILURE", invalidDate.getResult());
        assertEquals(admin.id(), invalidDate.getActorId());
        assertFalse(invalidDate.getReason().contains("private-invalid-value"));
        mvc.perform(get("/api/admin/reports").header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isBadRequest());
        AuditEvent missingFormat = events.findFirstByEventTypeOrderByOccurredAtDesc("REPORT_EXPORT");
        assertEquals("FAILURE", missingFormat.getResult());
        assertEquals(admin.id(), missingFormat.getActorId());
        assertEquals("Formato de reporte no admitido", missingFormat.getReason());
    }

    @Test
    void generationFailuresAreAuditedWithoutLeakingInternalDetails() {
        DashboardService dashboard = mock(DashboardService.class);
        ReportAudit audit = mock(ReportAudit.class);
        MarketplaceUser admin = new MarketplaceUser("Admin", "admin@example.test", "hash", AccountRole.ADMIN);
        when(dashboard.snapshot(null, null)).thenThrow(new IllegalStateException("secret-test-value"));
        ReportService service = new ReportService(dashboard, audit);
        DomainException error = assertThrows(DomainException.class, () -> service.generate(admin, "PDF", null, null));
        assertFalse(error.getMessage().contains("secret-test-value"));
        verify(audit).record(admin.getId(), "FAILURE", "Error al generar exportación");
        verify(audit, never()).record(any(), org.mockito.ArgumentMatchers.eq("SUCCESS"), anyString());
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

    private Listing listing(UUID hostId, String city) {
        MarketplaceUser host = users.findById(hostId).orElseThrow();
        Listing listing = new Listing(host, new ListingData("Prueba", "Prueba reporte", city, "Centro", "APARTMENT",
                4, new BigDecimal("100000"), Set.of("WIFI"), "Reglas", Set.of()));
        listing.setStatus(ListingStatus.ACTIVE);
        return listings.saveAndFlush(listing);
    }

    private void booking(UUID guestId, Listing listing, String amount, String status, String createdAt) {
        var details = new Reservation.ReservationDetails(LocalDate.now(), LocalDate.now().plusDays(2), 2, 1,
                new BigDecimal("100000"), new BigDecimal("200000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(amount), "COP", "TEST", 7, 2, new BigDecimal("50"));
        Reservation booking = reservations.saveAndFlush(new Reservation(users.findById(guestId).orElseThrow(), listing,
                UUID.randomUUID().toString(), "a".repeat(64), details, List.of()));
        jdbc.update("update reservations set status = ?, created_at = ? where id = ?", status, Timestamp.from(Instant.parse(createdAt)), booking.getId());
    }

    private record Account(UUID id, String token) { }
}
