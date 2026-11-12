package co.edu.unbosque.marketplace.review;

import co.edu.unbosque.marketplace.security.MarketplaceUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {
    private final ReviewService service;
    public ReviewController(ReviewService service) { this.service = service; }

    @GetMapping("/api/listings/{listingId}/reviews")
    public List<ReviewService.ReviewView> visible(@PathVariable UUID listingId) {
        return service.listVisible(listingId);
    }

    @PostMapping("/api/listings/{listingId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewService.ReviewView create(@AuthenticationPrincipal MarketplaceUser guest,
            @PathVariable UUID listingId, @Valid @RequestBody ReviewService.CreateRequest request) {
        return service.create(guest, listingId, request);
    }

    @GetMapping("/api/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReviewService.ModerationView> moderation(@AuthenticationPrincipal MarketplaceUser admin) {
        return service.listForModeration(admin);
    }

    @PostMapping("/api/admin/reviews/{reviewId}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewService.ModerationView hide(@AuthenticationPrincipal MarketplaceUser admin,
            @PathVariable UUID reviewId, @Valid @RequestBody ReviewService.HideRequest request) {
        return service.hide(admin, reviewId, request);
    }
}
