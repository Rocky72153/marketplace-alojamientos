package co.edu.unbosque.marketplace.booking;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.audit.AuditService;
import co.edu.unbosque.marketplace.listing.Listing;
import co.edu.unbosque.marketplace.listing.ListingExtraOption;
import co.edu.unbosque.marketplace.listing.ListingService;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final ListingService listings;
    private final ReservationRepository reservations;
    private final BookingPricePolicy pricing;
    private final TestPaymentSimulator payments;
    private final AuditService audit;

    public ReservationService(ListingService listings, ReservationRepository reservations, BookingPricePolicy pricing,
            TestPaymentSimulator payments, AuditService audit) {
        this.listings = listings;
        this.reservations = reservations;
        this.pricing = pricing;
        this.payments = payments;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public QuoteView quote(UUID listingId, QuoteRequest request) {
        Listing listing = listings.getActiveListing(listingId);
        return calculateQuote(listing, request).toView();
    }

    @Transactional
    public ReservationView reserve(MarketplaceUser guest, ReservationRequest request, String rawIdempotencyKey) {
        requireGuest(guest);
        String idempotencyKey = rawIdempotencyKey == null ? "" : rawIdempotencyKey.trim();
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw validation("Debes enviar una clave de idempotencia válida.");
        }
        String fingerprint = fingerprint(request);
        Reservation previous = reservations.findByGuest_IdAndIdempotencyKey(guest.getId(), idempotencyKey).orElse(null);
        if (previous != null) return returnIdempotent(previous, fingerprint);
        Listing listing = listings.lockActiveListing(request.listingId());
        previous = reservations.findByGuest_IdAndIdempotencyKey(guest.getId(), idempotencyKey).orElse(null);
        if (previous != null) return returnIdempotent(previous, fingerprint);

        Quote quote = calculateQuote(listing, request.toQuoteRequest());
        String paymentReference = payments.authorize();
        Reservation reservation = new Reservation(guest, listing, idempotencyKey, fingerprint,
                quote.checkIn(), quote.checkOut(), quote.nights(), quote.guests(), quote.pricePerNight(),
                quote.nightlySubtotal(), quote.cleaningFee(), quote.serviceFee(), quote.discountAmount(),
                quote.extrasTotal(), quote.total(), "COP", paymentReference, listing.getCancellationFreeRefundDays(),
                listing.getCancellationPartialRefundDays(), listing.getCancellationPartialRefundPercentage(),
                quote.extras().stream().map(ExtraLine::toReservationExtra).toList());
        reservation = reservations.saveAndFlush(reservation);

        listings.confirmReservation(listing.getId(), quote.checkIn(), quote.checkOut(), reservation.getId().toString());
        audit.recordEvent("PAYMENT_TEST_APPROVED", guest.getId(), "RESERVATION", reservation.getId(), "SUCCESS", paymentReference);
        audit.recordEvent("RESERVATION_CONFIRMED", guest.getId(), "RESERVATION", reservation.getId(), "SUCCESS", null);
        return ReservationView.from(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationView> history(MarketplaceUser guest) {
        requireGuest(guest);
        return reservations.findByGuest_IdOrderByCreatedAtDesc(guest.getId()).stream().map(ReservationView::from).toList();
    }

    @Transactional(readOnly = true)
    public ReservationView get(MarketplaceUser guest, UUID reservationId) {
        requireGuest(guest);
        Reservation reservation = reservations.findByIdAndGuest_Id(reservationId, guest.getId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la reserva."));
        return ReservationView.from(reservation);
    }

    @Transactional
    public ReservationView cancel(MarketplaceUser guest, UUID reservationId, CancelRequest request) {
        requireGuest(guest);
        Reservation reservation = reservations.findByIdAndGuest_Id(reservationId, guest.getId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "NOT_FOUND", "No se encontró la reserva."));
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw conflict("La reserva ya no se puede cancelar.");
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (!reservation.getCheckIn().isAfter(today)) {
            throw conflict("La reserva ya comenzó y no se puede cancelar desde este flujo.");
        }

        long daysBeforeCheckIn = ChronoUnit.DAYS.between(today, reservation.getCheckIn());
        BigDecimal refundPercentage;
        if (daysBeforeCheckIn >= reservation.getPolicyFreeRefundDays()) {
            refundPercentage = new BigDecimal("100.00");
        } else if (daysBeforeCheckIn >= reservation.getPolicyPartialRefundDays()) {
            refundPercentage = reservation.getPolicyPartialRefundPercentage();
        } else {
            refundPercentage = ZERO;
        }
        BigDecimal refundAmount = reservation.getTotal().multiply(refundPercentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        listings.releaseReservation(reservation.getListing().getId(), reservation.getId());
        reservation.cancel(request.reason().trim(), Instant.now(), refundPercentage, refundAmount);
        Reservation saved = reservations.save(reservation);
        String auditReason = request.reason().trim() + " | Reembolso de prueba: " + refundPercentage + "% / " + refundAmount + " COP";
        audit.recordEvent("RESERVATION_CANCELLED", guest.getId(), "RESERVATION", reservationId, "SUCCESS", auditReason);
        return ReservationView.from(saved);
    }

    private Quote calculateQuote(Listing listing, QuoteRequest request) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (request.checkIn() == null || request.checkOut() == null || !request.checkOut().isAfter(request.checkIn())) {
            throw validation("La salida debe ser posterior a la llegada.");
        }
        if (request.checkIn().isBefore(today)) throw validation("La llegada no puede estar en el pasado.");
        if (ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()) > 365) {
            throw validation("La estadía no puede superar 365 noches.");
        }
        if (request.guests() < 1 || request.guests() > listing.getCapacity()) {
            throw validation("El número de huéspedes debe estar entre 1 y la capacidad del alojamiento.");
        }
        if (!listings.hasAvailableRange(listing.getId(), request.checkIn(), request.checkOut())) {
            throw new DomainException(HttpStatus.CONFLICT, "UNAVAILABLE", "El alojamiento no está disponible para ese periodo.");
        }

        int nights = Math.toIntExact(ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()));
        BigDecimal nightlySubtotal = money(listing.getPricePerNight().multiply(BigDecimal.valueOf(nights)));
        BigDecimal cleaningFee = pricing.cleaningFee();
        BigDecimal serviceFee = money(nightlySubtotal.multiply(pricing.serviceFeeRate()));
        BigDecimal discountAmount = money(nightlySubtotal.multiply(pricing.discountRate()));
        List<ExtraLine> extraLines = calculateExtras(listing, request.extras());
        BigDecimal extrasTotal = extraLines.stream().map(ExtraLine::lineTotal).reduce(ZERO, BigDecimal::add);
        BigDecimal total = money(nightlySubtotal.add(cleaningFee).add(serviceFee).add(extrasTotal).subtract(discountAmount));
        if (total.signum() < 0 || total.precision() > 12) throw validation("El total de la cotización excede el límite admitido.");
        return new Quote(listing, request.checkIn(), request.checkOut(), nights, request.guests(),
                listing.getPricePerNight(), nightlySubtotal, cleaningFee, serviceFee, discountAmount, extraLines,
                extrasTotal, total);
    }

    private List<ExtraLine> calculateExtras(Listing listing, List<ExtraQuantity> requested) {
        List<ExtraQuantity> extras = requested == null ? List.of() : requested;
        Set<String> ids = new HashSet<>();
        List<ExtraLine> lines = new ArrayList<>();
        for (ExtraQuantity quantity : extras) {
            if (!ids.add(quantity.extraId())) throw validation("Un extra no se puede repetir en la cotización.");
            ListingExtraOption option = listing.getExtraOptions().stream().filter(candidate -> candidate.getId().equals(quantity.extraId()))
                    .findFirst().orElseThrow(() -> validation("Uno de los extras ya no está disponible."));
            BigDecimal lineTotal = money(option.getUnitPrice().multiply(BigDecimal.valueOf(quantity.quantity())));
            lines.add(new ExtraLine(option.getId(), option.getName(), quantity.quantity(), option.getUnitPrice(), lineTotal));
        }
        return List.copyOf(lines);
    }

    private String fingerprint(ReservationRequest request) {
        String extras = request.extras() == null ? "" : request.extras().stream()
                .sorted(Comparator.comparing(ExtraQuantity::extraId))
                .map(item -> item.extraId() + ":" + item.quantity()).reduce((left, right) -> left + "," + right).orElse("");
        String canonical = request.listingId() + "|" + request.checkIn() + "|" + request.checkOut() + "|"
                + request.guests() + "|" + extras;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No fue posible validar la idempotencia.", exception);
        }
    }

    private ReservationView returnIdempotent(Reservation reservation, String fingerprint) {
        if (!reservation.getRequestFingerprint().equals(fingerprint)) {
            throw conflict("La clave de idempotencia ya se usó con otros datos.");
        }
        return ReservationView.from(reservation);
    }

    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }

    private void requireGuest(MarketplaceUser guest) {
        if (guest == null || guest.getRole() != AccountRole.GUEST) {
            throw new DomainException(HttpStatus.FORBIDDEN, "GUEST_REQUIRED", "Solo una cuenta de huésped puede gestionar reservas.");
        }
    }

    private DomainException validation(String message) { return new DomainException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message); }
    private DomainException conflict(String message) { return new DomainException(HttpStatus.CONFLICT, "CONFLICT", message); }

    public record ExtraQuantity(@NotBlank @Size(max=36) String extraId, @Min(1) @Max(99) int quantity) { }
    public record QuoteRequest(@NotNull LocalDate checkIn, @NotNull LocalDate checkOut, @Min(1) int guests,
            @Valid List<ExtraQuantity> extras) { }
    public record ReservationRequest(@NotNull UUID listingId, @NotNull LocalDate checkIn, @NotNull LocalDate checkOut,
            @Min(1) int guests, @Valid List<ExtraQuantity> extras) {
        QuoteRequest toQuoteRequest() { return new QuoteRequest(checkIn, checkOut, guests, extras); }
    }
    public record CancelRequest(@NotBlank @Size(max=1000) String reason) { }
    public record ExtraLine(String id, String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        ReservationExtra toReservationExtra() { return new ReservationExtra(id, name, quantity, unitPrice, lineTotal); }
    }
    public record QuoteView(UUID listingId, String listingTitle, LocalDate checkIn, LocalDate checkOut, int nights,
            int guests, BigDecimal pricePerNight, BigDecimal nightlySubtotal, BigDecimal cleaningFee,
            BigDecimal serviceFee, BigDecimal discountAmount, List<ExtraLine> extras, BigDecimal extrasTotal,
            BigDecimal total, String currency, int freeRefundDays, int partialRefundDays,
            BigDecimal partialRefundPercentage) { }
    public record ReservationView(UUID id, UUID listingId, String listingTitle, LocalDate checkIn, LocalDate checkOut,
            int nights, int guests, BigDecimal pricePerNight, BigDecimal nightlySubtotal, BigDecimal cleaningFee,
            BigDecimal serviceFee, BigDecimal discountAmount, List<ReservationExtra> extras, BigDecimal extrasTotal,
            BigDecimal total, String currency, String paymentStatus, ReservationStatus status, Instant createdAt,
            String cancellationReason, Instant cancelledAt, BigDecimal refundPercentage, BigDecimal refundAmount) {
        static ReservationView from(Reservation reservation) {
            return new ReservationView(reservation.getId(), reservation.getListing().getId(),
                    reservation.getListing().getTitle(), reservation.getCheckIn(), reservation.getCheckOut(),
                    reservation.getNights(), reservation.getGuests(), reservation.getPricePerNight(),
                    reservation.getNightlySubtotal(), reservation.getCleaningFee(), reservation.getServiceFee(),
                    reservation.getDiscountAmount(), reservation.getExtras(), reservation.getExtrasTotal(),
                    reservation.getTotal(), reservation.getCurrency(), reservation.getPaymentStatus(),
                    reservation.getStatus(), reservation.getCreatedAt(), reservation.getCancellationReason(),
                    reservation.getCancelledAt(), reservation.getRefundPercentage(), reservation.getRefundAmount());
        }
    }

    private record Quote(Listing listing, LocalDate checkIn, LocalDate checkOut, int nights, int guests,
            BigDecimal pricePerNight, BigDecimal nightlySubtotal, BigDecimal cleaningFee, BigDecimal serviceFee,
            BigDecimal discountAmount, List<ExtraLine> extras, BigDecimal extrasTotal, BigDecimal total) {
        QuoteView toView() {
            return new QuoteView(listing.getId(), listing.getTitle(), checkIn, checkOut, nights, guests,
                    pricePerNight, nightlySubtotal, cleaningFee, serviceFee, discountAmount, extras, extrasTotal,
                    total, "COP", listing.getCancellationFreeRefundDays(), listing.getCancellationPartialRefundDays(),
                    listing.getCancellationPartialRefundPercentage());
        }
    }
}
