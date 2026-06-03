package com.hbcstore.hbcstore_api.review;

import com.hbcstore.hbcstore_api.catalog.Product;
import com.hbcstore.hbcstore_api.catalog.ProductRepository;
import com.hbcstore.hbcstore_api.order.OrderDetailRepository;
import com.hbcstore.hbcstore_api.order.StoreOrder;
import com.hbcstore.hbcstore_api.review.dto.ProductReviewRequest;
import com.hbcstore.hbcstore_api.review.dto.ProductReviewResponse;
import com.hbcstore.hbcstore_api.review.dto.ReviewEligibilityResponse;
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
    private final ReviewSettingsService reviewSettingsService;

    public ProductReviewService(
            ProductReviewRepository reviewRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderDetailRepository orderDetailRepository,
            ReviewSettingsService reviewSettingsService
    ) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.reviewSettingsService = reviewSettingsService;
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
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung"));
        ProductReview review = reviewRepository.findByProductIdAndUserId(productId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay danh gia"));
        return ProductReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public ReviewEligibilityResponse getReviewEligibility(Long productId, String email) {
        if (email == null || email.isBlank()) {
            return new ReviewEligibilityResponse(false);
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung"));

        boolean hasDeliveredOrder = hasDeliveredPurchase(user.getId(), productId);
        return new ReviewEligibilityResponse(hasDeliveredOrder);
    }

    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getAllForAdmin(String adminEmail) {
        User admin = userRepository.findByEmailIgnoreCase(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung"));
        if (admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Chi quan tri vien moi co the xem toan bo danh gia");
        }

        return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ProductReviewResponse::from)
                .toList();
    }

    @Transactional
    public ProductReviewResponse upsertMyReview(Long productId, String email, ProductReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay san pham"));
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung"));

        boolean hasDeliveredOrder = hasDeliveredPurchase(user.getId(), productId);
        if (!hasDeliveredOrder) {
            throw new IllegalArgumentException("Ban chi co the danh gia san pham tu don hang da giao");
        }

        ProductReview review = reviewRepository.findByProductIdAndUserId(productId, user.getId())
                .orElseGet(ProductReview::new);
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.rating());
        review.setContent(request.content().trim());
        review.setStatus(
                reviewSettingsService.isReviewApprovalEnabled()
                        ? ProductReview.ReviewStatus.PENDING
                        : ProductReview.ReviewStatus.APPROVED
        );

        return ProductReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public ProductReviewResponse updateStatus(Long reviewId, ProductReview.ReviewStatus status, String adminEmail) {
        User admin = userRepository.findByEmailIgnoreCase(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung"));
        if (admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Chi quan tri vien moi co the duyet danh gia");
        }

        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay danh gia"));
        review.setStatus(status);
        return ProductReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public ProductReviewResponse replyToReview(Long reviewId, String reply, String adminEmail) {
        User admin = userRepository.findByEmailIgnoreCase(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung"));
        if (admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Chi quan tri vien moi co the phan hoi danh gia");
        }

        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay danh gia"));
        review.setAdminReply(reply.trim());
        review.setRepliedAt(LocalDateTime.now());
        return ProductReviewResponse.from(reviewRepository.save(review));
    }

    private boolean hasDeliveredPurchase(Long userId, Long productId) {
        return orderDetailRepository.existsPurchasedProductByUserAndStatus(
                userId,
                productId,
                StoreOrder.OrderStatus.DELIVERED
        );
    }
}
