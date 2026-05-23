package com.reconcileguard.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "reconcileguard.security")
public class SecurityProperties {
    private String jwtSecret = "change-this-secret-to-at-least-32-characters";
    private long tokenTtlMinutes = 480;
    private boolean exposeVerificationToken = true;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getTokenTtlMinutes() {
        return tokenTtlMinutes;
    }

    public void setTokenTtlMinutes(long tokenTtlMinutes) {
        this.tokenTtlMinutes = tokenTtlMinutes;
    }



    public boolean isExposeVerificationToken() {
        return exposeVerificationToken;
    }

    public void setExposeVerificationToken(boolean exposeVerificationToken) {
        this.exposeVerificationToken = exposeVerificationToken;
    }
}

