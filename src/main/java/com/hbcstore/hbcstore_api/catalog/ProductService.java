package com.hbcstore.hbcstore_api.catalog;

import com.hbcstore.hbcstore_api.catalog.dto.ProductFacetsResponse;
import com.hbcstore.hbcstore_api.catalog.dto.ProductRequest;
import com.hbcstore.hbcstore_api.catalog.dto.ProductResponse;
import com.hbcstore.hbcstore_api.review.ProductReview;
import com.hbcstore.hbcstore_api.review.ProductReviewRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final BrandRepository brandRepository;
    private final ProductReviewRepository reviewRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            SubcategoryRepository subcategoryRepository,
            BrandRepository brandRepository,
            ProductReviewRepository reviewRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.brandRepository = brandRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll(String search) {
        return getAll(search, null, null, Map.of());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll(String search, Long categoryId, String grade) {
        Map<String, String> filters = new LinkedHashMap<>();
        if (grade != null && !grade.isBlank()) {
            filters.put("grade", grade);
        }
        return getAll(search, categoryId, null, filters);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll(
            String search,
            Long categoryId,
            Long subcategoryId,
            Map<String, String> attributeFilters
    ) {
        String normalizedSearch = normalize(search);
        List<Product> products = productRepository.searchWithFilters(normalizedSearch, categoryId, subcategoryId, null);
        List<Product> filtered = applyAttributeFilters(products, attributeFilters);
        return filtered.stream().map(this::toProductResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getPage(String search, Pageable pageable) {
        return getPage(search, null, null, Map.of(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getPage(String search, Long categoryId, String grade, Pageable pageable) {
        Map<String, String> filters = new LinkedHashMap<>();
        if (grade != null && !grade.isBlank()) {
            filters.put("grade", grade);
        }
        return getPage(search, categoryId, null, filters, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getPage(
            String search,
            Long categoryId,
            Long subcategoryId,
            Map<String, String> attributeFilters,
            Pageable pageable
    ) {
        String normalizedSearch = normalize(search);
        Page<Product> products = productRepository.searchWithFilters(normalizedSearch, categoryId, subcategoryId, null, pageable);
        List<Product> filtered = applyAttributeFilters(products.getContent(), attributeFilters);
        List<ProductResponse> content = filtered.stream().map(this::toProductResponse).toList();
        return new PageImpl<>(content, pageable, products.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductFacetsResponse getFacets(Long categoryId, Long subcategoryId) {
        List<Product> products = productRepository.searchWithFilters(null, categoryId, subcategoryId, null);
        Map<String, TreeSet<String>> facetMap = new LinkedHashMap<>();
        TreeSet<String> brandSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (Product product : products) {
            if (product.getStatus() == Product.ProductStatus.INACTIVE) continue;
            if (product.getBrand() != null && product.getBrand().getName() != null) {
                String brandName = product.getBrand().getName().trim();
                if (!brandName.isEmpty()) {
                    brandSet.add(brandName);
                }
            }
            for (ProductAttribute attribute : product.getProductAttributes()) {
                String key = normalizeKey(attribute.getAttributeName());
                String value = normalize(attribute.getAttributeValue());
                if (key == null || value == null) continue;
                facetMap.computeIfAbsent(key, ignored -> new TreeSet<>()).add(value);
            }
        }

        Map<String, List<String>> attributes = new LinkedHashMap<>();
        facetMap.forEach((key, values) -> attributes.put(key, new ArrayList<>(values)));
        return new ProductFacetsResponse(new ArrayList<>(brandSet), attributes);
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

    private List<Product> applyAttributeFilters(List<Product> products, Map<String, String> attributeFilters) {
        if (attributeFilters == null || attributeFilters.isEmpty()) return products;

        List<Map.Entry<String, String>> normalizedFilters = attributeFilters.entrySet().stream()
                .map(entry -> Map.entry(normalizeKey(entry.getKey()), normalize(entry.getValue())))
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .toList();

        if (normalizedFilters.isEmpty()) return products;

        List<Product> result = new ArrayList<>();
        for (Product product : products) {
            Map<String, String> attrMap = new LinkedHashMap<>();
            for (ProductAttribute attribute : product.getProductAttributes()) {
                String key = normalizeKey(attribute.getAttributeName());
                String value = normalize(attribute.getAttributeValue());
                if (key != null && value != null) {
                    attrMap.putIfAbsent(key, value);
                }
            }
            boolean matched = normalizedFilters.stream().allMatch(filter -> {
                String actual = attrMap.get(filter.getKey());
                return actual != null && actual.equalsIgnoreCase(filter.getValue());
            });
            if (matched) result.add(product);
        }
        return result;
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
        Category category = findCategory(request.categoryId());
        product.setName(request.name().trim());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setThumbnailUrl(request.thumbnailUrl());
        product.setDescription(request.description());
        product.setCategory(category);
        product.setSubcategory(resolveSubcategory(request.subcategoryId(), category));
        product.setBrand(findBrand(request.brandId()));
        product.setStatus(request.status() == null ? Product.ProductStatus.ACTIVE : request.status());
        syncProductImages(product, request.imageUrls());
        syncProductAttributes(product, request.attributes());
    }

    private Subcategory resolveSubcategory(Long subcategoryId, Category category) {
        if (subcategoryId == null) return null;
        Subcategory subcategory = subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Subcategory not found: " + subcategoryId));
        if (!subcategory.getCategory().getId().equals(category.getId())) {
            throw new IllegalArgumentException("Subcategory does not belong to selected category");
        }
        return subcategory;
    }

    private void syncProductImages(Product product, List<String> imageUrls) {
        product.getProductImages().clear();
        if (imageUrls == null) return;

        int sortOrder = 0;
        for (String rawUrl : imageUrls) {
            if (rawUrl == null) continue;
            String url = rawUrl.trim();
            if (url.isEmpty()) continue;

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
        if (attributes == null) return;

        for (ProductRequest.AttributeRequest item : attributes) {
            if (item == null || item.name() == null || item.value() == null) continue;
            String name = item.name().trim();
            String value = item.value().trim();
            if (name.isEmpty() || value.isEmpty()) continue;

            ProductAttribute attribute = new ProductAttribute();
            attribute.setProduct(product);
            attribute.setAttributeName(name);
            attribute.setAttributeValue(value);
            product.getProductAttributes().add(attribute);
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeKey(String value) {
        String normalized = normalize(value);
        if (normalized == null) return null;
        return normalized.toLowerCase(Locale.ROOT);
    }
}
