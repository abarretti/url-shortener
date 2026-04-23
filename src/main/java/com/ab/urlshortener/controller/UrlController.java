package com.ab.urlshortener.controller;

import com.ab.urlshortener.controller.request.ShortenUrlRequest;
import com.ab.urlshortener.controller.response.ShortUrlResponse;
import com.ab.urlshortener.controller.response.UrlResponse;
import com.ab.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService service;

    /**
     * Would be authenticated with a JWT Token or
     * SessionId in the header. Using a dummy
     * userId for now.
     *
     * @param userId .
     * @param request .
     * @return UrlResponse
     */
    @PostMapping
    public ResponseEntity<ShortUrlResponse> shortenUrl(
            @RequestHeader(name = "user_id") UUID userId,
            @Valid @RequestBody ShortenUrlRequest request) {
        var shortUrl = service.shorten(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ShortUrlResponse(shortUrl));
    }

    /**
     * Would be authenticated w/ a JWT Token or SessionId
     * in the header.
     *
     * @param shortUrl .
     * @return Void
     */
    @GetMapping("/{short_url}")
    public ResponseEntity<Void> getAndRedirectToLongUrl(
            @PathVariable(name = "short_url")
            @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "Alias must be between 6 and 8 alphanumeric characters.")
            @Size(min = 6, max = 8, message = "Alias must be between 6 and 8 alphanumeric characters.")
            String shortUrl
    ) {
        var longUrl = service.getLongUrl(shortUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }

    /**
     * Gets all shortUrls by a userId.
     *
     * @param userId .
     * @param pageable .
     * @return .
     */
    @GetMapping
    public ResponseEntity<Page<UrlResponse>> getShortUrls(
            @RequestHeader(name = "user_id") UUID userId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        var page = service.getUrls(userId, pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(page);
    }
}
