package co.edu.unbosque.marketplace.listing;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/listings")
public class ListingController {
    private final ListingService service;

    public ListingController(ListingService service) { this.service = service; }

    @GetMapping
    public SearchResponse search(@RequestParam(required=false) String city,
            @RequestParam(required=false) LocalDate checkIn, @RequestParam(required=false) LocalDate checkOut,
            @RequestParam(required=false) Integer guests, @RequestParam(required=false) String type,
            @RequestParam(required=false) BigDecimal minPrice, @RequestParam(required=false) BigDecimal maxPrice,
            @RequestParam(required=false) Set<String> amenityIds) {
        List<ListingView> items = service.search(city, checkIn, checkOut, guests, type, minPrice, maxPrice, amenityIds)
                .stream().map(ListingView::from).toList();
        return new SearchResponse(items, items.size());
    }

    @GetMapping("/{listingId}")
    public ListingView get(@PathVariable UUID listingId, @AuthenticationPrincipal MarketplaceUser principal) {
        return ListingView.from(service.getPublicOrOwned(listingId, principal));
    }

    @PostMapping
    public ListingView create(@AuthenticationPrincipal MarketplaceUser principal, @Valid @RequestBody ListingService.ListingInput input) {
        return ListingView.from(service.create(principal, input));
    }

    @PatchMapping("/{listingId}")
    public ListingView update(@AuthenticationPrincipal MarketplaceUser principal, @PathVariable UUID listingId,
            @Valid @RequestBody ListingService.ListingInput input) {
        return ListingView.from(service.update(principal, listingId, input));
    }

    @PatchMapping("/{listingId}/state")
    public ListingView changeState(@AuthenticationPrincipal MarketplaceUser principal, @PathVariable UUID listingId,
            @Valid @RequestBody ChangeStateRequest request) {
        return ListingView.from(service.changeState(principal, listingId, request.status(), request.reason()));
    }

    @GetMapping("/{listingId}/availability")
    public List<AvailabilityView> getAvailability(@PathVariable UUID listingId,
            @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to,
            @AuthenticationPrincipal MarketplaceUser principal) {
        Listing listing = service.getPublicOrOwned(listingId, principal);
        if (listing.getStatus() != ListingStatus.ACTIVE && principal == null) {
            throw new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la publicación.");
        }
        return service.getAvailability(listingId, from, to).stream().map(AvailabilityView::from).toList();
    }

    @PutMapping("/{listingId}/availability")
    public List<AvailabilityView> replaceAvailability(@AuthenticationPrincipal MarketplaceUser principal,
            @PathVariable UUID listingId, @Valid @RequestBody AvailabilityRequest request) {
        return service.replaceAvailability(principal, listingId, request.periods()).stream().map(AvailabilityView::from).toList();
    }

    public record ChangeStateRequest(@jakarta.validation.constraints.NotNull ListingStatus status,
            @jakarta.validation.constraints.Size(max=1000) String reason) { }
    public record AvailabilityRequest(@Valid List<ListingService.AvailabilityInput> periods) { }
    public record SearchResponse(List<ListingView> items, int total) { }
    public record ListingView(UUID id, UUID hostId, String hostName, String title, String description, String city,
            String location, String type, int capacity, BigDecimal pricePerNight, Set<String> amenityIds,
            String rules, Set<String> imageReferences, ListingStatus status) {
        static ListingView from(Listing listing) {
            return new ListingView(listing.getId(), listing.getHost().getId(), listing.getHost().getName(), listing.getTitle(),
                    listing.getDescription(), listing.getCity(), listing.getLocation(), listing.getType(), listing.getCapacity(),
                    listing.getPricePerNight(), listing.getAmenityIds(), listing.getRules(), listing.getImageReferences(), listing.getStatus());
        }
    }
    public record AvailabilityView(UUID id, LocalDate startDate, LocalDate endDate, AvailabilityType type) {
        static AvailabilityView from(AvailabilityPeriod period) {
            return new AvailabilityView(period.getId(), period.getStartDate(), period.getEndDate(), period.getType());
        }
    }
}
