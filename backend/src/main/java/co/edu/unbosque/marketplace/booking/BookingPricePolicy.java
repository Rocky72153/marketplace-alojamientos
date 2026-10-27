package co.edu.unbosque.marketplace.booking;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BookingPricePolicy {
    private final BigDecimal cleaningFee;
    private final BigDecimal serviceFeeRate;
    private final BigDecimal discountRate;

    public BookingPricePolicy(@Value("${marketplace.pricing.cleaning-fee-cop:35000.00}") BigDecimal cleaningFee,
            @Value("${marketplace.pricing.service-fee-rate:0.10}") BigDecimal serviceFeeRate,
            @Value("${marketplace.pricing.discount-rate:0.00}") BigDecimal discountRate) {
        if (cleaningFee.signum() < 0 || serviceFeeRate.signum() < 0 || discountRate.signum() < 0
                || discountRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("La configuración de precios no es válida.");
        }
        this.cleaningFee = cleaningFee.setScale(2, java.math.RoundingMode.HALF_UP);
        this.serviceFeeRate = serviceFeeRate;
        this.discountRate = discountRate;
    }

    public BigDecimal cleaningFee() { return cleaningFee; }
    public BigDecimal serviceFeeRate() { return serviceFeeRate; }
    public BigDecimal discountRate() { return discountRate; }
}
