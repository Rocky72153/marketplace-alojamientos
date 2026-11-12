package co.edu.unbosque.marketplace.admin;

import co.edu.unbosque.marketplace.security.MarketplaceUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class ModerationController {
    private final ModerationService moderation;
    public ModerationController(ModerationService moderation) { this.moderation = moderation; }

    @GetMapping("/users")
    public Page<ModerationService.UserView> users(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return moderation.users(page, size);
    }

    @GetMapping("/listings")
    public Page<ModerationService.ListingView> listings(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return moderation.listings(page, size);
    }

    @PostMapping("/users/{id}/restrict")
    public ModerationService.UserView restrict(@AuthenticationPrincipal MarketplaceUser admin,
            @PathVariable UUID id, @Valid @RequestBody ModerationService.RestrictRequest request) {
        return moderation.restrict(admin, id, request);
    }

    @GetMapping("/audit")
    public Page<ModerationService.AuditView> audit(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return moderation.audit(page, size);
    }

    @GetMapping("/audit/{id}")
    public ModerationService.AuditView auditEvent(@PathVariable UUID id) {
        return moderation.auditEvent(id);
    }

    @GetMapping("/reservations/manual-review")
    public List<ModerationService.ManualReviewView> manualReview() { return moderation.manualReview(); }

    @PostMapping("/reservations/{id}/resolve-review")
    public ModerationService.ManualReviewView resolveReview(@AuthenticationPrincipal MarketplaceUser admin,
            @PathVariable UUID id, @Valid @RequestBody ModerationService.RestrictRequest request) {
        return moderation.resolveReview(admin, id, request);
    }
}
