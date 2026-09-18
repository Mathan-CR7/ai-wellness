package com.kovanlabs.wellness.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds all {@code app.*} properties from application.yml / environment variables
 * into a single, type-safe configuration record.
 *
 * <p><b>Why a record?</b> Records are immutable by default in Java 21, which
 * makes configuration safe from accidental mutation at runtime.</p>
 *
 * <p>All sensitive values (JWT secret, DB password, API key) must be supplied
 * via environment variables — they are never hardcoded here.</p>
 *
 * <p>Nested components:
 * <ul>
 *   <li>{@link Jwt}       — JWT signing + expiry settings</li>
 *   <li>{@link Cors}      — CORS allowed origins</li>
 *   <li>{@link Activity}  — Configurable activity business thresholds</li>
 *   <li>{@link Ai}        — Spring AI / Gemini tuning params</li>
 * </ul>
 * </p>
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        Activity activity,
        Ai ai
) {

    /**
     * JWT configuration.
     *
     * @param secret         HS256/HS512 signing secret — injected from env var JWT_SECRET.
     * @param expirationMs   Access-token lifetime in milliseconds.
     * @param refreshExpMs   Refresh-token lifetime in milliseconds (future use).
     */
    public record Jwt(
            String secret,
            long expirationMs,
            long refreshExpMs
    ) {}

    /**
     * CORS configuration.
     *
     * @param allowedOrigins Comma-separated list of allowed frontend origins.
     *                       Injected from env var ALLOWED_ORIGINS.
     */
    public record Cors(
            String allowedOrigins
    ) {}

    /**
     * Activity-domain configuration.
     * These are configurable thresholds, NOT hardcoded business data.
     * They define application behaviour (e.g., minimum valid step count)
     * rather than actual user data.
     *
     * @param minValidSteps      Minimum steps to consider a sync record valid.
     * @param maxDailySteps      Sanity cap — steps above this are flagged for review.
     * @param summaryWeekDays    How many past days to include in a weekly summary.
     */
    public record Activity(
            int minValidSteps,
            int maxDailySteps,
            int summaryWeekDays
    ) {}

    /**
     * Spring AI / Gemini configuration.
     *
     * @param chatHistoryLimit   Max messages from conversation history sent to Gemini.
     * @param streamingEnabled   Toggle AI response streaming.
     * @param systemPromptPrefix A configurable system-level prompt prefix (not a
     *                           hardcoded AI response — just a behaviour directive).
     */
    public record Ai(
            int chatHistoryLimit,
            boolean streamingEnabled,
            String systemPromptPrefix
    ) {}
}
