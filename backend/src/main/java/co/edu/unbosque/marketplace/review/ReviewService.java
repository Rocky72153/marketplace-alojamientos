package co.edu.unbosque.marketplace.review;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.booking.Reservation;
import co.edu.unbosque.marketplace.booking.ReservationCompletionService;
import co.edu.unbosque.marketplace.booking.ReservationRepository;
import co.edu.unbosque.marketplace.booking.ReservationStatus;
import co.edu.unbosque.marketplace.listing.ListingService;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final ReservationRepository reservations;
    private final ReservationCompletionService completion;
    private final ListingService listings;
    private final MarketplaceUserRepository users;
    private final AuditService audit;

    public ReviewService(ReviewRepository reviews, ReservationRepository reservations,
            ReservationCompletionService completion, ListingService listings,
            MarketplaceUserRepository users, AuditService audit) {
        this.reviews = reviews;
        this.reservations = reservations;
        this.completion = completion;
        this.listings = listings;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ReviewView> listVisible(UUID listingId) {
        listings.getActiveListing(listingId);
        return reviews.findByListing_IdAndHiddenFalseOrderByCreatedAtDesc(listingId).stream()
                .map(ReviewView::from).toList();
    }

    @Transactional
    public ReviewView create(MarketplaceUser guest, UUID listingId, CreateRequest request) {
        if (guest == null || guest.getRole() != AccountRole.GUEST) {
            throw new DomainException(HttpStatus.FORBIDDEN, "GUEST_REQUIRED", "Solo los huéspedes pueden escribir reseñas.");
        }
        completion.completeDueReservations();
        // Lock the guest before checking uniqueness; only the reservation's guest may review it.
        users.lockForReservationById(guest.getId()).orElseThrow(() ->
                new DomainException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "La sesión ya no es válida."));
        Reservation booking = reservations.findByIdAndGuest_Id(request.bookingId(), guest.getId()).orElseThrow(() ->
                new DomainException(HttpStatus.FORBIDDEN, "REVIEW_NOT_ELIGIBLE", "La reserva no permite crear esta reseña."));
        if (!booking.getListing().getId().equals(listingId) || booking.getStatus() != ReservationStatus.COMPLETED) {
            throw new DomainException(HttpStatus.FORBIDDEN, "REVIEW_NOT_ELIGIBLE", "La reserva no permite crear esta reseña.");
        }
        if (reviews.existsByReservation_Id(booking.getId())) {
            throw new DomainException(HttpStatus.CONFLICT, "REVIEW_EXISTS", "Ya existe una reseña para esta reserva.");
        }
        return ReviewView.from(reviews.saveAndFlush(new Review(booking, request.rating(), request.content())));
    }

    @Transactional(readOnly = true)
    public List<ModerationView> listForModeration(MarketplaceUser admin) {
        requireAdmin(admin);
        return reviews.findAllByOrderByCreatedAtDesc().stream().map(ModerationView::from).toList();
    }

    @Transactional
    public ModerationView hide(MarketplaceUser admin, UUID reviewId, HideRequest request) {
        requireAdmin(admin);
        Review review = reviews.lockById(reviewId).orElseThrow(() ->
                new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la reseña."));
        if (review.isHidden()) {
            throw new DomainException(HttpStatus.CONFLICT, "REVIEW_ALREADY_HIDDEN", "La reseña ya está oculta.");
        }
        review.hide(admin.getId(), request.reason());
        audit.recordEvent("REVIEW_HIDDEN", admin.getId(), "REVIEW", review.getId(), "SUCCESS", request.reason().trim());
        return ModerationView.from(review);
    }

    private void requireAdmin(MarketplaceUser admin) {
        if (admin == null || admin.getRole() != AccountRole.ADMIN) {
            throw new DomainException(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "Solo un administrador puede moderar reseñas.");
        }
    }

    public record CreateRequest(@NotNull UUID bookingId, @NotNull @Min(1) @Max(5) Integer rating,
            @NotBlank @Size(max = 5000) String content) { }
    public record HideRequest(@NotBlank @Size(max = 1000) String reason) { }
    public record ReviewView(UUID id, UUID bookingId, String guestDisplayName, int rating, String content, Instant createdAt) {
        static ReviewView from(Review review) {
            return new ReviewView(review.getId(), review.getReservation().getId(), review.getGuest().getName(),
                    review.getRating(), review.getContent(), review.getCreatedAt());
        }
    }
    public record ModerationView(UUID id, UUID bookingId, UUID listingId, String listingTitle, String guestDisplayName,
            int rating, String content, Instant createdAt, boolean hidden, Instant hiddenAt, UUID hiddenBy, String hiddenReason) {
        static ModerationView from(Review review) {
            return new ModerationView(review.getId(), review.getReservation().getId(), review.getListing().getId(),
                    review.getListing().getTitle(), review.getGuest().getName(), review.getRating(), review.getContent(),
                    review.getCreatedAt(), review.isHidden(), review.getHiddenAt(), review.getHiddenBy(), review.getHiddenReason());
        }
    }
}
