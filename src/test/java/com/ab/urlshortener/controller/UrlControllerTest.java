package com.ab.urlshortener.controller;

import com.ab.urlshortener.controller.request.ShortenUrlRequest;
import com.ab.urlshortener.controller.response.UrlResponse;
import com.ab.urlshortener.service.UrlService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class Shortener {
        @Test
        void shouldReturnShortenedUrl() throws Exception {
            // Given
            var shortUrl = "jfn388";
            var userId = UUID.randomUUID();
            var request = new ShortenUrlRequest(
                    "https://helloworld.com?page=49939248",
                    shortUrl,
                    Instant.now().plus(1, DAYS)
            );
            when(service.shorten(userId, request)).thenReturn(shortUrl);

            // Then
            mockMvc.perform(post("/urls")
                            .header("user_id", userId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.short_url").value(shortUrl));
        }

        @ParameterizedTest
        @ValueSource(strings = {"aliasIsTooLong", "b!8?^a"})
        void shouldThrowExceptionInvalidAlias(String alias) throws Exception {
            // Given
            var request = new ShortenUrlRequest(
                    "https://helloworld.com?page=49939248",
                    alias,
                    null
            );

            // Then
            mockMvc.perform(post("/urls")
                            .header("user_id", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        void shouldThrowExceptionInvalidExpirationDate() throws Exception {
            // Given
            var request = new ShortenUrlRequest(
                    "https://helloworld.com?page=49939248",
                    null,
                    Instant.now().minus(1, DAYS)
            );

            // Then
            mockMvc.perform(post("/urls")
                            .header("user_id", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    class Redirect {
        @Test
        void shouldGetAndRedirectToLongUrl() throws Exception {
            // Given
            var shortUrl = "1234567";
            var longUrl = "https://helloworld.com";
            when(service.getLongUrl(shortUrl)).thenReturn(longUrl);

            // Then
            mockMvc.perform(get("/urls/" + shortUrl))
                    .andExpect(status().isFound())
                    .andExpect(header().string(HttpHeaders.LOCATION, longUrl));
        }

        @ParameterizedTest
        @ValueSource(strings = {"shortUrlIsTooLong", "b!8?^a"})
        void shouldThrowExceptionInvalidAlias(String shortUrl) throws Exception {
            mockMvc.perform(get("/urls/" + shortUrl))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    class GetUrls {
        @Test
        void shouldGetUrls() throws Exception {
            // Given
            var userId = UUID.randomUUID();
            var now = Instant.now();
            var expiresAt = now.plus(10, MINUTES);
            var response = new UrlResponse("hello1", "https://helloworld.com",
                    now, now, expiresAt);
            var page = new PageImpl<>(
                    List.of(response),
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")),
                    1);
            when(service.getUrls(eq(userId), any(Pageable.class))).thenReturn(page);

            // When
            mockMvc.perform(get("/urls")
                            .header("user_id", userId.toString())
                            .param("page", "0")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].short_url").value("hello1"))
                    .andExpect(jsonPath("$.content[0].long_url").value("https://helloworld.com"))
                    .andExpect(jsonPath("$.content[0].created_at").value(now.toString()))
                    .andExpect(jsonPath("$.content[0].updated_at").value(now.toString()))
                    .andExpect(jsonPath("$.content[0].expires_at").value(expiresAt.toString()))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.totalElements").value(1));

            // Then
            var captor = forClass(Pageable.class);
            verify(service).getUrls(eq(userId), captor.capture());
            var captured = captor.getValue();
            assertThat(captured.getPageNumber()).isEqualTo(0);
            assertThat(captured.getPageSize()).isEqualTo(10);
            assertThat(requireNonNull(captured.getSort().getOrderFor("createdAt")).getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }
    }
}
