package com.interview.shortlink.service;

import com.interview.shortlink.repository.ShortLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AccessCounterService {

    private static final String ACTIVE_CODES = "shortlink:counter:active";
    private static final String COUNTER_PREFIX = "shortlink:counter:delta:";
    private static final Logger log = LoggerFactory.getLogger(AccessCounterService.class);

    private final StringRedisTemplate redis;
    private final ShortLinkRepository repository;

    public AccessCounterService(StringRedisTemplate redis, ShortLinkRepository repository) {
        this.redis = redis;
        this.repository = repository;
    }

    public void increment(String code) {
        try {
            redis.opsForSet().add(ACTIVE_CODES, code);
            redis.opsForValue().increment(COUNTER_PREFIX + code);
        } catch (RuntimeException ex) {
            log.debug("Redis counter unavailable; writing through to database: {}", ex.getMessage());
            incrementDatabase(code, 1);
        }
    }

    public long pending(String code) {
        try {
            String value = redis.opsForValue().get(COUNTER_PREFIX + code);
            return value == null ? 0 : Long.parseLong(value);
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    @Scheduled(fixedDelayString = "${short-link.counter-flush-interval-ms}")
    public void flush() {
        Set<String> codes;
        try {
            codes = redis.opsForSet().members(ACTIVE_CODES);
        } catch (RuntimeException ex) {
            return;
        }
        if (codes == null) return;
        codes.forEach(this::flushOne);
    }

    void flushOne(String code) {
        String key = COUNTER_PREFIX + code;
        String captured;
        try {
            captured = redis.opsForValue().getAndSet(key, "0");
        } catch (RuntimeException ex) {
            return;
        }
        if (captured == null || "0".equals(captured)) return;
        long delta = Long.parseLong(captured);
        try {
            incrementDatabase(code, delta);
        } catch (RuntimeException ex) {
            try {
                redis.opsForValue().increment(key, delta);
            } catch (RuntimeException restoreFailure) {
                log.error("Access delta for {} could not be restored; delta={}", code, delta, restoreFailure);
            }
        }
    }

    @Transactional
    public void incrementDatabase(String code, long delta) {
        repository.incrementAccessCount(code, delta);
    }
}
