package com.interview.shortlink.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ShortCodeGeneratorTest {

    private final ShortCodeGenerator generator = new ShortCodeGenerator();

    @Test
    void generatesBase62CodesWithRequestedLength() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            String code = generator.generate(8);
            assertThat(code).matches("[0-9A-Za-z]{8}");
            generated.add(code);
        }
        assertThat(generated).hasSize(1_000);
    }
}
