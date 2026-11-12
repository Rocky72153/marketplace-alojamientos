package co.edu.unbosque.marketplace.review;

import co.edu.unbosque.marketplace.booking.Reservation;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(name = "uk_review_booking", columnNames = "booking_id"))
public class Review {
    @Id private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, updatable = false)
    private Reservation reservation;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false, updatable = false)
    private Listing listing;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false, updatable = false)
    private MarketplaceUser guest;
    @Column(nullable = false) private int rating;
    @Column(nullable = false, length = 5000) private String content;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private boolean hidden;
    @Column(name = "hidden_at") private Instant hiddenAt;
    @Column(name = "hidden_by") private UUID hiddenBy;
    @Column(name = "hidden_reason", length = 1000) private String hiddenReason;

    protected Review() { }

    public Review(Reservation reservation, int rating, String content) {
        this.id = UUID.randomUUID();
        this.reservation = reservation;
        this.listing = reservation.getListing();
        this.guest = reservation.getGuest();
        this.rating = rating;
        this.content = content.trim();
        this.createdAt = Instant.now();
    }

    public void hide(UUID actorId, String reason) {
        this.hidden = true;
        this.hiddenAt = Instant.now();
        this.hiddenBy = actorId;
        this.hiddenReason = reason.trim();
    }

    public UUID getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public Listing getListing() { return listing; }
    public MarketplaceUser getGuest() { return guest; }
    public int getRating() { return rating; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isHidden() { return hidden; }
    public Instant getHiddenAt() { return hiddenAt; }
    public UUID getHiddenBy() { return hiddenBy; }
    public String getHiddenReason() { return hiddenReason; }
}
