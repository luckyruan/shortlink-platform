package com.interview.shortlink.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateShortLinkRequest(
        @NotBlank @Size(max = 2048) String originalUrl,
        @Size(max = 200) String title,
        @Pattern(regexp = "^[0-9A-Za-z_-]{4,32}$", message = "customAlias must contain 4-32 letters, digits, '_' or '-'")
        String customAlias,
        @Future Instant expiresAt
) {
}
