package com.hbcstore.hbcstore_api.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(
        @NotNull Long productId,
        @NotNull @Min(1) @Max(10) Integer quantity
) {
}
