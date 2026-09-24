package com.kovanlabs.wellness.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Flyway migration strategy.
 * Automatically runs {@code flyway.repair()} prior to {@code flyway.migrate()} on application startup.
 * This automatically resolves failed schema migration records (e.g. from previous syntax errors)
 * without manual database intervention.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
