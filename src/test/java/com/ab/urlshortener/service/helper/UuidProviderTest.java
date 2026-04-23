package com.ab.urlshortener.service.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

@ExtendWith(MockitoExtension.class)
class UuidProviderTest {

    private final UuidProvider provider = new UuidProvider();

    @Test
    void shouldReturnUuid() {
        assertThat(provider.getRandomUuid(), instanceOf(UUID.class));
    }
}
