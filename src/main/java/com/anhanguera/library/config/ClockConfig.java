package com.anhanguera.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Relógio usado pelos services para obter a data de hoje. Ter um bean facilita
 * substituí-lo por um relógio fixo em testes.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
