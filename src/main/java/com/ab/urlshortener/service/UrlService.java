package com.ab.urlshortener.service;

import com.ab.urlshortener.config.ShortenerConfig;
import com.ab.urlshortener.controller.request.ShortenUrlRequest;
import com.ab.urlshortener.controller.response.UrlResponse;
import com.ab.urlshortener.entity.Url;
import com.ab.urlshortener.repository.UrlRepository;
import com.ab.urlshortener.service.helper.ClockProvider;
import com.ab.urlshortener.service.helper.UrlEncoder;
import com.ab.urlshortener.service.mapper.UrlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UrlService {
    private final ClockProvider clockProvider;
    private final ShortenerConfig config;
    private final UrlEncoder encoder;
    private final UrlRepository repository;
    private final UrlMapper mapper;

    /**
     * Shortens a Long Url and returns the Short Url.
     *
     * @param userId .
     * @param request .
     * @return shortUrl
     */
    @Transactional
    public String shorten(UUID userId, ShortenUrlRequest request) {
        var shortUrl = Optional.ofNullable(request.alias())
                .orElseGet(() -> encoder.encode(request.longUrl()));

        var expirationDate = Optional.ofNullable(request.expirationDate())
                .orElseGet(() -> clockProvider.getClock().instant()
                        .plus(config.getDefaultExpirationDays(), ChronoUnit.DAYS));

        var url = Url.builder()
                .userId(userId)
                .shortUrl(shortUrl)
                .longUrl(request.longUrl())
                .createdAt(clockProvider.getClock().instant())
                .expiresAt(expirationDate)
                .build();

        int attempt = 0;
        Url savedUrl = null;
        while (attempt < config.getMaxCollisionRetries()) {
            try {
                savedUrl = repository.saveAndFlush(url);
                break;
            } catch (DataIntegrityViolationException e) {
                if (request.alias() == null) {
                    url.setShortUrl(encoder.encode(request.longUrl()));
                } else {
                    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                            "The alias submitted for the url has already been selected. "
                                    + "Please resubmit another alias or remove for a system generated short url.");
                }
                attempt++;
            }
        }

        return Optional.ofNullable(savedUrl)
                .map(Url::getShortUrl)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Server is busy, please retry in a moment."));
    }

    /**
     * Gets the Long Url for a respective Short Url.
     *
     * @param shortUrl .
     * @return longUrl
     */
    public String getLongUrl(String shortUrl) {
        return repository.findByShortUrl(shortUrl)
                .map(Url::getLongUrl)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Invalid short url."));
    }

    /**
     * Returns Paginated Url object for respective userId parameter.
     *
     * @param userId .
     * @param pageable .
     * @return .
     */
    public Page<UrlResponse> getUrls(UUID userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable)
                .map(mapper::toResponse);
    }
}
