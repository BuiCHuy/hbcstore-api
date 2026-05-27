package com.hbcstore.hbcstore_api.refund.dto;

import com.hbcstore.hbcstore_api.refund.RefundRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundResponse(
        Long id,
        Long orderId,
        String orderCode,
        Long userId,
        String userEmail,
        String reason,
        RefundRequest.RefundStatus status,
        BigDecimal refundAmount,
        String adminNote,
        String processedByEmail,
        LocalDateTime processedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RefundResponse from(RefundRequest entity) {
        return new RefundResponse(
                entity.getId(),
                entity.getOrder().getId(),
                "#HBC" + String.format("%06d", entity.getOrder().getId()),
                entity.getUser().getId(),
                entity.getUser().getEmail(),
                entity.getReason(),
                entity.getStatus(),
                entity.getRefundAmount(),
                entity.getAdminNote(),
                entity.getProcessedBy() == null ? null : entity.getProcessedBy().getEmail(),
                entity.getProcessedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

