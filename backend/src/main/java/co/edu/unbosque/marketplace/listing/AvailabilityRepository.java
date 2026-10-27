package co.edu.unbosque.marketplace.listing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityRepository extends JpaRepository<AvailabilityPeriod, UUID> {
    List<AvailabilityPeriod> findByListing_IdOrderByStartDateAsc(UUID listingId);
    List<AvailabilityPeriod> findByListing_IdAndType(UUID listingId, AvailabilityType type);
    Optional<AvailabilityPeriod> findByListing_IdAndReservationReference(UUID listingId, String reservationReference);
    void deleteByListing_IdAndTypeIn(UUID listingId, List<AvailabilityType> types);
}
