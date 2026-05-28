package com.hbcstore.hbcstore_api.auth;

import com.hbcstore.hbcstore_api.user.User;
import com.hbcstore.hbcstore_api.user.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final String verifyBaseUrl;
    private final long ttlMinutes;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.auth.verify.base-url:http://localhost:5173/verify-email}") String verifyBaseUrl,
            @Value("${app.auth.verify.ttl-minutes:30}") long ttlMinutes
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.verifyBaseUrl = verifyBaseUrl;
        this.ttlMinutes = ttlMinutes;
    }

    @Transactional
    public void issueAndSend(User user) {
        tokenRepository.deleteByUser(user);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(ttlMinutes));
        tokenRepository.save(token);

        String verifyUrl = verifyBaseUrl + "?token=" + token.getToken();
        sendVerificationEmail(user.getEmail(), verifyUrl);
    }

    @Transactional
    public void verifyToken(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new IllegalArgumentException("Verification link is invalid"));

        if (token.getUsedAt() != null) {
            throw new IllegalArgumentException("Verification link was already used");
        }
        if (token.isExpired()) {
            throw new IllegalArgumentException("Verification link has expired");
        }

        User user = token.getUser();
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    @Transactional
    public void resend(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));
        if (user.getProvider() != User.AuthProvider.LOCAL) {
            throw new IllegalArgumentException("This account uses social login");
        }
        if (user.getStatus() == User.UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Email already verified");
        }
        issueAndSend(user);
    }

    private void sendVerificationEmail(String email, String verifyUrl) {
        if (mailSender == null) {
            log.warn("Mail sender is not configured. Verification URL for {}: {}", email, verifyUrl);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Xac thuc tai khoan HBC Store");
        message.setText("Nhan vao link de xac thuc tai khoan:\n" + verifyUrl);
        mailSender.send(message);
    }
}
