package com.hbcstore.hbcstore_api.review.dto;

import com.hbcstore.hbcstore_api.review.ProductReview;
import java.time.LocalDateTime;

public record ProductReviewResponse(
        Long id,
        Long productId,
        String productName,
        Long userId,
        String authorName,
        Integer rating,
        String content,
        String adminReply,
        LocalDateTime repliedAt,
        ProductReview.ReviewStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductReviewResponse from(ProductReview review) {
        return new ProductReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getProduct().getName(),
                review.getUser().getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getContent(),
                review.getAdminReply(),
                review.getRepliedAt(),
                review.getStatus(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
