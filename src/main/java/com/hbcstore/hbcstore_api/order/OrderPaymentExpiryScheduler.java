package com.hbcstore.hbcstore_api.order;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaymentExpiryScheduler {
    private final StoreOrderRepository orderRepository;
    private final OrderService orderService; // Inject OrderService

    @Scheduled(fixedDelay = 60000)
    public void cancelExpiredBankTransferOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<StoreOrder> expiredOrders =
                orderRepository.findByPaymentMethodAndPaymentStatusAndStatusAndPaymentExpiredAtBefore(
                        StoreOrder.PaymentMethod.BANK_TRANSFER,
                        StoreOrder.PaymentStatus.UNPAID,
                        StoreOrder.OrderStatus.PENDING,
                        now
                );

        if (expiredOrders.isEmpty()) {
            return;
        }

        for (StoreOrder order : expiredOrders) {
            orderService.autoCancelExpiredOrder(order); // Call the new method to rollback stock safely
        }
        log.info("Auto-cancelled {} expired unpaid bank-transfer orders", expiredOrders.size());
    }
}
