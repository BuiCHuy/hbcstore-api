package com.hbcstore.hbcstore_api.promotion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionProductRepository extends JpaRepository<PromotionProduct, Long> {
    List<PromotionProduct> findByPromotionId(Long promotionId);

    void deleteByPromotionId(Long promotionId);

    @Query("""
            select pp
            from PromotionProduct pp
            join fetch pp.promotion p
            where pp.product.id = :productId
              and p.status = com.hbcstore.hbcstore_api.promotion.Promotion$PromotionStatus.ACTIVE
              and p.startDate <= :now
              and p.endDate >= :now
            order by p.priority desc, p.createdAt asc
            """)
    List<PromotionProduct> findActiveByProductIdOrderByPriority(
            @Param("productId") Long productId,
            @Param("now") java.time.LocalDateTime now
    );

    @Query("""
            select pp
            from PromotionProduct pp
            join fetch pp.promotion p
            where pp.product.id = :productId
              and p.startDate <= :orderDate
              and p.endDate >= :orderDate
            order by p.priority desc, p.createdAt asc
            """)
    List<PromotionProduct> findMatchedByProductIdAtOrderDate(
            @Param("productId") Long productId,
            @Param("orderDate") java.time.LocalDateTime orderDate
    );
}
