package com.hbcstore.hbcstore_api.user.dto;

import com.hbcstore.hbcstore_api.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @Size(min = 6, max = 100) String password,
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 20) String phoneNumber,
        String address,
        User.Role role,
        User.UserStatus status
) {
}
