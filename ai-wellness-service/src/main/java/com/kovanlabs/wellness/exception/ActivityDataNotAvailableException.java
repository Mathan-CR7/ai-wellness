package com.kovanlabs.wellness.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when activity data is temporarily unavailable (e.g., Health Connect
 * sync has not yet run, or the external source returned an empty payload).
 *
 * <p>Maps to HTTP 503 Service Unavailable — the client should retry later
 * rather than treating this as a permanent 404 Not Found.</p>
 *
 * <p><b>Important:</b> callers must NOT substitute fake/zero activity data
 * when this exception is thrown.  The real state must propagate to the UI.</p>
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ActivityDataNotAvailableException extends RuntimeException {

    public ActivityDataNotAvailableException(String message) {
        super(message);
    }

    public ActivityDataNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
