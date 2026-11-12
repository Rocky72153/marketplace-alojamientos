package co.edu.unbosque.marketplace.booking;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByGuest_IdAndIdempotencyKey(UUID guestId, String idempotencyKey);
    List<Reservation> findByGuest_IdOrderByCreatedAtDesc(UUID guestId);
    Optional<Reservation> findByIdAndGuest_Id(UUID id, UUID guestId);

    @Modifying(flushAutomatically = true)
    @Query("update Reservation r set r.status = :completed where r.status = :confirmed and r.checkOut < :today")
    int completeBefore(@Param("today") LocalDate today, @Param("confirmed") ReservationStatus confirmed,
            @Param("completed") ReservationStatus completed);
}
