package co.edu.unbosque.marketplace.listing;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListingService {
    private static final String LISTING_ENTITY = "LISTING";
    private static final String SUCCESS = "SUCCESS";
    private final ListingRepository listings;
    private final AvailabilityRepository availability;
    private final AuditService audit;

    public ListingService(ListingRepository listings, AvailabilityRepository availability, AuditService audit) {
        this.listings = listings; this.availability = availability; this.audit = audit;
    }

    @Transactional
    public Listing create(MarketplaceUser principal, ListingInput input) {
        requireRole(principal, AccountRole.HOST, AccountRole.ADMIN);
        MarketplaceUser host = principal;
        Listing listing = new Listing(host, input.toData());
        applyCommercialTerms(listing, input, true);
        listing = listings.save(listing);
        audit.recordEvent("LISTING_CREATED", principal.getId(), LISTING_ENTITY, listing.getId(), SUCCESS, null);
        return listing;
    }

    @Transactional
    public Listing update(MarketplaceUser principal, UUID id, ListingInput input) {
        Listing listing = get(id);
        requireOwnerOrAdmin(principal, listing);
        if (listing.getStatus() == ListingStatus.BLOCKED) throw forbidden("Una publicación bloqueada no se puede editar.");
        listing.update(input.toData());
        applyCommercialTerms(listing, input, false);
        audit.recordEvent("LISTING_UPDATED", principal.getId(), LISTING_ENTITY, id, SUCCESS, null);
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
        audit.recordEvent("LISTING_STATE_CHANGED", principal.getId(), LISTING_ENTITY, id, SUCCESS, reason);
        return listing;
    }

    @Transactional(readOnly = true)
    public List<Listing> search(String city, LocalDate checkIn, LocalDate checkOut, Integer guests, String type,
            BigDecimal minPrice, BigDecimal maxPrice, Set<String> amenityIds) {
        if ((checkIn == null) != (checkOut == null)) throw validation("Debes indicar llegada y salida.");
        if (checkIn != null && !checkOut.isAfter(checkIn)) throw validation("La salida debe ser posterior a la llegada.");
        if (guests != null && guests < 1) throw validation("El número de huéspedes debe ser al menos 1.");
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)
                || minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) throw validation("El rango de precios no es válido.");
        SearchFilters filters = new SearchFilters(city, checkIn, checkOut, guests, type, minPrice, maxPrice, amenityIds);
        return listings.findByStatus(ListingStatus.ACTIVE).stream()
                .filter(listing -> matchesFilters(listing, filters))
                .sorted(Comparator.comparing(Listing::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
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
        return availability.findByListing_IdOrderByStartDateAsc(listing.getId()).stream()
                .filter(period -> from == null || period.overlaps(from, to))
                .sorted(Comparator.comparing(AvailabilityPeriod::getStartDate)).toList();
    }

    @Transactional(readOnly = true)
    public Listing getActiveListing(UUID listingId) {
        Listing listing = get(listingId);
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la publicación.");
        }
        return listing;
    }

    @Transactional
    public Listing lockActiveListing(UUID listingId) {
        Listing listing = getForUpdate(listingId);
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la publicación.");
        }
        return listing;
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableRange(UUID listingId, LocalDate startDate, LocalDate endDate) {
        Listing listing = get(listingId);
        List<AvailabilityPeriod> current = availability.findByListing_IdOrderByStartDateAsc(listingId);
        boolean conflicts = current.stream().anyMatch(period -> period.getType() != AvailabilityType.AVAILABLE
                && period.overlaps(startDate, endDate));
        return !conflicts && coversAvailability(current, startDate, endDate);
    }

    @Transactional
    public List<AvailabilityPeriod> replaceAvailability(MarketplaceUser principal, UUID listingId, List<AvailabilityInput> periods) {
        Listing listing = getForUpdate(listingId);
        requireOwnerOrAdmin(principal, listing);
        List<AvailabilityPeriod> current = availability.findByListing_IdOrderByStartDateAsc(listingId);
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
        audit.recordEvent("AVAILABILITY_UPDATED", principal.getId(), LISTING_ENTITY, listingId, SUCCESS, null);
        return availability.findByListing_IdOrderByStartDateAsc(listingId);
    }

    @Transactional
    public AvailabilityPeriod confirmReservation(UUID listingId, LocalDate startDate, LocalDate endDate,
            String reservationReference) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw validation("El intervalo de la reserva no es válido.");
        }
        if (reservationReference == null || reservationReference.isBlank() || reservationReference.length() > 100) {
            throw validation("La referencia de la reserva no es válida.");
        }

        Listing listing = getForUpdate(listingId);
        if (listing.getStatus() != ListingStatus.ACTIVE) throw conflict("La publicación ya no está activa.");
        List<AvailabilityPeriod> current = availability.findByListing_IdOrderByStartDateAsc(listingId);
        boolean conflicts = current.stream().anyMatch(period -> period.getType() != AvailabilityType.AVAILABLE
                && period.overlaps(startDate, endDate));
        List<AvailabilityPeriod> availablePeriods = current.stream()
                .filter(period -> period.getType() == AvailabilityType.AVAILABLE && period.overlaps(startDate, endDate))
                .toList();
        if (conflicts || !coversAvailability(availablePeriods, startDate, endDate)) {
            throw conflict("El intervalo ya no está disponible para reservar.");
        }

        availability.deleteAll(availablePeriods);
        for (AvailabilityPeriod period : availablePeriods) {
            if (period.getStartDate().isBefore(startDate)) {
                availability.save(new AvailabilityPeriod(listing, period.getStartDate(), startDate,
                        AvailabilityType.AVAILABLE, null));
            }
            if (period.getEndDate().isAfter(endDate)) {
                availability.save(new AvailabilityPeriod(listing, endDate, period.getEndDate(),
                        AvailabilityType.AVAILABLE, null));
            }
        }
        AvailabilityPeriod reserved = availability.save(new AvailabilityPeriod(listing, startDate, endDate,
                AvailabilityType.RESERVED, reservationReference));
        audit.recordEvent("AVAILABILITY_RESERVED", null, LISTING_ENTITY, listingId, SUCCESS, reservationReference);
        return reserved;
    }

    @Transactional
    public void releaseReservation(UUID listingId, UUID reservationId) {
        Listing listing = getForUpdate(listingId);
        AvailabilityPeriod reserved = availability.findByListing_IdAndReservationReference(listingId, reservationId.toString())
                .filter(period -> period.getType() == AvailabilityType.RESERVED)
                .orElseThrow(() -> conflict("No se encontró el periodo reservado para liberar."));
        availability.deleteById(reserved.getId());

        List<AvailabilityPeriod> existingAvailable = new ArrayList<>(availability
                .findByListing_IdAndType(listingId, AvailabilityType.AVAILABLE));
        List<AvailabilityPeriod> availablePeriods = new ArrayList<>(existingAvailable);
        availablePeriods.add(new AvailabilityPeriod(listing, reserved.getStartDate(), reserved.getEndDate(),
                AvailabilityType.AVAILABLE, null));
        availablePeriods.sort(Comparator.comparing(AvailabilityPeriod::getStartDate));
        availability.deleteAll(existingAvailable);

        List<AvailabilityPeriod> merged = new ArrayList<>();
        LocalDate mergedStart = null;
        LocalDate mergedEnd = null;
        for (AvailabilityPeriod period : availablePeriods) {
            if (mergedEnd == null || period.getStartDate().isAfter(mergedEnd)) {
                if (mergedStart != null) merged.add(new AvailabilityPeriod(listing, mergedStart, mergedEnd,
                        AvailabilityType.AVAILABLE, null));
                mergedStart = period.getStartDate();
                mergedEnd = period.getEndDate();
            } else if (period.getEndDate().isAfter(mergedEnd)) {
                mergedEnd = period.getEndDate();
            }
        }
        if (mergedStart != null) merged.add(new AvailabilityPeriod(listing, mergedStart, mergedEnd,
                AvailabilityType.AVAILABLE, null));
        availability.saveAll(merged);
        audit.recordEvent("AVAILABILITY_RELEASED", null, LISTING_ENTITY, listingId, SUCCESS, reservationId.toString());
    }

    private boolean isAvailable(Listing listing, LocalDate checkIn, LocalDate checkOut) {
        List<AvailabilityPeriod> periods = availability.findByListing_IdOrderByStartDateAsc(listing.getId());
        boolean covered = coversAvailability(periods, checkIn, checkOut);
        return covered && periods.stream().noneMatch(period -> period.getType() != AvailabilityType.AVAILABLE && period.overlaps(checkIn, checkOut));
    }

    private boolean coversAvailability(List<AvailabilityPeriod> periods, LocalDate startDate, LocalDate endDate) {
        LocalDate cursor = startDate;
        for (AvailabilityPeriod period : periods.stream().filter(p -> p.getType() == AvailabilityType.AVAILABLE)
                .sorted(Comparator.comparing(AvailabilityPeriod::getStartDate)).toList()) {
            if (!period.getEndDate().isAfter(cursor)) continue;
            if (period.getStartDate().isAfter(cursor)) return false;
            if (period.getEndDate().isAfter(cursor)) cursor = period.getEndDate();
            if (!cursor.isBefore(endDate)) return true;
        }
        return false;
    }

    private boolean matchesFilters(Listing listing, SearchFilters filters) {
        return matchesCity(listing, filters.city()) && matchesType(listing, filters.type()) && hasCapacity(listing, filters.guests())
                && withinPriceRange(listing, filters.minPrice(), filters.maxPrice()) && hasAmenities(listing, filters.amenityIds())
                && (filters.checkIn() == null || isAvailable(listing, filters.checkIn(), filters.checkOut()));
    }

    private boolean matchesCity(Listing listing, String city) {
        return city == null || listing.getCity().toLowerCase(Locale.ROOT).contains(city.trim().toLowerCase(Locale.ROOT));
    }

    private boolean matchesType(Listing listing, String type) {
        return type == null || listing.getType().equalsIgnoreCase(type.trim());
    }

    private boolean hasCapacity(Listing listing, Integer guests) {
        return guests == null || listing.getCapacity() >= guests;
    }

    private boolean withinPriceRange(Listing listing, BigDecimal minPrice, BigDecimal maxPrice) {
        return (minPrice == null || listing.getPricePerNight().compareTo(minPrice) >= 0)
                && (maxPrice == null || listing.getPricePerNight().compareTo(maxPrice) <= 0);
    }

    private boolean hasAmenities(Listing listing, Set<String> amenityIds) {
        return amenityIds == null || amenityIds.isEmpty() || listing.getAmenityIds().containsAll(amenityIds);
    }

    private Listing get(UUID id) { return listings.findById(id).orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la publicación.")); }
    private Listing getForUpdate(UUID id) { return listings.lockById(id).orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la publicación.")); }
    private void requireOwnerOrAdmin(MarketplaceUser principal, Listing listing) {
        if (principal == null || !(principal.getRole() == AccountRole.ADMIN || listing.getHost().getId().equals(principal.getId()))) throw forbidden("No tienes permiso sobre esta publicación.");
    }
    private void requireRole(MarketplaceUser principal, AccountRole... roles) {
        if (principal == null || java.util.Arrays.stream(roles).noneMatch(role -> principal.getRole() == role)) throw forbidden("No tienes permiso para esta operación.");
    }
    private DomainException forbidden(String message) { return new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", message); }
    private DomainException validation(String message) { return new DomainException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message); }
    private DomainException conflict(String message) { return new DomainException(HttpStatus.CONFLICT, "CONFLICT", message); }

    private void applyCommercialTerms(Listing listing, ListingInput input, boolean creating) {
        if (creating || input.extraOptions() != null) {
            List<ListingExtraOption> options = input.extraOptions() == null ? List.of() : input.extraOptions().stream()
                    .map(option -> new ListingExtraOption(option.id() == null || option.id().isBlank()
                            ? UUID.randomUUID().toString() : option.id(), option.name().trim(),
                            option.unitPrice().setScale(2, java.math.RoundingMode.HALF_UP)))
                    .toList();
            Set<String> ids = new HashSet<>();
            if (options.stream().anyMatch(option -> !ids.add(option.getId()))) throw validation("Cada extra debe tener un identificador único.");
            listing.setExtraOptions(options);
        }
        CancellationPolicyInput policy = input.cancellationPolicy();
        if (policy != null) {
            if (policy.freeRefundDays() < policy.partialRefundDays()) {
                throw validation("Los días para el reembolso completo deben ser mayores o iguales al umbral parcial.");
            }
            listing.setCancellationPolicy(policy.freeRefundDays(), policy.partialRefundDays(),
                    policy.partialRefundPercentage().setScale(2, java.math.RoundingMode.HALF_UP));
        }
    }

    public record ListingInput(@NotBlank @Size(max=160) String title, @NotBlank @Size(max=4000) String description,
            @NotBlank @Size(max=120) String city, @NotBlank @Size(max=240) String location,
            @NotBlank @Size(max=50) String type, @Min(1) int capacity,
            @NotNull @DecimalMin(value="0.01") BigDecimal pricePerNight, Set<String> amenityIds,
            @Size(max=2000) String rules, Set<String> imageReferences,
            @Valid List<ExtraInput> extraOptions, @Valid CancellationPolicyInput cancellationPolicy) {
        public ListingInput(String title, String description, String city, String location, String type, int capacity,
                BigDecimal pricePerNight, Set<String> amenityIds, String rules, Set<String> imageReferences) {
            this(title, description, city, location, type, capacity, pricePerNight, amenityIds, rules,
                    imageReferences, null, null);
        }

        ListingData toData() {
            return new ListingData(title, description, city, location, type, capacity, pricePerNight,
                    amenityIds, rules, imageReferences);
        }
    }
    public record ExtraInput(@Size(max=36) String id, @NotBlank @Size(max=80) String name,
            @NotNull @DecimalMin(value="0.01") BigDecimal unitPrice) { }
    public record CancellationPolicyInput(@Min(0) int freeRefundDays, @Min(0) int partialRefundDays,
            @NotNull @DecimalMin(value="0.00") @DecimalMax(value="100.00") BigDecimal partialRefundPercentage) { }
    public record AvailabilityInput(@NotNull LocalDate startDate, @NotNull LocalDate endDate, @NotNull AvailabilityType type) { }
    private record SearchFilters(String city, LocalDate checkIn, LocalDate checkOut, Integer guests, String type,
            BigDecimal minPrice, BigDecimal maxPrice, Set<String> amenityIds) { }
}
