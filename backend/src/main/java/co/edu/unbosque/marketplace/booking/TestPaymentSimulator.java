package co.edu.unbosque.marketplace.booking;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TestPaymentSimulator {
    private final String mode;

    public TestPaymentSimulator(@Value("${marketplace.payment.mode:SIMULATED_TEST}") String mode) {
        this.mode = mode;
    }

    public String authorize() {
        if (!"SIMULATED_TEST".equals(mode)) {
            throw new co.edu.unbosque.marketplace.api.DomainException(org.springframework.http.HttpStatus.CONFLICT,
                    "PAYMENT_MODE_MISMATCH", "Usa el flujo de pago de prueba configurado.");
        }
        return "TEST-" + UUID.randomUUID();
    }
}
