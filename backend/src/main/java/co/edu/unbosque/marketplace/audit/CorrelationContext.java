package co.edu.unbosque.marketplace.audit;

import java.util.UUID;
import org.slf4j.MDC;

/** Request-scoped identifier; background events receive a fresh identifier. */
public final class CorrelationContext {
    public static final String HEADER = "X-Correlation-ID";
    public static final String ATTRIBUTE = CorrelationContext.class.getName() + ".id";
    private static final String MDC_KEY = "correlationId";

    private CorrelationContext() { }

    public static String currentOrCreate() {
        String current = MDC.get(MDC_KEY);
        return current == null ? UUID.randomUUID().toString() : current;
    }

    static void set(String id) { MDC.put(MDC_KEY, id); }
    static void clear() { MDC.remove(MDC_KEY); }
}
