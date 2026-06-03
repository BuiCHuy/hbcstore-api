package com.hbcstore.hbcstore_api.promotion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionCategoryRepository extends JpaRepository<PromotionCategory, Long> {
    List<PromotionCategory> findByPromotionId(Long promotionId);

    void deleteByPromotionId(Long promotionId);

    @Query("""
            select pc
            from PromotionCategory pc
            join fetch pc.promotion p
            where pc.category.id = :categoryId
              and p.status = com.hbcstore.hbcstore_api.promotion.Promotion$PromotionStatus.ACTIVE
              and p.startDate <= :now
              and p.endDate >= :now
            order by p.priority desc, p.createdAt asc
            """)
    List<PromotionCategory> findActiveByCategoryIdOrderByPriority(
            @Param("categoryId") Long categoryId,
            @Param("now") java.time.LocalDateTime now
    );
}
