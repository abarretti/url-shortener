package com.ab.urlshortener.controller.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ShortenUrlRequest(
        @NotBlank(message = "Long url must contain at least one non-empty character.")
        String longUrl,

        @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "Alias must be between 6 and 8 alphanumeric characters.")
        @Size(min = 6, max = 8, message = "Alias must be between 6 and 8 alphanumeric characters.")
        String alias,

        @Future(message = "Expiration datetime must be in the future.")
        Instant expirationDate) {
}
