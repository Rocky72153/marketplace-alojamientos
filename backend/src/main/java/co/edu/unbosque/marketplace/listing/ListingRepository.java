package co.edu.unbosque.marketplace.listing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingRepository extends JpaRepository<Listing, UUID> {
    List<Listing> findByStatus(ListingStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select listing from Listing listing where listing.id = :id")
    Optional<Listing> lockById(@Param("id") UUID id);
}
