package com.hbcstore.hbcstore_api.notification.dto;

import com.hbcstore.hbcstore_api.notification.Notification;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        Long orderId,
        String orderCode,
        Long refundRequestId,
        String link,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static NotificationResponse from(Notification notification) {
        Long orderId = notification.getOrder() != null ? notification.getOrder().getId() : null;
        String orderCode = orderId == null ? null : "#HBC" + String.format("%06d", orderId);
        Long refundRequestId = notification.getRefundRequest() != null ? notification.getRefundRequest().getId() : null;
        return new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                orderId,
                orderCode,
                refundRequestId,
                notification.getLink(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
