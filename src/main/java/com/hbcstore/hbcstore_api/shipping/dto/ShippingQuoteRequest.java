package com.hbcstore.hbcstore_api.shipping.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ShippingQuoteRequest(
        @NotNull @DecimalMin("0") BigDecimal subtotal,
        String province,
        String shippingAddress
) {
}
