package co.edu.unbosque.marketplace.booking;

import co.edu.unbosque.marketplace.security.MarketplaceUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReservationController {
    private final ReservationService service;

    public ReservationController(ReservationService service) { this.service = service; }

    @PostMapping("/api/listings/{listingId}/quotes")
    public ReservationService.QuoteView quote(@PathVariable UUID listingId,
            @Valid @RequestBody ReservationService.QuoteRequest request) {
        return service.quote(listingId, request);
    }

    @PostMapping("/api/reservations")
    public ReservationService.ReservationView reserve(@AuthenticationPrincipal MarketplaceUser guest,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReservationService.ReservationRequest request) {
        return service.reserve(guest, request, idempotencyKey);
    }

    @GetMapping("/api/reservations")
    public List<ReservationService.ReservationView> history(@AuthenticationPrincipal MarketplaceUser guest) {
        return service.history(guest);
    }

    @GetMapping("/api/reservations/{reservationId}")
    public ReservationService.ReservationView get(@AuthenticationPrincipal MarketplaceUser guest,
            @PathVariable UUID reservationId) {
        return service.get(guest, reservationId);
    }

    @PostMapping("/api/reservations/{reservationId}/cancel")
    public ReservationService.ReservationView cancel(@AuthenticationPrincipal MarketplaceUser guest,
            @PathVariable UUID reservationId, @Valid @RequestBody ReservationService.CancelRequest request) {
        return service.cancel(guest, reservationId, request);
    }
}
