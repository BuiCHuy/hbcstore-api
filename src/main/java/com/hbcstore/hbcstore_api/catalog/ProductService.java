package com.hbcstore.hbcstore_api.catalog;

import com.hbcstore.hbcstore_api.catalog.dto.ProductRequest;
import com.hbcstore.hbcstore_api.catalog.dto.ProductResponse;
import com.hbcstore.hbcstore_api.review.ProductReview;
import com.hbcstore.hbcstore_api.review.ProductReviewRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductReviewRepository reviewRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            ProductReviewRepository reviewRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll(String search) {
        List<Product> products;
        if (search == null || search.isBlank()) {
            products = productRepository.findAll();
        } else {
            products = productRepository.search(search.trim());
        }
        return products.stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return toProductResponse(findProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        return toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        applyRequest(product, request);
        return toProductResponse(productRepository.save(product));
    }

    private ProductResponse toProductResponse(Product product) {
        long reviewCount = reviewRepository.countByProductIdAndStatus(
                product.getId(),
                ProductReview.ReviewStatus.APPROVED
        );
        Double avg = reviewRepository.getAverageRatingByProductIdAndStatus(
                product.getId(),
                ProductReview.ReviewStatus.APPROVED
        );
        double rating = avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0;
        return ProductResponse.from(product, rating, reviewCount);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        product.setStatus(Product.ProductStatus.INACTIVE);
        productRepository.save(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
    }

    private Brand findBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + id));
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setThumbnailUrl(request.thumbnailUrl());
        product.setDescription(request.description());
        product.setCategory(findCategory(request.categoryId()));
        product.setBrand(findBrand(request.brandId()));
        product.setStatus(request.status() == null ? Product.ProductStatus.ACTIVE : request.status());
        syncProductImages(product, request.imageUrls());
        syncProductAttributes(product, request.attributes());
    }

    private void syncProductImages(Product product, List<String> imageUrls) {
        product.getProductImages().clear();
        if (imageUrls == null) {
            return;
        }

        int sortOrder = 0;
        for (String rawUrl : imageUrls) {
            if (rawUrl == null) {
                continue;
            }
            String url = rawUrl.trim();
            if (url.isEmpty()) {
                continue;
            }

            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(url);
            image.setSortOrder(sortOrder++);
            product.getProductImages().add(image);
        }
    }

    private void syncProductAttributes(
            Product product,
            List<ProductRequest.AttributeRequest> attributes
    ) {
        product.getProductAttributes().clear();
        if (attributes == null) {
            return;
        }

        for (ProductRequest.AttributeRequest item : attributes) {
            if (item == null || item.name() == null || item.value() == null) {
                continue;
            }
            String name = item.name().trim();
            String value = item.value().trim();
            if (name.isEmpty() || value.isEmpty()) {
                continue;
            }

            ProductAttribute attribute = new ProductAttribute();
            attribute.setProduct(product);
            attribute.setAttributeName(name);
            attribute.setAttributeValue(value);
            product.getProductAttributes().add(attribute);
        }
    }
}
