package co.edu.unbosque.marketplace.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "entity_type", length = 80)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(length = 1000)
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(String eventType, UUID actorId, String entityType, UUID entityId, String result, String reason) {
        this.id = UUID.randomUUID();
        this.eventType = eventType;
        this.actorId = actorId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.result = result;
        this.reason = reason;
        this.occurredAt = Instant.now();
    }

}
