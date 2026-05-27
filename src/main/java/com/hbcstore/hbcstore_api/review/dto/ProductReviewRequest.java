package com.hbcstore.hbcstore_api.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProductReviewRequest(
        @Min(1) @Max(5) Integer rating,
        @NotBlank String content
) {
}
