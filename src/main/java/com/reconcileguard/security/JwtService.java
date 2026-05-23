package com.reconcileguard.security;

import com.reconcileguard.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.crypto.SecretKey;

@Service
public class JwtService {
    private final SecurityProperties properties;

    public JwtService(SecurityProperties properties) {
        this.properties = properties;
    }

    public String createToken(String userId, String email, UserRole role) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getTokenTtlMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(java.util.Date.from(now))
                .setExpiration(java.util.Date.from(exp))
                .claim("email", email)
                .claim("roles", List.of("ROLE_" + role.name()))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Instant expiresAt() {
        return Instant.now().plus(properties.getTokenTtlMinutes(), ChronoUnit.MINUTES);
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        byte[] bytes = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("RG_JWT_SECRET must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
