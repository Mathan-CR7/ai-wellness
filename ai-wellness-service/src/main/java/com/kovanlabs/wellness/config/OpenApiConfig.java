package com.kovanlabs.wellness.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 documentation configuration.
 *
 * <p>The server URL is injected from the {@code app.openapi.server-url} property
 * so it works correctly across dev, staging, and prod environments without
 * any hardcoded URLs.</p>
 *
 * <p>The Bearer token security scheme enables testing JWT-protected endpoints
 * directly from the Swagger UI at {@code /swagger-ui.html}.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Server URL injected from application config — NOT hardcoded.
     * Defaults to localhost:8080 for local development via application.yml.
     */
    @Value("${app.openapi.server-url}")
    private String serverUrl;

    @Bean
    public OpenAPI wellnessOpenAPI() {
        // ------------------------------------------------------------------
        // JWT Bearer security scheme — lets testers supply the token
        // directly in the Swagger UI "Authorize" dialog.
        // ------------------------------------------------------------------
        final String securitySchemeName = "bearerAuth";
        SecurityScheme jwtScheme = new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste your JWT access token here (without 'Bearer ' prefix).");

        return new OpenAPI()
                .info(new Info()
                        .title("AI Wellness Service API")
                        .version("1.0.0")
                        .description("""
                                AI-powered physical wellness backend.
                                
                                All data is dynamic — sourced from Health Connect, MySQL,
                                authenticated user context, and Gemini.
                                No hardcoded business data exists in this API.
                                """)
                        .contact(new Contact()
                                .name("Wellness Platform Team"))
                        .license(new License()
                                .name("Private — Internal Use Only")))
                .servers(List.of(
                        new Server()
                                .url(serverUrl)
                                .description("Current environment server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, jwtScheme));
    }
}
