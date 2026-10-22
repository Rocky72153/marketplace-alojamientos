package co.edu.unbosque.marketplace.profile;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.auth.AuthController.ProfileResponse;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile/me")
public class ProfileController {
    private final MarketplaceUserRepository users;
    private final AuditService audit;

    public ProfileController(MarketplaceUserRepository users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    @GetMapping
    public ProfileResponse get(@AuthenticationPrincipal MarketplaceUser principal) {
        return ProfileResponse.from(current(principal));
    }

    @PatchMapping
    public ProfileResponse update(@AuthenticationPrincipal MarketplaceUser principal, @Valid @RequestBody UpdateProfileRequest request) {
        MarketplaceUser user = current(principal);
        user.updateProfile(request.name(), request.phone(), request.photoReference());
        users.save(user);
        audit.record("PROFILE_UPDATED", user.getId(), "USER", user.getId(), "SUCCESS", null);
        return ProfileResponse.from(user);
    }

    @DeleteMapping
    public DeactivationResponse deactivate(@AuthenticationPrincipal MarketplaceUser principal) {
        MarketplaceUser user = current(principal);
        user.deactivate();
        users.save(user);
        audit.record("ACCOUNT_DEACTIVATED", user.getId(), "USER", user.getId(), "SUCCESS", null);
        return new DeactivationResponse("La cuenta fue desactivada.");
    }

    private MarketplaceUser current(MarketplaceUser principal) {
        return users.findById(principal.getId()).orElseThrow(() ->
                new DomainException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Sesión ausente o inválida."));
    }

    public record UpdateProfileRequest(@Size(min=1, max=120) String name, @Size(max=40) String phone,
            @Size(max=1000) String photoReference) { }
    public record DeactivationResponse(String message) { }
}
