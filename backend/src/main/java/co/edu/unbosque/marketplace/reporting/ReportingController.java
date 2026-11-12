package co.edu.unbosque.marketplace.reporting;

import co.edu.unbosque.marketplace.security.MarketplaceUser;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class ReportingController {
    private final DashboardService dashboard;
    private final ReportService reports;

    public ReportingController(DashboardService dashboard, ReportService reports) {
        this.dashboard = dashboard;
        this.reports = reports;
    }

    @GetMapping("/api/admin/dashboard")
    public DashboardService.Dashboard dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboard.snapshot(from, to);
    }

    @GetMapping("/api/admin/reports")
    public ResponseEntity<byte[]> report(@AuthenticationPrincipal MarketplaceUser admin,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        ReportService.Export result = reports.generateWithTextDates(admin, format, from, to);
        String extension = result.format().toLowerCase(java.util.Locale.ROOT);
        String type = "PDF".equals(result.format()) ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("reporte-marketplace." + extension).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(type)).body(result.bytes());
    }
}
