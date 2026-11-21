package com.interview.shortlink.repository;

import com.interview.shortlink.domain.ShortLink;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ShortLinkRepositoryTest {

    @Autowired ShortLinkRepository repository;

    @Test
    void atomicallyIncrementsAccessCount() {
        repository.saveAndFlush(new ShortLink("Ab12Cd34", "https://example.com", "example", null));

        assertThat(repository.incrementAccessCount("Ab12Cd34", 7)).isEqualTo(1);

        assertThat(repository.findByShortCode("Ab12Cd34")).get()
                .extracting(ShortLink::getAccessCount)
                .isEqualTo(7L);
    }
}
