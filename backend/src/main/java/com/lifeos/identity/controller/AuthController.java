package com.lifeos.identity.controller;

import com.lifeos.identity.dto.EmailRequest;
import com.lifeos.identity.dto.LoginRequest;
import com.lifeos.identity.dto.LoginResponse;
import com.lifeos.identity.dto.RefreshTokenRequest;
import com.lifeos.identity.dto.RegisterRequest;
import com.lifeos.identity.dto.ResetPasswordRequest;
import com.lifeos.identity.dto.TokenResponse;
import com.lifeos.identity.dto.VerifyEmailRequest;
import com.lifeos.identity.service.AuthService;
import com.lifeos.identity.service.DeviceInfo;
import com.lifeos.identity.service.LoginCommand;
import com.lifeos.identity.service.LoginResult;
import com.lifeos.identity.service.PasswordService;
import com.lifeos.identity.service.RegisterCommand;
import com.lifeos.identity.service.RegistrationService;
import com.lifeos.identity.service.TokenPair;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint xác thực công khai (API_CONVENTIONS §8.1). Controller chỉ kiểm cú pháp, lấy IP kết nối (đã qua
 * RemoteIpValve), gọi một Application Service và dựng DTO phản hồi. forgot/reset ở bước 1d.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private static final String BEARER = "Bearer";

    private final RegistrationService registrationService;
    private final AuthService authService;
    private final PasswordService passwordService;

    AuthController(RegistrationService registrationService, AuthService authService,
            PasswordService passwordService) {
        this.registrationService = registrationService;
        this.authService = authService;
        this.passwordService = passwordService;
    }

    /** 202, thân rỗng, giống hệt nhau dù email đã tồn tại hay chưa (SECURITY_RULES §3.3). */
    @ApiResponse(responseCode = "202", description = "Đã nhận (luôn như nhau dù email đã tồn tại)")
    @PostMapping("/register")
    ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        registrationService.register(new RegisterCommand(request.email(), request.password(), request.displayName(),
                request.timezone(), http.getRemoteAddr()));
        return ResponseEntity.accepted().build();
    }

    @ApiResponse(responseCode = "204", description = "Đã xác minh email")
    @PostMapping("/verify-email")
    ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request, HttpServletRequest http) {
        registrationService.verifyEmail(request.token(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    /** 202 luôn (trừ 429 theo IP). */
    @ApiResponse(responseCode = "202", description = "Đã nhận (luôn như nhau)")
    @PostMapping("/resend-verification")
    ResponseEntity<Void> resendVerification(@Valid @RequestBody EmailRequest request, HttpServletRequest http) {
        registrationService.resendVerification(request.email(), http.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        LoginRequest.Device device = request.device();
        LoginResult result = authService.login(new LoginCommand(request.email(), request.password(),
                request.deviceId(), new DeviceInfo(device.name().trim(), device.platform(),
                        device.appVersion()), http.getRemoteAddr()));
        TokenPair tokens = result.tokens();
        return new LoginResponse(BEARER, tokens.accessToken(), tokens.expiresIn(), tokens.refreshToken(),
                tokens.deviceId(), result.user());
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        TokenPair tokens = authService.refresh(request.refreshToken(), http.getRemoteAddr());
        return new TokenResponse(BEARER, tokens.accessToken(), tokens.expiresIn(), tokens.refreshToken(),
                tokens.deviceId());
    }

    /** 202 luôn (trừ 429 theo IP), giống hệt nhau dù email có tồn tại hay không (SECURITY_RULES §3.3). */
    @ApiResponse(responseCode = "202", description = "Đã nhận (luôn như nhau)")
    @PostMapping("/forgot-password")
    ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailRequest request, HttpServletRequest http) {
        passwordService.forgotPassword(request.email(), http.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    @ApiResponse(responseCode = "204", description = "Đã đặt lại mật khẩu")
    @PostMapping("/reset-password")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        passwordService.resetPassword(request.token(), request.newPassword(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    /** 204 luôn, kể cả token đã hết hạn/đã dùng/không tồn tại (API_CONVENTIONS §8.1, D75). */
    @ApiResponse(responseCode = "204", description = "Đã đăng xuất (luôn như nhau)")
    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest http) {
        authService.logout(request.refreshToken(), http.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
