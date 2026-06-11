package com.hbcstore.hbcstore_api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbcstore.hbcstore_api.user.User;
import com.hbcstore.hbcstore_api.user.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String FORGOT_PASSWORD_SUCCESS_MESSAGE =
            "Nếu email tồn tại, hệ thống đã gửi hướng dẫn đặt lại mật khẩu.";

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String resetBaseUrl;
    private final long ttlMinutes;
    private final String brevoApiKey;
    private final String brevoFromEmail;
    private final String brevoFromName;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.reset.base-url:http://localhost:5173/reset-password}") String resetBaseUrl,
            @Value("${app.auth.reset.ttl-minutes:15}") long ttlMinutes,
            @Value("${brevo.api-key:}") String brevoApiKey,
            @Value("${brevo.from-email:}") String brevoFromEmail,
            @Value("${brevo.from-name:HBC Store}") String brevoFromName
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.resetBaseUrl = resetBaseUrl;
        this.ttlMinutes = ttlMinutes;
        this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
        this.brevoFromEmail = brevoFromEmail == null ? "" : brevoFromEmail.trim();
        this.brevoFromName = brevoFromName == null ? "HBC Store" : brevoFromName.trim();
    }

    @Transactional
    public String requestReset(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getProvider() != User.AuthProvider.LOCAL || user.getStatus() == User.UserStatus.BANNED) {
            return FORGOT_PASSWORD_SUCCESS_MESSAGE;
        }

        tokenRepository.deleteByUser(user);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(ttlMinutes));
        tokenRepository.save(token);

        sendResetEmail(user.getEmail(), resetBaseUrl + "?token=" + token.getToken());
        return FORGOT_PASSWORD_SUCCESS_MESSAGE;
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new IllegalArgumentException("Liên kết đặt lại mật khẩu không hợp lệ"));

        if (token.getUsedAt() != null) {
            throw new IllegalArgumentException("Liên kết đặt lại mật khẩu đã được sử dụng");
        }
        if (token.isExpired()) {
            throw new IllegalArgumentException("Liên kết đặt lại mật khẩu đã hết hạn");
        }

        User user = token.getUser();
        if (user.getProvider() != User.AuthProvider.LOCAL) {
            throw new IllegalArgumentException("Tài khoản này đang dùng đăng nhập mạng xã hội");
        }
        if (user.getStatus() == User.UserStatus.BANNED) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    private void sendResetEmail(String toEmail, String resetUrl) {
        if (brevoApiKey.isBlank() || brevoFromEmail.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình BREVO_API_KEY hoặc BREVO_FROM_EMAIL.");
        }

        try {
            Map<String, Object> sender = new HashMap<>();
            sender.put("name", brevoFromName);
            sender.put("email", brevoFromEmail);

            Map<String, Object> to = new HashMap<>();
            to.put("email", toEmail);

            String html = "<p>Chào bạn,</p>"
                    + "<p>Nhấn vào liên kết dưới đây để đặt lại mật khẩu HBC Store:</p>"
                    + "<p><a href=\"" + resetUrl + "\">Đặt lại mật khẩu</a></p>"
                    + "<p>Liên kết có hiệu lực trong " + ttlMinutes + " phút.</p>"
                    + "<p>Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email.</p>";

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", sender);
            payload.put("to", new Object[]{to});
            payload.put("subject", "Đặt lại mật khẩu HBC Store");
            payload.put("htmlContent", html);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", brevoApiKey)
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code < 200 || code >= 300) {
                log.error("Brevo password reset send failed. status={}, body={}", code, response.body());
                String body = response.body() == null ? "" : response.body();
                if (body.length() > 240) {
                    body = body.substring(0, 240) + "...";
                }
                throw new IllegalStateException("Brevo API lỗi (status=" + code + "): " + body);
            }
        } catch (Exception ex) {
            String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            throw new IllegalStateException("Không gửi được email đặt lại mật khẩu qua Brevo: " + detail, ex);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
