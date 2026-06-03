package com.hbcstore.hbcstore_api.auth;

import com.hbcstore.hbcstore_api.auth.dto.AuthResponse;
import com.hbcstore.hbcstore_api.auth.dto.GoogleLoginRequest;
import com.hbcstore.hbcstore_api.auth.dto.LoginRequest;
import com.hbcstore.hbcstore_api.auth.dto.RegisterRequest;
import com.hbcstore.hbcstore_api.auth.dto.RegisterResponse;
import com.hbcstore.hbcstore_api.auth.dto.ResendVerificationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/google")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request);
    }

    @GetMapping("/verify-email")
    public RegisterResponse verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return new RegisterResponse("Xác thực email thành công. Bạn có thể đăng nhập ngay bây giờ.", false);
    }

    @PostMapping("/resend-verification")
    public RegisterResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.email());
        return new RegisterResponse("Liên kết xác thực đã được gửi lại.", true);
    }
}
