package com.kovanlabs.wellness;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the AI Wellness Service.
 *
 * <p>All business data (users, activities, teams, challenges, exercises,
 * AI responses) is loaded at runtime from MySQL, Health Connect, Gemini,
 * or authenticated user context.  Nothing is hardcoded here.</p>
 *
 * <p>Annotations:
 * <ul>
 *   <li>{@code @SpringBootApplication} — enables component scan, auto-configuration,
 *       and the Spring Boot application context.</li>
 *   <li>{@code @ConfigurationPropertiesScan} — discovers all
 *       {@code @ConfigurationProperties} beans automatically.</li>
 *   <li>{@code @EnableAsync} — allows {@code @Async} for non-blocking AI
 *       streaming and notification dispatch.</li>
 *   <li>{@code @EnableScheduling} — supports future scheduled health-sync
 *       jobs without requiring Quartz at this stage.</li>
 * </ul>
 * </p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
public class WellnessApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(WellnessApplication.class, args);
    }
}
