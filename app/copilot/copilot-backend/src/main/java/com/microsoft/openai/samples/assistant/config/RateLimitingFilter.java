// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.openai.samples.assistant.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int CHAT_REQUESTS_PER_MINUTE = 20;
    private static final int PAYMENTS_REQUESTS_PER_MINUTE = 10;
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final Map<String, Bucket> chatBuckets = buildBoundedMap();
    private final Map<String, Bucket> paymentsBuckets = buildBoundedMap();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientKey = request.getRemoteAddr();

        Bucket bucket = null;
        if (path.startsWith("/api/chat")) {
            bucket = chatBuckets.computeIfAbsent(clientKey, k -> buildBucket(CHAT_REQUESTS_PER_MINUTE));
        } else if (path.startsWith("/payments")) {
            bucket = paymentsBuckets.computeIfAbsent(clientKey, k -> buildBucket(PAYMENTS_REQUESTS_PER_MINUTE));
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Bucket buildBucket(int requestsPerMinute) {
        Bandwidth limit =
                Bandwidth.classic(
                        requestsPerMinute, Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private static <K, V> Map<K, V> buildBoundedMap() {
        return Collections.synchronizedMap(
                new LinkedHashMap<K, V>(MAX_TRACKED_CLIENTS, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                        return size() > MAX_TRACKED_CLIENTS;
                    }
                });
    }
}
