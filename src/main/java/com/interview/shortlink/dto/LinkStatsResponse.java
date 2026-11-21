package com.interview.shortlink.dto;

import java.time.Instant;

public record LinkStatsResponse(String shortCode, long accessCount, Instant generatedAt) {
}
