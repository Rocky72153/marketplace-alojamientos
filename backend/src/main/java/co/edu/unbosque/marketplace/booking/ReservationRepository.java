package co.edu.unbosque.marketplace.booking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByGuest_IdAndIdempotencyKey(UUID guestId, String idempotencyKey);
    List<Reservation> findByGuest_IdOrderByCreatedAtDesc(UUID guestId);
    Optional<Reservation> findByIdAndGuest_Id(UUID id, UUID guestId);
}
