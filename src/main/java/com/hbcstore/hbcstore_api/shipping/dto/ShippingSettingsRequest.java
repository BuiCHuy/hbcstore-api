package com.hbcstore.hbcstore_api.shipping.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ShippingSettingsRequest(
        @NotNull @DecimalMin("0") BigDecimal northFee,
        @NotNull @DecimalMin("0") BigDecimal centralFee,
        @NotNull @DecimalMin("0") BigDecimal southFee,
        @NotNull @DecimalMin("0") BigDecimal freeShippingThreshold
) {
}
