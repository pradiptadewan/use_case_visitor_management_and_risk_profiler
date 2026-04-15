package com.vigigate.backend.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    Clock appClock(@Value("${app.time-zone:Asia/Jakarta}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
