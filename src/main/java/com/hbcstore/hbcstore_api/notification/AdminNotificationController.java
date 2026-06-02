package com.hbcstore.hbcstore_api.notification;

import com.hbcstore.hbcstore_api.notification.dto.AdminNotificationListResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {
    private final AdminNotificationService adminNotificationService;

    public AdminNotificationController(AdminNotificationService adminNotificationService) {
        this.adminNotificationService = adminNotificationService;
    }

    @GetMapping
    public AdminNotificationListResponse getLatest(Principal principal) {
        return adminNotificationService.getLatest(principal.getName());
    }

    @PatchMapping("/read-all")
    public AdminNotificationListResponse markAllAsRead(Principal principal) {
        return adminNotificationService.markAllAsRead(principal.getName());
    }
}
