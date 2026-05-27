package com.hbcstore.hbcstore_api.ai.dto;

import java.util.List;

public record ChatResponse(
        String answer,
        String interpretedQuery,
        String source,
        List<ChatProduct> suggestedProducts
) {
    public record ChatProduct(
            Long id,
            String name,
            String image,
            String brand,
            String category,
            double price
    ) {
    }
}
