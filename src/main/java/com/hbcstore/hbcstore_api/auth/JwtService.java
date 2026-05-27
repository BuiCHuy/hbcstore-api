package com.hbcstore.hbcstore_api.auth;

import com.hbcstore.hbcstore_api.user.User;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.jwt.secret:hbc-store-dev-secret-change-me}")
    private String secret;

    @Value("${app.jwt.expiration-seconds:86400}")
    private long expirationSeconds;

    public String generateToken(User user) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = issuedAt + expirationSeconds;

        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = "{"
                + "\"sub\":\"" + escapeJson(user.getEmail()) + "\","
                + "\"uid\":" + user.getId() + ","
                + "\"role\":\"" + user.getRole().name() + "\","
                + "\"iat\":" + issuedAt + ","
                + "\"exp\":" + expiresAt
                + "}";

        String unsignedToken = base64Url(header.getBytes(StandardCharsets.UTF_8))
                + "."
                + base64Url(payload.getBytes(StandardCharsets.UTF_8));

        return unsignedToken + "." + sign(unsignedToken);
    }

    public boolean isValid(String token) {
        try {
            String[] parts = splitToken(token);
            String unsignedToken = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
                return false;
            }

            Long expiration = getLongClaim(token, "exp");
            return expiration != null && expiration > Instant.now().getEpochSecond();
        } catch (Exception ex) {
            return false;
        }
    }

    public String getSubject(String token) {
        return getStringClaim(token, "sub");
    }

    private String[] splitToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT token");
        }
        return parts;
    }

    private String getPayloadJson(String token) {
        String[] parts = splitToken(token);
        byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
        return new String(payload, StandardCharsets.UTF_8);
    }

    private String getStringClaim(String token, String claim) {
        String payload = getPayloadJson(token);
        String key = "\"" + claim + "\":\"";
        int start = payload.indexOf(key);
        if (start < 0) return null;
        int valueStart = start + key.length();
        int valueEnd = payload.indexOf("\"", valueStart);
        if (valueEnd < 0) return null;
        return payload.substring(valueStart, valueEnd).replace("\\\"", "\"");
    }

    private Long getLongClaim(String token, String claim) {
        String payload = getPayloadJson(token);
        String key = "\"" + claim + "\":";
        int start = payload.indexOf(key);
        if (start < 0) return null;
        int valueStart = start + key.length();
        int valueEnd = valueStart;
        while (valueEnd < payload.length() && Character.isDigit(payload.charAt(valueEnd))) {
            valueEnd++;
        }
        if (valueStart == valueEnd) return null;
        return Long.parseLong(payload.substring(valueStart, valueEnd));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return base64Url(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign JWT token", ex);
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}