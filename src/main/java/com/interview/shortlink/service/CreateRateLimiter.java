package com.interview.shortlink.service;

import com.interview.shortlink.config.ShortLinkProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CreateRateLimiter {

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return current
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ShortLinkProperties properties;
    private final ConcurrentHashMap<String, LocalWindow> fallback = new ConcurrentHashMap<>();

    public CreateRateLimiter(StringRedisTemplate redis, ShortLinkProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public boolean allow(String clientId) {
        long minute = Instant.now().getEpochSecond() / 60;
        try {
            Long count = redis.execute(SCRIPT, List.of("shortlink:rate:create:" + clientId + ":" + minute), "70");
            return count != null && count <= properties.createLimitPerMinute();
        } catch (RuntimeException ex) {
            LocalWindow window = fallback.compute(clientId, (key, old) -> {
                if (old == null || old.minute != minute) return new LocalWindow(minute);
                old.count.incrementAndGet();
                return old;
            });
            return window.count.get() <= properties.createLimitPerMinute();
        }
    }

    private static final class LocalWindow {
        private final long minute;
        private final AtomicInteger count = new AtomicInteger(1);

        private LocalWindow(long minute) { this.minute = minute; }
    }
}
