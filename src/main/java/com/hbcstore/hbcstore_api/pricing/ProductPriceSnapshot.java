package com.hbcstore.hbcstore_api.pricing;

import java.math.BigDecimal;

public record ProductPriceSnapshot(
        BigDecimal unitPrice,
        BigDecimal originalPrice,
        Integer discountPercent,
        Long promotionId,
        boolean usesPromotionStock
) {
}
