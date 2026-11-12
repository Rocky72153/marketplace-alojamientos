package co.edu.unbosque.marketplace.audit;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final StructuredEventLogger logger;

    public AuditService(AuditEventRepository repository, StructuredEventLogger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    public void recordEvent(String eventType, UUID actorId, String entityType, UUID entityId, String result, String reason) {
        AuditEvent event = repository.save(new AuditEvent(eventType, actorId, entityType, entityId, result, reason));
        Runnable emit = () -> logger.event(event.getOccurredAt(), event.getEventType(), event.getActorId(),
                event.getEntityType(), event.getEntityId(), event.getResult(), event.getCorrelationId());
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { emit.run(); }
            });
        } else {
            emit.run();
        }
    }
}
