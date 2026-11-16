package co.edu.unbosque.marketplace.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Durable record for an isolated Stripe test checkout; no key or card data is stored. */
@Entity
@Table(name = "stripe_test_checkout_attempts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stripe_attempt_reservation", columnNames = "reservation_id"),
        @UniqueConstraint(name = "uk_stripe_attempt_session", columnNames = "session_id") })
public class StripeCheckoutAttempt {
    public enum State { PREPARING, OPEN, PAID, EXPIRED, REFUND_PENDING, REFUND_FAILED, REFUND_REVIEW, REFUNDED, CANCELLED }
    @Id private UUID id;
    @Column(name = "reservation_id", nullable = false) private UUID reservationId;
    @Column(name = "guest_id", nullable = false) private UUID guestId;
    @Column(name = "listing_id", nullable = false) private UUID listingId;
    @Column(name = "amount_minor", nullable = false) private long amountMinor;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "session_id", length = 100) private String sessionId;
    @Column(name = "checkout_url", length = 2048) private String checkoutUrl;
    @Column(name = "payment_intent_id", length = 100) private String paymentIntentId;
    @Column(name = "refund_amount_minor") private Long refundAmountMinor;
    @Column(name = "refund_retry_deadline") private Instant refundRetryDeadline;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private State state;

    protected StripeCheckoutAttempt() { }
    public StripeCheckoutAttempt(UUID reservationId, UUID guestId, UUID listingId, long amountMinor, Instant expiresAt) {
        if (amountMinor <= 0) throw new IllegalArgumentException("El importe debe ser positivo.");
        this.id = UUID.randomUUID(); this.reservationId = Objects.requireNonNull(reservationId);
        this.guestId = Objects.requireNonNull(guestId); this.listingId = Objects.requireNonNull(listingId);
        this.amountMinor = amountMinor; this.currency = "cop";
        this.expiresAt = Objects.requireNonNull(expiresAt); this.state = State.PREPARING;
    }
    public UUID getId() { return id; }
    public UUID getReservationId() { return reservationId; }
    public UUID getGuestId() { return guestId; }
    public UUID getListingId() { return listingId; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getSessionId() { return sessionId; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public String getPaymentIntentId() { return paymentIntentId; }
    public Long getRefundAmountMinor() { return refundAmountMinor; }
    public Instant getRefundRetryDeadline() { return refundRetryDeadline; }
    public State getState() { return state; }
    public void opened(String sessionId, String checkoutUrl) {
        if (state != State.PREPARING) throw new IllegalStateException("El intento no está pendiente de apertura.");
        validateSession(sessionId, checkoutUrl);
        this.sessionId = sessionId; this.checkoutUrl = checkoutUrl; this.state = State.OPEN;
    }
    /** Records a recovered session without reopening expired inventory. */
    public void bindExpiredSession(String sessionId, String checkoutUrl) {
        if (state != State.EXPIRED || this.sessionId != null) throw invalid();
        validateSession(sessionId, checkoutUrl);
        this.sessionId = sessionId; this.checkoutUrl = checkoutUrl;
    }
    /** Binds a separately verified provider session without reopening expired inventory.
     * Stripe may no longer return a checkout URL after the session has completed. */
    public void bindVerifiedSession(String sessionId, String checkoutUrl) {
        if ((state != State.PREPARING && state != State.EXPIRED) || this.sessionId != null) throw invalid();
        if (checkoutUrl != null) validateSession(sessionId, checkoutUrl);
        else if (sessionId == null || !sessionId.startsWith("cs_test_") || sessionId.length() > 100) throw invalid();
        this.sessionId = sessionId;
        this.checkoutUrl = checkoutUrl;
        if (state == State.PREPARING) this.state = State.OPEN;
    }
    private static void validateSession(String sessionId, String checkoutUrl) {
        if (sessionId == null || !sessionId.startsWith("cs_test_") || sessionId.length() > 100
                || checkoutUrl == null || checkoutUrl.length() > 2048) throw invalid();
        URI url;
        try { url = URI.create(checkoutUrl); } catch (IllegalArgumentException failure) { throw invalid(); }
        if (!"https".equals(url.getScheme()) || !"checkout.stripe.com".equals(url.getHost())
                || url.getUserInfo() != null || (url.getPort() != -1 && url.getPort() != 443)) throw invalid();
    }
    public void paid(String intent) {
        if (state != State.OPEN) throw invalid();
        requirePaymentIntent(intent);
        this.paymentIntentId = intent; this.state = State.PAID;
    }
    public void expired() {
        if (state != State.OPEN && state != State.PREPARING) throw invalid();
        this.state = State.EXPIRED;
    }
    public void requireRefund(String intent, long amountMinor) {
        if (state != State.PAID && state != State.EXPIRED) throw invalid();
        requirePaymentIntent(intent);
        if ((state == State.PAID && !intent.equals(paymentIntentId)) || amountMinor <= 0
                || amountMinor > this.amountMinor) throw invalid();
        this.paymentIntentId = intent; this.refundAmountMinor = amountMinor; this.state = State.REFUND_PENDING;
        // Stripe may prune idempotency keys after 24 h. End automatic retries
        // earlier than that even if the first provider request is delayed.
        this.refundRetryDeadline = Instant.now().plusSeconds(23 * 3600L);
    }
    public void refunded() {
        if (state != State.REFUND_PENDING) throw invalid();
        this.state = State.REFUNDED;
    }
    public void refundFailed() {
        if (state != State.REFUND_PENDING) throw invalid();
        this.state = State.REFUND_FAILED;
    }
    public void refundNeedsReview() {
        if (state != State.REFUND_PENDING) throw invalid();
        this.state = State.REFUND_REVIEW;
    }
    public void cancelledWithoutRefund() {
        if (state != State.PAID) throw invalid();
        this.state = State.CANCELLED;
    }
    private static void requirePaymentIntent(String intent) {
        if (intent == null || !intent.startsWith("pi_") || intent.length() > 100) throw invalid();
    }
    private static IllegalStateException invalid() {
        return new IllegalStateException("La transición del intento de pago de prueba no es válida.");
    }
}
