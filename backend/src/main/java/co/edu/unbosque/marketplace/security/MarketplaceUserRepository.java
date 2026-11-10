package co.edu.unbosque.marketplace.security;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarketplaceUserRepository extends JpaRepository<MarketplaceUser, UUID> {

    Optional<MarketplaceUser> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from MarketplaceUser user where user.id = :id")
    Optional<MarketplaceUser> lockForReservationById(@Param("id") UUID id);
}
