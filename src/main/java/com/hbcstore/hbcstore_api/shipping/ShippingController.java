package com.hbcstore.hbcstore_api.shipping;

import com.hbcstore.hbcstore_api.shipping.dto.ShippingQuoteRequest;
import com.hbcstore.hbcstore_api.shipping.dto.ShippingQuoteResponse;
import com.hbcstore.hbcstore_api.shipping.dto.ShippingSettingsRequest;
import com.hbcstore.hbcstore_api.shipping.dto.ShippingSettingsResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ShippingController {
    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @GetMapping("/shipping/settings")
    public ShippingSettingsResponse getSettings() {
        return shippingService.getSettings();
    }

    @PostMapping("/shipping/quote")
    public ShippingQuoteResponse quote(@Valid @RequestBody ShippingQuoteRequest request) {
        return shippingService.quote(request.subtotal(), request.province(), request.shippingAddress());
    }

    @PutMapping("/admin/shipping/settings")
    public ShippingSettingsResponse updateSettings(
            @Valid @RequestBody ShippingSettingsRequest request,
            Principal principal
    ) {
        return shippingService.updateSettings(request, principal == null ? "" : principal.getName());
    }
}
