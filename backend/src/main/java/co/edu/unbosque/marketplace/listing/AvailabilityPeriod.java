package co.edu.unbosque.marketplace.listing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "availability_periods")
public class AvailabilityPeriod {
    @Id private UUID id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AvailabilityType type;
    @Column(name = "reservation_reference", length = 100) private String reservationReference;

    protected AvailabilityPeriod() { }
    public AvailabilityPeriod(Listing listing, LocalDate startDate, LocalDate endDate, AvailabilityType type, String reservationReference) {
        this.id = UUID.randomUUID(); this.listing = listing; this.startDate = startDate; this.endDate = endDate;
        this.type = type; this.reservationReference = reservationReference;
    }
    public UUID getId() { return id; }
    public Listing getListing() { return listing; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public AvailabilityType getType() { return type; }
    public String getReservationReference() { return reservationReference; }
    public boolean overlaps(LocalDate start, LocalDate end) { return startDate.isBefore(end) && endDate.isAfter(start); }
}
