package com.hbcstore.hbcstore_api.catalog.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.hbcstore.hbcstore_api.catalog.Product;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotNull @Min(0) Integer stockQuantity,
        @Size(max = 500) String thumbnailUrl,
        String description,
        @NotNull Long categoryId,
        Long subcategoryId,
        @NotNull Long brandId,
        Product.ProductStatus status,
        @JsonAlias({"image_urls", "images"}) List<String> imageUrls,
        @JsonAlias({"product_attributes", "specifications"}) List<@Valid AttributeRequest> attributes
) {
    public record AttributeRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 255) String value
    ) {
    }
}
