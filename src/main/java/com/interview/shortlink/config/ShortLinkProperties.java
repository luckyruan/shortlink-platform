package com.interview.shortlink.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "short-link")
public record ShortLinkProperties(
        @NotBlank String baseUrl,
        @Min(6) @Max(12) int codeLength,
        Duration cacheTtl,
        Duration negativeCacheTtl,
        @Min(1) int createLimitPerMinute,
        @Min(1000) int counterFlushIntervalMs
) {
}
