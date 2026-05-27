package com.hbcstore.hbcstore_api.promotion.dto;

import com.hbcstore.hbcstore_api.promotion.Promotion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PromotionResponse(
        Long id,
        String name,
        String description,
        Promotion.DiscountType discountType,
        BigDecimal discountValue,
        LocalDate startDate,
        LocalDate endDate,
        Promotion.PromotionStatus status,
        Integer priority,
        PromotionRequest.TargetType targetType,
        List<Long> targetIds,
        Integer saleStockLimit,
        Integer soldCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
