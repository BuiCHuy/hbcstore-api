package com.hbcstore.hbcstore_api.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderQuoteResponse(
        List<OrderQuoteItemResponse> items,
        Long couponId,
        String couponCode,
        BigDecimal subtotalAmount,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount
) {
}
