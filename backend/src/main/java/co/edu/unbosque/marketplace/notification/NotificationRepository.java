package co.edu.unbosque.marketplace.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(UUID recipientId);
    Optional<Notification> findByIdAndRecipient_Id(UUID id, UUID recipientId);
}
