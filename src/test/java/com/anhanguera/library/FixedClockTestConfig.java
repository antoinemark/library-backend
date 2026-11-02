package com.anhanguera.library;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Relógio dos testes: congelado no instante em que o contexto do Spring sobe, que é quando
 * o data.sql calcula as datas do seed a partir de CURRENT_DATE. Assim a "data de hoje" dos
 * services e dos testes continua igual à do seed mesmo se a suíte atravessar a meia-noite.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockTestConfig {

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(Instant.now(), ZoneId.systemDefault());
    }
}
