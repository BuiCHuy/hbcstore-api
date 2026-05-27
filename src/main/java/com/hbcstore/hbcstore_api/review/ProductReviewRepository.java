package com.hbcstore.hbcstore_api.review;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, ProductReview.ReviewStatus status);

    Optional<ProductReview> findByProductIdAndUserId(Long productId, Long userId);

    long countByProductIdAndStatus(Long productId, ProductReview.ReviewStatus status);

    List<ProductReview> findAllByOrderByCreatedAtDesc();

    @Query("""
            select avg(r.rating)
            from ProductReview r
            where r.product.id = :productId
              and r.status = :status
            """)
    Double getAverageRatingByProductIdAndStatus(
            @Param("productId") Long productId,
            @Param("status") ProductReview.ReviewStatus status
    );
}
