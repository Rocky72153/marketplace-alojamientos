package co.edu.unbosque.marketplace.admin;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.audit.AuditEvent;
import co.edu.unbosque.marketplace.audit.AuditEventRepository;
import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.booking.Reservation;
import co.edu.unbosque.marketplace.booking.ReservationRepository;
import co.edu.unbosque.marketplace.booking.ReservationStatus;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.listing.ListingRepository;
import co.edu.unbosque.marketplace.listing.ListingStatus;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.AccountState;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModerationService {
    private final MarketplaceUserRepository users;
    private final AuditEventRepository auditEvents;
    private final AuditService audit;
    private final ReservationRepository reservations;
    private final ListingRepository listings;

    public ModerationService(MarketplaceUserRepository users, AuditEventRepository auditEvents,
            AuditService audit, ReservationRepository reservations, ListingRepository listings) {
        this.users = users;
        this.auditEvents = auditEvents;
        this.audit = audit;
        this.reservations = reservations;
        this.listings = listings;
    }

    @Transactional(readOnly = true)
    public Page<UserView> users(int page, int size) {
        return users.findAll(paging(page, size, "name")).map(UserView::from);
    }

    @Transactional(readOnly = true)
    public Page<ListingView> listings(int page, int size) {
        return listings.findAll(paging(page, size, "createdAt")).map(ListingView::from);
    }

    @Transactional
    public UserView restrict(MarketplaceUser admin, UUID id, RestrictRequest request) {
        if (admin.getId().equals(id)) {
            throw new DomainException(HttpStatus.CONFLICT, "SELF_RESTRICTION", "No puedes restringir tu propia cuenta.");
        }
        MarketplaceUser target = users.lockForReservationById(id).orElseThrow(() ->
                new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la cuenta."));
        if (target.getAccountState() != AccountState.ACTIVE) {
            throw new DomainException(HttpStatus.CONFLICT, "ACCOUNT_NOT_ACTIVE", "La cuenta no está activa.");
        }
        target.restrict();
        audit.recordEvent("ACCOUNT_RESTRICTED", admin.getId(), "USER", id, "SUCCESS", request.reason().trim());
        return UserView.from(target);
    }

    @Transactional(readOnly = true)
    public Page<AuditView> audit(int page, int size) {
        return auditEvents.findAll(paging(page, size, "occurredAt")).map(AuditView::from);
    }

    @Transactional(readOnly = true)
    public AuditView auditEvent(UUID id) {
        return auditEvents.findById(id).map(AuditView::from).orElseThrow(() ->
                new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró el evento de auditoría."));
    }

    @Transactional(readOnly = true)
    public List<ManualReviewView> manualReview() {
        return reservations.findByManualReviewRequiredTrueOrderByCheckInAsc().stream()
                .map(ManualReviewView::from).toList();
    }

    @Transactional
    public ManualReviewView resolveReview(MarketplaceUser admin, UUID id, RestrictRequest request) {
        Reservation reservation = reservations.lockById(id).orElseThrow(() ->
                new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la reserva."));
        if (!reservation.isManualReviewRequired()) {
            throw new DomainException(HttpStatus.CONFLICT, "REVIEW_NOT_REQUIRED", "La reserva no está pendiente de revisión.");
        }
        reservation.resolveManualReview();
        audit.recordEvent("RESERVATION_REVIEW_RESOLVED", admin.getId(), "RESERVATION", id, "SUCCESS", request.reason().trim());
        return ManualReviewView.from(reservation);
    }

    private PageRequest paging(int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "La página o su tamaño no son válidos.");
        }
        return PageRequest.of(page, size, Sort.by(sort).descending());
    }

    public record RestrictRequest(@NotBlank @Size(max = 1000) String reason) { }
    public record UserView(UUID id, String name, String email, AccountRole role, AccountState accountState) {
        static UserView from(MarketplaceUser user) {
            return new UserView(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getAccountState());
        }
    }
    public record ListingView(UUID id, String title, String city, ListingStatus status) {
        static ListingView from(Listing listing) {
            return new ListingView(listing.getId(), listing.getTitle(), listing.getCity(), listing.getStatus());
        }
    }
    public record AuditView(UUID id, String eventType, UUID actorId, String entityType, UUID entityId,
            String result, String reason, Instant occurredAt) {
        static AuditView from(AuditEvent event) {
            return new AuditView(event.getId(), event.getEventType(), event.getActorId(), event.getEntityType(),
                    event.getEntityId(), event.getResult(), event.getReason(), event.getOccurredAt());
        }
    }
    public record ManualReviewView(UUID id, UUID listingId, String listingTitle, UUID guestId,
            LocalDate checkIn, LocalDate checkOut, ReservationStatus status) {
        static ManualReviewView from(Reservation reservation) {
            return new ManualReviewView(reservation.getId(), reservation.getListing().getId(),
                    reservation.getListing().getTitle(), reservation.getGuest().getId(), reservation.getCheckIn(),
                    reservation.getCheckOut(), reservation.getStatus());
        }
    }
}
