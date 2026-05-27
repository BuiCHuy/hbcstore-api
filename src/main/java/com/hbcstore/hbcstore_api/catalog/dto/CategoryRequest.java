package com.hbcstore.hbcstore_api.catalog.dto;

import com.hbcstore.hbcstore_api.catalog.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        Category.Status status
) {
}