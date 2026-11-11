package co.edu.unbosque.marketplace.audit;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    public void recordEvent(String eventType, UUID actorId, String entityType, UUID entityId, String result, String reason) {
        repository.save(new AuditEvent(eventType, actorId, entityType, entityId, result, reason));
    }
}
