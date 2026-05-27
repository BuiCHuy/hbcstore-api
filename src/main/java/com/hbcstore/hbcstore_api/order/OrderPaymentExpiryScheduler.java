package com.hbcstore.hbcstore_api.order;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaymentExpiryScheduler {
    private final StoreOrderRepository orderRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
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
            order.setStatus(StoreOrder.OrderStatus.CANCELLED);
        }
        orderRepository.saveAll(expiredOrders);
        log.info("Auto-cancelled {} expired unpaid bank-transfer orders", expiredOrders.size());
    }
}
