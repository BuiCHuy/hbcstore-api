package com.hbcstore.hbcstore_api.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hbcstore.hbcstore_api.ai.dto.AiSearchResponse;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GeminiSearchService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiSearchService(GeminiProperties geminiProperties) {
        this.geminiProperties = geminiProperties;
    }

    public AiSearchResponse interpret(String query) {
        String normalized = normalize(query);
        if (normalized.isBlank()) {
            return new AiSearchResponse("", "", List.of(), isUsable(), "fallback");
        }
        if (!isUsable()) {
            return fallback(normalized);
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(geminiProperties.getTimeoutMs()))
                    .build();
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + geminiProperties.getModel()
                    + ":generateContent?key="
                    + URLEncoder.encode(geminiProperties.getApiKey(), StandardCharsets.UTF_8);

            String prompt = """
                    You are an ecommerce search normalizer.
                    User query: "%s"
                    Return strict JSON only:
                    {"normalizedQuery":"...","keywords":["...","..."]}
                    No markdown, no prose.
                    """.formatted(normalized.replace("\"", ""));

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(geminiProperties.getTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fallback(normalized);
            }

            Map<String, Object> payload = objectMapper.readValue(response.body(), MAP_TYPE);
            String text = extractCandidateText(payload);
            if (text == null || text.isBlank()) {
                return fallback(normalized);
            }

            Map<String, Object> parsed = parseJsonObject(text);
            String normalizedQuery = normalize(String.valueOf(parsed.getOrDefault("normalizedQuery", normalized)));
            List<String> keywords = extractKeywords(parsed.get("keywords"), normalizedQuery);
            return new AiSearchResponse(normalized, normalizedQuery, keywords, true, "gemini");
        } catch (Exception ex) {
            return fallback(normalized);
        }
    }

    public String generateAnswer(String prompt, String fallbackText) {
        String normalizedPrompt = normalize(prompt);
        if (normalizedPrompt.isBlank()) return fallbackText;
        if (!isUsable()) return fallbackText;

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(geminiProperties.getTimeoutMs()))
                    .build();
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + geminiProperties.getModel()
                    + ":generateContent?key="
                    + URLEncoder.encode(geminiProperties.getApiKey(), StandardCharsets.UTF_8);

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", normalizedPrompt)))
                    )
            );

            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(geminiProperties.getTimeoutMs()))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return fallbackText;

            Map<String, Object> payload = objectMapper.readValue(response.body(), MAP_TYPE);
            String text = extractCandidateText(payload);
            return (text == null || text.isBlank()) ? fallbackText : text.trim();
        } catch (Exception ex) {
            return fallbackText;
        }
    }

    private AiSearchResponse fallback(String normalized) {
        return new AiSearchResponse(normalized, normalized, splitKeywords(normalized), isUsable(), "fallback");
    }

    private boolean isUsable() {
        return geminiProperties.isEnabled()
                && geminiProperties.getApiKey() != null
                && !geminiProperties.getApiKey().isBlank();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static List<String> splitKeywords(String text) {
        LinkedHashSet<String> uniq = new LinkedHashSet<>();
        for (String token : text.toLowerCase().split("\\s+")) {
            String t = token.trim();
            if (!t.isBlank()) uniq.add(t);
        }
        return new ArrayList<>(uniq);
    }

    private static String extractCandidateText(Map<String, Object> payload) {
        Object candidatesRaw = payload.get("candidates");
        if (!(candidatesRaw instanceof List<?> candidates) || candidates.isEmpty()) return null;
        Object first = candidates.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) return null;
        Object contentRaw = firstMap.get("content");
        if (!(contentRaw instanceof Map<?, ?> contentMap)) return null;
        Object partsRaw = contentMap.get("parts");
        if (!(partsRaw instanceof List<?> parts) || parts.isEmpty()) return null;
        Object part0 = parts.get(0);
        if (!(part0 instanceof Map<?, ?> partMap)) return null;
        Object text = partMap.get("text");
        return text == null ? null : String.valueOf(text);
    }

    private Map<String, Object> parseJsonObject(String text) {
        String raw = text.trim();
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            raw = raw.substring(start, end + 1);
        }
        try {
            return objectMapper.readValue(raw, MAP_TYPE);
        } catch (Exception ex) {
            return Map.of("normalizedQuery", text, "keywords", splitKeywords(text));
        }
    }

    private static List<String> extractKeywords(Object rawKeywords, String fallbackQuery) {
        if (rawKeywords instanceof List<?> list) {
            LinkedHashSet<String> uniq = new LinkedHashSet<>();
            for (Object item : list) {
                if (item == null) continue;
                String token = String.valueOf(item).trim().toLowerCase();
                if (!token.isBlank()) uniq.add(token);
            }
            if (!uniq.isEmpty()) return new ArrayList<>(uniq);
        }
        return splitKeywords(fallbackQuery);
    }
}
