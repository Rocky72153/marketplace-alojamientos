package co.edu.unbosque.marketplace.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
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

    @Test
    void reportsJsonFailureWhenIssuingToken() {
        ObjectMapper mapper = mock(ObjectMapper.class);
        MarketplaceUser user = mock(MarketplaceUser.class);
        JacksonException failure = mock(JacksonException.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(mapper.writeValueAsBytes(any())).thenThrow(failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> new JwtService(SECRET, mapper).issue(user));
        assertSame(failure, thrown.getCause());
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
