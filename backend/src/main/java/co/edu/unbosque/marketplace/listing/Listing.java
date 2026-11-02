package co.edu.unbosque.marketplace.listing;

import co.edu.unbosque.marketplace.security.MarketplaceUser;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "listings")
public class Listing {
    @Id private UUID id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private MarketplaceUser host;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, length = 4000) private String description;
    @Column(nullable = false, length = 120) private String city;
    @Column(nullable = false, length = 240) private String location;
    @Column(name = "listing_type", nullable = false, length = 50) private String type;
    @Column(nullable = false) private int capacity;
    @Column(name = "price_per_night", nullable = false, precision = 12, scale = 2) private BigDecimal pricePerNight;
    @Column(length = 2000) private String rules;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private ListingStatus status;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_amenities", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "amenity_id", nullable = false, length = 80)
    private Set<String> amenityIds = new LinkedHashSet<>();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_images", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "image_reference", nullable = false, length = 1000)
    private Set<String> imageReferences = new LinkedHashSet<>();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_extra_options", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "extra_position")
    private List<ListingExtraOption> extraOptions = new ArrayList<>();
    @Column(name = "cancellation_free_refund_days", nullable = false, columnDefinition = "integer default 7")
    private int cancellationFreeRefundDays = 7;
    @Column(name = "cancellation_partial_refund_days", nullable = false, columnDefinition = "integer default 2")
    private int cancellationPartialRefundDays = 2;
    @Column(name = "cancellation_partial_refund_percentage", nullable = false, precision = 5, scale = 2,
            columnDefinition = "decimal(5,2) default 50.00")
    private BigDecimal cancellationPartialRefundPercentage = new BigDecimal("50.00");
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Listing() { }

    public Listing(MarketplaceUser host, ListingData details) {
        this.id = UUID.randomUUID();
        this.host = host;
        this.status = ListingStatus.DRAFT;
        update(details);
    }

    public void update(ListingData details) {
        this.title = details.title().trim(); this.description = details.description().trim(); this.city = details.city().trim();
        this.location = details.location().trim(); this.type = details.type().trim(); this.capacity = details.capacity();
        this.pricePerNight = details.pricePerNight();
        this.amenityIds = details.amenityIds() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(details.amenityIds());
        this.rules = details.rules() == null ? null : details.rules().trim();
        this.imageReferences = details.imageReferences() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(details.imageReferences());
    }

    public void setStatus(ListingStatus status) { this.status = status; }
    public void setExtraOptions(List<ListingExtraOption> extraOptions) {
        this.extraOptions = extraOptions == null ? new ArrayList<>() : new ArrayList<>(extraOptions);
    }
    public void setCancellationPolicy(int freeRefundDays, int partialRefundDays, BigDecimal partialRefundPercentage) {
        this.cancellationFreeRefundDays = freeRefundDays;
        this.cancellationPartialRefundDays = partialRefundDays;
        this.cancellationPartialRefundPercentage = partialRefundPercentage;
    }
    @PrePersist void beforeInsert() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void beforeUpdate() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public MarketplaceUser getHost() { return host; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCity() { return city; }
    public String getLocation() { return location; }
    public String getType() { return type; }
    public int getCapacity() { return capacity; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public String getRules() { return rules; }
    public ListingStatus getStatus() { return status; }
    public Set<String> getAmenityIds() { return Set.copyOf(amenityIds); }
    public Set<String> getImageReferences() { return Set.copyOf(imageReferences); }
    public List<ListingExtraOption> getExtraOptions() { return List.copyOf(extraOptions); }
    public int getCancellationFreeRefundDays() { return cancellationFreeRefundDays; }
    public int getCancellationPartialRefundDays() { return cancellationPartialRefundDays; }
    public BigDecimal getCancellationPartialRefundPercentage() { return cancellationPartialRefundPercentage; }
}
