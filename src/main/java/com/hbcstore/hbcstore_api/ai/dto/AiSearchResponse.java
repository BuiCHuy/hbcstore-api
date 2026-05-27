package com.hbcstore.hbcstore_api.ai.dto;

import java.util.List;

public record AiSearchResponse(
        String query,
        String normalizedQuery,
        List<String> keywords,
        boolean aiEnabled,
        String source
) {
}
