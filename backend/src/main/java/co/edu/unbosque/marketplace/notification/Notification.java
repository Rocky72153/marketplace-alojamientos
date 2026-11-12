package co.edu.unbosque.marketplace.notification;

import co.edu.unbosque.marketplace.security.MarketplaceUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notification_recipient_created",
        columnList = "recipient_id,created_at"))
public class Notification {
    @Id
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false, updatable = false)
    private MarketplaceUser recipient;

    @Column(nullable = false, length = 40, updatable = false)
    private String type;

    @Column(nullable = false, length = 500, updatable = false)
    private String message;

    @Column(name = "reference_type", nullable = false, length = 20, updatable = false)
    private String referenceType;

    @Column(name = "reference_id", nullable = false, updatable = false)
    private UUID referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() { }

    Notification(MarketplaceUser recipient, String type, String message, String referenceType, UUID referenceId) {
        this.id = UUID.randomUUID();
        this.recipient = recipient;
        this.type = type;
        this.message = message;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    @PrePersist
    void beforeInsert() { if (createdAt == null) createdAt = Instant.now(); }

    void markRead() { if (readAt == null) readAt = Instant.now().truncatedTo(ChronoUnit.MICROS); }

    public UUID getId() { return id; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getReferenceType() { return referenceType; }
    public UUID getReferenceId() { return referenceId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
}
