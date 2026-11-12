package co.edu.unbosque.marketplace.notification;

import co.edu.unbosque.marketplace.security.MarketplaceUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) { this.notifications = notifications; }

    @GetMapping
    public List<NotificationService.NotificationView> list(@AuthenticationPrincipal MarketplaceUser principal) {
        return notifications.list(principal);
    }

    @GetMapping("/{notificationId}")
    public NotificationService.NotificationView get(@AuthenticationPrincipal MarketplaceUser principal,
            @PathVariable UUID notificationId) {
        return notifications.get(principal, notificationId);
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationService.NotificationView markRead(@AuthenticationPrincipal MarketplaceUser principal,
            @PathVariable UUID notificationId) {
        return notifications.markRead(principal, notificationId);
    }
}
