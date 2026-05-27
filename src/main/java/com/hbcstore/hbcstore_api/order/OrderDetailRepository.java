package com.hbcstore.hbcstore_api.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByOrderId(Long orderId);

    @Query("""
            select count(od) > 0
            from OrderDetail od
            where od.order.user.id = :userId
              and od.product.id = :productId
              and od.order.status = :status
            """)
    boolean existsPurchasedProductByUserAndStatus(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("status") StoreOrder.OrderStatus status
    );
}
