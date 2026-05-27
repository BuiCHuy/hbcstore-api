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
            throw new IllegalArgumentException("Missing Google idToken");
        }
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("Google login is not configured");
        }
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken.trim();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("Invalid Google token");
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
                throw new IllegalArgumentException("Google audience mismatch");
            }
            if (!ALLOWED_ISSUERS.contains(issuer)) {
                throw new IllegalArgumentException("Google issuer mismatch");
            }
            if (sub.isBlank() || email.isBlank()) {
                throw new IllegalArgumentException("Google profile is invalid");
            }
            if (!emailVerified) {
                throw new IllegalArgumentException("Google email is not verified");
            }
            if (exp > 0 && Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
                throw new IllegalArgumentException("Google token expired");
            }

            return new GoogleProfile(sub, email.trim().toLowerCase(), name == null ? "" : name.trim());
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot verify Google token", ex);
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
