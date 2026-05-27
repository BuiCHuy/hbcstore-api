package com.hbcstore.hbcstore_api.review.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewReplyRequest(
        @NotBlank String reply
) {
}
