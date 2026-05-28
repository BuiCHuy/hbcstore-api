package com.hbcstore.hbcstore_api.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String name,
        String image,
        String brand,
        String category,
        BigDecimal price,
        Integer quantity
) {
}
