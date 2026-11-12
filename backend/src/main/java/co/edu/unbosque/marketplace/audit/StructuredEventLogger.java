package co.edu.unbosque.marketplace.audit;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Logs only fixed event metadata, never reasons, request data, credentials or exceptions. */
@Component
public class StructuredEventLogger {
    private static final Logger LOG = LoggerFactory.getLogger(StructuredEventLogger.class);
    private final ObjectMapper mapper;

    public StructuredEventLogger(ObjectMapper mapper) { this.mapper = mapper; }

    public void event(Instant timestamp, String eventType, UUID actorId, String entityType,
            UUID entityId, String result, String correlationId) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("timestamp", timestamp.toString());
        fields.put("eventType", eventType);
        fields.put("actorId", actorId);
        fields.put("entityType", entityType);
        fields.put("entityId", entityId);
        fields.put("result", result);
        fields.put("correlationId", correlationId);
        LOG.info("{}", mapper.writeValueAsString(fields));
    }

    public void httpError(int status, String correlationId) {
        event(Instant.now(), "HTTP_ERROR", null, null, null, "ERROR_" + status, correlationId);
    }
}
