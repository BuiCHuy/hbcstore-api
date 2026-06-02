package com.hbcstore.hbcstore_api.catalog;

import com.hbcstore.hbcstore_api.catalog.dto.SubcategoryRequest;
import com.hbcstore.hbcstore_api.catalog.dto.SubcategoryResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubcategoryService {
    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;

    public SubcategoryService(SubcategoryRepository subcategoryRepository, CategoryRepository categoryRepository) {
        this.subcategoryRepository = subcategoryRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<SubcategoryResponse> getAll(Long categoryId) {
        List<Subcategory> list = categoryId == null
                ? subcategoryRepository.findAll()
                : subcategoryRepository.findAllByCategoryId(categoryId);
        return list.stream().map(SubcategoryResponse::from).toList();
    }

    @Transactional
    public SubcategoryResponse create(SubcategoryRequest request) {
        Subcategory subcategory = new Subcategory();
        applyRequest(subcategory, request);
        return SubcategoryResponse.from(subcategoryRepository.save(subcategory));
    }

    @Transactional
    public SubcategoryResponse update(Long id, SubcategoryRequest request) {
        Subcategory subcategory = findSubcategory(id);
        applyRequest(subcategory, request);
        return SubcategoryResponse.from(subcategoryRepository.save(subcategory));
    }

    @Transactional
    public void delete(Long id) {
        Subcategory subcategory = findSubcategory(id);
        subcategory.setStatus(Subcategory.Status.INACTIVE);
        subcategoryRepository.save(subcategory);
    }

    private void applyRequest(Subcategory subcategory, SubcategoryRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.categoryId()));
        subcategory.setName(request.name().trim());
        subcategory.setDescription(request.description());
        subcategory.setIconUrl(request.iconUrl());
        subcategory.setCategory(category);
        subcategory.setStatus(request.status() == null ? Subcategory.Status.ACTIVE : request.status());
    }

    private Subcategory findSubcategory(Long id) {
        return subcategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subcategory not found: " + id));
    }
}
