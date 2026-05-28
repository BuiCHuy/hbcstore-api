package com.hbcstore.hbcstore_api.order;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
            select (count(o) > 0)
            from StoreOrder o
            where o.user.id = :userId
              and o.coupon.id = :couponId
              and o.status <> :cancelledStatus
            """)
    boolean existsCouponUsageByUserExcludingCancelled(
            @Param("userId") Long userId,
            @Param("couponId") Long couponId,
            @Param("cancelledStatus") StoreOrder.OrderStatus cancelledStatus
    );
}
