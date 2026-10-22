package co.edu.unbosque.marketplace.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceUserRepository extends JpaRepository<MarketplaceUser, UUID> {

    Optional<MarketplaceUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
