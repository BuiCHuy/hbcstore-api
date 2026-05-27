package com.hbcstore.hbcstore_api.review.dto;

import com.hbcstore.hbcstore_api.review.ProductReview;
import jakarta.validation.constraints.NotNull;

public record ReviewStatusRequest(
        @NotNull ProductReview.ReviewStatus status
) {
}
