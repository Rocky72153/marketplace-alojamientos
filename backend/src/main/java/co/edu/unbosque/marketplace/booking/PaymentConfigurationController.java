package co.edu.unbosque.marketplace.booking;

import co.edu.unbosque.marketplace.api.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentConfigurationController {
    private final String mode;
    public PaymentConfigurationController(@Value("${marketplace.payment.mode:SIMULATED_TEST}") String mode) { this.mode = mode; }
    @GetMapping("/api/payments/config")
    public Config config() {
        if (!"SIMULATED_TEST".equals(mode) && !"STRIPE_TEST".equals(mode))
            throw new DomainException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_NOT_CONFIGURED", "El pago no está configurado.");
        return new Config(mode);
    }
    public record Config(String mode) { }
}
