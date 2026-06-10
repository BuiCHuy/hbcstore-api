package com.hbcstore.hbcstore_api.notification.dto;

import java.util.List;

public record NotificationListResponse(
        long unreadCount,
        List<NotificationResponse> notifications
) {
}
