package com.hbcstore.hbcstore_api.promotion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionBrandRepository extends JpaRepository<PromotionBrand, Long> {
    List<PromotionBrand> findByPromotionId(Long promotionId);

    void deleteByPromotionId(Long promotionId);
}
