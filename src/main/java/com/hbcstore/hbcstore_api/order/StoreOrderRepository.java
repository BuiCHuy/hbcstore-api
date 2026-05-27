package com.hbcstore.hbcstore_api.order;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreOrderRepository extends JpaRepository<StoreOrder, Long> {
    List<StoreOrder> findByUserEmailOrderByOrderDateDesc(String email);
    List<StoreOrder> findByUserEmailIgnoreCaseOrGuestEmailIgnoreCaseOrderByOrderDateDesc(
            String userEmail,
            String guestEmail
    );

    List<StoreOrder> findByPaymentMethodAndPaymentStatusAndStatusAndPaymentExpiredAtBefore(
            StoreOrder.PaymentMethod paymentMethod,
            StoreOrder.PaymentStatus paymentStatus,
            StoreOrder.OrderStatus status,
            LocalDateTime threshold
    );
}
