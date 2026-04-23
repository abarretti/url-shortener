package com.ab.urlshortener.repository;

import com.ab.urlshortener.entity.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UrlRepository extends JpaRepository<Url, UUID> {
    Optional<Url> findByShortUrl(String shortUrl);
    Page<Url> findByUserId(UUID userId, Pageable pageable);
}
