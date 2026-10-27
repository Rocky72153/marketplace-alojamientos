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
            throw new IllegalStateException("El flujo de reservas solo admite pagos de prueba simulados.");
        }
        return "TEST-" + UUID.randomUUID();
    }
}
