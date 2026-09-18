package com.kovanlabs.wellness.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the AI service (Gemini) is unavailable or returns an error.
 * Maps to HTTP 503 Service Unavailable.
 *
 * <p><b>Important:</b> callers must NOT substitute hardcoded AI responses
 * when this exception is thrown. The real error state must reach the UI.</p>
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
