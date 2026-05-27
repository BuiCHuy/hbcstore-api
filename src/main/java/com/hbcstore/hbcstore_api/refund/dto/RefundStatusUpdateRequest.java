package com.hbcstore.hbcstore_api.refund.dto;

import com.hbcstore.hbcstore_api.refund.RefundRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RefundStatusUpdateRequest(
        @NotNull RefundRequest.RefundStatus status,
        @Size(max = 2000) String adminNote
) {
}

