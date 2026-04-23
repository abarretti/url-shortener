package com.ab.urlshortener.service;

import com.ab.urlshortener.config.ShortenerConfig;
import com.ab.urlshortener.controller.request.ShortenUrlRequest;
import com.ab.urlshortener.controller.response.UrlResponse;
import com.ab.urlshortener.entity.Url;
import com.ab.urlshortener.repository.UrlRepository;
import com.ab.urlshortener.service.helper.ClockProvider;
import com.ab.urlshortener.service.helper.UrlEncoder;
import com.ab.urlshortener.service.mapper.UrlMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private ClockProvider clockProvider;
    @Mock
    private ShortenerConfig config;
    @Mock
    private UrlEncoder encoder;
    @Mock
    private UrlRepository repository;
    @Mock
    private UrlMapper mapper;
    @InjectMocks
    private UrlService service;

    @Nested
    class Shorten {
        @Test
        void shouldShortenUrl() {
            // Given
            var userId = UUID.randomUUID();
            var longUrl = "https://helloworld.com?page=1234";
            var shortUrl = "d4DB21";
            var request = new ShortenUrlRequest(
                    longUrl,
                    null,
                    null);
            when(encoder.encode(longUrl)).thenReturn(shortUrl);
            var clock = mock(Clock.class);
            when(clockProvider.getClock()).thenReturn(clock);
            var now = Instant.now();
            when(clock.instant()).thenReturn(now);
            when(config.getDefaultExpirationDays()).thenReturn(1);
            var expirationDate = now.plus(1, DAYS);
            var url = Url.builder()
                    .userId(userId)
                    .shortUrl(shortUrl)
                    .longUrl(longUrl)
                    .createdAt(now)
                    .expiresAt(expirationDate)
                    .build();
            when(config.getMaxCollisionRetries()).thenReturn(5);
            var savedUrl = mock(Url.class);
            when(savedUrl.getShortUrl()).thenReturn(shortUrl);
            when(repository.saveAndFlush(url)).thenReturn(savedUrl);

            // When
            var actual = service.shorten(userId, request);

            // Then
            assertEquals(shortUrl, actual);
        }

        @Test
        void shouldShortenUrlWithAliasAndExpirationDate() {
            // Given
            var userId = UUID.randomUUID();
            var longUrl = "https://helloworld.com?page=1234";
            var shortUrl = "hello21";
            var now = Instant.now();
            var expirationDate = now.plus(30, DAYS);
            var request = new ShortenUrlRequest(
                    longUrl,
                    shortUrl,
                    expirationDate);
            var clock = mock(Clock.class);
            when(clockProvider.getClock()).thenReturn(clock);
            when(clock.instant()).thenReturn(now);
            var url = Url.builder()
                    .userId(userId)
                    .shortUrl(shortUrl)
                    .longUrl(longUrl)
                    .createdAt(now)
                    .expiresAt(expirationDate)
                    .build();
            when(config.getMaxCollisionRetries()).thenReturn(5);
            var savedUrl = mock(Url.class);
            when(savedUrl.getShortUrl()).thenReturn(shortUrl);
            when(repository.saveAndFlush(url)).thenReturn(savedUrl);

            // When
            var actual = service.shorten(userId, request);

            // Then
            verify(encoder, never()).encode(longUrl);
            verify(config, never()).getDefaultExpirationDays();
            assertEquals(shortUrl, actual);
        }

        @Test
        void shouldHandleShortUrlCollision() {
            // Given
            var userId = UUID.randomUUID();
            var longUrl = "https://helloworld.com?page=1234";
            var shortUrl1 = "d4DB21";
            var request = new ShortenUrlRequest(
                    longUrl,
                    null,
                    null);
            when(encoder.encode(longUrl)).thenReturn(shortUrl1);
            var clock = mock(Clock.class);
            when(clockProvider.getClock()).thenReturn(clock);
            var now = Instant.now();
            when(clock.instant()).thenReturn(now);
            when(config.getDefaultExpirationDays()).thenReturn(1);
            var expirationDate = now.plus(1, DAYS);
            var url = Url.builder()
                    .userId(userId)
                    .shortUrl(shortUrl1)
                    .longUrl(longUrl)
                    .createdAt(now)
                    .expiresAt(expirationDate)
                    .build();
            when(config.getMaxCollisionRetries()).thenReturn(5);
            var shortUrl2 = "b132j71";
            when(encoder.encode(longUrl)).thenReturn(shortUrl2);
            var savedUrl = mock(Url.class);
            when(repository.saveAndFlush(url))
                    .thenThrow(new DataIntegrityViolationException("constraint violation"))
                    .thenReturn(savedUrl);
            when(savedUrl.getShortUrl()).thenReturn(shortUrl2);

            // When
            var actual = service.shorten(userId, request);

            // Then
            assertEquals(shortUrl2, actual);
            verify(repository, times(2)).saveAndFlush(url);
        }

        @Test
        void shouldThrowExceptionDuplicateAlias() {
            // Given
            var userId = UUID.randomUUID();
            var longUrl = "https://helloworld.com?page=1234";
            var shortUrl = "hello21";
            var now = Instant.now();
            var expirationDate = now.plus(30, DAYS);
            var request = new ShortenUrlRequest(
                    longUrl,
                    shortUrl,
                    expirationDate);
            var clock = mock(Clock.class);
            when(clockProvider.getClock()).thenReturn(clock);
            when(clock.instant()).thenReturn(now);
            var url = Url.builder()
                    .userId(userId)
                    .shortUrl(shortUrl)
                    .longUrl(longUrl)
                    .createdAt(now)
                    .expiresAt(expirationDate)
                    .build();
            when(config.getMaxCollisionRetries()).thenReturn(5);
            when(repository.saveAndFlush(url))
                    .thenThrow(new DataIntegrityViolationException("constraint violation"));

            // When
            var exception = assertThrows(ResponseStatusException.class,
                    () -> service.shorten(userId, request));

            // Then
            verify(encoder, never()).encode(longUrl);
            assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, exception.getStatusCode());
        }

        @Test
        void shouldThrowExceptionCollisionRetriesExhausted() {
            // Given
            var userId = UUID.randomUUID();
            var longUrl = "https://helloworld.com?page=1234";
            var shortUrl = "d4DB21";
            var request = new ShortenUrlRequest(
                    longUrl,
                    null,
                    null);
            when(encoder.encode(longUrl)).thenReturn(shortUrl);
            var clock = mock(Clock.class);
            when(clockProvider.getClock()).thenReturn(clock);
            var now = Instant.now();
            when(clock.instant()).thenReturn(now);
            when(config.getDefaultExpirationDays()).thenReturn(1);
            var expirationDate = now.plus(1, DAYS);
            var url = Url.builder()
                    .userId(userId)
                    .shortUrl(shortUrl)
                    .longUrl(longUrl)
                    .createdAt(now)
                    .expiresAt(expirationDate)
                    .build();
            when(config.getMaxCollisionRetries()).thenReturn(5);
            when(repository.saveAndFlush(url))
                    .thenThrow(new DataIntegrityViolationException("constraint violation"));

            // When
            var exception = assertThrows(ResponseStatusException.class,
                    () -> service.shorten(userId, request));

            // Then
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        }
    }

    @Nested
    class GetLongUrl {
        @Test
        void shouldGetLongUrl() {
            // Given
            var shortUrl = "hello1";
            var longUrl = "https://helloworld.com";
            var url = Url.builder()
                    .shortUrl(shortUrl)
                    .longUrl(longUrl)
                    .build();
            when(repository.findByShortUrl(shortUrl)).thenReturn(Optional.of(url));

            // When
            var actual = service.getLongUrl(shortUrl);

            // Then
            assertEquals(longUrl, actual);
        }

        @Test
        void shouldThrowNotFoundException() {
            // Given
            var shortUrl = "hello1";
            when(repository.findByShortUrl(shortUrl)).thenReturn(Optional.empty());

            // When
            var exception = assertThrows(ResponseStatusException.class,
                    () -> service.getLongUrl(shortUrl));

            // Then
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }

    @Nested
    class GetUrls {
        @Test
        void shouldGetUrls() {
            // Given
            var userId = UUID.randomUUID();
            var pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            var shortUrl = "hello1";
            var longUrl = "https://helloworld.com";
            var url = Url.builder()
                    .shortUrl(shortUrl)
                    .longUrl(longUrl)
                    .build();
            var urlPage = new PageImpl<>(List.of(url), pageable, 1);
            when(repository.findByUserId(userId, pageable)).thenReturn(urlPage);
            var now = Instant.now();
            var urlResponse = new UrlResponse(
                    shortUrl,
                    longUrl,
                    now,
                    now,
                    now.plus(10, MINUTES)
            );
            when(mapper.toResponse(url)).thenReturn(urlResponse);

            // When
            var actual = service.getUrls(userId, pageable);

            // Then
            assertEquals(1, actual.getTotalElements());
            assertEquals(shortUrl, actual.getContent().getFirst().shortUrl());
            assertEquals(longUrl, actual.getContent().getFirst().longUrl());
            assertEquals(now, actual.getContent().getFirst().createdAt());
            assertEquals(now, actual.getContent().getFirst().updatedAt());
            assertEquals(now.plus(10, MINUTES), actual.getContent().getFirst().expiresAt());
        }
    }
}
