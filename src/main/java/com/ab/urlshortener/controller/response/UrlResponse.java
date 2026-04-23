package com.ab.urlshortener.controller.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UrlResponse(
        String shortUrl,
        String longUrl,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt
) {
}
