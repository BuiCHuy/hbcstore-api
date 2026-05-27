package com.hbcstore.hbcstore_api.payment.payos.dto;

import jakarta.validation.constraints.NotNull;

public record PayOSCreatePaymentRequest(
        @NotNull Long orderId
) {
}

