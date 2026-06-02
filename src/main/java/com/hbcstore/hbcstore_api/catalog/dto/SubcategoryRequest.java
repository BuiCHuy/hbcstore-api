package com.hbcstore.hbcstore_api.catalog.dto;

import com.hbcstore.hbcstore_api.catalog.Subcategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubcategoryRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @Size(max = 500) String iconUrl,
        @NotNull Long categoryId,
        Subcategory.Status status
) {
}
