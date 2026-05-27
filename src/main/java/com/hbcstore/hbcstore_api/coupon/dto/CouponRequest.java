package com.hbcstore.hbcstore_api.coupon.dto;

import com.hbcstore.hbcstore_api.coupon.Coupon;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CouponRequest(
        @NotBlank @Size(max = 50) String code,
        @NotNull Coupon.DiscountType discountType,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal discountValue,
        BigDecimal minOrderValue,
        BigDecimal maxDiscountAmount,
        LocalDate startDate,
        @NotNull LocalDate endDate,
        Integer usageLimit,
        Coupon.CouponStatus status
) {
}
