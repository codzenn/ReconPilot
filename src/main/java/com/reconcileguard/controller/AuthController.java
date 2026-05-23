package com.reconcileguard.controller;

import com.reconcileguard.dto.AuthTokenResponse;
import com.reconcileguard.dto.LoginRequest;
import com.reconcileguard.dto.SignUpRequest;
import com.reconcileguard.dto.SignUpResponse;
import com.reconcileguard.dto.UserMeResponse;
import com.reconcileguard.security.AuthPrincipal;
import com.reconcileguard.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignUpResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return authService.signUp(request);
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

}
