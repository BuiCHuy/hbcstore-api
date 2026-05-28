package com.hbcstore.hbcstore_api.auth.dto;

public record RegisterResponse(
        String message,
        boolean requiresEmailVerification
) {
}
