package com.hbcstore.hbcstore_api.promotion;

import com.hbcstore.hbcstore_api.catalog.BrandRepository;
import com.hbcstore.hbcstore_api.catalog.CategoryRepository;
import com.hbcstore.hbcstore_api.catalog.ProductRepository;
import com.hbcstore.hbcstore_api.promotion.dto.PromotionRequest;
import com.hbcstore.hbcstore_api.promotion.dto.PromotionResponse;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionService {
    private final PromotionRepository promotionRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final PromotionCategoryRepository promotionCategoryRepository;
    private final PromotionBrandRepository promotionBrandRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    public PromotionService(
            PromotionRepository promotionRepository,
            PromotionProductRepository promotionProductRepository,
            PromotionCategoryRepository promotionCategoryRepository,
            PromotionBrandRepository promotionBrandRepository,
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository
    ) {
        this.promotionRepository = promotionRepository;
        this.promotionProductRepository = promotionProductRepository;
        this.promotionCategoryRepository = promotionCategoryRepository;
        this.promotionBrandRepository = promotionBrandRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
    }

    public List<PromotionResponse> getAll() {
        return promotionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public PromotionResponse getById(Long id) {
        return toResponse(findPromotion(id));
    }

    @Transactional
    public PromotionResponse create(PromotionRequest request) {
        Promotion promotion = new Promotion();
        apply(promotion, request);
        Promotion savedPromotion = promotionRepository.save(promotion);
        saveTargets(savedPromotion, request);
        return toResponse(savedPromotion);
    }

    @Transactional
    public PromotionResponse update(Long id, PromotionRequest request) {
        Promotion promotion = findPromotion(id);
        apply(promotion, request);
        clearTargets(id);
        saveTargets(promotion, request);
        return toResponse(promotion);
    }

    @Transactional
    public void delete(Long id) {
        Promotion promotion = findPromotion(id);
        promotion.setStatus(Promotion.PromotionStatus.INACTIVE);
    }

    private void apply(Promotion promotion, PromotionRequest request) {
        promotion.setName(request.name());
        promotion.setDescription(request.description());
        promotion.setDiscountType(request.discountType());
        promotion.setDiscountValue(request.discountValue());
        promotion.setStartDate(request.startDate().atStartOfDay());
        promotion.setEndDate(request.endDate().atTime(LocalTime.MAX));
        promotion.setStatus(request.status() == null ? Promotion.PromotionStatus.ACTIVE : request.status());
        promotion.setPriority(request.priority() == null ? 0 : request.priority());
    }

    private void saveTargets(Promotion promotion, PromotionRequest request) {
        if (request.targetType() == PromotionRequest.TargetType.PRODUCT) {
            request.targetIds().forEach(productId -> {
                PromotionProduct target = new PromotionProduct();
                target.setPromotion(promotion);
                target.setProduct(productRepository.getReferenceById(productId));
                target.setSaleStockLimit(request.saleStockLimit());
                target.setSoldCount(0);
                promotionProductRepository.save(target);
            });
            return;
        }

        if (request.targetType() == PromotionRequest.TargetType.CATEGORY) {
            request.targetIds().forEach(categoryId -> {
                PromotionCategory target = new PromotionCategory();
                target.setPromotion(promotion);
                target.setCategory(categoryRepository.getReferenceById(categoryId));
                promotionCategoryRepository.save(target);
            });
            return;
        }

        request.targetIds().forEach(brandId -> {
            PromotionBrand target = new PromotionBrand();
            target.setPromotion(promotion);
            target.setBrand(brandRepository.getReferenceById(brandId));
            promotionBrandRepository.save(target);
        });
    }

    private void clearTargets(Long promotionId) {
        promotionProductRepository.deleteByPromotionId(promotionId);
        promotionCategoryRepository.deleteByPromotionId(promotionId);
        promotionBrandRepository.deleteByPromotionId(promotionId);
    }

    private PromotionResponse toResponse(Promotion promotion) {
        List<PromotionProduct> products = promotionProductRepository.findByPromotionId(promotion.getId());
        if (!products.isEmpty()) {
            return new PromotionResponse(
                    promotion.getId(),
                    promotion.getName(),
                    promotion.getDescription(),
                    promotion.getDiscountType(),
                    promotion.getDiscountValue(),
                    promotion.getStartDate().toLocalDate(),
                    promotion.getEndDate().toLocalDate(),
                    promotion.getStatus(),
                    promotion.getPriority(),
                    PromotionRequest.TargetType.PRODUCT,
                    products.stream().map(item -> item.getProduct().getId()).toList(),
                    products.getFirst().getSaleStockLimit(),
                    products.stream().mapToInt(PromotionProduct::getSoldCount).sum(),
                    promotion.getCreatedAt(),
                    promotion.getUpdatedAt()
            );
        }

        List<PromotionCategory> categories = promotionCategoryRepository.findByPromotionId(promotion.getId());
        if (!categories.isEmpty()) {
            return new PromotionResponse(
                    promotion.getId(),
                    promotion.getName(),
                    promotion.getDescription(),
                    promotion.getDiscountType(),
                    promotion.getDiscountValue(),
                    promotion.getStartDate().toLocalDate(),
                    promotion.getEndDate().toLocalDate(),
                    promotion.getStatus(),
                    promotion.getPriority(),
                    PromotionRequest.TargetType.CATEGORY,
                    categories.stream().map(item -> item.getCategory().getId()).toList(),
                    null,
                    0,
                    promotion.getCreatedAt(),
                    promotion.getUpdatedAt()
            );
        }

        List<PromotionBrand> brands = promotionBrandRepository.findByPromotionId(promotion.getId());
        return new PromotionResponse(
                promotion.getId(),
                promotion.getName(),
                promotion.getDescription(),
                promotion.getDiscountType(),
                promotion.getDiscountValue(),
                promotion.getStartDate().toLocalDate(),
                promotion.getEndDate().toLocalDate(),
                promotion.getStatus(),
                promotion.getPriority(),
                PromotionRequest.TargetType.BRAND,
                brands.stream().map(item -> item.getBrand().getId()).toList(),
                null,
                0,
                promotion.getCreatedAt(),
                promotion.getUpdatedAt()
        );
    }

    private Promotion findPromotion(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found"));
    }
}
