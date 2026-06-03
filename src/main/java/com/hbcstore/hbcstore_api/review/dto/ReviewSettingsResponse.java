package com.hbcstore.hbcstore_api.review.dto;

import com.hbcstore.hbcstore_api.review.ReviewSettings;

public record ReviewSettingsResponse(
        boolean reviewApprovalEnabled
) {
    public static ReviewSettingsResponse from(ReviewSettings settings) {
        return new ReviewSettingsResponse(settings.isReviewApprovalEnabled());
    }
}
