package co.edu.unbosque.marketplace.reporting;

import co.edu.unbosque.marketplace.audit.AuditService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportAudit {
    private final AuditService audit;
    public ReportAudit(AuditService audit) { this.audit = audit; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID adminId, String result, String details) {
        audit.recordEvent("REPORT_EXPORT", adminId, "REPORT", null, result, details);
    }
}
