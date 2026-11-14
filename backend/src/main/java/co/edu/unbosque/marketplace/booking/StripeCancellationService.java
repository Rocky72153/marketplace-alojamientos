package co.edu.unbosque.marketplace.booking;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.listing.ListingService;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.notification.NotificationService;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "marketplace.payment.mode", havingValue = "STRIPE_TEST")
public class StripeCancellationService {
    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private final StripeCheckoutAttemptRepository attempts;
    private final ReservationRepository reservations;
    private final MarketplaceUserRepository users;
    private final ListingService inventory;
    private final ListingRepository listings;
    private final AuditService audit;
    private final NotificationService notifications;

    public StripeCancellationService(StripeCheckoutAttemptRepository attempts, ReservationRepository reservations,
            MarketplaceUserRepository users, ListingService inventory, ListingRepository listings, AuditService audit, NotificationService notifications) {
        this.attempts = attempts; this.reservations = reservations; this.users = users;
        this.inventory = inventory; this.listings = listings; this.audit = audit; this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public boolean hasAttempt(UUID reservationId) { return attempts.findByReservationId(reservationId).isPresent(); }

    @Transactional
    public ReservationService.ReservationView cancel(MarketplaceUser guest, UUID reservationId,
            ReservationService.CancelRequest request) {
        if (guest == null || guest.getRole() != AccountRole.GUEST) throw forbidden();
        var found = attempts.findByReservationId(reservationId).orElseThrow(this::notFound);
        StripeCheckoutAttempt attempt = attempts.lockById(found.getId()).orElseThrow(this::notFound);
        if (!guest.getId().equals(attempt.getGuestId())) throw notFound();
        MarketplaceUser lockedGuest = users.lockForReservationById(guest.getId()).orElseThrow(this::forbidden);
        if (!lockedGuest.isEnabled() || !lockedGuest.isAccountNonLocked()) throw forbidden();
        // Listing state and availability operations lock the listing before reservations.
        // Use the same order as moderation to avoid a reservation/listing lock cycle.
        listings.lockById(attempt.getListingId()).orElseThrow(this::notFound);
        Reservation reservation = reservations.lockById(reservationId).orElseThrow(this::notFound);
        if (attempt.getState() != StripeCheckoutAttempt.State.PAID || reservation.getStatus() != ReservationStatus.CONFIRMED)
            throw conflict();
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZONE);
        if (!reservation.getCheckIn().isAfter(today)) throw conflict();
        long days = ChronoUnit.DAYS.between(today, reservation.getCheckIn());
        long hours = ChronoUnit.HOURS.between(now, reservation.getCheckIn().atStartOfDay(ZONE).toInstant());
        BigDecimal pct = days >= reservation.getPolicyFreeRefundDays() ? new BigDecimal("100.00")
                : hours >= reservation.getPolicyPartialRefundDays() * 24L ? reservation.getPolicyPartialRefundPercentage()
                : BigDecimal.ZERO.setScale(2);
        BigDecimal amount = reservation.getTotal().multiply(pct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        long minor = amount.movePointRight(2).longValueExact();
        inventory.releaseReservation(attempt.getListingId(), reservationId);
        reservation.cancelStripeTest(request.reason().trim(), now, pct, amount);
        if (minor > 0) attempt.requireRefund(attempt.getPaymentIntentId(), minor);
        else attempt.cancelledWithoutRefund();
        audit.recordEvent("STRIPE_TEST_RESERVATION_CANCELLED", guest.getId(), "RESERVATION", reservationId, "SUCCESS", null);
        notifications.reservationCancelled(reservation);
        return ReservationService.ReservationView.from(reservation, false);
    }

    private DomainException notFound() { return new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la reserva."); }
    private DomainException forbidden() { return new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Sesión no permitida."); }
    private DomainException conflict() { return new DomainException(HttpStatus.CONFLICT, "CONFLICT", "La reserva ya no se puede cancelar."); }
}
