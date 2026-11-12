package co.edu.unbosque.marketplace.booking;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationCompletionScheduler {
    private final ReservationCompletionService completion;

    public ReservationCompletionScheduler(ReservationCompletionService completion) {
        this.completion = completion;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 0 * * *", zone = "America/Bogota")
    public void completeFinishedStays() {
        completion.completeDueReservations();
    }
}
