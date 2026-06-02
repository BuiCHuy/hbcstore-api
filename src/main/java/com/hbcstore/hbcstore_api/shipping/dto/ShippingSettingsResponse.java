package com.hbcstore.hbcstore_api.shipping.dto;

import com.hbcstore.hbcstore_api.shipping.ShippingSettings;
import java.math.BigDecimal;

public record ShippingSettingsResponse(
        BigDecimal northFee,
        BigDecimal centralFee,
        BigDecimal southFee,
        BigDecimal freeShippingThreshold
) {
    public static ShippingSettingsResponse from(ShippingSettings settings) {
        return new ShippingSettingsResponse(
                settings.getNorthFee(),
                settings.getCentralFee(),
                settings.getSouthFee(),
                settings.getFreeShippingThreshold()
        );
    }
}
