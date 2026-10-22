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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
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
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Listing() { }

    public Listing(MarketplaceUser host, String title, String description, String city, String location, String type,
            int capacity, BigDecimal pricePerNight, Set<String> amenityIds, String rules, Set<String> imageReferences) {
        this.id = UUID.randomUUID();
        this.host = host;
        this.status = ListingStatus.DRAFT;
        update(title, description, city, location, type, capacity, pricePerNight, amenityIds, rules, imageReferences);
    }

    public void update(String title, String description, String city, String location, String type, int capacity,
            BigDecimal pricePerNight, Set<String> amenityIds, String rules, Set<String> imageReferences) {
        this.title = title.trim(); this.description = description.trim(); this.city = city.trim();
        this.location = location.trim(); this.type = type.trim(); this.capacity = capacity; this.pricePerNight = pricePerNight;
        this.amenityIds = amenityIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(amenityIds);
        this.rules = rules == null ? null : rules.trim();
        this.imageReferences = imageReferences == null ? new LinkedHashSet<>() : new LinkedHashSet<>(imageReferences);
    }

    public void setStatus(ListingStatus status) { this.status = status; }
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
}
