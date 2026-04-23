package com.ab.urlshortener.service.helper;

import com.ab.urlshortener.config.ShortenerConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Base62UrlEncoder implements UrlEncoder {

    private static final String BASE_62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final ShortenerConfig config;
    private final UuidProvider provider;

    @Override
    public String encode(String longUrl) {
        long longUrlHashId = UUID.nameUUIDFromBytes(longUrl.getBytes()).getMostSignificantBits();
        long saltedId = salt(longUrlHashId);
        return toBase62(saltedId);
    }

    private long salt(long id) {
        int hash = (config.getBase62Salt() + provider.getRandomUuid() + id).hashCode();
        return Math.abs((long) hash);
    }

    private String toBase62(long id) {
        StringBuilder sb = new StringBuilder();
        int len = config.getAliasLength();
        while (len > 0) {
            sb.append(BASE_62.charAt((int)(id % 62)));
            id /= 62;
            len--;
        }
        return sb.toString();
    }
}
