package com.hbcstore.hbcstore_api.shipping;

import com.hbcstore.hbcstore_api.shipping.dto.ShippingQuoteResponse;
import com.hbcstore.hbcstore_api.shipping.dto.ShippingSettingsRequest;
import com.hbcstore.hbcstore_api.shipping.dto.ShippingSettingsResponse;
import com.hbcstore.hbcstore_api.user.User;
import com.hbcstore.hbcstore_api.user.UserRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingService {
    private static final Long SETTINGS_ID = 1L;

    private static final List<String> NORTH_PROVINCES = List.of(
            "Hà Nội", "Hải Phòng", "Quảng Ninh", "Ninh Bình", "Hưng Yên", "Bắc Ninh",
            "Phú Thọ", "Tuyên Quang", "Lào Cai", "Thái Nguyên", "Lạng Sơn", "Cao Bằng",
            "Lai Châu", "Điện Biên", "Sơn La"
    );

    private static final List<String> CENTRAL_PROVINCES = List.of(
            "Thanh Hóa", "Nghệ An", "Hà Tĩnh", "Quảng Trị", "Huế", "Đà Nẵng",
            "Quảng Ngãi", "Gia Lai", "Khánh Hòa", "Lâm Đồng", "Đắk Lắk"
    );

    private static final List<String> SOUTH_PROVINCES = List.of(
            "TP Hồ Chí Minh", "Đồng Nai", "Tây Ninh", "Vĩnh Long", "Đồng Tháp",
            "An Giang", "Cần Thơ", "Cà Mau"
    );

    private static final Map<String, String> LEGACY_PROVINCE_ALIASES = Map.ofEntries(
            Map.entry("hồ chí minh", "TP Hồ Chí Minh"),
            Map.entry("ho chi minh", "TP Hồ Chí Minh"),
            Map.entry("bình dương", "TP Hồ Chí Minh"),
            Map.entry("binh duong", "TP Hồ Chí Minh"),
            Map.entry("bà rịa vũng tàu", "TP Hồ Chí Minh"),
            Map.entry("ba ria vung tau", "TP Hồ Chí Minh"),
            Map.entry("long an", "Tây Ninh"),
            Map.entry("tiền giang", "Đồng Tháp"),
            Map.entry("tien giang", "Đồng Tháp"),
            Map.entry("bến tre", "Vĩnh Long"),
            Map.entry("ben tre", "Vĩnh Long"),
            Map.entry("trà vinh", "Vĩnh Long"),
            Map.entry("tra vinh", "Vĩnh Long"),
            Map.entry("sóc trăng", "Cần Thơ"),
            Map.entry("soc trang", "Cần Thơ"),
            Map.entry("hậu giang", "Cần Thơ"),
            Map.entry("hau giang", "Cần Thơ"),
            Map.entry("bạc liêu", "Cà Mau"),
            Map.entry("bac lieu", "Cà Mau"),
            Map.entry("kiên giang", "An Giang"),
            Map.entry("kien giang", "An Giang"),
            Map.entry("hòa bình", "Phú Thọ"),
            Map.entry("hoa binh", "Phú Thọ"),
            Map.entry("vĩnh phúc", "Phú Thọ"),
            Map.entry("vinh phuc", "Phú Thọ"),
            Map.entry("hà nam", "Ninh Bình"),
            Map.entry("ha nam", "Ninh Bình"),
            Map.entry("nam định", "Ninh Bình"),
            Map.entry("nam dinh", "Ninh Bình"),
            Map.entry("thái bình", "Hưng Yên"),
            Map.entry("thai binh", "Hưng Yên"),
            Map.entry("hải dương", "Hải Phòng"),
            Map.entry("hai duong", "Hải Phòng"),
            Map.entry("bắc giang", "Bắc Ninh"),
            Map.entry("bac giang", "Bắc Ninh"),
            Map.entry("yên bái", "Lào Cai"),
            Map.entry("yen bai", "Lào Cai"),
            Map.entry("hà giang", "Tuyên Quang"),
            Map.entry("ha giang", "Tuyên Quang"),
            Map.entry("bắc kạn", "Thái Nguyên"),
            Map.entry("bac kan", "Thái Nguyên"),
            Map.entry("quảng bình", "Quảng Trị"),
            Map.entry("quang binh", "Quảng Trị"),
            Map.entry("thừa thiên huế", "Huế"),
            Map.entry("thua thien hue", "Huế"),
            Map.entry("quảng nam", "Đà Nẵng"),
            Map.entry("quang nam", "Đà Nẵng"),
            Map.entry("kon tum", "Quảng Ngãi"),
            Map.entry("bình định", "Gia Lai"),
            Map.entry("binh dinh", "Gia Lai"),
            Map.entry("phú yên", "Đắk Lắk"),
            Map.entry("phu yen", "Đắk Lắk"),
            Map.entry("ninh thuận", "Khánh Hòa"),
            Map.entry("ninh thuan", "Khánh Hòa"),
            Map.entry("bình thuận", "Lâm Đồng"),
            Map.entry("binh thuan", "Lâm Đồng"),
            Map.entry("đắk nông", "Lâm Đồng"),
            Map.entry("dak nong", "Lâm Đồng")
    );

    private final ShippingSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    public ShippingService(ShippingSettingsRepository settingsRepository, UserRepository userRepository) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
    }

    public ShippingSettingsResponse getSettings() {
        return ShippingSettingsResponse.from(getOrCreateSettings());
    }

    @Transactional
    public ShippingSettingsResponse updateSettings(ShippingSettingsRequest request, String adminEmail) {
        User admin = userRepository.findByEmailIgnoreCase(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản quản trị"));
        if (admin.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Chỉ quản trị viên mới có thể cập nhật cài đặt phí ship");
        }

        ShippingSettings settings = getOrCreateSettings();
        settings.setNorthFee(request.northFee());
        settings.setCentralFee(request.centralFee());
        settings.setSouthFee(request.southFee());
        settings.setFreeShippingThreshold(request.freeShippingThreshold());
        return ShippingSettingsResponse.from(settingsRepository.save(settings));
    }

    public ShippingQuoteResponse quote(BigDecimal subtotal, String province, String shippingAddress) {
        ShippingSettings settings = getOrCreateSettings();
        BigDecimal amount = subtotal == null ? BigDecimal.ZERO : subtotal;
        String resolvedProvince = resolveProvince(province, shippingAddress);
        String region = getRegion(resolvedProvince);
        boolean freeShipping = amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.compareTo(settings.getFreeShippingThreshold()) > 0;

        BigDecimal fee = BigDecimal.ZERO;
        if (!freeShipping) {
            fee = switch (region) {
                case "central" -> settings.getCentralFee();
                case "south" -> settings.getSouthFee();
                default -> settings.getNorthFee();
            };
        }

        return new ShippingQuoteResponse(fee, region, getRegionLabel(region), freeShipping);
    }

    public BigDecimal calculateFee(BigDecimal subtotal, String shippingAddress) {
        return quote(subtotal, null, shippingAddress).shippingFee();
    }

    private ShippingSettings getOrCreateSettings() {
        return settingsRepository.findById(SETTINGS_ID).orElseGet(() -> {
            ShippingSettings settings = new ShippingSettings();
            settings.setId(SETTINGS_ID);
            return settingsRepository.save(settings);
        });
    }

    private String resolveProvince(String province, String shippingAddress) {
        String direct = normalizeProvince(province);
        if (direct != null) return direct;

        String source = normalizeText(shippingAddress);
        if (source.isBlank()) return null;

        for (String candidate : allCurrentProvinces()) {
            if (source.contains(normalizeText(candidate))) {
                return candidate;
            }
        }
        for (Map.Entry<String, String> entry : LEGACY_PROVINCE_ALIASES.entrySet()) {
            if (source.contains(normalizeText(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String normalizeProvince(String province) {
        if (province == null || province.isBlank()) return null;
        String normalized = normalizeText(province);
        for (String candidate : allCurrentProvinces()) {
            if (normalized.equals(normalizeText(candidate))) {
                return candidate;
            }
        }
        return LEGACY_PROVINCE_ALIASES.get(normalized);
    }

    private String getRegion(String province) {
        if (CENTRAL_PROVINCES.contains(province)) return "central";
        if (SOUTH_PROVINCES.contains(province)) return "south";
        return "north";
    }

    private String getRegionLabel(String region) {
        return switch (region) {
            case "central" -> "Miền Trung";
            case "south" -> "Miền Nam";
            default -> "Miền Bắc";
        };
    }

    private List<String> allCurrentProvinces() {
        return java.util.stream.Stream.of(NORTH_PROVINCES, CENTRAL_PROVINCES, SOUTH_PROVINCES)
                .flatMap(List::stream)
                .toList();
    }

    private String normalizeText(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase();
        return normalized.replaceAll("[^a-z0-9]+", " ").trim();
    }
}
