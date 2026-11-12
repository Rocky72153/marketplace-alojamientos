package co.edu.unbosque.marketplace.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Stable identity for synthetic listings.  Keeping this record after a listing
 * is edited or removed prevents the importer from silently recreating it.
 */
@Entity
@Table(name = "demo_seed_entries")
public class DemoSeedEntry {
    @Id
    @Column(name = "seed_number")
    private Integer seedNumber;

    // Null marks an ambiguous/missing legacy entry that must not be recreated.
    @Column(name = "listing_id", unique = true)
    private UUID listingId;

    protected DemoSeedEntry() { }

    DemoSeedEntry(int seedNumber, UUID listingId) {
        this.seedNumber = seedNumber;
        this.listingId = listingId;
    }
}
