package com.hbcstore.hbcstore_api.review.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewSettingsRequest(
        @NotNull Boolean reviewApprovalEnabled
) {
}
