package com.hbcstore.hbcstore_api.payment.payos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbcstore.hbcstore_api.order.StoreOrder;
import com.hbcstore.hbcstore_api.order.StoreOrderRepository;
import com.hbcstore.hbcstore_api.payment.payos.dto.PayOSCreatePaymentResponse;
import com.hbcstore.hbcstore_api.user.User;
import com.hbcstore.hbcstore_api.user.UserRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayOSPaymentService {
    private static final long BANK_TRANSFER_PAYMENT_EXPIRE_MINUTES = 15L;
    private static final long ORDER_CODE_FACTOR = 1_000_000L;

    private final StoreOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PayOSProperties payOSProperties;
    private final ObjectMapper objectMapper;

    public PayOSPaymentService(
            StoreOrderRepository orderRepository,
            UserRepository userRepository,
            PayOSProperties payOSProperties
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.payOSProperties = payOSProperties;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public PayOSCreatePaymentResponse createPayment(Long orderId, String principalEmail) {
        StoreOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        validateOrderOwnership(order, principalEmail);
        validateOrderForPayOS(order);

        if (order.getPaymentExpiredAt() == null || order.getPaymentExpiredAt().isBefore(LocalDateTime.now())) {
            order.setPaymentExpiredAt(LocalDateTime.now().plusMinutes(BANK_TRANSFER_PAYMENT_EXPIRE_MINUTES));
            orderRepository.save(order);
        }

        if (!payOSProperties.isEnabled() || payOSProperties.isMockMode()) {
            return new PayOSCreatePaymentResponse(
                    "mock-" + UUID.randomUUID(),
                    "HBC-" + order.getId(),
                    "",
                    payOSProperties.getReturnUrl(),
                    "Đã tạo liên kết thanh toán PayOS ở chế độ thử nghiệm",
                    0
            );
        }

        if (payOSProperties.getApiKey().isBlank() || payOSProperties.getClientId().isBlank()) {
            throw new IllegalStateException("PayOS credentials are missing");
        }

        try {
            long amount = toAmount(order.getTotalAmount());
            long orderCode = buildOrderCode(order.getId());
            String description = ("HBC-" + order.getId());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("orderCode", orderCode);
            payload.put("amount", amount);
            payload.put("description", description);
            payload.put("returnUrl", payOSProperties.getReturnUrl());
            payload.put("cancelUrl", payOSProperties.getReturnUrl());
            payload.put("expiredAt", toEpochSeconds(order.getPaymentExpiredAt()));
            payload.put("signature", signCreatePaymentPayload(orderCode, amount, description, payOSProperties.getReturnUrl(), payOSProperties.getReturnUrl()));

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(payOSProperties.getApiBaseUrl() + "/v2/payment-requests"))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", payOSProperties.getClientId())
                    .header("x-api-key", payOSProperties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("PayOS create session failed: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            JsonNode data = json.path("data");
            String payUrl = data.path("checkoutUrl").asText("");
            String qrCode = data.path("qrCode").asText("");
            String requestId = data.path("paymentLinkId").asText(UUID.randomUUID().toString());
            if (payUrl.isBlank()) {
                throw new IllegalStateException("PayOS response missing paymentLink");
            }

            return new PayOSCreatePaymentResponse(
                    requestId,
                    String.valueOf(orderCode),
                    qrCode,
                    payUrl,
                    "Đã tạo liên kết thanh toán",
                    0
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo liên kết thanh toán PayOS", ex);
        }
    }

    @Transactional
    public Map<String, String> handleWebhook(Map<String, Object> payload) {
        if (!verifyChecksumIfProvided(payload)) {
            return Map.of("code", "97", "message", "Chữ ký xác thực không hợp lệ");
        }

        Long orderId = extractOrderId(payload);
        if (orderId == null) {
            return Map.of("code", "01", "message", "Không tìm thấy đơn hàng");
        }

        StoreOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return Map.of("code", "01", "message", "Không tìm thấy đơn hàng");
        }

        if (order.getPaymentStatus() == StoreOrder.PaymentStatus.PAID) {
            return Map.of("code", "00", "message", "Đơn hàng đã được thanh toán");
        }

        if (isPaidEvent(payload)) {
            order.setPaymentStatus(StoreOrder.PaymentStatus.PAID);
            order.setPaymentExpiredAt(null);
            orderRepository.save(order);
            return Map.of("code", "00", "message", "Thanh toán đã được xác nhận");
        }

        return Map.of("code", "00", "message", "Đã nhận sự kiện thanh toán");
    }

    @Transactional
    public Map<String, String> handleReturn(Map<String, String> payload) {
        String code = asUpperString(payload.get("code"));
        String status = asUpperString(payload.get("status"));
        boolean canceledOrFailed =
                "CANCELLED".equals(status)
                        || "CANCELED".equals(status)
                        || "FAILED".equals(status)
                        || "EXPIRED".equals(status)
                        || "ERROR".equals(status)
                        || "TRUE".equals(asUpperString(payload.get("cancel")));
        boolean paid = !canceledOrFailed && ("PAID".equals(status) || "SUCCESS".equals(status));

        Long orderId = resolveLocalOrderId(parseLong(payload.get("orderCode")));
        if (orderId == null) {
            orderId = parseLong(payload.get("orderId"));
        }
        String paymentLinkId = payload.get("id");
        if (orderId == null) {
            if (paymentLinkId != null && !paymentLinkId.isBlank()) {
                orderId = resolveLocalOrderId(fetchOrderCodeByPaymentLinkId(paymentLinkId.trim()));
            }
        }
        if (orderId == null) {
            return Map.of("code", "01", "message", "Không tìm thấy đơn hàng");
        }

        // Source of truth: query PayOS link status when payment link id is available.
        if (paymentLinkId != null && !paymentLinkId.isBlank()) {
            String remoteStatus = fetchPaymentLinkStatusById(paymentLinkId.trim());
            if ("PAID".equals(remoteStatus) || "SUCCESS".equals(remoteStatus)) {
                paid = true;
            } else if ("CANCELLED".equals(remoteStatus) || "CANCELED".equals(remoteStatus) || "FAILED".equals(remoteStatus) || "EXPIRED".equals(remoteStatus)) {
                paid = false;
            }
        } else if ("00".equals(code) && status.isBlank()) {
            // Fall back for legacy return shape where only "code=00" is present.
            paid = true;
        }

        StoreOrder order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return Map.of("code", "01", "message", "Không tìm thấy đơn hàng");
        }

        if (paid) {
            order.setPaymentStatus(StoreOrder.PaymentStatus.PAID);
            order.setPaymentExpiredAt(null);
            orderRepository.save(order);
            return Map.of("code", "00", "message", "Thanh toán đã được xác nhận");
        }

        return Map.of("code", "00", "message", "Đã nhận kết quả thanh toán");
    }

    public Map<String, Object> getSettings() {
        String webhookUrl = payOSProperties.getWebhookUrl() == null || payOSProperties.getWebhookUrl().isBlank()
                ? "http://localhost:8080/api/payments/payos/webhook"
                : payOSProperties.getWebhookUrl();

        return Map.of(
                "enabled", payOSProperties.isEnabled(),
                "mockMode", payOSProperties.isMockMode(),
                "merchantId", maskValue(payOSProperties.getClientId()),
                "webhookUrl", webhookUrl,
                "returnUrl", payOSProperties.getReturnUrl(),
                "hasApiKey", payOSProperties.getApiKey() != null && !payOSProperties.getApiKey().isBlank(),
                "hasChecksumKey", payOSProperties.getChecksumKey() != null && !payOSProperties.getChecksumKey().isBlank()
        );
    }

    private void validateOrderOwnership(StoreOrder order, String principalEmail) {
        if (principalEmail == null || principalEmail.isBlank()) {
            throw new IllegalArgumentException("Yêu cầu chưa đăng nhập");
        }
        User user = userRepository.findByEmail(principalEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        if (user.getRole() == User.Role.ADMIN) {
            throw new IllegalArgumentException("Tài khoản quản trị không thể tạo thanh toán");
        }
        if (order.getUser() == null || !Objects.equals(order.getUser().getId(), user.getId())) {
            throw new IllegalArgumentException("Bạn chỉ có thể thanh toán đơn hàng của chính mình");
        }
    }

    private void validateOrderForPayOS(StoreOrder order) {
        if (order.getPaymentMethod() != StoreOrder.PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("Đơn hàng này không dùng phương thức chuyển khoản");
        }
        if (order.getPaymentStatus() == StoreOrder.PaymentStatus.PAID) {
            throw new IllegalArgumentException("Đơn hàng đã được thanh toán");
        }
        if (order.getStatus() == StoreOrder.OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Đơn hàng đã hủy không thể thanh toán");
        }
        if (order.getStatus() == StoreOrder.OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Đơn hàng đã giao không thể thanh toán");
        }
    }

    private boolean verifyChecksumIfProvided(Map<String, Object> payload) {
        Object signatureObj = payload.get("signature");
        Object dataObj = payload.get("data");
        if (!(signatureObj instanceof String signature) || !(dataObj instanceof Map<?, ?> dataRaw)) {
            return true;
        }
        String checksumKey = payOSProperties.getChecksumKey();
        if (checksumKey == null || checksumKey.isBlank()) {
            return true;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : dataRaw.entrySet()) {
            if (entry.getKey() != null) {
                data.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }

        String signingData = buildSigningData(data);
        String expected = hmacSha256(checksumKey, signingData);
        return expected.equalsIgnoreCase(signature);
    }

    private static String buildSigningData(Map<String, Object> data) {
        Map<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() != null) {
                sorted.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    private static String hmacSha256(String secret, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(key);
            byte[] bytes = hmac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (byte b : bytes) {
                hash.append(String.format("%02x", b));
            }
            return hash.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể xác minh chữ ký thanh toán", ex);
        }
    }

    private static boolean isPaidEvent(Map<String, Object> payload) {
        Object successObj = payload.get("success");
        if (successObj instanceof Boolean success && success) return true;

        String code = asUpperString(payload.get("code"));
        if ("00".equals(code)) return true;

        Object payloadObj = payload.get("payload");
        if (payloadObj instanceof Map<?, ?> p) {
            String status = asUpperString(p.get("status"));
            if ("COMPLETED".equals(status) || "PAID".equals(status) || "SUCCESS".equals(status)) return true;
        }

        Object dataObj = payload.get("data");
        if (dataObj instanceof Map<?, ?> data) {
            String status = asUpperString(data.get("status"));
            if ("COMPLETED".equals(status) || "PAID".equals(status) || "SUCCESS".equals(status)) return true;
            String dCode = asUpperString(data.get("code"));
            return "00".equals(dCode);
        }

        return false;
    }

    private Long extractOrderId(Map<String, Object> payload) {
        Long fromPayload = extractOrderIdFromNode(payload.get("payload"));
        if (fromPayload != null) return fromPayload;
        return extractOrderIdFromNode(payload.get("data"));
    }

    @SuppressWarnings("unchecked")
    private Long extractOrderIdFromNode(Object node) {
        if (!(node instanceof Map<?, ?> map)) return null;

        Object merchantReference = map.get("merchantReference");
        Long fromReference = parseOrderIdFromReference(merchantReference == null ? null : merchantReference.toString());
        if (fromReference != null) return fromReference;

        Object orderCode = map.get("orderCode");
        if (orderCode instanceof Number n) {
            return resolveLocalOrderId(n.longValue());
        }
        if (orderCode != null) {
            try {
                return resolveLocalOrderId(Long.parseLong(orderCode.toString()));
            } catch (NumberFormatException ignored) {
            }
        }

        Object identifiersObj = map.get("identifiers");
        if (identifiersObj instanceof Map<?, ?> identifiers) {
            Object ref = identifiers.get("merchantReference");
            Long id = parseOrderIdFromReference(ref == null ? null : ref.toString());
            if (id != null) return id;
        }

        return null;
    }

    private static Long parseOrderIdFromReference(String merchantReference) {
        if (merchantReference == null || merchantReference.isBlank()) return null;
        String[] parts = merchantReference.split("-");
        if (parts.length >= 2 && "HBC".equalsIgnoreCase(parts[0])) {
            try {
                return Long.parseLong(parts[1]);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String asUpperString(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toUpperCase();
    }

    private static Long parseLong(Object value) {
        if (value == null) return null;
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long fetchOrderCodeByPaymentLinkId(String paymentLinkId) {
        if (payOSProperties.getApiKey() == null || payOSProperties.getApiKey().isBlank()) return null;
        if (payOSProperties.getClientId() == null || payOSProperties.getClientId().isBlank()) return null;
        if (paymentLinkId == null || paymentLinkId.isBlank()) return null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(payOSProperties.getApiBaseUrl() + "/v2/payment-requests/" + paymentLinkId))
                    .header("x-client-id", payOSProperties.getClientId())
                    .header("x-api-key", payOSProperties.getApiKey())
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (data.isMissingNode()) return null;
            return parseLong(data.path("orderCode").asText(null));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String fetchPaymentLinkStatusById(String paymentLinkId) {
        if (payOSProperties.getApiKey() == null || payOSProperties.getApiKey().isBlank()) return "";
        if (payOSProperties.getClientId() == null || payOSProperties.getClientId().isBlank()) return "";
        if (paymentLinkId == null || paymentLinkId.isBlank()) return "";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(payOSProperties.getApiBaseUrl() + "/v2/payment-requests/" + paymentLinkId))
                    .header("x-client-id", payOSProperties.getClientId())
                    .header("x-api-key", payOSProperties.getApiKey())
                    .GET()
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "";
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            if (data.isMissingNode()) return "";
            return asUpperString(data.path("status").asText(""));
        } catch (Exception ignored) {
            return "";
        }
    }

    private String signCreatePaymentPayload(long orderCode, long amount, String description, String returnUrl, String cancelUrl) {
        String raw = "amount=" + amount
                + "&cancelUrl=" + cancelUrl
                + "&description=" + description
                + "&orderCode=" + orderCode
                + "&returnUrl=" + returnUrl;
        return hmacSha256(payOSProperties.getChecksumKey(), raw);
    }

    private static long toEpochSeconds(LocalDateTime dateTime) {
        if (dateTime == null) {
            return java.time.Instant.now().plusSeconds(15 * 60).getEpochSecond();
        }
        return dateTime.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
    }

    private static long buildOrderCode(Long orderId) {
        long base = Math.abs(orderId == null ? 0L : orderId);
        long suffix = Math.abs(System.currentTimeMillis() % ORDER_CODE_FACTOR);
        return base * ORDER_CODE_FACTOR + suffix;
    }

    private Long resolveLocalOrderId(Long rawOrderCode) {
        if (rawOrderCode == null) return null;
        if (orderRepository.existsById(rawOrderCode)) {
            return rawOrderCode;
        }
        long candidate = rawOrderCode / ORDER_CODE_FACTOR;
        if (candidate > 0 && orderRepository.existsById(candidate)) {
            return candidate;
        }
        return null;
    }

    private static String maskValue(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.length() <= 6) return "******";
        return value.substring(0, 3) + "..." + value.substring(value.length() - 3);
    }

    private static long toAmount(BigDecimal amount) {
        if (amount == null) return 0L;
        return amount.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
    }
}
