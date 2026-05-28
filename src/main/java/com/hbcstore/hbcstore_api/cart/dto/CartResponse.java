package com.hbcstore.hbcstore_api.cart.dto;

import java.util.List;

public record CartResponse(
        Long cartId,
        Long userId,
        List<CartItemResponse> items
) {
}
