package com.reconcileguard.service;

import com.reconcileguard.domain.AuditEvent;
import com.reconcileguard.domain.UserAccount;
import com.reconcileguard.domain.UserRole;
import com.reconcileguard.dto.AuthTokenResponse;
import com.reconcileguard.dto.LoginRequest;
import com.reconcileguard.dto.SignUpRequest;
import com.reconcileguard.dto.SignUpResponse;
import com.reconcileguard.dto.UserMeResponse;
import com.reconcileguard.repository.AuditEventRepository;
import com.reconcileguard.repository.UserAccountRepository;
import com.reconcileguard.security.JwtService;
import com.reconcileguard.security.SecurityProperties;
import com.reconcileguard.security.TokenGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final AuditEventRepository auditEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityProperties securityProperties;

    public AuthService(
            UserAccountRepository userAccountRepository,
            AuditEventRepository auditEventRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SecurityProperties securityProperties
    ) {
        this.userAccountRepository = userAccountRepository;
        this.auditEventRepository = auditEventRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
    }

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        String email = normalizeEmail(request.email());
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(CONFLICT, "Email already registered");
        }
        assertPasswordStrength(request.password());

        String userId = UUID.randomUUID().toString();
        UserAccount user = new UserAccount(
                userId,
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserRole.OPS
        );
        user.verifyEmail();
        userAccountRepository.save(user);

        auditEventRepository.save(new AuditEvent(
                email,
                "AUTH_SIGNUP",
                userId,
                "accountActivated=true"
        ));

        return new SignUpResponse(userId);
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    auditEventRepository.save(new AuditEvent(email, "AUTH_LOGIN_FAILURE", "AUTH", "reason=NOT_FOUND"));
                    return new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
                });


        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditEventRepository.save(new AuditEvent(user.getEmail(), "AUTH_LOGIN_FAILURE", user.getId(), "reason=BAD_PASSWORD"));
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
        }

        auditEventRepository.save(new AuditEvent(
                user.getEmail(),
                "AUTH_LOGIN",
                user.getId(),
                "role=" + user.getRole().name()
        ));

        String token = jwtService.createToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthTokenResponse(
                token,
                "Bearer",
                jwtService.expiresAt(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }

    @Transactional(readOnly = true)
    public UserMeResponse me(String userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid session"));
        return new UserMeResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }


    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static void assertPasswordStrength(String password) {
        if (password == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Password required");
        }
        String value = password.trim();
        boolean ok = value.length() >= 8
                && value.chars().anyMatch(Character::isLowerCase)
                && value.chars().anyMatch(Character::isUpperCase)
                && value.chars().anyMatch(Character::isDigit)
                && value.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!ok) {
            throw new ResponseStatusException(BAD_REQUEST, "Password must include upper, lower, digit, and symbol");
        }
    }
}

