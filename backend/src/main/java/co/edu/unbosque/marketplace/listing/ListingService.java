package co.edu.unbosque.marketplace.listing;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListingService {
    private final ListingRepository listings;
    private final AvailabilityRepository availability;
    private final MarketplaceUserRepository users;
    private final AuditService audit;

    public ListingService(ListingRepository listings, AvailabilityRepository availability,
            MarketplaceUserRepository users, AuditService audit) {
        this.listings = listings; this.availability = availability; this.users = users; this.audit = audit;
    }

    @Transactional
    public Listing create(MarketplaceUser principal, ListingInput input) {
        requireRole(principal, AccountRole.HOST, AccountRole.ADMIN);
        MarketplaceUser host = principal;
        Listing listing = listings.save(new Listing(host, input.title(), input.description(), input.city(), input.location(),
                input.type(), input.capacity(), input.pricePerNight(), input.amenityIds(), input.rules(), input.imageReferences()));
        audit.record("LISTING_CREATED", principal.getId(), "LISTING", listing.getId(), "SUCCESS", null);
        return listing;
    }

    @Transactional
    public Listing update(MarketplaceUser principal, UUID id, ListingInput input) {
        Listing listing = get(id);
        requireOwnerOrAdmin(principal, listing);
        if (listing.getStatus() == ListingStatus.BLOCKED) throw forbidden("Una publicación bloqueada no se puede editar.");
        listing.update(input.title(), input.description(), input.city(), input.location(), input.type(), input.capacity(),
                input.pricePerNight(), input.amenityIds(), input.rules(), input.imageReferences());
        audit.record("LISTING_UPDATED", principal.getId(), "LISTING", id, "SUCCESS", null);
        return listings.save(listing);
    }

    @Transactional
    public Listing changeState(MarketplaceUser principal, UUID id, ListingStatus target, String reason) {
        Listing listing = get(id);
        boolean admin = principal.getRole() == AccountRole.ADMIN;
        requireOwnerOrAdmin(principal, listing);
        ListingStatus current = listing.getStatus();
        if (target == ListingStatus.BLOCKED && (!admin || reason == null || reason.isBlank())) {
            throw forbidden("Solo administración puede bloquear una publicación e indicar el motivo.");
        }
        boolean allowed = admin
                ? target != current
                : (current == ListingStatus.DRAFT && (target == ListingStatus.ACTIVE || target == ListingStatus.PENDING_REVIEW))
                    || (current == ListingStatus.ACTIVE && target == ListingStatus.PAUSED)
                    || (current == ListingStatus.PAUSED && target == ListingStatus.ACTIVE);
        if (!allowed) throw new DomainException(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", "El cambio de estado solicitado no está permitido.");
        listing.setStatus(target);
        listings.save(listing);
        audit.record("LISTING_STATE_CHANGED", principal.getId(), "LISTING", id, "SUCCESS", reason);
        return listing;
    }

    @Transactional(readOnly = true)
    public List<Listing> search(String city, LocalDate checkIn, LocalDate checkOut, Integer guests, String type,
            BigDecimal minPrice, BigDecimal maxPrice, Set<String> amenityIds) {
        if ((checkIn == null) != (checkOut == null)) throw validation("Debes indicar llegada y salida.");
        if (checkIn != null && !checkOut.isAfter(checkIn)) throw validation("La salida debe ser posterior a la llegada.");
        if (guests != null && guests < 1) throw validation("El número de huéspedes debe ser al menos 1.");
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0
                || minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) throw validation("El rango de precios no es válido.");
        List<Listing> results = new ArrayList<>();
        for (Listing listing : listings.findByStatus(ListingStatus.ACTIVE)) {
            if (city != null && !listing.getCity().toLowerCase(Locale.ROOT).contains(city.trim().toLowerCase(Locale.ROOT))) continue;
            if (type != null && !listing.getType().equalsIgnoreCase(type.trim())) continue;
            if (guests != null && listing.getCapacity() < guests) continue;
            if (minPrice != null && listing.getPricePerNight().compareTo(minPrice) < 0) continue;
            if (maxPrice != null && listing.getPricePerNight().compareTo(maxPrice) > 0) continue;
            if (amenityIds != null && !amenityIds.isEmpty() && !listing.getAmenityIds().containsAll(amenityIds)) continue;
            if (checkIn != null && !isAvailable(listing, checkIn, checkOut)) continue;
            results.add(listing);
        }
        results.sort(Comparator.comparing(Listing::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return results;
    }

    @Transactional(readOnly = true)
    public Listing getPublicOrOwned(UUID id, MarketplaceUser principal) {
        Listing listing = get(id);
        if (listing.getStatus() != ListingStatus.ACTIVE && (principal == null
                || !(principal.getRole() == AccountRole.ADMIN || listing.getHost().getId().equals(principal.getId())))) {
            throw new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la publicación.");
        }
        return listing;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityPeriod> getAvailability(UUID listingId, LocalDate from, LocalDate to) {
        Listing listing = get(listingId);
        if ((from == null) != (to == null) || from != null && !to.isAfter(from)) throw validation("El intervalo consultado no es válido.");
        return availability.findByListing_Id(listing.getId()).stream()
                .filter(period -> from == null || period.overlaps(from, to))
                .sorted(Comparator.comparing(AvailabilityPeriod::getStartDate)).toList();
    }

    @Transactional
    public List<AvailabilityPeriod> replaceAvailability(MarketplaceUser principal, UUID listingId, List<AvailabilityInput> periods) {
        Listing listing = get(listingId);
        requireOwnerOrAdmin(principal, listing);
        List<AvailabilityPeriod> current = availability.findByListing_Id(listingId);
        List<AvailabilityInput> incoming = periods == null ? List.of() : periods;
        List<AvailabilityInput> sorted = incoming.stream().sorted(Comparator.comparing(AvailabilityInput::startDate)).toList();
        for (int i = 0; i < sorted.size(); i++) {
            AvailabilityInput period = sorted.get(i);
            if (period.type() == AvailabilityType.RESERVED) throw validation("Las reservas no se pueden crear desde esta operación.");
            if (!period.endDate().isAfter(period.startDate())) throw validation("La salida debe ser posterior a la llegada.");
            if (i > 0 && sorted.get(i - 1).endDate().isAfter(period.startDate())) throw conflict("Los intervalos enviados se superponen.");
            if (current.stream().anyMatch(existing -> existing.getType() == AvailabilityType.RESERVED
                    && existing.overlaps(period.startDate(), period.endDate()))) throw conflict("El intervalo coincide con una reserva confirmada.");
        }
        availability.deleteByListing_IdAndTypeIn(listingId, List.of(AvailabilityType.AVAILABLE, AvailabilityType.BLOCKED));
        List<AvailabilityPeriod> saved = sorted.stream().map(period -> new AvailabilityPeriod(listing, period.startDate(),
                period.endDate(), period.type(), null)).toList();
        availability.saveAll(saved);
        audit.record("AVAILABILITY_UPDATED", principal.getId(), "LISTING", listingId, "SUCCESS", null);
        return availability.findByListing_Id(listingId);
    }

    private boolean isAvailable(Listing listing, LocalDate checkIn, LocalDate checkOut) {
        List<AvailabilityPeriod> periods = availability.findByListing_Id(listing.getId());
        boolean covered = periods.stream().anyMatch(period -> period.getType() == AvailabilityType.AVAILABLE
                && !period.getStartDate().isAfter(checkIn) && !period.getEndDate().isBefore(checkOut));
        return covered && periods.stream().noneMatch(period -> period.getType() != AvailabilityType.AVAILABLE && period.overlaps(checkIn, checkOut));
    }

    private Listing get(UUID id) { return listings.findById(id).orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la publicación.")); }
    private void requireOwnerOrAdmin(MarketplaceUser principal, Listing listing) {
        if (principal == null || !(principal.getRole() == AccountRole.ADMIN || listing.getHost().getId().equals(principal.getId()))) throw forbidden("No tienes permiso sobre esta publicación.");
    }
    private void requireRole(MarketplaceUser principal, AccountRole... roles) {
        if (principal == null || java.util.Arrays.stream(roles).noneMatch(role -> principal.getRole() == role)) throw forbidden("No tienes permiso para esta operación.");
    }
    private DomainException forbidden(String message) { return new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", message); }
    private DomainException validation(String message) { return new DomainException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message); }
    private DomainException conflict(String message) { return new DomainException(HttpStatus.CONFLICT, "CONFLICT", message); }

    public record ListingInput(@NotBlank @Size(max=160) String title, @NotBlank @Size(max=4000) String description,
            @NotBlank @Size(max=120) String city, @NotBlank @Size(max=240) String location,
            @NotBlank @Size(max=50) String type, @Min(1) int capacity,
            @NotNull @DecimalMin(value="0.01") BigDecimal pricePerNight, Set<String> amenityIds,
            @Size(max=2000) String rules, Set<String> imageReferences) { }
    public record AvailabilityInput(@NotNull LocalDate startDate, @NotNull LocalDate endDate, @NotNull AvailabilityType type) { }
}
