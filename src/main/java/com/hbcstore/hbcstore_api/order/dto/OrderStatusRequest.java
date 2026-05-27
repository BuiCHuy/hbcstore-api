package com.hbcstore.hbcstore_api.order.dto;

import com.hbcstore.hbcstore_api.order.StoreOrder;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(
        @NotNull StoreOrder.OrderStatus status
) {
}
