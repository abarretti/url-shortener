package com.ab.urlshortener.service.mapper;

import com.ab.urlshortener.controller.response.UrlResponse;
import com.ab.urlshortener.entity.Url;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class UrlMapperTest {

    private final UrlMapper mapper = Mappers.getMapper(UrlMapper.class);

    @Test
    void shouldMapToUrlResponse() {
        // Given
        var shortUrl = "hello1";
        var longUrl = "https://helloworld.com";
        var now = Instant.now();
        var url = Url.builder()
                .id(UUID.randomUUID())
                .shortUrl(shortUrl)
                .longUrl(longUrl)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(now.plus(10, MINUTES))
                .build();

        // When
        var actual = mapper.toResponse(url);

        // Then
        var expected = new UrlResponse(
                shortUrl,
                longUrl,
                now,
                now,
                now.plus(10, MINUTES)
        );
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expected);
    }
}
