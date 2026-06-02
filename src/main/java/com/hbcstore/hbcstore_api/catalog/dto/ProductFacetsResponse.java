package com.hbcstore.hbcstore_api.catalog.dto;

import java.util.List;
import java.util.Map;

public record ProductFacetsResponse(
        List<String> brands,
        Map<String, List<String>> attributes
) {
}
