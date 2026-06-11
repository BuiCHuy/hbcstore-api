package com.hbcstore.hbcstore_api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 120) String token,
        @NotBlank @Size(min = 6, max = 255) String password
) {
}
