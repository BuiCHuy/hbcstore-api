package com.hbcstore.hbcstore_api.auth.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}