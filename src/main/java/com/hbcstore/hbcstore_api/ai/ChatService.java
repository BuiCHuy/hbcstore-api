package com.hbcstore.hbcstore_api.ai;

import com.hbcstore.hbcstore_api.ai.dto.AiSearchResponse;
import com.hbcstore.hbcstore_api.ai.dto.ChatResponse;
import com.hbcstore.hbcstore_api.catalog.Brand;
import com.hbcstore.hbcstore_api.catalog.BrandRepository;
import com.hbcstore.hbcstore_api.catalog.Category;
import com.hbcstore.hbcstore_api.catalog.CategoryRepository;
import com.hbcstore.hbcstore_api.catalog.Product;
import com.hbcstore.hbcstore_api.catalog.ProductService;
import com.hbcstore.hbcstore_api.catalog.Subcategory;
import com.hbcstore.hbcstore_api.catalog.SubcategoryRepository;
import com.hbcstore.hbcstore_api.catalog.dto.ProductResponse;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChatService {
    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "(duoi|dưới|tren|trên|tu|từ|den|đến|toi|tới|khoang|khoảng|tam|tầm)?\\s*"
                    + "(\\d+[\\d\\.,]*)\\s*"
                    + "(tr|triệu|trieu|m|k|nghìn|ngàn|ngan|cu|củ)?\\s*"
                    + "(ruoi|rưỡi)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "(tu|từ)\\s*(\\d+[\\d\\.,]*)\\s*(tr|triệu|trieu|m|k|nghìn|ngàn|ngan|cu|củ)?\\s*(ruoi|rưỡi)?"
                    + "\\s*(den|đến|toi|tới)\\s*"
                    + "(\\d+[\\d\\.,]*)\\s*(tr|triệu|trieu|m|k|nghìn|ngàn|ngan|cu|củ)?\\s*(ruoi|rưỡi)?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final int MAX_HISTORY_SESSIONS = 300;
    private static final Map<String, String> CATEGORY_CANONICAL_MAP = new LinkedHashMap<>();
    private static final Map<String, List<String>> THEME_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, String> GUIDE_RESPONSES = new LinkedHashMap<>();

    static {
        CATEGORY_CANONICAL_MAP.put("mo hinh nhan vat", "mô hình nhân vật");
        CATEGORY_CANONICAL_MAP.put("nhan vat", "mô hình nhân vật");
        CATEGORY_CANONICAL_MAP.put("figure", "mô hình nhân vật");

        CATEGORY_CANONICAL_MAP.put("bo lap rap", "bộ lắp ráp");
        CATEGORY_CANONICAL_MAP.put("lap rap", "bộ lắp ráp");
        CATEGORY_CANONICAL_MAP.put("model kit", "bộ lắp ráp");

        CATEGORY_CANONICAL_MAP.put("hop ngau nhien", "hộp ngẫu nhiên");
        CATEGORY_CANONICAL_MAP.put("blind box", "hộp ngẫu nhiên");
        CATEGORY_CANONICAL_MAP.put("ngau nhien", "hộp ngẫu nhiên");

        CATEGORY_CANONICAL_MAP.put("lego", "lego");

        CATEGORY_CANONICAL_MAP.put("dung cu", "dụng cụ");
        CATEGORY_CANONICAL_MAP.put("tool", "dụng cụ");

        THEME_KEYWORDS.put("gundam", List.of(
                "gundam", "gunpla", "hg", "rg", "mg", "pg", "sd", "real grade", "master grade", "perfect grade"
        ));
        THEME_KEYWORDS.put("anime", List.of(
                "anime", "manga", "naruto", "one piece", "dragon ball", "demon slayer",
                "jujutsu", "attack on titan", "pokemon", "sailor moon", "bleach",
                "luffy", "zoro", "nami", "sanji", "ace", "itachi", "sasuke", "kakashi",
                "tanjiro", "nezuko", "gojo", "sukuna", "eren", "mikasa", "levi"
        ));
        THEME_KEYWORDS.put("marvel", List.of(
                "marvel", "avengers", "iron man", "spider-man", "spiderman", "captain america",
                "thor", "hulk", "doctor strange", "black panther", "deadpool", "wolverine", "venom",
                "tony stark", "peter parker", "steve rogers", "thanos", "groot", "rocket",
                "scarlet witch", "wanda", "vision", "loki", "ant-man"
        ));
        THEME_KEYWORDS.put("dc", List.of(
                "dc", "batman", "superman", "wonder woman", "flash", "joker", "aquaman", "green lantern"
        ));

        GUIDE_RESPONSES.put("dat hang", """
                Hướng dẫn đặt hàng nhanh:
                1) Đăng nhập tài khoản khách hàng.
                2) Vào trang sản phẩm, thêm vào giỏ hoặc bấm Mua ngay.
                3) Điền thông tin nhận hàng, chọn COD hoặc Chuyển khoản.
                4) Kiểm tra tổng tiền, mã giảm giá rồi xác nhận đặt hàng.
                5) Theo dõi trạng thái tại mục Đơn hàng của tôi.
                """);
        GUIDE_RESPONSES.put("coupon", """
                Cách dùng mã giảm giá:
                1) Thêm sản phẩm vào giỏ hàng.
                2) Tại giỏ hàng hoặc modal xác nhận đặt hàng, nhập mã giảm giá.
                3) Bấm áp dụng để hệ thống trừ tiền theo điều kiện mã.
                4) Nếu mã hợp lệ, tổng thanh toán sẽ cập nhật ngay.
                """);
        GUIDE_RESPONSES.put("thanh toan", """
                Hướng dẫn thanh toán:
                - COD: đặt hàng xong, thanh toán khi nhận hàng.
                - Chuyển khoản: đặt hàng, mở link thanh toán và hoàn tất trong thời gian cho phép.
                - Sau khi thanh toán thành công, trạng thái thanh toán sẽ cập nhật trong đơn hàng.
                """);
        GUIDE_RESPONSES.put("theo doi don", """
                Cách theo dõi đơn hàng:
                1) Đăng nhập và vào mục Đơn hàng của tôi.
                2) Chọn đơn cần xem để mở chi tiết.
                3) Theo dõi trạng thái: Chờ xử lý -> Đã xác nhận -> Đang giao -> Hoàn thành.
                4) Nếu đơn chưa giao và đang chờ xử lý, bạn có thể hủy theo chính sách hệ thống.
                """);
        GUIDE_RESPONSES.put("hoan tien", """
                Cách yêu cầu hoàn tiền:
                1) Mở chi tiết đơn hàng của bạn.
                2) Chọn yêu cầu hoàn tiền và nhập lý do rõ ràng.
                3) Đơn sẽ chuyển sang trạng thái chờ admin duyệt.
                4) Kết quả duyệt/từ chối sẽ hiển thị trong chi tiết đơn.
                """);
        GUIDE_RESPONSES.put("danh gia", """
                Cách đánh giá sản phẩm:
                1) Vào trang chi tiết sản phẩm đã mua.
                2) Chọn số sao và nhập nội dung đánh giá.
                3) Gửi đánh giá, hệ thống lưu và chờ trạng thái hiển thị theo quy định quản trị.
                4) Bạn có thể chỉnh sửa đánh giá của chính mình khi được hỗ trợ.
                """);
        GUIDE_RESPONSES.put("tai khoan", """
                Hướng dẫn tài khoản:
                - Đăng ký: tạo tài khoản bằng email/mật khẩu hoặc Google (nếu đã bật).
                - Đăng nhập: dùng thông tin đã đăng ký để truy cập giỏ hàng, đơn hàng và hồ sơ.
                - Hồ sơ cá nhân: cập nhật họ tên, số điện thoại, địa chỉ trong trang Profile.
                """);
    }

    private final GeminiSearchService geminiSearchService;
    private final ProductService productService;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final Map<String, SessionContext> sessionMemory = new ConcurrentHashMap<>();

    public ChatService(
            GeminiSearchService geminiSearchService,
            ProductService productService,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            SubcategoryRepository subcategoryRepository
    ) {
        this.geminiSearchService = geminiSearchService;
        this.productService = productService;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
    }

    public ChatResponse chat(String message, String sessionId) {
        String sessionKey = (sessionId == null || sessionId.isBlank()) ? "anonymous" : sessionId.trim();
        SessionContext previous = sessionMemory.get(sessionKey);
        String rawQuery = message == null ? "" : message.trim();
        String guideAnswer = detectGuideAnswer(rawQuery);
        if (guideAnswer != null) {
            return new ChatResponse(
                    guideAnswer,
                    rawQuery,
                    "guide",
                    List.of()
            );
        }
        SearchIntent baseIntent = mergeIntent(parseIntent(message, rawQuery), rawQuery, previous);

        List<ProductResponse> dbFirstCandidates = productService.getAll(rawQuery).stream()
                .filter(item -> item.status() == Product.ProductStatus.ACTIVE)
                .toList();
        List<ProductResponse> dbFirstScoped = applyFilters(dbFirstCandidates, baseIntent);

        boolean usedAiFallback = dbFirstScoped.isEmpty();
        AiSearchResponse interpreted;
        String interpretedQuery;
        SearchIntent intent;
        List<ProductResponse> productsForSuggestion;

        if (!usedAiFallback) {
            interpretedQuery = rawQuery;
            interpreted = new AiSearchResponse(rawQuery, interpretedQuery, List.of(), false, "db-first");
            intent = baseIntent;
            productsForSuggestion = rankForSuggestion(dbFirstScoped, interpretedQuery, intent).stream()
                    .limit(8)
                    .toList();
        } else {
            interpreted = geminiSearchService.interpret(rawQuery);
            interpretedQuery = interpreted.normalizedQuery() == null || interpreted.normalizedQuery().isBlank()
                    ? rawQuery
                    : interpreted.normalizedQuery();
            intent = mergeIntent(parseIntent(message, interpretedQuery), interpretedQuery, previous);

            List<ProductResponse> productsForAi = productService.getAll("").stream()
                    .filter(item -> item.status() == Product.ProductStatus.ACTIVE)
                    .limit(200)
                    .toList();
            List<ProductResponse> scopedForSuggestion = applyFilters(productsForAi, intent);
            productsForSuggestion = rankForSuggestion(scopedForSuggestion, interpretedQuery, intent).stream()
                    .limit(8)
                    .toList();
        }

        List<ChatResponse.ChatProduct> suggestedProducts = productsForSuggestion.stream()
                .map(item -> new ChatResponse.ChatProduct(
                        item.id(),
                        item.name(),
                        item.thumbnailUrl(),
                        item.brandName(),
                        formatCatalogLabel(item),
                        item.price() == null ? 0 : item.price().doubleValue()
                ))
                .toList();

        String fallbackAnswer = buildFallbackAnswer(interpretedQuery, suggestedProducts);
        String aiPrompt = buildPrompt(message, interpretedQuery, productsForSuggestion, intent);
        String answer = geminiSearchService.generateAnswer(aiPrompt, fallbackAnswer);
        rememberSession(sessionKey, intent);

        log.info("chat session={} source={} mode={} minPrice={} maxPrice={} brand='{}' category='{}' results={}",
                sessionKey,
                interpreted.source(),
                usedAiFallback ? "ai-fallback" : "db-first",
                intent.minPrice(),
                intent.maxPrice(),
                intent.brand(),
                intent.category(),
                intent.subcategory(),
                suggestedProducts.size()
        );

        return new ChatResponse(
                answer,
                interpretedQuery,
                interpreted.source(),
                suggestedProducts
        );
    }

    private SearchIntent parseIntent(String message, String interpretedQuery) {
        String raw = normalize(message + " " + interpretedQuery);
        String keyword = interpretedQuery == null ? "" : interpretedQuery.trim();

        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        Matcher rangeMatcher = RANGE_PATTERN.matcher(raw);
        if (rangeMatcher.find()) {
            BigDecimal from = parseMoney(rangeMatcher.group(2), rangeMatcher.group(3), rangeMatcher.group(4));
            BigDecimal to = parseMoney(rangeMatcher.group(6), rangeMatcher.group(7), rangeMatcher.group(8));
            if (from != null && to != null) {
                minPrice = from.min(to);
                maxPrice = from.max(to);
            }
        }

        Matcher matcher = PRICE_PATTERN.matcher(raw);
        while (matcher.find()) {
            String operator = normalize(matcher.group(1));
            BigDecimal value = parseMoney(matcher.group(2), matcher.group(3), matcher.group(4));
            if (value == null) continue;
            if (operator.contains("duoi") || operator.contains("dưới") || operator.contains("den")
                    || operator.contains("đến") || operator.contains("toi") || operator.contains("tới")) {
                maxPrice = maxPrice == null ? value : maxPrice.min(value);
            } else if (operator.contains("tren") || operator.contains("trên")) {
                minPrice = minPrice == null ? value : minPrice.max(value);
            } else if (operator.contains("tu") || operator.contains("từ")) {
                minPrice = minPrice == null ? value : minPrice.max(value);
            } else if (operator.contains("khoang") || operator.contains("khoảng") || operator.contains("tam") || operator.contains("tầm")) {
                BigDecimal delta = value.multiply(BigDecimal.valueOf(0.15d));
                minPrice = value.subtract(delta);
                maxPrice = value.add(delta);
            }
        }

        String brand = detectBrand(raw);
        String category = detectCategory(raw);
        String subcategory = detectSubcategory(raw);
        if (subcategory != null && category == null) {
            category = detectCategoryFromSubcategory(subcategory);
        }
        String theme = detectTheme(raw);
        boolean inStockOnly = raw.contains("còn hàng") || raw.contains("co hang");
        boolean cheap = raw.contains("giá rẻ") || raw.contains("gia re") || raw.contains("bình dân");
        boolean premium = raw.contains("cao cấp") || raw.contains("premium") || raw.contains("đắt");
        String sort = cheap ? "priceAsc" : (premium ? "priceDesc" : null);

        return new SearchIntent(keyword, brand, category, subcategory, minPrice, maxPrice, inStockOnly, sort, theme);
    }

    private SearchIntent mergeIntent(SearchIntent current, String currentQuery, SessionContext previous) {
        if (previous == null) return current;
        if (!shouldInheritContext(current, currentQuery, previous.intent)) {
            return current;
        }
        String keyword = choose(current.keyword(), previous.intent.keyword());
        String brand = choose(current.brand(), previous.intent.brand());
        String category = choose(current.category(), previous.intent.category());
        String subcategory = choose(current.subcategory(), previous.intent.subcategory());
        BigDecimal minPrice = current.minPrice() != null ? current.minPrice() : previous.intent.minPrice();
        BigDecimal maxPrice = current.maxPrice() != null ? current.maxPrice() : previous.intent.maxPrice();
        boolean inStock = current.inStockOnly() || previous.intent.inStockOnly();
        String sort = current.sort() != null ? current.sort() : previous.intent.sort();
        String theme = current.theme() != null ? current.theme() : previous.intent.theme();
        return new SearchIntent(keyword, brand, category, subcategory, minPrice, maxPrice, inStock, sort, theme);
    }

    private boolean shouldInheritContext(SearchIntent current, String currentQuery, SearchIntent previous) {
        if (previous == null) {
            return false;
        }
        if (hasExplicitCatalogIntent(current)) {
            return false;
        }

        String normalizedQuery = normalize(currentQuery);
        if (normalizedQuery.isBlank()) {
            return true;
        }

        if (containsFollowUpHint(normalizedQuery)) {
            return true;
        }

        boolean hasOnlyPriceChange = current.minPrice() != null || current.maxPrice() != null || current.sort() != null;
        boolean hasStockChangeOnly = current.inStockOnly();
        return hasOnlyPriceChange || hasStockChangeOnly;
    }

    private boolean hasExplicitCatalogIntent(SearchIntent intent) {
        return intent.brand() != null
                || intent.category() != null
                || intent.subcategory() != null
                || intent.theme() != null;
    }

    private boolean containsFollowUpHint(String normalizedQuery) {
        return normalizedQuery.contains("con ")
                || normalizedQuery.contains("còn ")
                || normalizedQuery.contains("them ")
                || normalizedQuery.contains("thêm ")
                || normalizedQuery.contains("khac")
                || normalizedQuery.contains("khác")
                || normalizedQuery.contains("loai do")
                || normalizedQuery.contains("loại đó")
                || normalizedQuery.contains("mau do")
                || normalizedQuery.contains("mẫu đó")
                || normalizedQuery.contains("tam nay")
                || normalizedQuery.contains("tầm này")
                || normalizedQuery.contains("gia nay")
                || normalizedQuery.contains("giá này")
                || normalizedQuery.contains("re hon")
                || normalizedQuery.contains("rẻ hơn")
                || normalizedQuery.contains("dat hon")
                || normalizedQuery.contains("đắt hơn")
                || normalizedQuery.contains("the nao")
                || normalizedQuery.contains("thế nào");
    }

    private List<ProductResponse> applyFilters(List<ProductResponse> products, SearchIntent intent) {
        List<ProductResponse> filtered = products.stream()
                .filter(p -> intent.brand() == null || normalize(p.brandName()).contains(intent.brand()))
                .filter(p -> intent.category() == null || normalize(p.categoryName()).contains(intent.category()))
                .filter(p -> intent.subcategory() == null || normalize(p.subcategoryName()).contains(intent.subcategory()))
                .filter(p -> intent.minPrice() == null || p.price().compareTo(intent.minPrice()) >= 0)
                .filter(p -> intent.maxPrice() == null || p.price().compareTo(intent.maxPrice()) <= 0)
                .filter(p -> !intent.inStockOnly() || (p.stockQuantity() != null && p.stockQuantity() > 0))
                .filter(p -> matchesTheme(p, intent.theme()))
                .sorted(sortComparator(intent.sort()))
                .toList();
        // Theme filtering can be too strict for short catalogs; relax if no results.
        if (!filtered.isEmpty() || intent.theme() == null) {
            return filtered;
        }
        return products.stream()
                .filter(p -> intent.brand() == null || normalize(p.brandName()).contains(intent.brand()))
                .filter(p -> intent.category() == null || normalize(p.categoryName()).contains(intent.category()))
                .filter(p -> intent.subcategory() == null || normalize(p.subcategoryName()).contains(intent.subcategory()))
                .filter(p -> intent.minPrice() == null || p.price().compareTo(intent.minPrice()) >= 0)
                .filter(p -> intent.maxPrice() == null || p.price().compareTo(intent.maxPrice()) <= 0)
                .filter(p -> !intent.inStockOnly() || (p.stockQuantity() != null && p.stockQuantity() > 0))
                .sorted(sortComparator(intent.sort()))
                .toList();
    }

    private Comparator<ProductResponse> sortComparator(String sort) {
        if ("priceDesc".equals(sort)) {
            return Comparator.comparing(ProductResponse::price).reversed();
        }
        return Comparator.comparing(ProductResponse::price);
    }

    private List<ProductResponse> rankForSuggestion(
            List<ProductResponse> products,
            String interpretedQuery,
            SearchIntent intent
    ) {
        List<ProductResponse> scoped = products;
        if (intent.theme() != null && !intent.theme().isBlank()) {
            scoped = products.stream()
                    .filter(p -> matchesTheme(p, intent.theme()))
                    .toList();
        }

        String normalizedQuery = normalize(interpretedQuery);
        Set<String> queryTokens = new HashSet<>(List.of(normalizedQuery.split("\\s+")));
        queryTokens.removeIf(String::isBlank);

        return scoped.stream()
                .sorted((a, b) -> Integer.compare(
                        scoreProduct(b, queryTokens, intent),
                        scoreProduct(a, queryTokens, intent)
                ))
                .toList();
    }

    private int scoreProduct(ProductResponse product, Set<String> queryTokens, SearchIntent intent) {
        int score = 0;
        String haystack = buildProductHaystack(product);

        for (String token : queryTokens) {
            if (token.length() >= 2 && haystack.contains(token)) {
                score += 3;
            }
        }
        if (intent.theme() != null && matchesTheme(product, intent.theme())) {
            score += 4;
        }
        if (intent.brand() != null && normalize(product.brandName()).contains(intent.brand())) {
            score += 3;
        }
        if (intent.category() != null && normalize(product.categoryName()).contains(intent.category())) {
            score += 3;
        }
        if (intent.subcategory() != null && normalize(product.subcategoryName()).contains(intent.subcategory())) {
            score += 5;
        }
        if (intent.minPrice() != null && product.price().compareTo(intent.minPrice()) >= 0) {
            score += 1;
        }
        if (intent.maxPrice() != null && product.price().compareTo(intent.maxPrice()) <= 0) {
            score += 1;
        }
        return score;
    }

    private void rememberSession(String sessionKey, SearchIntent intent) {
        if (sessionMemory.size() > MAX_HISTORY_SESSIONS) {
            sessionMemory.clear();
        }
        sessionMemory.put(sessionKey, new SessionContext(intent));
    }

    private String detectBrand(String raw) {
        List<Brand> brands = brandRepository.findAll();
        for (Brand brand : brands) {
            String name = normalize(brand.getName());
            if (!name.isBlank() && raw.contains(name)) {
                return name;
            }
        }
        return null;
    }

    private String detectCategory(String raw) {
        String normalizedRaw = normalize(raw);
        for (Map.Entry<String, String> entry : CATEGORY_CANONICAL_MAP.entrySet()) {
            if (normalizedRaw.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        List<Category> categories = categoryRepository.findAll();
        for (Category category : categories) {
            String name = normalize(category.getName());
            if (!name.isBlank() && raw.contains(name)) {
                return name;
            }
        }
        return null;
    }

    private String detectSubcategory(String raw) {
        List<Subcategory> subcategories = subcategoryRepository.findAll();
        String bestMatch = null;
        int bestLength = -1;
        for (Subcategory subcategory : subcategories) {
            if (subcategory.getStatus() != Subcategory.Status.ACTIVE) {
                continue;
            }
            String name = normalize(subcategory.getName());
            if (!name.isBlank() && raw.contains(name) && name.length() > bestLength) {
                bestMatch = name;
                bestLength = name.length();
            }
        }
        return bestMatch;
    }

    private String detectCategoryFromSubcategory(String normalizedSubcategory) {
        List<Subcategory> subcategories = subcategoryRepository.findAll();
        for (Subcategory subcategory : subcategories) {
            if (subcategory.getStatus() != Subcategory.Status.ACTIVE) {
                continue;
            }
            String name = normalize(subcategory.getName());
            if (name.equals(normalizedSubcategory) && subcategory.getCategory() != null) {
                return normalize(subcategory.getCategory().getName());
            }
        }
        return null;
    }

    private String detectTheme(String raw) {
        String normalizedRaw = normalize(raw);
        // Ưu tiên gundam để tránh bị nuốt vào anime.
        List<String> gundamTokens = THEME_KEYWORDS.get("gundam");
        if (gundamTokens != null) {
            for (String token : gundamTokens) {
                if (normalizedRaw.contains(normalize(token))) {
                    return "gundam";
                }
            }
        }
        for (Map.Entry<String, List<String>> entry : THEME_KEYWORDS.entrySet()) {
            if ("gundam".equals(entry.getKey())) {
                continue;
            }
            for (String token : entry.getValue()) {
                if (normalizedRaw.contains(normalize(token))) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private boolean matchesTheme(ProductResponse product, String theme) {
        if (theme == null) return true;
        List<String> tokens = THEME_KEYWORDS.get(theme);
        if (tokens == null || tokens.isEmpty()) return true;
        String haystack = buildProductHaystack(product);
        for (String token : tokens) {
            if (haystack.contains(normalize(token))) {
                return true;
            }
        }
        return false;
    }

    private String buildProductHaystack(ProductResponse product) {
        String attributeText = product.attributes() == null ? "" : product.attributes().stream()
                .map(attribute -> (attribute.name() == null ? "" : attribute.name()) + " "
                        + (attribute.value() == null ? "" : attribute.value()))
                .reduce("", (left, right) -> left + " " + right);
        return normalize(
                (product.name() == null ? "" : product.name()) + " "
                        + (product.description() == null ? "" : product.description()) + " "
                        + (product.categoryName() == null ? "" : product.categoryName()) + " "
                        + (product.subcategoryName() == null ? "" : product.subcategoryName()) + " "
                        + (product.brandName() == null ? "" : product.brandName()) + " "
                        + attributeText
        );
    }

    private static BigDecimal parseMoney(String numberPart, String unitPart, String halfPart) {
        if (numberPart == null || numberPart.isBlank()) return null;
        String clean = numberPart.trim();
        boolean hasDot = clean.contains(".");
        boolean hasComma = clean.contains(",");
        if (hasDot && hasComma) {
            clean = clean.replace(".", "").replace(",", ".");
        } else if (hasComma) {
            clean = clean.replace(",", ".");
        } else if (hasDot) {
            // 1.200.000 -> bỏ dấu ngăn cách nghìn; 1.2 -> giữ số thập phân
            int firstDot = clean.indexOf('.');
            boolean likelyThousandGrouping = firstDot >= 0
                    && clean.substring(firstDot + 1).length() == 3
                    && clean.chars().filter(ch -> ch == '.').count() >= 1;
            if (likelyThousandGrouping) {
                clean = clean.replace(".", "");
            }
        }
        double value;
        try {
            value = Double.parseDouble(clean);
        } catch (Exception ex) {
            return null;
        }
        if (halfPart != null && !halfPart.isBlank()) {
            value += 0.5d;
        }
        String unit = unitPart == null ? "" : unitPart.toLowerCase(Locale.ROOT);
        if (unit.equals("tr") || unit.contains("triệu") || unit.contains("trieu") || unit.equals("m") || unit.contains("cu") || unit.contains("củ")) {
            value *= 1_000_000d;
        } else if (unit.equals("k") || unit.contains("nghìn") || unit.contains("ngàn") || unit.contains("ngan")) {
            value *= 1_000d;
        }
        return BigDecimal.valueOf(Math.round(value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private static String choose(String current, String previous) {
        String c = current == null ? "" : current.trim();
        if (!c.isBlank()) return c;
        return previous == null ? "" : previous;
    }

    private static String foldVietnamese(String value) {
        String n = normalize(value);
        String noMarks = Normalizer.normalize(n, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd');
        return noMarks.replaceAll("\\s+", " ").trim();
    }

    private String detectGuideAnswer(String rawQuery) {
        String normalized = normalize(rawQuery);
        String folded = foldVietnamese(normalized);
        if (folded.contains("huong dan")
                || folded.contains("cach ")
                || folded.contains("lam sao")
                || folded.contains("how to")
                || folded.contains("tro giup")
                || folded.contains("help")) {
            if (folded.contains("dat hang") || folded.contains("mua")) return GUIDE_RESPONSES.get("dat hang");
            if (folded.contains("coupon") || folded.contains("voucher") || folded.contains("ma giam")) return GUIDE_RESPONSES.get("coupon");
            if (folded.contains("thanh toan") || folded.contains("chuyen khoan") || folded.contains("cod")) return GUIDE_RESPONSES.get("thanh toan");
            if (folded.contains("theo doi") || folded.contains("don hang")) return GUIDE_RESPONSES.get("theo doi don");
            if (folded.contains("hoan tien") || folded.contains("refund")) return GUIDE_RESPONSES.get("hoan tien");
            if (folded.contains("danh gia") || folded.contains("review")) return GUIDE_RESPONSES.get("danh gia");
            if (folded.contains("tai khoan") || folded.contains("dang nhap") || folded.contains("dang ky")) return GUIDE_RESPONSES.get("tai khoan");
            return """
                    Mình có thể hướng dẫn các mục chính:
                    - Cách đặt hàng
                    - Cách áp mã giảm giá
                    - Cách thanh toán COD/chuyển khoản
                    - Cách theo dõi đơn hàng
                    - Cách yêu cầu hoàn tiền
                    - Cách đánh giá sản phẩm
                    Bạn hãy chọn một mục để mình hướng dẫn chi tiết.
                    """;
        }
        if (normalized.isBlank()) return null;
        boolean isGuideIntent = normalized.contains("huong dan")
                || normalized.contains("hướng dẫn")
                || normalized.contains("cach ")
                || normalized.contains("làm sao")
                || normalized.contains("lam sao")
                || normalized.contains("how to")
                || normalized.contains("tro giup")
                || normalized.contains("trợ giúp");
        if (!isGuideIntent) return null;

        if (normalized.contains("dat hang") || normalized.contains("đặt hàng") || normalized.contains("mua")) {
            return GUIDE_RESPONSES.get("dat hang");
        }
        if (normalized.contains("coupon") || normalized.contains("voucher") || normalized.contains("ma giam")
                || normalized.contains("mã giảm")) {
            return GUIDE_RESPONSES.get("coupon");
        }
        if (normalized.contains("thanh toan") || normalized.contains("thanh toán") || normalized.contains("chuyen khoan")
                || normalized.contains("chuyển khoản") || normalized.contains("cod")) {
            return GUIDE_RESPONSES.get("thanh toan");
        }
        if (normalized.contains("theo doi") || normalized.contains("theo dõi") || normalized.contains("don hang")
                || normalized.contains("đơn hàng")) {
            return GUIDE_RESPONSES.get("theo doi don");
        }
        if (normalized.contains("hoan tien") || normalized.contains("hoàn tiền") || normalized.contains("refund")) {
            return GUIDE_RESPONSES.get("hoan tien");
        }
        if (normalized.contains("danh gia") || normalized.contains("đánh giá") || normalized.contains("review")) {
            return GUIDE_RESPONSES.get("danh gia");
        }
        if (normalized.contains("tai khoan") || normalized.contains("tài khoản") || normalized.contains("dang nhap")
                || normalized.contains("đăng nhập") || normalized.contains("dang ky") || normalized.contains("đăng ký")) {
            return GUIDE_RESPONSES.get("tai khoan");
        }
        return """
                Mình có thể hướng dẫn các mục chính:
                - Cách đặt hàng
                - Cách áp mã giảm giá
                - Cách thanh toán COD/chuyển khoản
                - Cách theo dõi đơn hàng
                - Cách yêu cầu hoàn tiền
                - Cách đánh giá sản phẩm
                Bạn hãy chọn một mục để mình hướng dẫn chi tiết.
                """;
    }

    private static String buildFallbackAnswer(String interpretedQuery, List<ChatResponse.ChatProduct> products) {
        if (products.isEmpty()) {
            return "Mình chưa tìm thấy sản phẩm phù hợp với \"" + interpretedQuery + "\". Bạn thử đổi từ khóa hoặc nêu rõ hãng/danh mục nhé.";
        }
        String first = products.get(0).name();
        return "Mình đã tìm thấy " + products.size() + " sản phẩm phù hợp. Gợi ý nổi bật: " + first + ". Bạn có thể xem thêm danh sách bên dưới.";
    }

    private static String buildPrompt(
            String userMessage,
            String interpretedQuery,
            List<ProductResponse> products,
            SearchIntent intent
    ) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < products.size(); i++) {
            ProductResponse p = products.get(i);
            String attributes = p.attributes() == null ? "" : p.attributes().stream()
                    .map(attribute -> "%s: %s".formatted(attribute.name(), attribute.value()))
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            context.append(i + 1)
                    .append(". ")
                    .append(p.name())
                    .append(" | ")
                    .append(p.brandName())
                    .append(" | ")
                    .append(p.subcategoryName() == null || p.subcategoryName().isBlank()
                            ? p.categoryName()
                            : p.categoryName() + " / " + p.subcategoryName())
                    .append(" | ")
                    .append(p.price() == null ? 0L : p.price().longValue())
                    .append(" VND");
            if (!attributes.isBlank()) {
                context.append(" | thuộc tính: ").append(attributes);
            }
            context.append("\n");
        }

        return """
                Bạn là trợ lý mua sắm cho cửa hàng mô hình HBC.
                Trả lời bằng tiếng Việt, ngắn gọn 2-4 câu, không bịa dữ liệu ngoài context.
                Nếu không có sản phẩm, hãy đề xuất cách tìm khác.

                Câu hỏi người dùng: %s
                Query đã chuẩn hóa: %s
                Bộ lọc:
                - brand: %s
                - category: %s
                - subcategory: %s
                - minPrice: %s
                - maxPrice: %s
                - inStockOnly: %s
                - theme: %s
                Danh mục hợp lệ chỉ gồm:
                - mô hình nhân vật
                - bộ lắp ráp
                - hộp ngẫu nhiên
                - lego
                - dụng cụ
                Nếu có danh mục con thì ưu tiên bám sát danh mục con khi tư vấn.
                Danh sách sản phẩm tìm được:
                %s
                """.formatted(
                userMessage,
                interpretedQuery,
                intent.brand(),
                intent.category(),
                intent.subcategory(),
                intent.minPrice(),
                intent.maxPrice(),
                intent.inStockOnly(),
                intent.theme(),
                context
        );
    }

    private String formatCatalogLabel(ProductResponse product) {
        if (product.subcategoryName() == null || product.subcategoryName().isBlank()) {
            return product.categoryName();
        }
        return product.categoryName() + " / " + product.subcategoryName();
    }

    private record SearchIntent(
            String keyword,
            String brand,
            String category,
            String subcategory,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean inStockOnly,
            String sort,
            String theme
    ) {
    }

    private record SessionContext(SearchIntent intent) {
    }
}
