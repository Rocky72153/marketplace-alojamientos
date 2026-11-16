package co.edu.unbosque.marketplace.booking;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.listing.ListingService;
import co.edu.unbosque.marketplace.listing.ListingStatus;
import co.edu.unbosque.marketplace.notification.NotificationService;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Database-only operations: Stripe network requests happen outside these transactions. */
@Service
@ConditionalOnProperty(name = "marketplace.payment.mode", havingValue = "STRIPE_TEST")
public class StripeCheckoutTransactions {
    private final StripeCheckoutAttemptRepository attempts;
    private final ReservationRepository reservations;
    private final ReservationService booking;
    private final ListingService listingService;
    private final ListingRepository listings;
    private final MarketplaceUserRepository users;
    private final AuditService audit;
    private final NotificationService notifications;

    public StripeCheckoutTransactions(StripeCheckoutAttemptRepository attempts, ReservationRepository reservations,
            ReservationService booking, ListingService listingService, ListingRepository listings,
            MarketplaceUserRepository users, AuditService audit, NotificationService notifications) {
        this.attempts = attempts; this.reservations = reservations; this.booking = booking;
        this.listingService = listingService; this.listings = listings; this.users = users;
        this.audit = audit; this.notifications = notifications;
    }

    @Transactional
    public StripeCheckoutAttempt prepare(MarketplaceUser principal, ReservationService.ReservationRequest request, String rawKey) {
        if (principal == null || principal.getRole() != AccountRole.GUEST) throw forbidden();
        String key = rawKey == null ? "" : rawKey.trim();
        if (key.isBlank() || key.length() > 100) throw badRequest("Se requiere una clave de idempotencia válida.");
        MarketplaceUser guest = users.lockForReservationById(principal.getId()).orElseThrow(this::forbidden);
        if (!guest.isEnabled() || !guest.isAccountNonLocked()) throw forbidden();
        String fingerprint = booking.fingerprint(request);
        Reservation previous = reservations.findByGuest_IdAndIdempotencyKey(guest.getId(), key).orElse(null);
        if (previous != null) {
            if (!previous.getRequestFingerprint().equals(fingerprint)) throw conflict("La clave ya se usó con otros datos.");
            return attempts.findByReservationId(previous.getId()).orElseThrow(() -> conflict("La clave pertenece a otra reserva."));
        }
        Listing listing = listingService.lockActiveListing(request.listingId());
        ReservationService.QuoteView quote = booking.quote(listing.getId(), request.toQuoteRequest());
        long amount = minorUnits(quote.total());
        var details = new Reservation.ReservationDetails(quote.checkIn(), quote.checkOut(), quote.nights(), quote.guests(),
                quote.pricePerNight(), quote.nightlySubtotal(), quote.cleaningFee(), quote.serviceFee(), quote.discountAmount(),
                quote.extrasTotal(), quote.total(), quote.currency(), "STRIPE-TEST-PENDING", quote.freeRefundDays(),
                quote.partialRefundDays(), quote.partialRefundPercentage());
        Reservation reservation = new Reservation(guest, listing, key, fingerprint, details,
                quote.extras().stream().map(ReservationService.ExtraLine::toReservationExtra).toList());
        reservation.awaitStripeTestPayment();
        reservation = reservations.saveAndFlush(reservation);
        listingService.confirmReservation(listing.getId(), quote.checkIn(), quote.checkOut(), reservation.getId().toString());
        StripeCheckoutAttempt attempt = new StripeCheckoutAttempt(reservation.getId(), guest.getId(), listing.getId(),
                amount, Instant.now().plusSeconds(3600));
        attempts.saveAndFlush(attempt);
        audit.recordEvent("STRIPE_TEST_CHECKOUT_PREPARED", guest.getId(), "RESERVATION", reservation.getId(), "SUCCESS", null);
        return attempt;
    }

    @Transactional(readOnly = true)
    public StripeCheckoutAttempt get(UUID id, MarketplaceUser guest) {
        if (guest == null || guest.getRole() != AccountRole.GUEST) throw forbidden();
        return attempts.findByIdAndGuestId(id, guest.getId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró el pago de prueba."));
    }

    @Transactional
    public StripeCheckoutAttempt bind(UUID attemptId, StripeTestGateway.CheckoutSession session) {
        StripeCheckoutAttempt attempt = attempts.lockById(attemptId).orElseThrow();
        if (attempt.getState() == StripeCheckoutAttempt.State.PREPARING) attempt.opened(session.id(), session.url());
        else if (attempt.getState() == StripeCheckoutAttempt.State.EXPIRED && attempt.getSessionId() == null)
            attempt.bindExpiredSession(session.id(), session.url());
        else if (attempt.getSessionId() == null) throw conflict("No se puede asociar la sesión al intento.");
        else if (attempt.getSessionId() != null && !attempt.getSessionId().equals(session.id()))
            throw conflict("La sesión de prueba no coincide con el intento.");
        return attempt;
    }

    @Transactional(readOnly = true)
    public StripeCheckoutAttempt getForVerifiedEvent(UUID attemptId) {
        return attempts.findById(attemptId).orElseThrow(() -> new DomainException(
                HttpStatus.SERVICE_UNAVAILABLE, "STRIPE_TEST_RETRY", "Evento pendiente de conciliación."));
    }

    @Transactional
    public void bindVerified(UUID attemptId, StripeTestGateway.CheckoutSession session) {
        StripeCheckoutAttempt attempt = attempts.lockById(attemptId).orElseThrow();
        if (attempt.getSessionId() == null) attempt.bindVerifiedSession(session.id(), session.url());
        else if (!attempt.getSessionId().equals(session.id())) throw conflict("La sesión de prueba no coincide con el intento.");
    }

    private long minorUnits(BigDecimal total) {
        try {
            long value = total.movePointRight(2).longValueExact();
            if (value < 50 || value > 9_999_999_999L) throw badRequest("El importe está fuera del rango de pago de prueba.");
            return value;
        } catch (ArithmeticException invalid) { throw badRequest("El importe de pago no es válido."); }
    }
    private DomainException forbidden() { return new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "La sesión no puede iniciar el pago."); }
    private DomainException conflict(String message) { return new DomainException(HttpStatus.CONFLICT, "CONFLICT", message); }
    private DomainException badRequest(String message) { return new DomainException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message); }
}
