package co.edu.unbosque.marketplace.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class ReservationExtra {
    @Column(name = "extra_id", nullable = false, length = 36)
    private String extraId;

    @Column(name = "extra_name", nullable = false, length = 80)
    private String name;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    protected ReservationExtra() { }

    public ReservationExtra(String extraId, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.extraId = extraId;
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    public String getExtraId() { return extraId; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
