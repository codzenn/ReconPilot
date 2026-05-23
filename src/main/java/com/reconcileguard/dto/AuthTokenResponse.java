package com.reconcileguard.dto;

import java.time.Instant;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        String userId,
        String email,
        String fullName,
        String role
) {
}

