package com.hbcstore.hbcstore_api.promotion.dto;

import com.hbcstore.hbcstore_api.promotion.Promotion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PromotionRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotNull Promotion.DiscountType discountType,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal discountValue,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        Promotion.PromotionStatus status,
        Integer priority,
        @NotNull TargetType targetType,
        @NotEmpty List<Long> targetIds,
        Integer saleStockLimit
) {
    public enum TargetType {
        PRODUCT,
        CATEGORY,
        BRAND
    }
}
