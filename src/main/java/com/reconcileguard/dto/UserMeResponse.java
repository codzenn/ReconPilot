package com.reconcileguard.dto;

public record UserMeResponse(
        String userId,
        String email,
        String fullName,
        String role
) {
}

