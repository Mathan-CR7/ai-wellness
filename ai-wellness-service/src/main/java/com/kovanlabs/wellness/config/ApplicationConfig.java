package com.kovanlabs.wellness.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * General application-level configuration beans.
 *
 * <p>This class wires beans that are shared across the entire application.
 * No business data lives here — only technical infrastructure.</p>
 */
@Configuration
public class ApplicationConfig {

    private final AppProperties appProperties;

    public ApplicationConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    /**
     * BCryptPasswordEncoder — used wherever user passwords are hashed.
     * BCrypt is deliberately slow to resist brute-force attacks.
     * Strength 12 is a good production baseline; adjust via config later
     * if performance profiling warrants it.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configures Jackson's ObjectMapper with:
     * - Java 8+ date/time module so LocalDate, Instant etc. serialise correctly.
     * - Disabled WRITE_DATES_AS_TIMESTAMPS so dates appear as ISO-8601 strings.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Global CORS configuration.
     *
     * <p>Allowed origins are driven by the {@code app.cors.allowed-origins}
     * configuration property (injected from env var {@code ALLOWED_ORIGINS}).
     * Nothing is hardcoded here.</p>
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String[] origins = Arrays
                        .stream(appProperties.cors().allowedOrigins().split(","))
                        .map(String::trim)
                        .toArray(String[]::new);

                registry.addMapping("/api/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);

                // WebSocket CORS is handled separately in WebSocketConfig
            }
        };
    }
}
