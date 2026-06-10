package com.hbcstore.hbcstore_api.notification;

import com.hbcstore.hbcstore_api.notification.dto.NotificationListResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class UserNotificationController {
    private final NotificationService notificationService;

    public UserNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationListResponse getLatestMine(Principal principal) {
        return notificationService.getLatestMine(principal.getName());
    }

    @PatchMapping("/read-all")
    public NotificationListResponse markAllAsRead(Principal principal) {
        return notificationService.markAllMineAsRead(principal.getName());
    }
}
