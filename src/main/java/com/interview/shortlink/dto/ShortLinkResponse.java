package com.interview.shortlink.dto;

import com.interview.shortlink.domain.LinkStatus;
import com.interview.shortlink.domain.ShortLink;

import java.time.Instant;

public record ShortLinkResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        String title,
        LinkStatus status,
        Instant expiresAt,
        long accessCount,
        Instant createdAt
) {
    public static ShortLinkResponse from(ShortLink link, String baseUrl, long accessCount) {
        return new ShortLinkResponse(
                link.getShortCode(), baseUrl + link.getShortCode(), link.getOriginalUrl(),
                link.getTitle(), link.getStatus(), link.getExpiresAt(), accessCount, link.getCreatedAt()
        );
    }
}
