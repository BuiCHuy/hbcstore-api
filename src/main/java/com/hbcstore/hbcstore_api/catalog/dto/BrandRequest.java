package com.hbcstore.hbcstore_api.catalog.dto;

import com.hbcstore.hbcstore_api.catalog.Brand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 100) String country,
        String description,
        @Size(max = 500) String logoUrl,
        Brand.Status status
) {
}
