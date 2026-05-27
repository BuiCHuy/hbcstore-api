package com.hbcstore.hbcstore_api.catalog;

import com.hbcstore.hbcstore_api.catalog.dto.BrandRequest;
import com.hbcstore.hbcstore_api.catalog.dto.BrandResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrandService {
    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> getAll() {
        return brandRepository.findAll().stream()
                .map(BrandResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BrandResponse getById(Long id) {
        return BrandResponse.from(findBrand(id));
    }

    @Transactional
    public BrandResponse create(BrandRequest request) {
        Brand brand = new Brand();
        applyRequest(brand, request);
        return BrandResponse.from(brandRepository.save(brand));
    }

    @Transactional
    public BrandResponse update(Long id, BrandRequest request) {
        Brand brand = findBrand(id);
        applyRequest(brand, request);
        return BrandResponse.from(brandRepository.save(brand));
    }

    @Transactional
    public void delete(Long id) {
        Brand brand = findBrand(id);
        brand.setStatus(Brand.Status.INACTIVE);
        brandRepository.save(brand);
    }

    Brand findBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found: " + id));
    }

    private void applyRequest(Brand brand, BrandRequest request) {
        brand.setName(request.name().trim());
        brand.setCountry(request.country());
        brand.setDescription(request.description());
        brand.setLogoUrl(request.logoUrl());
        brand.setStatus(request.status() == null ? Brand.Status.ACTIVE : request.status());
    }
}
