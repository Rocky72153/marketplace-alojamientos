package co.edu.unbosque.marketplace.auth;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.JwtService;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final MarketplaceUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService audit;
    private final String adminBootstrapKey;

    public AuthController(MarketplaceUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
            AuditService audit, @Value("${app.security.admin-bootstrap-key:}") String adminBootstrapKey) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.audit = audit;
        this.adminBootstrapKey = adminBootstrapKey;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "X-Admin-Bootstrap", required = false) String bootstrapKey) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new DomainException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "No fue posible crear la cuenta con esos datos.");
        }
        AccountRole role = request.role() == null ? AccountRole.GUEST : request.role();
        if (role == AccountRole.ADMIN && !validBootstrapKey(bootstrapKey)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "ROLE_NOT_ALLOWED", "No tienes permiso para asignar ese rol.");
        }
        MarketplaceUser user = users.save(new MarketplaceUser(request.name().trim(), email,
                passwordEncoder.encode(request.password()), role));
        audit.record("ACCOUNT_REGISTERED", user.getId(), "USER", user.getId(), "SUCCESS", null);
        return response(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        String email = normalizeEmail(request.email());
        MarketplaceUser user = users.findByEmail(email).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword()) || !user.isEnabled() || !user.isAccountNonLocked()) {
            audit.record("LOGIN", user == null ? null : user.getId(), "USER", user == null ? null : user.getId(), "REJECTED", null);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Correo o contraseña inválidos.");
        }
        audit.record("LOGIN", user.getId(), "USER", user.getId(), "SUCCESS", null);
        return response(user);
    }

    @PostMapping("/recovery")
    public RecoveryResponse recovery(@Valid @RequestBody RecoveryRequest request) {
        String email = normalizeEmail(request.email());
        MarketplaceUser user = users.findByEmail(email).orElse(null);
        audit.record("RECOVERY_REQUESTED", user == null ? null : user.getId(), "USER", user == null ? null : user.getId(), "RECEIVED", null);
        return new RecoveryResponse("Si la cuenta existe, recibirás instrucciones para continuar.");
    }

    private AuthResponse response(MarketplaceUser user) {
        JwtService.IssuedToken token = jwtService.issue(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), ProfileResponse.from(user));
    }

    private boolean validBootstrapKey(String supplied) {
        if (adminBootstrapKey == null || adminBootstrapKey.isBlank() || supplied == null) return false;
        return java.security.MessageDigest.isEqual(adminBootstrapKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    static String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    public record RegisterRequest(@NotBlank @Size(max=120) String name, @NotBlank @Email @Size(max=320) String email,
            @NotBlank @Size(min=8, max=72) String password, AccountRole role) { }
    public record LoginRequest(@NotBlank @Email @Size(max=320) String email, @NotBlank @Size(max=72) String password) { }
    public record RecoveryRequest(@NotBlank @Email @Size(max=320) String email) { }
    public record RecoveryResponse(String message) { }
    public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, ProfileResponse user) { }
    public record ProfileResponse(UUID id, String name, String email, AccountRole role, String accountState,
            String phone, String photoReference) {
        public static ProfileResponse from(MarketplaceUser user) {
            return new ProfileResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                    user.getAccountState().name(), user.getPhone(), user.getPhotoReference());
        }
    }
}
