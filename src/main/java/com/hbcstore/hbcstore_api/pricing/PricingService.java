package com.hbcstore.hbcstore_api.pricing;

import com.hbcstore.hbcstore_api.catalog.Product;
import com.hbcstore.hbcstore_api.coupon.Coupon;
import com.hbcstore.hbcstore_api.promotion.Promotion;
import com.hbcstore.hbcstore_api.promotion.PromotionBrand;
import com.hbcstore.hbcstore_api.promotion.PromotionBrandRepository;
import com.hbcstore.hbcstore_api.promotion.PromotionCategory;
import com.hbcstore.hbcstore_api.promotion.PromotionCategoryRepository;
import com.hbcstore.hbcstore_api.promotion.PromotionProduct;
import com.hbcstore.hbcstore_api.promotion.PromotionProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PricingService {
    private final PromotionProductRepository promotionProductRepository;
    private final PromotionCategoryRepository promotionCategoryRepository;
    private final PromotionBrandRepository promotionBrandRepository;

    public PricingService(
            PromotionProductRepository promotionProductRepository,
            PromotionCategoryRepository promotionCategoryRepository,
            PromotionBrandRepository promotionBrandRepository
    ) {
        this.promotionProductRepository = promotionProductRepository;
        this.promotionCategoryRepository = promotionCategoryRepository;
        this.promotionBrandRepository = promotionBrandRepository;
    }

    public ProductPriceSnapshot resolveProductPrice(Product product, int quantity) {
        return resolveProductPrice(product, quantity, LocalDateTime.now());
    }

    public ProductPriceSnapshot resolveProductPrice(Product product, int quantity, LocalDateTime now) {
        BigDecimal basePrice = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        List<Candidate> candidates = new ArrayList<>();

        for (PromotionProduct item : promotionProductRepository.findActiveByProductIdOrderByPriority(product.getId(), now)) {
            if (!hasEnoughSaleStock(item, quantity)) {
                continue;
            }
            candidates.add(new Candidate(item.getPromotion(), computeDiscountedPrice(basePrice, item.getPromotion()), true));
        }
        if (product.getCategory() != null) {
            for (PromotionCategory item : promotionCategoryRepository.findActiveByCategoryIdOrderByPriority(product.getCategory().getId(), now)) {
                candidates.add(new Candidate(item.getPromotion(), computeDiscountedPrice(basePrice, item.getPromotion()), false));
            }
        }
        if (product.getBrand() != null) {
            for (PromotionBrand item : promotionBrandRepository.findActiveByBrandIdOrderByPriority(product.getBrand().getId(), now)) {
                candidates.add(new Candidate(item.getPromotion(), computeDiscountedPrice(basePrice, item.getPromotion()), false));
            }
        }

        Candidate best = candidates.stream()
                .filter(candidate -> candidate.discountedPrice() != null && candidate.discountedPrice().compareTo(basePrice) < 0)
                .min(Comparator
                        .comparing((Candidate candidate) -> candidate.promotion().getPriority(), Comparator.reverseOrder())
                        .thenComparing(Candidate::discountedPrice)
                        .thenComparing(candidate -> candidate.promotion().getCreatedAt()))
                .orElse(null);

        if (best == null) {
            return new ProductPriceSnapshot(basePrice, null, 0, null, false);
        }

        int discountPercent = basePrice.signum() <= 0
                ? 0
                : best.discountedPrice()
                .subtract(basePrice)
                .abs()
                .multiply(BigDecimal.valueOf(100))
                .divide(basePrice, 0, RoundingMode.HALF_UP)
                .intValue();

        return new ProductPriceSnapshot(
                best.discountedPrice(),
                basePrice,
                discountPercent,
                best.promotion().getId(),
                best.usesPromotionStock()
        );
    }

    public BigDecimal calculateCouponDiscount(Coupon coupon, BigDecimal subtotal) {
        if (coupon == null || subtotal == null || subtotal.signum() <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = switch (coupon.getDiscountType()) {
            case PERCENTAGE -> subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> coupon.getDiscountValue();
        };

        if (coupon.getMaxDiscountAmount() != null && coupon.getMaxDiscountAmount().signum() > 0) {
            discount = discount.min(coupon.getMaxDiscountAmount());
        }

        if (discount.signum() < 0) {
            discount = BigDecimal.ZERO;
        }
        return discount.min(subtotal);
    }

    private boolean hasEnoughSaleStock(PromotionProduct promotionProduct, int quantity) {
        Integer stockLimit = promotionProduct.getSaleStockLimit();
        if (stockLimit == null || stockLimit <= 0) {
            return true;
        }
        int soldCount = promotionProduct.getSoldCount() == null ? 0 : promotionProduct.getSoldCount();
        return soldCount + quantity <= stockLimit;
    }

    private BigDecimal computeDiscountedPrice(BigDecimal basePrice, Promotion promotion) {
        if (promotion == null || basePrice == null || basePrice.signum() <= 0) {
            return null;
        }

        BigDecimal discounted = switch (promotion.getDiscountType()) {
            case PERCENTAGE -> basePrice.subtract(
                    basePrice.multiply(promotion.getDiscountValue())
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
            );
            case FIXED_AMOUNT -> basePrice.subtract(promotion.getDiscountValue());
            case FIXED_PRICE -> promotion.getDiscountValue();
        };

        if (discounted.signum() < 0) {
            discounted = BigDecimal.ZERO;
        }
        if (discounted.compareTo(basePrice) >= 0) {
            return null;
        }
        return discounted;
    }

    private record Candidate(Promotion promotion, BigDecimal discountedPrice, boolean usesPromotionStock) {
    }
}
