package com.hbcstore.hbcstore_api.auth;

import com.hbcstore.hbcstore_api.user.User;
import com.hbcstore.hbcstore_api.user.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnverifiedAccountCleanupScheduler {
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cleanupExpiredUnverifiedAccounts() {
        List<EmailVerificationToken> expiredTokens =
                tokenRepository.findByUsedAtIsNullAndExpiresAtBefore(LocalDateTime.now());
        if (expiredTokens.isEmpty()) {
            return;
        }

        int deletedCount = 0;
        for (EmailVerificationToken token : expiredTokens) {
            User user = token.getUser();
            if (user.getProvider() == User.AuthProvider.LOCAL && user.getStatus() == User.UserStatus.INACTIVE) {
                tokenRepository.deleteByUser(user);
                userRepository.delete(user);
                deletedCount++;
            } else {
                token.setUsedAt(LocalDateTime.now());
                tokenRepository.save(token);
            }
        }

        if (deletedCount > 0) {
            log.info("Auto-deleted {} unverified expired local accounts", deletedCount);
        }
    }
}
