package co.edu.unbosque.marketplace.listing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityRepository extends JpaRepository<AvailabilityPeriod, UUID> {
    List<AvailabilityPeriod> findByListing_Id(UUID listingId);
    List<AvailabilityPeriod> findByListing_IdAndType(UUID listingId, AvailabilityType type);
    void deleteByListing_IdAndTypeIn(UUID listingId, List<AvailabilityType> types);
}
