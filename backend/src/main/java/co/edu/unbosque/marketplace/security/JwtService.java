package co.edu.unbosque.marketplace.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(8);
    private static final String HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
    private final byte[] secret;
    private final ObjectMapper objectMapper;

    public JwtService(@Value("${app.security.jwt-secret:}") String configuredSecret, ObjectMapper objectMapper) {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            this.secret = new byte[32];
            new SecureRandom().nextBytes(this.secret);
        } else {
            byte[] configuredBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
            if (configuredBytes.length < 32) {
                throw new IllegalStateException("AUTH_TOKEN_SECRET must contain at least 32 bytes");
            }
            this.secret = configuredBytes;
        }
        this.objectMapper = objectMapper;
    }

    public IssuedToken issue(MarketplaceUser user) {
        Instant expiresAt = Instant.now().plus(TOKEN_LIFETIME);
        try {
            byte[] payload = objectMapper.writeValueAsBytes(new TokenClaims(user.getId().toString(), expiresAt.getEpochSecond()));
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
            String content = HEADER + "." + encodedPayload;
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(sign(content));
            return new IssuedToken(content + "." + signature, expiresAt);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not create access token", exception);
        }
    }

    public UUID subject(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !HEADER.equals(parts[0])) {
                return null;
            }
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(sign(parts[0] + "." + parts[1]), suppliedSignature)) {
                return null;
            }
            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (!claims.hasNonNull("sub") || !claims.hasNonNull("exp")
                    || claims.get("exp").asLong() <= Instant.now().getEpochSecond()) {
                return null;
            }
            return UUID.fromString(claims.get("sub").asText());
        } catch (IllegalArgumentException | java.io.IOException exception) {
            return null;
        }
    }

    private byte[] sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Could not sign access token", exception);
        }
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }

    private record TokenClaims(String sub, long exp) {
    }
}
