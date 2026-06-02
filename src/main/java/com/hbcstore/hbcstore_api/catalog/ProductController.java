package com.hbcstore.hbcstore_api.catalog;

import com.hbcstore.hbcstore_api.catalog.dto.ProductRequest;
import com.hbcstore.hbcstore_api.catalog.dto.ProductFacetsResponse;
import com.hbcstore.hbcstore_api.catalog.dto.ProductResponse;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Object getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) String grade,
            @RequestParam MultiValueMap<String, String> requestParams,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Map<String, String> attributeFilters = extractAttributeFilters(requestParams);
        if (grade != null && !grade.isBlank()) {
            attributeFilters.putIfAbsent("grade", grade);
        }

        if (page != null || size != null) {
            int resolvedPage = page == null || page < 0 ? 0 : page;
            int resolvedSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
            Page<ProductResponse> paged = productService.getPage(search, categoryId, subcategoryId, attributeFilters, PageRequest.of(resolvedPage, resolvedSize));
            return paged;
        }
        return productService.getAll(search, categoryId, subcategoryId, attributeFilters);
    }

    @GetMapping("/facets")
    public ProductFacetsResponse getFacets(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subcategoryId
    ) {
        return productService.getFacets(categoryId, subcategoryId);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    private Map<String, String> extractAttributeFilters(MultiValueMap<String, String> params) {
        Map<String, String> filters = new LinkedHashMap<>();
        params.forEach((key, values) -> {
            if (key == null || !key.startsWith("attr.")) return;
            if (values == null || values.isEmpty()) return;
            String attrKey = key.substring("attr.".length()).trim();
            if (attrKey.isEmpty()) return;
            String attrValue = values.getFirst();
            if (attrValue == null || attrValue.trim().isEmpty()) return;
            filters.put(attrKey, attrValue.trim());
        });
        return filters;
    }
}
