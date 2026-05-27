package com.hbcstore.hbcstore_api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank @Size(max = 1000) String message,
        @Size(max = 100) String sessionId
) {
}
