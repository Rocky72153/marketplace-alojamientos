package co.edu.unbosque.marketplace.listing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingRepository extends JpaRepository<Listing, UUID> {
    List<Listing> findByStatus(ListingStatus status);

    Page<Listing> findByHost_Id(UUID hostId, Pageable pageable);

    @Query("""
            select distinct listing from Listing listing
            where listing.status = :status
              and (:restrictCity = false or listing.city in :cities)
              and (:guests is null or listing.capacity >= :guests)
              and (:restrictType = false or upper(listing.type) = :type)
              and (:minPrice is null or listing.pricePerNight >= :minPrice)
              and (:maxPrice is null or listing.pricePerNight <= :maxPrice)
              and (:amenityCount = 0 or
                    (select count(distinct amenity)
                     from Listing amenityListing join amenityListing.amenityIds amenity
                     where amenityListing.id = listing.id and amenity in :amenityIds) = :amenityCount)
            order by listing.createdAt desc
            """)
    List<Listing> searchCandidates(@Param("status") ListingStatus status,
            @Param("restrictCity") boolean restrictCity, @Param("cities") List<String> cities,
            @Param("guests") Integer guests, @Param("restrictType") boolean restrictType,
            @Param("type") String type,
            @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice,
            @Param("amenityCount") int amenityCount, @Param("amenityIds") Set<String> amenityIds);

    @Query("select distinct listing.city from Listing listing where listing.status = :status order by listing.city")
    List<String> findDistinctCitiesByStatus(@Param("status") ListingStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select listing from Listing listing where listing.id = :id")
    Optional<Listing> lockById(@Param("id") UUID id);
}
