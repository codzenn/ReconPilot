package com.reconcileguard.security;

public final class AuthPrincipal {
    private final String userId;
    private final String email;

    public AuthPrincipal(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return userId;
    }
}

