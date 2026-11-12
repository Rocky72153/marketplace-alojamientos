package co.edu.unbosque.marketplace.reporting;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private static final Logger LOG = LoggerFactory.getLogger(ReportService.class);
    private final DashboardService dashboard;
    private final ReportAudit audit;

    public ReportService(DashboardService dashboard, ReportAudit audit) {
        this.dashboard = dashboard;
        this.audit = audit;
    }

    public Export generateWithTextDates(MarketplaceUser admin, String format, String from, String to) {
        LocalDate start;
        LocalDate end;
        try {
            start = from == null ? null : LocalDate.parse(from);
            end = to == null ? null : LocalDate.parse(to);
        } catch (DateTimeParseException exception) {
            audit.record(admin.getId(), "FAILURE", "Fecha inválida para exportación");
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "Las fechas deben tener formato AAAA-MM-DD válido.");
        }
        return generate(admin, format, start, end);
    }

    public Export generate(MarketplaceUser admin, String format, LocalDate from, LocalDate to) {
        if (!"PDF".equals(format) && !"XLSX".equals(format)) {
            audit.record(admin.getId(), "FAILURE", "Formato de reporte no admitido");
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_REPORT_FORMAT", "El formato debe ser PDF o XLSX.");
        }
        try {
            DashboardService.Dashboard snapshot = dashboard.snapshot(from, to);
            List<Line> lines = lines(snapshot);
            byte[] bytes = "PDF".equals(format) ? pdf(lines) : xlsx(lines);
            audit.record(admin.getId(), "SUCCESS", "formato=" + format + "; desde=" + from + "; hasta=" + to);
            return new Export(bytes, format);
        } catch (DomainException exception) {
            audit.record(admin.getId(), "FAILURE", "Parámetros inválidos para exportación");
            throw exception;
        } catch (IOException | RuntimeException exception) {
            LOG.error("Falló la generación del reporte administrativo: {}", exception.getClass().getSimpleName());
            audit.record(admin.getId(), "FAILURE", "Error al generar exportación");
            throw new DomainException(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT_FAILED", "No fue posible generar el reporte.");
        }
    }

    private List<Line> lines(DashboardService.Dashboard snapshot) {
        List<Line> result = new ArrayList<>();
        result.add(new Line("Marketplace - reporte administrativo", ""));
        result.add(new Line("Corte", snapshot.generatedAt().toString()));
        result.add(new Line("Zona horaria", snapshot.timezone()));
        result.add(new Line("Desde", snapshot.from() == null ? "Sin límite" : snapshot.from().toString()));
        result.add(new Line("Hasta", snapshot.to() == null ? "Sin límite" : snapshot.to().toString()));
        result.add(new Line("Usuarios", Long.toString(snapshot.users())));
        result.add(new Line("Publicaciones activas", Long.toString(snapshot.activeListings())));
        snapshot.bookingsByStatus().forEach((status, count) -> result.add(new Line("Reservas " + status, count.toString())));
        result.add(new Line("Ingresos estimados - pagos simulados", snapshot.estimatedRevenue().toPlainString() + " " + snapshot.currency()));
        result.add(new Line("Ciudades con mayor demanda", "Reservas confirmadas/completadas"));
        snapshot.topCities().forEach(city -> result.add(new Line(city.city(), Long.toString(city.bookings()))));
        result.add(new Line("Definición usuarios", snapshot.definitions().users()));
        result.add(new Line("Definición publicaciones", snapshot.definitions().activeListings()));
        result.add(new Line("Definición reservas", snapshot.definitions().bookings()));
        result.add(new Line("Definición ingresos", snapshot.definitions().estimatedRevenue()));
        result.add(new Line("Definición demanda", snapshot.definitions().topCities()));
        return result;
    }

    private byte[] xlsx(List<Line> lines) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Resumen");
            int index = 0;
            for (Line line : lines) {
                Row row = sheet.createRow(index++);
                row.createCell(0).setCellValue(line.label());
                row.createCell(1).setCellValue(line.value());
            }
            sheet.setColumnWidth(0, 44 * 256);
            sheet.setColumnWidth(1, 90 * 256);
            workbook.write(bytes);
            return bytes.toByteArray();
        }
    }

    private byte[] pdf(List<Line> lines) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            List<String> paragraphs = new ArrayList<>();
            for (Line line : lines) {
                String value = supported(font, line.label() + (line.value().isEmpty() ? "" : ": " + line.value()));
                while (value.length() > 80) {
                    paragraphs.add(value.substring(0, 80));
                    value = value.substring(80);
                }
                paragraphs.add(value);
            }
            for (int start = 0; start < paragraphs.size(); start += 48) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(font, 10);
                    stream.setLeading(14);
                    stream.newLineAtOffset(40, 750);
                    for (String paragraph : paragraphs.subList(start, Math.min(start + 48, paragraphs.size()))) {
                        stream.showText(paragraph);
                        stream.newLine();
                    }
                    stream.endText();
                }
            }
            document.save(bytes);
            return bytes.toByteArray();
        }
    }

    private String supported(PDType1Font font, String text) {
        StringBuilder result = new StringBuilder();
        text.codePoints().forEach(codePoint -> {
            String character = new String(Character.toChars(codePoint));
            try { font.encode(character); result.append(character); }
            catch (IllegalArgumentException | IOException unsupported) { result.append('?'); }
        });
        return result.toString();
    }

    private record Line(String label, String value) { }
    public static final class Export {
        private final byte[] bytes;
        private final String format;

        Export(byte[] bytes, String format) {
            this.bytes = bytes.clone();
            this.format = format;
        }

        public byte[] bytes() { return bytes.clone(); }
        public String format() { return format; }
    }
}
