package com.reconcileguard.security;

import com.reconcileguard.config.RateLimitProperties;
import com.reconcileguard.config.RequestIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitProperties properties;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/")
                || uri.equals("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = clientKey(request);
        long minute = Instant.now().getEpochSecond() / 60;
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket());

        if (!bucket.tryAcquire(minute, properties.getPerMinute())) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Object requestId = request.getAttribute(RequestIdFilter.ATTRIBUTE);
            response.getWriter().write("{\"error\":\"RATE_LIMITED\",\"message\":\"Too many requests\",\"requestId\":\""
                    + String.valueOf(requestId) + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            return authentication.getName();
        }
        return request.getRemoteAddr();
    }

    private static final class Bucket {
        private long windowMinute;
        private int count;

        synchronized boolean tryAcquire(long currentMinute, int limit) {
            if (currentMinute != windowMinute) {
                windowMinute = currentMinute;
                count = 0;
            }
            if (count >= limit) {
                return false;
            }
            count++;
            return true;
        }
    }
}
