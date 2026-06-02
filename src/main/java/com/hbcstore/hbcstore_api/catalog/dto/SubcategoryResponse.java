package com.hbcstore.hbcstore_api.catalog.dto;

import com.hbcstore.hbcstore_api.catalog.Subcategory;
import java.time.LocalDateTime;

public record SubcategoryResponse(
        Long id,
        String name,
        String description,
        String iconUrl,
        Subcategory.Status status,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SubcategoryResponse from(Subcategory subcategory) {
        return new SubcategoryResponse(
                subcategory.getId(),
                subcategory.getName(),
                subcategory.getDescription(),
                subcategory.getIconUrl(),
                subcategory.getStatus(),
                subcategory.getCategory().getId(),
                subcategory.getCategory().getName(),
                subcategory.getCreatedAt(),
                subcategory.getUpdatedAt()
        );
    }
}
