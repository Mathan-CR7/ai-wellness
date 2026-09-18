package com.kovanlabs.wellness.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the AI service (Gemini) does not respond within the configured timeout.
 * Maps to HTTP 504 Gateway Timeout.
 *
 * <p><b>No fallback is provided.</b> The real timeout state must reach the client
 * so the user can retry or be informed of the degraded service.</p>
 */
@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
public class AiTimeoutException extends RuntimeException {

    public AiTimeoutException(String message) {
        super(message);
    }

    public AiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
