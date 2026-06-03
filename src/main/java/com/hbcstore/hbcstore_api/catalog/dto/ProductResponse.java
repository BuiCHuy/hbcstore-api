package com.hbcstore.hbcstore_api.catalog.dto;

import com.hbcstore.hbcstore_api.catalog.Product;
import com.hbcstore.hbcstore_api.catalog.ProductAttribute;
import com.hbcstore.hbcstore_api.catalog.ProductImage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer discountPercent,
        Long promotionId,
        Integer stockQuantity,
        String thumbnailUrl,
        String description,
        List<String> images,
        List<AttributeResponse> attributes,
        Long categoryId,
        String categoryName,
        Long subcategoryId,
        String subcategoryName,
        Long brandId,
        String brandName,
        Double rating,
        Long reviewCount,
        Product.ProductStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(
            Product product,
            Double rating,
            Long reviewCount,
            BigDecimal displayPrice,
            BigDecimal originalPrice,
            Integer discountPercent,
            Long promotionId
    ) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                displayPrice,
                originalPrice,
                discountPercent,
                promotionId,
                product.getStockQuantity(),
                product.getThumbnailUrl(),
                product.getDescription(),
                product.getProductImages().stream()
                        .sorted(Comparator.comparing(ProductImage::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                        .map(ProductImage::getImageUrl)
                        .toList(),
                product.getProductAttributes().stream()
                        .map(AttributeResponse::from)
                        .toList(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getSubcategory() != null ? product.getSubcategory().getId() : null,
                product.getSubcategory() != null ? product.getSubcategory().getName() : null,
                product.getBrand().getId(),
                product.getBrand().getName(),
                rating,
                reviewCount,
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public record AttributeResponse(
            String name,
            String value
    ) {
        public static AttributeResponse from(ProductAttribute attribute) {
            return new AttributeResponse(
                    attribute.getAttributeName(),
                    attribute.getAttributeValue()
            );
        }
    }
}
