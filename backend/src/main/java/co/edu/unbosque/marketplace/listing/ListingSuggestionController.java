package co.edu.unbosque.marketplace.listing;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public discovery cards. Select only the fields shown on the home page and
 * bound the query in the database, without loading the full listing catalogue.
 */
@RestController
@RequestMapping("/api/listings")
public class ListingSuggestionController {
    private static final int SUGGESTION_LIMIT = 8;
    private final EntityManager entityManager;

    public ListingSuggestionController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GetMapping("/suggestions")
    @Transactional(readOnly = true)
    public SuggestionsResponse suggestions() {
        List<Suggestion> items = entityManager.createQuery("""
                select l.id, l.title, l.city, l.type, l.capacity, l.pricePerNight
                from Listing l where l.status = :status
                  and l.host.accountState = co.edu.unbosque.marketplace.security.AccountState.ACTIVE
                order by l.createdAt desc, l.id asc
                """, Object[].class)
                .setParameter("status", ListingStatus.ACTIVE)
                .setMaxResults(SUGGESTION_LIMIT)
                .getResultList().stream()
                .map(row -> new Suggestion((UUID) row[0], (String) row[1], (String) row[2],
                        (String) row[3], (Integer) row[4], (BigDecimal) row[5]))
                .toList();
        return new SuggestionsResponse(items);
    }

    public record Suggestion(UUID id, String title, String city, String type,
            int capacity, BigDecimal pricePerNight) { }
    public record SuggestionsResponse(List<Suggestion> items) { }
}
