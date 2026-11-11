package co.edu.unbosque.marketplace.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class JwtServiceMigrationTest {
    private static final String SECRET = "migration-test-secret-with-at-least-32-bytes";
    private static final String HEADER = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
    private final JwtService service = new JwtService(SECRET, JsonMapper.builder().build());

    @Test
    void acceptsTokenCreatedWithThePreviousJsonFormat() throws Exception {
        UUID subject = UUID.randomUUID();
        String payload = "{\"sub\":\"" + subject + "\",\"exp\":" + Instant.now().plusSeconds(3600).getEpochSecond() + "}";

        assertEquals(subject, service.subject(signedToken(payload)));
    }

    @Test
    void rejectsSignedTokenWithMalformedJson() throws Exception {
        assertNull(service.subject(signedToken("{\"sub\":")));
    }

    private String signedToken(String payload) throws Exception {
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String content = HEADER + "." + encodedPayload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        return content + "." + signature;
    }
}
