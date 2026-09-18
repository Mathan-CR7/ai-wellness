package com.kovanlabs.wellness.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised exception handler for all REST controllers.
 *
 * <p>Uses RFC 9457 Problem Details (Spring 6 {@link ProblemDetail}) to return
 * structured, machine-readable error responses.</p>
 *
 * <p>Application states are explicit and never silently converted to
 * fake-success responses:
 * <ul>
 *   <li>{@code DATA_UNAVAILABLE}   — activity or provider data is unavailable</li>
 *   <li>{@code PERMISSION_REQUIRED} — Health Connect permission not granted</li>
 *   <li>{@code UNAUTHORIZED}        — authentication failure</li>
 *   <li>{@code FORBIDDEN}           — authorisation failure</li>
 *   <li>{@code NOT_FOUND}           — resource does not exist</li>
 *   <li>{@code AI_SERVICE_UNAVAILABLE} — Gemini returned an error</li>
 *   <li>{@code VALIDATION_ERROR}    — request payload failed Bean Validation</li>
 * </ul>
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -----------------------------------------------------------------------
    // Domain exceptions
    // -----------------------------------------------------------------------

    @ExceptionHandler(ActivityDataNotAvailableException.class)
    public ResponseEntity<ProblemDetail> handleActivityDataNotAvailable(
            ActivityDataNotAvailableException ex, WebRequest request) {

        log.warn("Activity data unavailable: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setType(URI.create("https://wellness.example.com/errors/data-unavailable"));
        problem.setTitle("Activity Data Unavailable");
        problem.setDetail(ex.getMessage());
        problem.setProperty("status", "DATA_UNAVAILABLE");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(AiTimeoutException.class)
    public ResponseEntity<ProblemDetail> handleAiTimeout(
            AiTimeoutException ex, WebRequest request) {

        log.error("AI service timeout: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.GATEWAY_TIMEOUT);
        problem.setType(URI.create("https://wellness.example.com/errors/ai-timeout"));
        problem.setTitle("AI Service Timeout");
        problem.setDetail("The AI service did not respond in time. Please try again in a moment.");
        problem.setProperty("status", "AI_TIMEOUT");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(problem);
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ProblemDetail> handleAiServiceException(
            AiServiceException ex, WebRequest request) {

        log.error("AI service error: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setType(URI.create("https://wellness.example.com/errors/ai-service-unavailable"));
        problem.setTitle("AI Service Unavailable");
        problem.setDetail("The AI service is currently unavailable. Please try again later.");
        problem.setProperty("status", "AI_SERVICE_UNAVAILABLE");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create("https://wellness.example.com/errors/not-found"));
        problem.setTitle("Resource Not Found");
        problem.setDetail(ex.getMessage());
        problem.setProperty("status", "NOT_FOUND");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorized(
            UnauthorizedException ex, WebRequest request) {

        log.warn("Authorization failure: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setType(URI.create("https://wellness.example.com/errors/forbidden"));
        problem.setTitle("Access Denied");
        problem.setDetail(ex.getMessage());
        problem.setProperty("status", "FORBIDDEN");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    // -----------------------------------------------------------------------
    // Spring Security exceptions
    // -----------------------------------------------------------------------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(
            AuthenticationException ex, WebRequest request) {

        log.warn("Authentication failure: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setType(URI.create("https://wellness.example.com/errors/unauthorized"));
        problem.setTitle("Authentication Required");
        problem.setDetail("Please provide a valid authentication token.");
        problem.setProperty("status", "UNAUTHORIZED");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {

        log.warn("Access denied: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setType(URI.create("https://wellness.example.com/errors/forbidden"));
        problem.setTitle("Access Denied");
        problem.setDetail("You do not have permission to access this resource.");
        problem.setProperty("status", "FORBIDDEN");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    // -----------------------------------------------------------------------
    // Validation exceptions
    // -----------------------------------------------------------------------

    /**
     * Handles {@code @Valid} / {@code @Validated} failures on request bodies.
     * Returns a map of field → violation message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> violations = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            violations.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("https://wellness.example.com/errors/validation-error"));
        problem.setTitle("Validation Error");
        problem.setDetail("One or more request fields failed validation.");
        problem.setProperty("status", "VALIDATION_ERROR");
        problem.setProperty("violations", violations);
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("https://wellness.example.com/errors/validation-error"));
        problem.setTitle("Constraint Violation");
        problem.setDetail(ex.getMessage());
        problem.setProperty("status", "VALIDATION_ERROR");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // -----------------------------------------------------------------------
    // Business rule violations
    // -----------------------------------------------------------------------

    /**
     * Handles duplicate email registration or any explicit business rule violation.
     * Returns 409 Conflict instead of the generic 500.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Business rule violation: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setType(URI.create("https://wellness.example.com/errors/conflict"));
        problem.setTitle("Conflict");
        problem.setDetail(ex.getMessage());
        problem.setProperty("status", "CONFLICT");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Handles DB unique constraint violations (e.g., duplicate email at DB level).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {

        log.warn("Data integrity violation: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setType(URI.create("https://wellness.example.com/errors/conflict"));
        problem.setTitle("Conflict");
        problem.setDetail("A record with the provided data already exists.");
        problem.setProperty("status", "CONFLICT");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Handles malformed JSON or unrecognized fields in request body.
     * Returns 400 Bad Request instead of the generic 500.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {

        log.warn("Malformed request body: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("https://wellness.example.com/errors/bad-request"));
        problem.setTitle("Bad Request");
        problem.setDetail("Request body is missing, malformed, or contains unrecognized fields.");
        problem.setProperty("status", "BAD_REQUEST");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // -----------------------------------------------------------------------
    // Catch-all — unexpected exceptions
    // -----------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(
            Exception ex, WebRequest request) {

        // Log with full stack trace for unexpected errors
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(URI.create("https://wellness.example.com/errors/internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setDetail("An unexpected error occurred. Please contact support if this persists.");
        problem.setProperty("status", "INTERNAL_ERROR");
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
