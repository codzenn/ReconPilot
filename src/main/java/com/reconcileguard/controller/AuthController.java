package com.reconcileguard.controller;

import com.reconcileguard.dto.AuthTokenResponse;
import com.reconcileguard.dto.LoginRequest;
import com.reconcileguard.dto.PasswordResetConfirmRequest;
import com.reconcileguard.dto.PasswordResetRequest;
import com.reconcileguard.dto.SignUpRequest;
import com.reconcileguard.dto.SignUpResponse;
import com.reconcileguard.dto.UserMeResponse;
import com.reconcileguard.security.AuthPrincipal;
import com.reconcileguard.security.SecurityProperties;
import com.reconcileguard.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final SecurityProperties securityProperties;

    public AuthController(AuthService authService, SecurityProperties securityProperties) {
        this.authService = authService;
        this.securityProperties = securityProperties;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignUpResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
    }

    @GetMapping("/verify")
    public Map<String, Object> verify(@RequestParam String token) {
        authService.verifyEmail(token);
        return Map.of("verified", true);
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserMeResponse me(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        return authService.me(principal.getUserId());
    }

    @PostMapping("/password-reset/request")
    public Map<String, Object> requestReset(@Valid @RequestBody PasswordResetRequest request) {
        String token = authService.requestPasswordReset(request);
        return Map.of(
                "requested", true,
                "resetToken", securityProperties.isExposeVerificationToken() ? token : null
        );
    }

    @PostMapping("/password-reset/confirm")
    public Map<String, Object> confirmReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return Map.of("reset", true);
    }
}
