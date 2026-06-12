package com.project.aspect;

import com.project.annotation.RateLimit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE) // Execute after security filters
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    public RateLimitAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return joinPoint.proceed(); // Skip if not authenticated (should be handled by security)
        }

        String userId = auth.getName(); // Usually email or subject
        String key = "rate_limit:" + joinPoint.getSignature().getName() + ":" + userId;
        
        long now = Instant.now().toEpochMilli();
        long windowStart = now - (rateLimit.windowSeconds() * 1000L);

        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        // Remove old entries outside the sliding window
        zSetOps.removeRangeByScore(key, 0, windowStart);

        // Count requests in the current window
        Long currentRequests = zSetOps.zCard(key);

        if (currentRequests != null && currentRequests >= rateLimit.requests()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please try again later.");
        }

        // Add current request
        zSetOps.add(key, String.valueOf(now) + "-" + Math.random(), now);
        
        // Set expiry on the key to automatically clean up inactive users
        redisTemplate.expire(key, rateLimit.windowSeconds(), TimeUnit.SECONDS);

        return joinPoint.proceed();
    }
}
