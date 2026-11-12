package co.edu.unbosque.marketplace.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import co.edu.unbosque.marketplace.audit.CorrelationContext;
import co.edu.unbosque.marketplace.audit.StructuredEventLogger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private final StructuredEventLogger logger;

    public ApiExceptionHandler(StructuredEventLogger logger) { this.logger = logger; }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException exception, HttpServletRequest request) {
        return error(exception.getStatus(), exception.getCode(), exception.getMessage(), request);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleValidation(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos de entrada inválidos.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "CONFLICT", "La operación entra en conflicto con datos existentes.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "No tienes permiso para realizar esta operación.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        // Preserve framework HTTP errors (for example missing resources or unsupported methods),
        // while avoiding exception messages that can contain request data.
        if (exception instanceof ErrorResponse frameworkError && frameworkError.getStatusCode().is4xxClientError()) {
            return error(frameworkError.getStatusCode(), "REQUEST_ERROR", "No fue posible procesar la solicitud.", request);
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "No fue posible completar la operación.", request);
    }

    private ResponseEntity<ApiError> error(HttpStatusCode status, String code, String message, HttpServletRequest request) {
        Object attribute = request.getAttribute(CorrelationContext.ATTRIBUTE);
        String correlationId = attribute instanceof String id ? id : CorrelationContext.currentOrCreate();
        logger.httpError(status.value(), correlationId);
        return ResponseEntity.status(status).body(new ApiError(code, message, correlationId));
    }
}
