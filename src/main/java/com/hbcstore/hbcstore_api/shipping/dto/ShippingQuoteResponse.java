package com.hbcstore.hbcstore_api.shipping.dto;

import java.math.BigDecimal;

public record ShippingQuoteResponse(
        BigDecimal shippingFee,
        String region,
        String regionLabel,
        boolean freeShipping
) {
}
