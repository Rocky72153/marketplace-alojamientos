package co.edu.unbosque.marketplace.booking;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StripeCheckoutAttemptRepository extends JpaRepository<StripeCheckoutAttempt, UUID> {
    Optional<StripeCheckoutAttempt> findByReservationId(UUID reservationId);
    Optional<StripeCheckoutAttempt> findByIdAndGuestId(UUID id, UUID guestId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from StripeCheckoutAttempt a where a.id = :id")
    Optional<StripeCheckoutAttempt> lockById(@Param("id") UUID id);
    List<StripeCheckoutAttempt> findTop10ByStateInAndExpiresAtLessThanEqualOrderByIdAsc(
            Collection<StripeCheckoutAttempt.State> states, Instant now);
    List<StripeCheckoutAttempt> findTop10ByStateInAndExpiresAtLessThanEqualAndIdGreaterThanOrderByIdAsc(
            Collection<StripeCheckoutAttempt.State> states, Instant now, UUID after);
    List<StripeCheckoutAttempt> findTop10ByStateOrderByIdAsc(StripeCheckoutAttempt.State state);
    List<StripeCheckoutAttempt> findTop10ByStateAndIdGreaterThanOrderByIdAsc(StripeCheckoutAttempt.State state, UUID after);
    List<StripeCheckoutAttempt> findTop10ByStateAndSessionIdIsNullOrderByIdAsc(StripeCheckoutAttempt.State state);
    List<StripeCheckoutAttempt> findTop10ByStateAndSessionIdIsNullAndIdGreaterThanOrderByIdAsc(
            StripeCheckoutAttempt.State state, UUID after);
}
