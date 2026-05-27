package com.hbcstore.hbcstore_api.promotion;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionCategoryRepository extends JpaRepository<PromotionCategory, Long> {
    List<PromotionCategory> findByPromotionId(Long promotionId);

    void deleteByPromotionId(Long promotionId);
}
