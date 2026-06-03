package com.hbcstore.hbcstore_api.order.dto;

import java.math.BigDecimal;

public record OrderQuoteItemResponse(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
