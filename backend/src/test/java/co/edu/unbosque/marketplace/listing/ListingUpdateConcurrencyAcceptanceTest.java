package co.edu.unbosque.marketplace.listing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.unbosque.marketplace.api.DomainException;
import co.edu.unbosque.marketplace.security.AccountRole;
import co.edu.unbosque.marketplace.security.MarketplaceUser;
import co.edu.unbosque.marketplace.security.MarketplaceUserRepository;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ListingUpdateConcurrencyAcceptanceTest {
    @Autowired MarketplaceUserRepository users;
    @Autowired ListingRepository listings;
    @Autowired ListingService service;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void hostCannotOverwritePublicationBlockedByConcurrentModeration() throws Exception {
        MarketplaceUser host = users.saveAndFlush(new MarketplaceUser("Anfitrión", UUID.randomUUID() + "@example.test",
                "hash", AccountRole.HOST));
        Listing listing = listings.saveAndFlush(new Listing(host, new ListingData("Título original", "Descripción",
                "Bogotá", "Centro", "ROOM", 2, new BigDecimal("100000.00"), Set.of(), "", Set.of())));
        UUID id = listing.getId();
        ListingService.ListingInput changes = new ListingService.ListingInput("Título editado", "Descripción",
                "Bogotá", "Centro", "ROOM", 2, new BigDecimal("100000.00"), Set.of(), "", Set.of());
        CountDownLatch moderatorHasLock = new CountDownLatch(1);
        CountDownLatch commitModeration = new CountDownLatch(1);
        CountDownLatch editorStarted = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> moderation = workers.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(ignored -> {
                Listing locked = listings.lockById(id).orElseThrow();
                locked.setStatus(ListingStatus.BLOCKED);
                listings.saveAndFlush(locked);
                moderatorHasLock.countDown();
                try {
                    if (!commitModeration.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Moderation timed out");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
            }));
            assertTrue(moderatorHasLock.await(10, TimeUnit.SECONDS));
            Future<?> edit = workers.submit(() -> { editorStarted.countDown(); service.update(host, id, changes); });
            assertTrue(editorStarted.await(10, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> edit.get(500, TimeUnit.MILLISECONDS));
            commitModeration.countDown();
            moderation.get(10, TimeUnit.SECONDS);
            ExecutionException failure = assertThrows(ExecutionException.class, () -> edit.get(10, TimeUnit.SECONDS));
            DomainException forbidden = assertInstanceOf(DomainException.class, failure.getCause());
            assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatus());
            Listing actual = listings.findById(id).orElseThrow();
            assertEquals(ListingStatus.BLOCKED, actual.getStatus());
            assertEquals("Título original", actual.getTitle());
        } finally {
            commitModeration.countDown();
            workers.shutdownNow();
        }
    }
}
