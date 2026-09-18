package com.kovanlabs.wellness.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (user, activity, team, challenge, exercise)
 * does not exist in the database.
 *
 * <p>Maps to HTTP 404 Not Found.</p>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(String.format("%s not found with identifier: %s", resource, identifier));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
