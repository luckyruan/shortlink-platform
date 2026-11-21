package com.interview.shortlink.service;

import com.interview.shortlink.config.ShortLinkProperties;
import com.interview.shortlink.domain.LinkStatus;
import com.interview.shortlink.domain.ShortLink;
import com.interview.shortlink.dto.CreateShortLinkRequest;
import com.interview.shortlink.dto.LinkStatsResponse;
import com.interview.shortlink.dto.ShortLinkResponse;
import com.interview.shortlink.exception.BadRequestException;
import com.interview.shortlink.exception.ConflictException;
import com.interview.shortlink.exception.LinkUnavailableException;
import com.interview.shortlink.exception.NotFoundException;
import com.interview.shortlink.repository.ShortLinkRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Locale;

@Service
public class ShortLinkService {

    private static final int MAX_CODE_ATTEMPTS = 6;

    private final ShortLinkRepository repository;
    private final ShortCodeGenerator codeGenerator;
    private final LinkCacheService cache;
    private final AccessCounterService counter;
    private final ShortLinkProperties properties;

    public ShortLinkService(ShortLinkRepository repository, ShortCodeGenerator codeGenerator,
                            LinkCacheService cache, AccessCounterService counter,
                            ShortLinkProperties properties) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.cache = cache;
        this.counter = counter;
        this.properties = properties;
    }

    public ShortLinkResponse create(CreateShortLinkRequest request) {
        validateTarget(request.originalUrl());
        String alias = normalizeAlias(request.customAlias());
        if (alias != null) {
            if (repository.existsByShortCode(alias)) {
                throw new ConflictException("Custom alias is already in use");
            }
            try {
                ShortLink created = persist(alias, request);
                evictAfterCommit(alias);
                return response(created);
            } catch (DataIntegrityViolationException race) {
                throw new ConflictException("Custom alias is already in use");
            }
        }

        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String generated = codeGenerator.generate(properties.codeLength());
            try {
                ShortLink created = persist(generated, request);
                evictAfterCommit(generated);
                return response(created);
            } catch (DataIntegrityViolationException collision) {
                if (attempt == MAX_CODE_ATTEMPTS - 1) {
                    throw new IllegalStateException("Could not allocate a unique short code", collision);
                }
            }
        }
        throw new IllegalStateException("Could not allocate a short code");
    }

    public String resolveTarget(String code) {
        var cached = cache.get(code);
        if (cached.isPresent()) {
            if (LinkCacheService.NULL_MARKER.equals(cached.get())) {
                throw new NotFoundException("Short link does not exist");
            }
            counter.increment(code);
            return cached.get();
        }

        ShortLink link = repository.findByShortCode(code).orElse(null);
        if (link == null) {
            cache.putMissing(code);
            throw new NotFoundException("Short link does not exist");
        }
        if (!link.isAvailableAt(Instant.now())) {
            throw new LinkUnavailableException("Short link is disabled or expired");
        }
        cache.putTarget(code, link.getOriginalUrl(), link.getExpiresAt());
        counter.increment(code);
        return link.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public ShortLinkResponse get(String code) {
        return response(requireLink(code));
    }

    @Transactional
    public ShortLinkResponse changeStatus(String code, LinkStatus status) {
        ShortLink link = requireLink(code);
        link.changeStatus(status);
        evictAfterCommit(code);
        return response(repository.save(link));
    }

    @Transactional(readOnly = true)
    public LinkStatsResponse stats(String code) {
        ShortLink link = requireLink(code);
        return new LinkStatsResponse(code, link.getAccessCount() + counter.pending(code), Instant.now());
    }

    private ShortLink persist(String code, CreateShortLinkRequest request) {
        ShortLink link = new ShortLink(code, request.originalUrl().trim(), blankToNull(request.title()), request.expiresAt());
        return repository.saveAndFlush(link);
    }

    private void evictAfterCommit(String code) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { cache.evict(code); }
            });
        } else {
            cache.evict(code);
        }
    }

    private ShortLink requireLink(String code) {
        return repository.findByShortCode(code)
                .orElseThrow(() -> new NotFoundException("Short link does not exist"));
    }

    private ShortLinkResponse response(ShortLink link) {
        return ShortLinkResponse.from(link, properties.baseUrl(), link.getAccessCount() + counter.pending(link.getShortCode()));
    }

    private static void validateTarget(String value) {
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null) {
                throw new BadRequestException("originalUrl must be an absolute HTTP or HTTPS URL");
            }
        } catch (URISyntaxException ex) {
            throw new BadRequestException("originalUrl is not a valid URL");
        }
    }

    private static String normalizeAlias(String alias) {
        return alias == null || alias.isBlank() ? null : alias.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
