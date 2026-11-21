package com.interview.shortlink.service;

import com.interview.shortlink.config.ShortLinkProperties;
import com.interview.shortlink.domain.LinkStatus;
import com.interview.shortlink.domain.ShortLink;
import com.interview.shortlink.dto.CreateShortLinkRequest;
import com.interview.shortlink.exception.BadRequestException;
import com.interview.shortlink.exception.NotFoundException;
import com.interview.shortlink.repository.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

    @Mock ShortLinkRepository repository;
    @Mock ShortCodeGenerator generator;
    @Mock LinkCacheService cache;
    @Mock AccessCounterService counter;

    ShortLinkService service;

    @BeforeEach
    void setUp() {
        ShortLinkProperties properties = new ShortLinkProperties(
                "http://localhost:8080/r/", 8, Duration.ofHours(24), Duration.ofMinutes(1), 60, 5000
        );
        service = new ShortLinkService(repository, generator, cache, counter, properties);
    }

    @Test
    void createsLinkWithGeneratedCode() {
        when(generator.generate(8)).thenReturn("Ab12Cd34");
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new CreateShortLinkRequest(
                "https://example.com/article", "article", null, Instant.now().plusSeconds(3600)
        ));

        assertThat(response.shortCode()).isEqualTo("Ab12Cd34");
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/r/Ab12Cd34");
        assertThat(response.status()).isEqualTo(LinkStatus.ACTIVE);
        verify(cache).evict("Ab12Cd34");
    }

    @Test
    void rejectsNonHttpTarget() {
        assertThatThrownBy(() -> service.create(new CreateShortLinkRequest(
                "javascript:alert(1)", null, null, null
        ))).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void resolvesCachedTargetAndCountsVisit() {
        when(cache.get("Ab12Cd34")).thenReturn(Optional.of("https://example.com"));

        assertThat(service.resolveTarget("Ab12Cd34")).isEqualTo("https://example.com");

        verify(counter).increment("Ab12Cd34");
        verifyNoInteractions(repository);
    }

    @Test
    void cachesMissingCodeToProtectDatabase() {
        when(cache.get("missing1")).thenReturn(Optional.empty());
        when(repository.findByShortCode("missing1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveTarget("missing1"))
                .isInstanceOf(NotFoundException.class);

        verify(cache).putMissing("missing1");
    }

    @Test
    void changesStatusAndInvalidatesCache() {
        ShortLink link = new ShortLink("Ab12Cd34", "https://example.com", null, null);
        when(repository.findByShortCode("Ab12Cd34")).thenReturn(Optional.of(link));
        when(repository.save(link)).thenReturn(link);

        var response = service.changeStatus("Ab12Cd34", LinkStatus.DISABLED);

        assertThat(response.status()).isEqualTo(LinkStatus.DISABLED);
        verify(cache).evict("Ab12Cd34");
    }

    @Test
    void invalidatesOnlyAfterTransactionCommit() {
        ShortLink link = new ShortLink("Ab12Cd34", "https://example.com", null, null);
        when(repository.findByShortCode("Ab12Cd34")).thenReturn(Optional.of(link));
        when(repository.save(link)).thenReturn(link);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.changeStatus("Ab12Cd34", LinkStatus.DISABLED);
            verify(cache, never()).evict(anyString());
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(cache).evict("Ab12Cd34");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
