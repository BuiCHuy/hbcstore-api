package com.hbcstore.hbcstore_api.payment.payos;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.payos")
public class PayOSProperties {
    private boolean enabled = false;
    private boolean mockMode = true;
    private String apiBaseUrl = "https://api-merchant.payos.vn";
    private String clientId = "";
    private String apiKey = "";
    private String merchantId = "";
    private String checksumKey = "";
    private String currency = "VND";
    private String returnUrl = "http://localhost:5173/orders";
    private String webhookUrl = "http://localhost:8080/api/payments/payos/webhook";
}
