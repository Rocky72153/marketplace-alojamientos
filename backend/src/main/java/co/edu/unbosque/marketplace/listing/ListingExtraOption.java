package co.edu.unbosque.marketplace.listing;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public class ListingExtraOption {
    @Column(name = "extra_id", nullable = false, length = 36)
    private String id;

    @Column(name = "extra_name", nullable = false, length = 80)
    private String name;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    protected ListingExtraOption() { }

    public ListingExtraOption(String id, String name, BigDecimal unitPrice) {
        this.id = id;
        this.name = name;
        this.unitPrice = unitPrice;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
