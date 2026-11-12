package co.edu.unbosque.marketplace.audit;

import co.edu.unbosque.marketplace.api.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {
    private final StructuredEventLogger logger;
    private final ObjectMapper mapper;

    public RequestCorrelationFilter(StructuredEventLogger logger, ObjectMapper mapper) {
        this.logger = logger;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        request.setAttribute(CorrelationContext.ATTRIBUTE, id);
        response.setHeader(CorrelationContext.HEADER, id);
        CorrelationContext.set(id);
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            // Failures in security or other filters can happen before MVC's exception handler.
            // Never include the exception message or request data in the response or log.
            logger.httpError(500, id);
            if (response.isCommitted()) throw exception;
            response.resetBuffer();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(),
                    new ApiError("INTERNAL_ERROR", "No fue posible completar la operación.", id));
        } finally {
            CorrelationContext.clear();
        }
    }
}
