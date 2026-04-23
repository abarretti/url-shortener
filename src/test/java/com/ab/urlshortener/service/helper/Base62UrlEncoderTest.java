package com.ab.urlshortener.service.helper;

import com.ab.urlshortener.config.ShortenerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class Base62UrlEncoderTest {

    @Mock
    private ShortenerConfig config;

    @Mock
    private UuidProvider provider;

    @InjectMocks
    private Base62UrlEncoder encoder;

    @Test
    void shouldEncodeLongUrl() {
        // Given
        var longUrl = "https://helloworld.com";
        var uuid = UUID.randomUUID();
        when(config.getBase62Salt()).thenReturn("salt");
        when(provider.getRandomUuid()).thenReturn(uuid);
        when(config.getAliasLength()).thenReturn(7);

        // When
        var actual = encoder.encode(longUrl);

        // Then
        assertThat(actual, matchesPattern("^[a-zA-Z0-9]{6,8}$"));
    }
}
