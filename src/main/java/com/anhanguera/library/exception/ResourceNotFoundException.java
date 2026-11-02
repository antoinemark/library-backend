package com.anhanguera.library.exception;

/**
 * Recurso inexistente. Respondida com HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
