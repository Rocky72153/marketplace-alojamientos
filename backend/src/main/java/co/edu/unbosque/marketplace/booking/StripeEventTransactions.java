package co.edu.unbosque.marketplace.booking;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.listing.ListingService;
import co.edu.unbosque.marketplace.listing.ListingStatus;
import co.edu.unbosque.marketplace.notification.NotificationService;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies verified events and inventory changes in the same database transaction. */
@Service
@ConditionalOnProperty(name = "marketplace.payment.mode", havingValue = "STRIPE_TEST")
public class StripeEventTransactions {
    private final StripeCheckoutAttemptRepository attempts;
    private final StripeProcessedEventRepository processed;
    private final ReservationRepository reservations;
    private final ListingRepository listings;
    private final ListingService inventory;
    private final MarketplaceUserRepository users;
    private final AuditService audit;
    private final NotificationService notifications;

    public StripeEventTransactions(StripeCheckoutAttemptRepository attempts, StripeProcessedEventRepository processed,
            ReservationRepository reservations, ListingRepository listings, ListingService inventory,
            MarketplaceUserRepository users, AuditService audit, NotificationService notifications) {
        this.attempts = attempts; this.processed = processed; this.reservations = reservations;
        this.listings = listings; this.inventory = inventory; this.users = users;
        this.audit = audit; this.notifications = notifications;
    }

    @Transactional
    public void process(StripeTestGateway.VerifiedEvent event) {
        if (event.attemptId() == null) return;
        StripeCheckoutAttempt attempt = attempts.lockById(event.attemptId()).orElseThrow(this::retry);
        if (processed.existsById(event.eventId())) return;
        StripeCheckoutEventDecision.Action action;
        try { action = StripeCheckoutEventDecision.classify(attempt, event); }
        catch (IllegalArgumentException ex) { throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_WEBHOOK", "Evento de pago inválido."); }
        if (action == StripeCheckoutEventDecision.Action.RETRY) throw retry();
        if (action == StripeCheckoutEventDecision.Action.CONFIRM) {
            MarketplaceUser guest = users.lockForReservationById(attempt.getGuestId()).orElseThrow(this::retry);
            Listing listing = listings.lockById(attempt.getListingId()).orElseThrow(this::retry);
            MarketplaceUser host = users.lockForReservationById(listing.getHost().getId()).orElseThrow(this::retry);
            Reservation reservation = reservations.lockById(attempt.getReservationId()).orElseThrow(this::retry);
            if (reservation.getStatus() != ReservationStatus.PENDING_PAYMENT) throw retry();
            if (!Instant.now().isBefore(attempt.getExpiresAt()) || !guest.isEnabled() || !guest.isAccountNonLocked()
                    || listing.getStatus() != ListingStatus.ACTIVE || !host.isEnabled() || !host.isAccountNonLocked()) {
                inventory.releaseReservation(attempt.getListingId(), attempt.getReservationId());
                reservation.expireStripeTestPayment();
                attempt.expired();
                attempt.requireRefund(event.paymentIntentId(), attempt.getAmountMinor());
                audit.recordEvent("STRIPE_TEST_REFUND_REQUIRED", attempt.getGuestId(), "RESERVATION", attempt.getReservationId(), "SUCCESS", null);
            } else {
                reservation.confirmStripeTestPayment(event.paymentIntentId());
                attempt.paid(event.paymentIntentId());
                notifications.reservationConfirmed(reservation);
                audit.recordEvent("STRIPE_TEST_PAYMENT_CONFIRMED", attempt.getGuestId(), "RESERVATION", attempt.getReservationId(), "SUCCESS", null);
            }
        } else if (action == StripeCheckoutEventDecision.Action.EXPIRE) {
            expireLocked(attempt);
        } else if (action == StripeCheckoutEventDecision.Action.REFUND_LATE) {
            attempt.requireRefund(event.paymentIntentId(), attempt.getAmountMinor());
        }
        processed.save(new StripeProcessedEvent(event.eventId()));
    }

    @Transactional
    public void expireIfDue(UUID attemptId, Instant now) {
        StripeCheckoutAttempt attempt = attempts.lockById(attemptId).orElseThrow(this::retry);
        if ((attempt.getState() == StripeCheckoutAttempt.State.PREPARING || attempt.getState() == StripeCheckoutAttempt.State.OPEN)
                && !attempt.getExpiresAt().isAfter(now)) expireLocked(attempt);
    }

    private void expireLocked(StripeCheckoutAttempt attempt) {
        listings.lockById(attempt.getListingId()).orElseThrow(this::retry);
        Reservation reservation = reservations.lockById(attempt.getReservationId()).orElseThrow(this::retry);
        if (reservation.getStatus() != ReservationStatus.PENDING_PAYMENT) throw retry();
        inventory.releaseReservation(attempt.getListingId(), attempt.getReservationId());
        reservation.expireStripeTestPayment();
        // No session ID means an earlier response might have been lost. If no
        // event arrives while the CLI is online, a human must reconcile it.
        if (attempt.getSessionId() == null) reservation.requireManualReview();
        attempt.expired();
    }

    @Transactional
    public void finishRefund(UUID id) {
        StripeCheckoutAttempt attempt = attempts.lockById(id).orElseThrow(this::retry);
        if (attempt.getState() == StripeCheckoutAttempt.State.REFUND_PENDING) {
            Reservation reservation = reservations.lockById(attempt.getReservationId()).orElseThrow(this::retry);
            reservation.stripeTestRefunded();
            attempt.refunded();
        }
    }

    @Transactional
    public void failRefund(UUID id) {
        StripeCheckoutAttempt attempt = attempts.lockById(id).orElseThrow(this::retry);
        if (attempt.getState() == StripeCheckoutAttempt.State.REFUND_PENDING) {
            Reservation reservation = reservations.lockById(attempt.getReservationId()).orElseThrow(this::retry);
            reservation.stripeTestRefundFailed();
            attempt.refundFailed();
            audit.recordEvent("STRIPE_TEST_REFUND_FAILED", attempt.getGuestId(), "RESERVATION", attempt.getReservationId(), "FAILURE", null);
        }
    }

    @Transactional
    public void markRefundUncertain(UUID id) {
        StripeCheckoutAttempt attempt = attempts.lockById(id).orElseThrow(this::retry);
        if (attempt.getState() == StripeCheckoutAttempt.State.REFUND_PENDING) {
            Reservation reservation = reservations.lockById(attempt.getReservationId()).orElseThrow(this::retry);
            reservation.stripeTestRefundUncertain();
            attempt.refundNeedsReview();
            audit.recordEvent("STRIPE_TEST_REFUND_REVIEW", attempt.getGuestId(), "RESERVATION", attempt.getReservationId(), "PENDING", null);
        }
    }

    private DomainException retry() {
        return new DomainException(HttpStatus.SERVICE_UNAVAILABLE, "STRIPE_TEST_RETRY", "Evento pendiente de conciliación.");
    }
}
