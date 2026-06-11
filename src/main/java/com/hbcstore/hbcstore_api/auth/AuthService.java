package com.hbcstore.hbcstore_api.auth;

import com.hbcstore.hbcstore_api.auth.dto.AuthResponse;
import com.hbcstore.hbcstore_api.auth.dto.GoogleLoginRequest;
import com.hbcstore.hbcstore_api.auth.dto.LoginRequest;
import com.hbcstore.hbcstore_api.auth.dto.RegisterRequest;
import com.hbcstore.hbcstore_api.auth.dto.RegisterResponse;
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
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            GoogleTokenVerifier googleTokenVerifier,
            EmailVerificationService emailVerificationService,
            PasswordResetService passwordResetService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null) {
            if (existingUser.getProvider() == User.AuthProvider.LOCAL
                    && existingUser.getStatus() == User.UserStatus.INACTIVE) {
                emailVerificationService.issueAndSend(existingUser);
                return new RegisterResponse(
                        "Email này đã đăng ký nhưng chưa xác thực. Chúng tôi đã gửi lại link mới (hiệu lực 30 phút).",
                        true
                );
            }
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setPhoneNumber(blankToNull(request.phoneNumber()));
        user.setAddress(blankToNull(request.address()));
        user.setRole(User.Role.CUSTOMER);
        user.setStatus(User.UserStatus.INACTIVE);
        user.setProvider(User.AuthProvider.LOCAL);
        user.setProviderId(null);

        User savedUser = userRepository.save(user);
        emailVerificationService.issueAndSend(savedUser);
        return new RegisterResponse(
                "Đăng ký thành công. Vui lòng kiểm tra email để xác thực trong 30 phút, quá hạn tài khoản sẽ bị xóa.",
                true
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng");
        }

        if (user.getStatus() == User.UserStatus.BANNED) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa");
        }
        if (user.getStatus() == User.UserStatus.INACTIVE && user.getProvider() == User.AuthProvider.LOCAL) {
            throw new IllegalArgumentException("Vui lòng xác thực email trước khi đăng nhập");
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
            throw new IllegalArgumentException("Tài khoản đã bị khóa");
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

    public void verifyEmail(String token) {
        emailVerificationService.verifyToken(token);
    }

    public void resendVerification(String email) {
        emailVerificationService.resend(email);
    }

    public String forgotPassword(String email) {
        return passwordResetService.requestReset(email);
    }

    public void resetPassword(String token, String password) {
        passwordResetService.resetPassword(token, password);
    }
}
