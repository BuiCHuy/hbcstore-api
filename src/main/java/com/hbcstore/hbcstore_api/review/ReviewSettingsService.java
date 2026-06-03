package com.hbcstore.hbcstore_api.review;

import com.hbcstore.hbcstore_api.review.dto.ReviewSettingsRequest;
import com.hbcstore.hbcstore_api.review.dto.ReviewSettingsResponse;
import com.hbcstore.hbcstore_api.user.User;
import com.hbcstore.hbcstore_api.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewSettingsService {
    private static final Long SETTINGS_ID = 1L;

    private final ReviewSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    public ReviewSettingsService(ReviewSettingsRepository settingsRepository, UserRepository userRepository) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ReviewSettingsResponse getSettings() {
        return ReviewSettingsResponse.from(getOrCreateSettings());
    }

    @Transactional
    public ReviewSettingsResponse updateSettings(ReviewSettingsRequest request, String adminEmail) {
        User admin = userRepository.findByEmailIgnoreCase(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay tai khoan quan tri"));
        if (admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Chi quan tri vien moi co the cap nhat cai dat danh gia");
        }

        ReviewSettings settings = getOrCreateSettings();
        settings.setReviewApprovalEnabled(Boolean.TRUE.equals(request.reviewApprovalEnabled()));
        return ReviewSettingsResponse.from(settingsRepository.save(settings));
    }

    @Transactional(readOnly = true)
    public boolean isReviewApprovalEnabled() {
        return getOrCreateSettings().isReviewApprovalEnabled();
    }

    private ReviewSettings getOrCreateSettings() {
        return settingsRepository.findById(SETTINGS_ID).orElseGet(() -> {
            ReviewSettings settings = new ReviewSettings();
            settings.setId(SETTINGS_ID);
            return settingsRepository.save(settings);
        });
    }
}
