package com.hbcstore.hbcstore_api.coupon;

import com.hbcstore.hbcstore_api.coupon.dto.CouponRequest;
import com.hbcstore.hbcstore_api.coupon.dto.CouponResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {
    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> getAll() {
        return couponRepository.findAll().stream()
                .map(CouponResponse::from)
                .toList();
    }

    @Transactional
    public CouponResponse create(CouponRequest request) {
        couponRepository.findByCodeIgnoreCase(request.code().trim())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Coupon code already exists");
                });

        Coupon coupon = new Coupon();
        applyRequest(coupon, request);
        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse update(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + id));

        couponRepository.findByCodeIgnoreCase(request.code().trim())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new IllegalArgumentException("Coupon code already exists");
                    }
                });

        applyRequest(coupon, request);
        return CouponResponse.from(couponRepository.save(coupon));
    }

    @Transactional
    public void delete(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + id));
        coupon.setStatus(Coupon.CouponStatus.INACTIVE);
        couponRepository.save(coupon);
    }

    private void applyRequest(Coupon coupon, CouponRequest request) {
        coupon.setCode(request.code().trim().toUpperCase());
        coupon.setDiscountType(request.discountType());
        coupon.setDiscountValue(request.discountValue());
        coupon.setMinOrderValue(request.minOrderValue());
        coupon.setMaxDiscountAmount(request.maxDiscountAmount());

        LocalDateTime startAt = request.startDate() == null
                ? LocalDateTime.now()
                : request.startDate().atStartOfDay();
        LocalDateTime endAt = request.endDate().atTime(23, 59, 59);
        if (endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        coupon.setStartDate(startAt);
        coupon.setEndDate(endAt);
        coupon.setUsageLimit(request.usageLimit());
        if (coupon.getUsedCount() == null) {
            coupon.setUsedCount(0);
        }
        coupon.setStatus(request.status() == null ? Coupon.CouponStatus.ACTIVE : request.status());
    }
}
