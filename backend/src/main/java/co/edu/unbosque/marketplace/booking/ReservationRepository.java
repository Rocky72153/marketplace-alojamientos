package co.edu.unbosque.marketplace.booking;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByGuest_IdAndIdempotencyKey(UUID guestId, String idempotencyKey);
    List<Reservation> findByGuest_IdOrderByCreatedAtDesc(UUID guestId);
    Optional<Reservation> findByIdAndGuest_Id(UUID id, UUID guestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> lockById(@Param("id") UUID id);

    List<Reservation> findByManualReviewRequiredTrueOrderByCheckInAsc();

    @Modifying(flushAutomatically = true)
    @Query("update Reservation r set r.manualReviewRequired = true where r.listing.id = :listingId and r.status = :confirmed and r.checkIn >= :today")
    int flagFutureForManualReview(@Param("listingId") UUID listingId, @Param("today") LocalDate today,
            @Param("confirmed") ReservationStatus confirmed);

    @Modifying(flushAutomatically = true)
    @Query("update Reservation r set r.status = :completed where r.status = :confirmed and r.checkOut < :today")
    int completeBefore(@Param("today") LocalDate today, @Param("confirmed") ReservationStatus confirmed,
            @Param("completed") ReservationStatus completed);
}
