package com.hbcstore.hbcstore_api.order.dto;

import com.hbcstore.hbcstore_api.order.OrderDetail;
import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        String productImage,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
    public static OrderItemResponse from(OrderDetail detail) {
        return new OrderItemResponse(
                detail.getId(),
                detail.getProduct().getId(),
                detail.getProductName(),
                detail.getProductImage(),
                detail.getQuantity(),
                detail.getUnitPrice(),
                detail.getTotalPrice()
        );
    }
}
