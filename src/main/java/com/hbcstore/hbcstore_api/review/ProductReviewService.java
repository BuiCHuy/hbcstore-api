package com.hbcstore.hbcstore_api.review;

import com.hbcstore.hbcstore_api.catalog.Product;
import com.hbcstore.hbcstore_api.catalog.ProductRepository;
import com.hbcstore.hbcstore_api.order.OrderDetailRepository;
import com.hbcstore.hbcstore_api.order.StoreOrder;
import com.hbcstore.hbcstore_api.review.dto.ProductReviewRequest;
import com.hbcstore.hbcstore_api.review.dto.ProductReviewResponse;
import com.hbcstore.hbcstore_api.user.User;
import com.hbcstore.hbcstore_api.user.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductReviewService {
    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderDetailRepository orderDetailRepository;

    public ProductReviewService(
            ProductReviewRepository reviewRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderDetailRepository orderDetailRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderDetailRepository = orderDetailRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getApprovedByProductId(Long productId) {
        return reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(
                        productId,
                        ProductReview.ReviewStatus.APPROVED
                ).stream()
                .map(ProductReviewResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductReviewResponse getMyReviewByProductId(Long productId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        ProductReview review = reviewRepository.findByProductIdAndUserId(productId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        return ProductReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getAllForAdmin(String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Only admin can view all reviews");
        }

        return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ProductReviewResponse::from)
                .toList();
    }

    @Transactional
    public ProductReviewResponse upsertMyReview(Long productId, String email, ProductReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean hasDeliveredOrder = orderDetailRepository.existsPurchasedProductByUserAndStatus(
                user.getId(),
                productId,
                StoreOrder.OrderStatus.DELIVERED
        );
        if (!hasDeliveredOrder) {
            throw new IllegalArgumentException("You can only review products from delivered orders");
        }

        ProductReview review = reviewRepository.findByProductIdAndUserId(productId, user.getId())
                .orElseGet(ProductReview::new);
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.rating());
        review.setContent(request.content().trim());
        review.setStatus(ProductReview.ReviewStatus.PENDING);

        return ProductReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public ProductReviewResponse updateStatus(Long reviewId, ProductReview.ReviewStatus status, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Only admin can moderate reviews");
        }

        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        review.setStatus(status);
        return ProductReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public ProductReviewResponse replyToReview(Long reviewId, String reply, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Only admin can reply reviews");
        }

        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        review.setAdminReply(reply.trim());
        review.setRepliedAt(LocalDateTime.now());
        return ProductReviewResponse.from(reviewRepository.save(review));
    }
}
