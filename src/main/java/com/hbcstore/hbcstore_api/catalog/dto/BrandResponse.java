package com.hbcstore.hbcstore_api.catalog.dto;

import com.hbcstore.hbcstore_api.catalog.Brand;

public record BrandResponse(
        Long id,
        String name,
        String country,
        String description,
        String logoUrl,
        Brand.Status status
) {
    public static BrandResponse from(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getCountry(),
                brand.getDescription(),
                brand.getLogoUrl(),
                brand.getStatus()
        );
    }
}
