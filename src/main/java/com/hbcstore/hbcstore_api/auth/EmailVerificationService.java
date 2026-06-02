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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String verifyBaseUrl;
    private final long ttlMinutes;
    private final String brevoApiKey;
    private final String brevoFromEmail;
    private final String brevoFromName;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            @Value("${app.auth.verify.base-url:http://localhost:5173/verify-email}") String verifyBaseUrl,
            @Value("${app.auth.verify.ttl-minutes:30}") long ttlMinutes,
            @Value("${brevo.api-key:}") String brevoApiKey,
            @Value("${brevo.from-email:}") String brevoFromEmail,
            @Value("${brevo.from-name:HBC Store}") String brevoFromName
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.verifyBaseUrl = verifyBaseUrl;
        this.ttlMinutes = ttlMinutes;
        this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
        this.brevoFromEmail = brevoFromEmail == null ? "" : brevoFromEmail.trim();
        this.brevoFromName = brevoFromName == null ? "HBC Store" : brevoFromName.trim();
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
        if (brevoApiKey.isBlank() || brevoFromEmail.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình BREVO_API_KEY hoặc BREVO_FROM_EMAIL.");
        }
        sendByBrevo(email, verifyUrl);
    }

    private void sendByBrevo(String toEmail, String verifyUrl) {
        try {
            Map<String, Object> sender = new HashMap<>();
            sender.put("name", brevoFromName);
            sender.put("email", brevoFromEmail);

            Map<String, Object> to = new HashMap<>();
            to.put("email", toEmail);

            String html = "<p>Chào bạn,</p>"
                    + "<p>Nhấn vào liên kết dưới đây để xác thực tài khoản HBC Store:</p>"
                    + "<p><a href=\"" + verifyUrl + "\">Xác thực tài khoản</a></p>"
                    + "<p>Liên kết có hiệu lực trong " + ttlMinutes + " phút.</p>";

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", sender);
            payload.put("to", new Object[]{to});
            payload.put("subject", "Xác thực tài khoản HBC Store");
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
                log.error("Brevo send failed. status={}, body={}", code, response.body());
                String body = response.body() == null ? "" : response.body();
                if (body.length() > 240) {
                    body = body.substring(0, 240) + "...";
                }
                throw new IllegalStateException("Brevo API lỗi (status=" + code + "): " + body);
            }
        } catch (Exception ex) {
            String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            throw new IllegalStateException("Không gửi được email xác thực qua Brevo: " + detail, ex);
        }
    }
}
