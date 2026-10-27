package co.edu.unbosque.marketplace.booking;

import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservations", uniqueConstraints = @UniqueConstraint(
        name = "uk_reservation_guest_idempotency", columnNames = {"guest_id", "idempotency_key"}))
public class Reservation {
    @Id
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private MarketplaceUser guest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private int nights;

    @Column(nullable = false)
    private int guests;

    @Column(name = "price_per_night", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerNight;

    @Column(name = "nightly_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal nightlySubtotal;

    @Column(name = "cleaning_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal cleaningFee;

    @Column(name = "service_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal serviceFee;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "extras_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal extrasTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_status", nullable = false, length = 32)
    private String paymentStatus;

    @Column(name = "payment_reference", nullable = false, length = 100)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReservationStatus status;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "refund_percentage", precision = 5, scale = 2)
    private BigDecimal refundPercentage;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "policy_free_refund_days", nullable = false)
    private int policyFreeRefundDays;

    @Column(name = "policy_partial_refund_days", nullable = false)
    private int policyPartialRefundDays;

    @Column(name = "policy_partial_refund_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal policyPartialRefundPercentage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reservation_extras", joinColumns = @JoinColumn(name = "reservation_id"))
    @OrderColumn(name = "line_number")
    private List<ReservationExtra> extras = new ArrayList<>();

    protected Reservation() { }

    public Reservation(MarketplaceUser guest, Listing listing, String idempotencyKey, String requestFingerprint, LocalDate checkIn,
            LocalDate checkOut, int nights, int guests, BigDecimal pricePerNight, BigDecimal nightlySubtotal,
            BigDecimal cleaningFee, BigDecimal serviceFee, BigDecimal discountAmount, BigDecimal extrasTotal,
            BigDecimal total, String currency, String paymentReference, int policyFreeRefundDays,
            int policyPartialRefundDays, BigDecimal policyPartialRefundPercentage,
            List<ReservationExtra> extras) {
        this.id = UUID.randomUUID();
        this.guest = guest;
        this.listing = listing;
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.guests = guests;
        this.pricePerNight = pricePerNight;
        this.nightlySubtotal = nightlySubtotal;
        this.cleaningFee = cleaningFee;
        this.serviceFee = serviceFee;
        this.discountAmount = discountAmount;
        this.extrasTotal = extrasTotal;
        this.total = total;
        this.currency = currency;
        this.paymentStatus = "TEST_APPROVED";
        this.paymentReference = paymentReference;
        this.status = ReservationStatus.CONFIRMED;
        this.policyFreeRefundDays = policyFreeRefundDays;
        this.policyPartialRefundDays = policyPartialRefundDays;
        this.policyPartialRefundPercentage = policyPartialRefundPercentage;
        this.extras = new ArrayList<>(extras);
    }

    @PrePersist
    void beforeInsert() { if (createdAt == null) createdAt = Instant.now(); }

    public void cancel(String reason, Instant at, BigDecimal percentage, BigDecimal amount) {
        this.status = ReservationStatus.CANCELLED;
        this.cancellationReason = reason;
        this.cancelledAt = at;
        this.refundPercentage = percentage;
        this.refundAmount = amount;
        this.paymentStatus = amount.signum() > 0 ? "TEST_REFUNDED" : "TEST_NO_REFUND";
    }

    public UUID getId() { return id; }
    public MarketplaceUser getGuest() { return guest; }
    public Listing getListing() { return listing; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getNights() { return nights; }
    public int getGuests() { return guests; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public BigDecimal getNightlySubtotal() { return nightlySubtotal; }
    public BigDecimal getCleaningFee() { return cleaningFee; }
    public BigDecimal getServiceFee() { return serviceFee; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getExtrasTotal() { return extrasTotal; }
    public BigDecimal getTotal() { return total; }
    public String getCurrency() { return currency; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getPaymentReference() { return paymentReference; }
    public ReservationStatus getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }
    public Instant getCancelledAt() { return cancelledAt; }
    public BigDecimal getRefundPercentage() { return refundPercentage; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public int getPolicyFreeRefundDays() { return policyFreeRefundDays; }
    public int getPolicyPartialRefundDays() { return policyPartialRefundDays; }
    public BigDecimal getPolicyPartialRefundPercentage() { return policyPartialRefundPercentage; }
    public Instant getCreatedAt() { return createdAt; }
    public List<ReservationExtra> getExtras() { return List.copyOf(extras); }
}
