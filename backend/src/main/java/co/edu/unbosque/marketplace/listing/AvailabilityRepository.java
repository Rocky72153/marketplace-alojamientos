package co.edu.unbosque.marketplace.listing;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilityRepository extends JpaRepository<AvailabilityPeriod, UUID> {
    List<AvailabilityPeriod> findByListing_IdOrderByStartDateAsc(UUID listingId);
    List<AvailabilityPeriod> findByListing_IdAndType(UUID listingId, AvailabilityType type);
    @Query("""
            select period from AvailabilityPeriod period
            where period.listing.id in :listingIds
              and period.startDate < :checkOut
              and period.endDate > :checkIn
            order by period.listing.id, period.startDate
            """)
    List<AvailabilityPeriod> findSearchPeriods(@Param("listingIds") List<UUID> listingIds,
            @Param("checkOut") LocalDate checkOut, @Param("checkIn") LocalDate checkIn);
    Optional<AvailabilityPeriod> findByListing_IdAndReservationReference(UUID listingId, String reservationReference);
    void deleteByListing_IdAndTypeIn(UUID listingId, List<AvailabilityType> types);
}
