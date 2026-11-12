package co.edu.unbosque.marketplace.notification;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.booking.Reservation;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.listing.ListingStatus;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) { this.notifications = notifications; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reservationConfirmed(Reservation reservation) {
        Listing listing = reservation.getListing();
        record(reservation.getGuest(), "RESERVATION_CONFIRMED",
                "Tu reserva de «" + listing.getTitle() + "» fue confirmada.", "RESERVATION", reservation.getId());
        record(listing.getHost(), "RESERVATION_CONFIRMED",
                "Recibiste una reserva confirmada para «" + listing.getTitle() + "».", "RESERVATION", reservation.getId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reservationCancelled(Reservation reservation) {
        Listing listing = reservation.getListing();
        record(reservation.getGuest(), "RESERVATION_CANCELLED",
                "Tu reserva de «" + listing.getTitle() + "» fue cancelada.", "RESERVATION", reservation.getId());
        record(listing.getHost(), "RESERVATION_CANCELLED",
                "Se canceló una reserva de «" + listing.getTitle() + "».", "RESERVATION", reservation.getId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void listingStateChanged(Listing listing, ListingStatus previous, ListingStatus current) {
        if (previous == current) return;
        record(listing.getHost(), "LISTING_STATE_CHANGED",
                "Tu publicación «" + listing.getTitle() + "» cambió a " + statusLabel(current) + ".",
                "LISTING", listing.getId());
    }

    @Transactional(readOnly = true)
    public List<NotificationView> list(MarketplaceUser principal) {
        UUID recipientId = requireRecipient(principal);
        return notifications.findByRecipient_IdOrderByCreatedAtDesc(recipientId).stream()
                .map(NotificationView::from).toList();
    }

    @Transactional(readOnly = true)
    public NotificationView get(MarketplaceUser principal, UUID id) {
        return NotificationView.from(findOwn(principal, id));
    }

    @Transactional
    public NotificationView markRead(MarketplaceUser principal, UUID id) {
        Notification notification = findOwn(principal, id);
        notification.markRead();
        return NotificationView.from(notification);
    }

    private Notification findOwn(MarketplaceUser principal, UUID id) {
        return notifications.findByIdAndRecipient_Id(id, requireRecipient(principal))
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la notificación."));
    }

    private UUID requireRecipient(MarketplaceUser principal) {
        if (principal == null || principal.getId() == null) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Debes iniciar sesión.");
        }
        return principal.getId();
    }

    private void record(MarketplaceUser recipient, String type, String message, String referenceType, UUID referenceId) {
        notifications.save(new Notification(recipient, type, message, referenceType, referenceId));
    }

    private String statusLabel(ListingStatus status) {
        return switch (status) {
            case DRAFT -> "borrador";
            case PENDING_REVIEW -> "pendiente de revisión";
            case ACTIVE -> "activa";
            case PAUSED -> "pausada";
            case BLOCKED -> "bloqueada";
        };
    }

    public record NotificationView(UUID id, String type, String message, String referenceType,
            UUID referenceId, Instant createdAt, Instant readAt) {
        static NotificationView from(Notification notification) {
            return new NotificationView(notification.getId(), notification.getType(), notification.getMessage(),
                    notification.getReferenceType(), notification.getReferenceId(),
                    notification.getCreatedAt(), notification.getReadAt());
        }
    }
}
