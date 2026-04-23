package com.ab.urlshortener.service.helper;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidProvider {
    public UUID getRandomUuid() {
        return UUID.randomUUID();
    }
}
