package com.hbcstore.hbcstore_api.review;

import com.hbcstore.hbcstore_api.review.dto.ProductReviewRequest;
import com.hbcstore.hbcstore_api.review.dto.ProductReviewResponse;
import com.hbcstore.hbcstore_api.review.dto.ReviewReplyRequest;
import com.hbcstore.hbcstore_api.review.dto.ReviewStatusRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProductReviewController {
    private final ProductReviewService reviewService;

    public ProductReviewController(ProductReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/products/{productId}/reviews")
    public List<ProductReviewResponse> getApprovedReviews(@PathVariable Long productId) {
        return reviewService.getApprovedByProductId(productId);
    }

    @GetMapping("/reviews/my/products/{productId}")
    public ProductReviewResponse getMyReviewByProduct(
            @PathVariable Long productId,
            Principal principal
    ) {
        if (principal == null) {
            throw new IllegalArgumentException("Please login");
        }
        return reviewService.getMyReviewByProductId(productId, principal.getName());
    }

    @GetMapping("/reviews")
    public List<ProductReviewResponse> getAllReviewsForAdmin(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Please login");
        }
        return reviewService.getAllForAdmin(principal.getName());
    }

    @PostMapping("/reviews/products/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductReviewResponse createOrUpdateMyReview(
            @PathVariable Long productId,
            @Valid @RequestBody ProductReviewRequest request,
            Principal principal
    ) {
        if (principal == null) {
            throw new IllegalArgumentException("Please login to submit review");
        }
        return reviewService.upsertMyReview(productId, principal.getName(), request);
    }

    @PatchMapping("/reviews/{reviewId}/status")
    public ProductReviewResponse updateStatus(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewStatusRequest request,
            Principal principal
    ) {
        if (principal == null) {
            throw new IllegalArgumentException("Please login");
        }
        return reviewService.updateStatus(reviewId, request.status(), principal.getName());
    }

    @PatchMapping("/reviews/{reviewId}/reply")
    public ProductReviewResponse replyToReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReplyRequest request,
            Principal principal
    ) {
        if (principal == null) {
            throw new IllegalArgumentException("Please login");
        }
        return reviewService.replyToReview(reviewId, request.reply(), principal.getName());
    }
}
