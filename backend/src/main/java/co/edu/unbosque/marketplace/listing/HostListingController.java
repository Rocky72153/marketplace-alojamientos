package co.edu.unbosque.marketplace.listing;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HostListingController {
    private final ListingRepository listings;

    public HostListingController(ListingRepository listings) { this.listings = listings; }

    @GetMapping("/api/host/listings")
    @PreAuthorize("hasRole('HOST')")
    @Transactional(readOnly = true)
    public Page<ListingController.ListingView> owned(@AuthenticationPrincipal MarketplaceUser host,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "La página o su tamaño no son válidos.");
        }
        return listings.findByHost_Id(host.getId(), PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(ListingController.ListingView::from);
    }
}
