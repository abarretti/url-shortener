package com.ab.urlshortener.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadLocalRandom;

@Configuration
@ConfigurationProperties(prefix = "url-shortener.shortener")
@Getter
@Setter
public class ShortenerConfig {
    private int defaultExpirationDays;
    private String base62Salt;
    private int minAliasLen;
    private int maxAliasLen;
    private int maxCollisionRetries;

    public int getAliasLength() {
        return ThreadLocalRandom.current().nextInt(minAliasLen, maxAliasLen + 1);
    }
}
