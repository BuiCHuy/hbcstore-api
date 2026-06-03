package com.hbcstore.hbcstore_api.promotion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionBrandRepository extends JpaRepository<PromotionBrand, Long> {
    List<PromotionBrand> findByPromotionId(Long promotionId);

    void deleteByPromotionId(Long promotionId);

    @Query("""
            select pb
            from PromotionBrand pb
            join fetch pb.promotion p
            where pb.brand.id = :brandId
              and p.status = com.hbcstore.hbcstore_api.promotion.Promotion$PromotionStatus.ACTIVE
              and p.startDate <= :now
              and p.endDate >= :now
            order by p.priority desc, p.createdAt asc
            """)
    List<PromotionBrand> findActiveByBrandIdOrderByPriority(
            @Param("brandId") Long brandId,
            @Param("now") java.time.LocalDateTime now
    );
}
