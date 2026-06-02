package com.hbcstore.hbcstore_api.notification.dto;

import com.hbcstore.hbcstore_api.notification.AdminNotification;
import java.time.LocalDateTime;

public record AdminNotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        Long orderId,
        String orderCode,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static AdminNotificationResponse from(AdminNotification notification) {
        Long orderId = notification.getOrder() != null ? notification.getOrder().getId() : null;
        String orderCode = orderId == null ? null : "#HBC" + String.format("%06d", orderId);
        return new AdminNotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                orderId,
                orderCode,
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
