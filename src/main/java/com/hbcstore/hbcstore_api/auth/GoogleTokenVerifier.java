package com.hbcstore.hbcstore_api.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenVerifier {
    private static final Set<String> ALLOWED_ISSUERS = Set.of(
            "accounts.google.com",
            "https://accounts.google.com"
    );

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.auth.google.client-id:}")
    private String googleClientId;

    public GoogleProfile verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("Thiếu idToken của Google");
        }
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("Đăng nhập Google chưa được cấu hình");
        }
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken.trim();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("Token đăng nhập Google không hợp lệ");
            }
            JsonNode node = objectMapper.readTree(response.body());
            String audience = node.path("aud").asText("");
            String issuer = node.path("iss").asText("");
            String sub = node.path("sub").asText("");
            String email = node.path("email").asText("");
            String name = node.path("name").asText("");
            boolean emailVerified = "true".equalsIgnoreCase(node.path("email_verified").asText(""));
            long exp = parseLong(node.path("exp").asText(""));

            if (!googleClientId.equals(audience)) {
                throw new IllegalArgumentException("Ứng dụng Google không khớp cấu hình");
            }
            if (!ALLOWED_ISSUERS.contains(issuer)) {
                throw new IllegalArgumentException("Nguồn xác thực Google không hợp lệ");
            }
            if (sub.isBlank() || email.isBlank()) {
                throw new IllegalArgumentException("Thông tin tài khoản Google không hợp lệ");
            }
            if (!emailVerified) {
                throw new IllegalArgumentException("Email Google chưa được xác thực");
            }
            if (exp > 0 && Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
                throw new IllegalArgumentException("Phiên đăng nhập Google đã hết hạn");
            }

            return new GoogleProfile(sub, email.trim().toLowerCase(), name == null ? "" : name.trim());
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể xác minh đăng nhập Google", ex);
        }
    }

    private static long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public record GoogleProfile(
            String providerId,
            String email,
            String fullName
    ) {
    }
}
