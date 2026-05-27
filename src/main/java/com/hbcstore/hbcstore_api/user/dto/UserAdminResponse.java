package com.hbcstore.hbcstore_api.user.dto;

import com.hbcstore.hbcstore_api.user.User;
import java.time.LocalDateTime;

public record UserAdminResponse(
        Long id,
        String email,
        String fullName,
        String phoneNumber,
        String address,
        User.Role role,
        User.UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserAdminResponse from(User user) {
        return new UserAdminResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
