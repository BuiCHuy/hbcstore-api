package com.hbcstore.hbcstore_api.order.dto;

import com.hbcstore.hbcstore_api.order.StoreOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        Long couponId,
        String customerName,
        String customerPhone,
        String customerEmail,
        String shippingAddress,
        LocalDateTime orderDate,
        StoreOrder.OrderStatus status,
        StoreOrder.PaymentMethod paymentMethod,
        StoreOrder.PaymentStatus paymentStatus,
        LocalDateTime paymentExpiredAt,
        BigDecimal subtotalAmount,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        Integer itemCount,
        List<OrderItemResponse> items
) {
}
