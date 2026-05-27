package com.hbcstore.hbcstore_api.coupon.dto;

import com.hbcstore.hbcstore_api.coupon.Coupon;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
        Long id,
        String code,
        Coupon.DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderValue,
        BigDecimal maxDiscountAmount,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer usageLimit,
        Integer usedCount,
        Coupon.CouponStatus status
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinOrderValue(),
                coupon.getMaxDiscountAmount(),
                coupon.getStartDate(),
                coupon.getEndDate(),
                coupon.getUsageLimit(),
                coupon.getUsedCount(),
                coupon.getStatus()
        );
    }
}
