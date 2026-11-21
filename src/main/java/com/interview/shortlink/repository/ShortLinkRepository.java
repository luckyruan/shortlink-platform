package com.interview.shortlink.repository;

import com.interview.shortlink.domain.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    Optional<ShortLink> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update ShortLink s set s.accessCount = s.accessCount + :delta where s.shortCode = :code")
    int incrementAccessCount(@Param("code") String code, @Param("delta") long delta);
}
