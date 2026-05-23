package com.reconcileguard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "RG_PASSWORD_RESET")
public class PasswordResetToken {
    @Id
    @Column(name = "TOKEN", length = 90)
    private String token;

    @ManyToOne(optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserAccount user;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "USED", nullable = false)
    private boolean used;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(String token, UserAccount user, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
        this.used = false;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isUsable() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }

    public void markUsed() {
        this.used = true;
    }

    public String getToken() {
        return token;
    }

    public UserAccount getUser() {
        return user;
    }
}

