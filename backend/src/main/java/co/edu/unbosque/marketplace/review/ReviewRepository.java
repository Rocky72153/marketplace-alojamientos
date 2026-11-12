package co.edu.unbosque.marketplace.review;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    boolean existsByReservation_Id(UUID reservationId);
    @Query("select review.reservation.id from Review review where review.guest.id = :guestId")
    List<UUID> findReviewedReservationIdsByGuest(@Param("guestId") UUID guestId);
    List<Review> findByListing_IdAndHiddenFalseOrderByCreatedAtDesc(UUID listingId);
    List<Review> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select review from Review review where review.id = :id")
    Optional<Review> lockById(@Param("id") UUID id);
}
