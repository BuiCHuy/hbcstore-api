package com.hbcstore.hbcstore_api.auth;

import com.hbcstore.hbcstore_api.auth.dto.AuthResponse;
import com.hbcstore.hbcstore_api.auth.dto.GoogleLoginRequest;
import com.hbcstore.hbcstore_api.auth.dto.LoginRequest;
import com.hbcstore.hbcstore_api.auth.dto.RegisterRequest;
import com.hbcstore.hbcstore_api.auth.dto.UserResponse;
import com.hbcstore.hbcstore_api.user.User;
import com.hbcstore.hbcstore_api.user.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleTokenVerifier googleTokenVerifier
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhoneNumber(blankToNull(request.phoneNumber()));
        user.setAddress(blankToNull(request.address()));
        user.setRole(User.Role.CUSTOMER);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setProvider(User.AuthProvider.LOCAL);
        user.setProviderId(null);

        User savedUser = userRepository.save(user);
        return toAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (user.getStatus() == User.UserStatus.BANNED) {
            throw new IllegalArgumentException("Account is banned");
        }

        return toAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleTokenVerifier.GoogleProfile profile = googleTokenVerifier.verify(request.idToken());
        User user = userRepository
                .findByProviderAndProviderId(User.AuthProvider.GOOGLE, profile.providerId())
                .orElseGet(() -> createGoogleUser(profile));

        if (user.getStatus() == User.UserStatus.BANNED) {
            throw new IllegalArgumentException("Account is banned");
        }
        return toAuthResponse(user);
    }

    private User createGoogleUser(GoogleTokenVerifier.GoogleProfile profile) {
        User user = userRepository.findByEmail(profile.email())
                .orElseGet(User::new);

        user.setEmail(profile.email());
        user.setFullName(profile.fullName().isBlank() ? profile.email() : profile.fullName());
        user.setRole(User.Role.CUSTOMER);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setProvider(User.AuthProvider.GOOGLE);
        user.setProviderId(profile.providerId());
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        }
        return userRepository.save(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
