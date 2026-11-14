package co.edu.unbosque.marketplace.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "stripe_test_processed_events")
public class StripeProcessedEvent {
    @Id @Column(length = 100) private String id;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;
    protected StripeProcessedEvent() { }
    public StripeProcessedEvent(String id) { this.id = id; this.processedAt = Instant.now(); }
}
