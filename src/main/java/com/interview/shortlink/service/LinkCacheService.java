package com.interview.shortlink.service;

import com.interview.shortlink.config.ShortLinkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class LinkCacheService {

    public static final String NULL_MARKER = "__NULL__";
    private static final String PREFIX = "shortlink:target:";
    private static final Logger log = LoggerFactory.getLogger(LinkCacheService.class);

    private final StringRedisTemplate redis;
    private final ShortLinkProperties properties;

    public LinkCacheService(StringRedisTemplate redis, ShortLinkProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public Optional<String> get(String code) {
        try {
            return Optional.ofNullable(redis.opsForValue().get(PREFIX + code));
        } catch (RuntimeException ex) {
            log.debug("Redis cache unavailable; falling back to database: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public void putTarget(String code, String target, Instant expiresAt) {
        Duration ttl = properties.cacheTtl();
        if (expiresAt != null) {
            Duration untilExpiry = Duration.between(Instant.now(), expiresAt);
            if (untilExpiry.isNegative() || untilExpiry.isZero()) return;
            if (untilExpiry.compareTo(ttl) < 0) ttl = untilExpiry;
        }
        put(code, target, ttl);
    }

    public void putMissing(String code) {
        put(code, NULL_MARKER, properties.negativeCacheTtl());
    }

    public void evict(String code) {
        try {
            redis.delete(PREFIX + code);
        } catch (RuntimeException ex) {
            log.debug("Could not evict Redis key: {}", ex.getMessage());
        }
    }

    private void put(String code, String value, Duration ttl) {
        try {
            redis.opsForValue().set(PREFIX + code, value, ttl);
        } catch (RuntimeException ex) {
            log.debug("Could not write Redis cache: {}", ex.getMessage());
        }
    }
}
