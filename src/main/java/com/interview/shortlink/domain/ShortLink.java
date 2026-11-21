package com.interview.shortlink.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "short_links", indexes = {
        @Index(name = "idx_short_links_created_at", columnList = "created_at"),
        @Index(name = "idx_short_links_expires_at", columnList = "expires_at")
})
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 32)
    private String shortCode;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private LinkStatus status = LinkStatus.ACTIVE;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "access_count", nullable = false)
    private long accessCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShortLink() {
    }

    public ShortLink(String shortCode, String originalUrl, String title, Instant expiresAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.title = title;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isAvailableAt(Instant now) {
        return status == LinkStatus.ACTIVE && (expiresAt == null || expiresAt.isAfter(now));
    }

    public void changeStatus(LinkStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public String getTitle() { return title; }
    public LinkStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public long getAccessCount() { return accessCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
