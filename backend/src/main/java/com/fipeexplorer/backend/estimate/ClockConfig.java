package com.fipeexplorer.backend.estimate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Bean injetável em vez de LocalDate.now() direto — testes fixam a data sem depender do relógio real. */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
