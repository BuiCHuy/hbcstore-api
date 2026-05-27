package com.hbcstore.hbcstore_api.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    private boolean enabled = false;
    private String apiKey = "";
    private String model = "gemini-1.5-flash";
    private int timeoutMs = 10000;
}
