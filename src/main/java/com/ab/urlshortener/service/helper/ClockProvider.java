package com.ab.urlshortener.service.helper;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
@Getter
public class ClockProvider {
    private final Clock clock = Clock.systemUTC();
}
