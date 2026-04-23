package com.ab.urlshortener;

import com.ab.urlshortener.controller.request.ShortenUrlRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class UrlIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("urlshortener_test_db")
                    .withUsername("urlshortener")
                    .withPassword("urlshortener");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private WebServerApplicationContext serverContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebTestClient client;

    @BeforeEach
    void setup() {
        int port = serverContext.getWebServer().getPort();
        this.client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @AfterEach
    void cleanup() {
        jdbc.execute("TRUNCATE TABLE urls RESTART IDENTITY CASCADE");
    }

    @Test
    void shouldShortenUrl() {
        // Given
        var request = new ShortenUrlRequest(
                "https://helloworld.com",
                null,
                Instant.now().plusSeconds(3600)
        );

        // Then
        client.post()
                .uri("/urls")
                .header("user_id", UUID.randomUUID().toString())
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.short_url")
                .value(shortUrl -> assertThat((String) shortUrl, matchesPattern("^[a-zA-Z0-9]{6,8}$")));
    }

    @Test
    void shouldShortenUrlWithAlias() {
        // Given
        var alias = "hello1";
        var request = new ShortenUrlRequest(
                "https://helloworld.com",
                alias,
                Instant.now().plusSeconds(3600)
        );

        // Then
        client.post()
                .uri("/urls")
                .header("user_id", UUID.randomUUID().toString())
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.short_url")
                .value(shortUrl -> assertEquals(alias, shortUrl));
    }

    @Test
    void shouldRedirectToLongUrl() throws JsonProcessingException {
        // Given
        var longUrl = "https://helloworld.com";
        var request = new ShortenUrlRequest(
                longUrl,
                null,
                Instant.now().plusSeconds(3600)
        );
        var urlResponse = client.post()
                .uri("/urls")
                .header("user_id", UUID.randomUUID().toString())
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        var jsonNode = objectMapper.readTree(urlResponse);
        var shortUrl = jsonNode.get("short_url").asText();

        // Then
        client.get()
                .uri("/urls/" + shortUrl)
                .exchange()
                .expectStatus().isFound()
                .expectHeader().valueEquals("Location",longUrl);
    }

    @Test
    void shouldThrowExceptionInvalidShortUrl() {
        client.get()
                .uri("/urls/8r3NI2z")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldGetUrls() {
        // Given
        final var userId = UUID.randomUUID().toString();
        var now1 = Instant.now();
        var expiresAt = now1.plus(1, ChronoUnit.DAYS);
        var longUrl1 = "https://helloworld.com";
        var shortUrl1 = "hello1";
        var request1 = new ShortenUrlRequest(
                longUrl1,
                shortUrl1,
                expiresAt
        );
        client.post()
                .uri("/urls")
                .header("user_id", userId)
                .bodyValue(request1)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult();
        var longUrl2 = "https://youhadmeathelloworld.com";
        var shortUrl2 = "hello2";
        var request2 = new ShortenUrlRequest(
                longUrl2,
                shortUrl2,
                expiresAt
        );
        client.post()
                .uri("/urls")
                .header("user_id", userId)
                .bodyValue(request2)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult();

        // Then
        client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/urls")
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .build())
                .header("user_id", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(2)
                .jsonPath("$.totalPages").isEqualTo(1)
                .jsonPath("$.size").isEqualTo(10)
                .jsonPath("$.number").isEqualTo(0)
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.content[0].short_url").isEqualTo(shortUrl2)
                .jsonPath("$.content[0].long_url").isEqualTo(longUrl2)
                .jsonPath("$.content[0].created_at").exists()
                .jsonPath("$.content[0].updated_at").exists()
                .jsonPath("$.content[0].expires_at").exists()
                .jsonPath("$.content[1].short_url").isEqualTo(shortUrl1)
                .jsonPath("$.content[1].long_url").isEqualTo(longUrl1)
                .jsonPath("$.content[1].created_at").exists()
                .jsonPath("$.content[1].updated_at").exists()
                .jsonPath("$.content[1].expires_at").exists();
    }
}
