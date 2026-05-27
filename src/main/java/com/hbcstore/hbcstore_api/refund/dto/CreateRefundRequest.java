package com.hbcstore.hbcstore_api.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRefundRequest(
        @NotNull Long orderId,
        @NotBlank @Size(max = 2000) String reason
) {
}

