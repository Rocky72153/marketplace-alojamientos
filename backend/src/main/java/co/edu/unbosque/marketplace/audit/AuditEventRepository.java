package co.edu.unbosque.marketplace.audit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    long countByEventType(String eventType);
    AuditEvent findFirstByEventTypeOrderByOccurredAtDesc(String eventType);
}
