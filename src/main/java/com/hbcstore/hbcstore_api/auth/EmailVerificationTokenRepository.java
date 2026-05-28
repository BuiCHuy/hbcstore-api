package com.hbcstore.hbcstore_api.auth;

import com.hbcstore.hbcstore_api.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    List<EmailVerificationToken> findByUsedAtIsNullAndExpiresAtBefore(LocalDateTime time);
    void deleteByUser(User user);
    boolean existsByUserAndUsedAtIsNull(User user);
}
