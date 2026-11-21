package com.ucentral.gestionestres.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Tareas programadas (limpieza de ventanas HU026, clasificación horaria HU003) y reloj del sistema.
 * El reloj se inyecta para poder probar con fechas fijas.
 */
@Configuration
@EnableScheduling
public class TareasConfig {

    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }
}
