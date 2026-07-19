package com.example.socialmedia.gateway;

import java.io.IOException;
import java.time.Duration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration(proxyBeanMethods = false)
public class RateLimitConfig {

    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    @Bean
    Cache<String, Bucket> rateLimitBuckets() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofMinutes(2))
                .build();
    }

    static final class RateLimitFilter extends OncePerRequestFilter {

        private static final long AUTH_CAPACITY = 10;
        private static final long WRITE_CAPACITY = 60;

        private final Cache<String, Bucket> buckets;

        RateLimitFilter(Cache<String, Bucket> buckets) {
            this.buckets = buckets;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            LimitKey limitKey = limitKey(request);
            if (limitKey == null) {
                filterChain.doFilter(request, response);
                return;
            }

            Bucket bucket = buckets.get(limitKey.cacheKey(), ignored -> bucket(limitKey.capacity()));
            var probe = bucket.tryConsumeAndReturnRemaining(1);
            response.setHeader("X-RateLimit-Remaining", Long.toString(probe.getRemainingTokens()));
            if (probe.isConsumed()) {
                filterChain.doFilter(request, response);
                return;
            }

            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(problem(429, "Too Many Requests", request));
        }

        private static LimitKey limitKey(HttpServletRequest request) {
            String path = request.getRequestURI();
            if ("POST".equals(request.getMethod())
                    && ("/api/v1/auth/register".equals(path) || "/api/v1/auth/login".equals(path))) {
                return new LimitKey("auth:" + request.getRemoteAddr(), AUTH_CAPACITY);
            }

            if (!isWrite(request.getMethod())) {
                return null;
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwt && jwt.isAuthenticated()) {
                return new LimitKey("write:" + jwt.getToken().getSubject(), WRITE_CAPACITY);
            }
            return null;
        }

        private static boolean isWrite(String method) {
            return "POST".equals(method) || "PUT".equals(method)
                    || "PATCH".equals(method) || "DELETE".equals(method);
        }

        private static Bucket bucket(long capacity) {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(capacity)
                    .refillGreedy(capacity, REFILL_PERIOD)
                    .build();
            return Bucket.builder().addLimit(limit).build();
        }

        private static String problem(int status, String title, HttpServletRequest request) {
            Object value = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
            String correlationId = value == null ? "unknown" : value.toString();
            return "{\"type\":\"about:blank\",\"title\":\"" + title
                    + "\",\"status\":" + status + ",\"correlationId\":\""
                    + correlationId + "\"}";
        }

        private record LimitKey(String cacheKey, long capacity) {
        }
    }
}
