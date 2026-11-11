package co.edu.unbosque.marketplace.api;

public record ApiError(String code, String message, String correlationId) {
}
