package com.hbcstore.hbcstore_api.notification.dto;

import java.util.List;

public record AdminNotificationListResponse(
        long unreadCount,
        List<AdminNotificationResponse> notifications
) {
}
