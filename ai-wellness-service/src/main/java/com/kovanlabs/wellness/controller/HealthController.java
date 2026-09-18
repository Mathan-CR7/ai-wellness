package com.kovanlabs.wellness.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Lightweight health check endpoint — exposed without authentication.
 * Used by load balancers, Docker health checks, and monitoring systems
 * to confirm that the application is running.
 *
 * <p>This does NOT duplicate Spring Boot Actuator's {@code /actuator/health}
 * endpoint — it is a simple application-level ping that is always public.</p>
 */
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Application health check")
public class HealthController {

    @GetMapping
    @Operation(
            summary = "Application health check",
            description = "Returns 200 OK with a timestamp to confirm the service is running."
    )
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ai-wellness-service",
                "timestamp", Instant.now()
        ));
    }
}
