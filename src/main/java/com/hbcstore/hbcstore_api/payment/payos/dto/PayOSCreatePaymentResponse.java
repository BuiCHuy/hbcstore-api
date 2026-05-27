package com.hbcstore.hbcstore_api.payment.payos.dto;

public record PayOSCreatePaymentResponse(
        String requestId,
        String orderId,
        String qrCodeUrl,
        String payUrl,
        String message,
        Integer resultCode
) {
}
