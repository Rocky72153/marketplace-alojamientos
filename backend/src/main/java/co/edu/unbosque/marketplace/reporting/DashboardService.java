package co.edu.unbosque.marketplace.reporting;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.booking.ReservationStatus;
import co.edu.unbosque.marketplace.listing.ListingStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private static final String RESERVATION_RANGE = " from Reservation r where r.createdAt <= :cutoff";
    private final EntityManager entityManager;

    public DashboardService(EntityManager entityManager) { this.entityManager = entityManager; }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Dashboard snapshot(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "La fecha inicial no puede ser posterior a la final.");
        }
        if ((from != null && (from.getYear() < 1 || from.getYear() > 9998))
                || (to != null && (to.getYear() < 1 || to.getYear() > 9998))) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", "La fecha debe estar entre los años 1 y 9998.");
        }
        Instant cutoff = Instant.now();
        String range = RESERVATION_RANGE + (from == null ? "" : " and r.createdAt >= :start")
                + (to == null ? "" : " and r.createdAt < :end");
        long users = entityManager.createQuery("select count(u) from MarketplaceUser u where u.createdAt <= :cutoff", Long.class)
                .setParameter("cutoff", cutoff).getSingleResult();
        long listings = entityManager.createQuery("select count(l) from Listing l where l.status = :active and l.createdAt <= :cutoff", Long.class)
                .setParameter("active", ListingStatus.ACTIVE).setParameter("cutoff", cutoff).getSingleResult();
        Map<ReservationStatus, Long> states = new EnumMap<>(ReservationStatus.class);
        for (ReservationStatus state : ReservationStatus.values()) states.put(state, 0L);
        query("select r.status, count(r)" + range + " group by r.status", Object[].class, from, to, cutoff)
                .getResultList().forEach(row -> states.put((ReservationStatus) row[0], (Long) row[1]));
        BigDecimal revenue = query("select coalesce(sum(r.total), 0)" + range
                        + " and r.status in (:included)", BigDecimal.class, from, to, cutoff)
                .setParameter("included", List.of(ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED)).getSingleResult();
        List<CityDemand> cities = query("select r.listing.city, count(r)" + range
                        + " and r.status in (:included) group by r.listing.city order by count(r) desc, r.listing.city asc",
                Object[].class, from, to, cutoff)
                .setParameter("included", List.of(ReservationStatus.CONFIRMED, ReservationStatus.COMPLETED))
                .setMaxResults(10).getResultList().stream().map(row -> new CityDemand((String) row[0], (Long) row[1])).toList();
        return new Dashboard(cutoff, from, to, ZONE.getId(), users, listings, states, revenue, "COP", cities,
                new Definitions("Total de cuentas creadas hasta el corte; todos los estados.",
                        "Publicaciones actualmente activas creadas hasta el corte; sin filtro de periodo.",
                        "Reservas por estado actual, filtradas por fecha de creación en Bogotá; extremos inclusivos.",
                        "Suma de totales de reservas confirmadas y completadas del periodo; excluye canceladas. Pagos simulados, no ingresos reales.",
                        "Hasta 10 ciudades por número de reservas confirmadas o completadas creadas en el periodo; empates por ciudad."));
    }

    private <T> TypedQuery<T> query(String jpql, Class<T> type, LocalDate from, LocalDate to, Instant cutoff) {
        TypedQuery<T> query = entityManager.createQuery(jpql, type).setParameter("cutoff", cutoff);
        if (from != null) query.setParameter("start", from.atStartOfDay(ZONE).toInstant());
        if (to != null) query.setParameter("end", to.plusDays(1).atStartOfDay(ZONE).toInstant());
        return query;
    }

    public record CityDemand(String city, long bookings) { }
    public record Definitions(String users, String activeListings, String bookings, String estimatedRevenue, String topCities) { }
    public record Dashboard(Instant generatedAt, LocalDate from, LocalDate to, String timezone,
            long users, long activeListings, Map<ReservationStatus, Long> bookingsByStatus,
            BigDecimal estimatedRevenue, String currency, List<CityDemand> topCities, Definitions definitions) { }
}
