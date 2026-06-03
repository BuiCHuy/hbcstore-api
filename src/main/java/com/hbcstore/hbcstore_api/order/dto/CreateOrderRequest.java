package com.hbcstore.hbcstore_api.order.dto;

import com.hbcstore.hbcstore_api.order.StoreOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank @Size(max = 150) String customerName,
        @NotBlank @Size(max = 20) String customerPhone,
        @Size(max = 150) String customerEmail,
        @NotBlank String shippingAddress,
        @NotNull StoreOrder.PaymentMethod paymentMethod,
        Long couponId,
        @Size(max = 50) String couponCode,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        @NotEmpty List<@Valid CreateOrderItemRequest> items
) {
}
