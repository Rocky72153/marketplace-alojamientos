package co.edu.unbosque.marketplace.booking;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationCompletionService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");
    private final ReservationRepository reservations;
    private final Clock clock;

    @Autowired
    public ReservationCompletionService(ReservationRepository reservations) {
        this(reservations, Clock.system(BUSINESS_ZONE));
    }

    ReservationCompletionService(ReservationRepository reservations, Clock clock) {
        this.reservations = reservations;
        this.clock = clock;
    }

    @Transactional
    public int completeDueReservations() {
        return reservations.completeBefore(LocalDate.now(clock.withZone(BUSINESS_ZONE)),
                ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED);
    }
}
