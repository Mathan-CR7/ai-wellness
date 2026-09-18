package com.kovanlabs.wellness.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an authenticated principal attempts an operation they are
 * not authorised to perform (e.g., accessing another user's private activity).
 *
 * <p>Maps to HTTP 403 Forbidden.</p>
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
