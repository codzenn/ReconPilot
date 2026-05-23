package com.reconcileguard.dto;

public record SignUpResponse(
        String userId,
        boolean emailVerificationRequired,
        String verificationToken
) {
}

