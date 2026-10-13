package com.example.floodmonitor.exception;

/** Etwas wurde nicht gefunden -> 404 Not Found */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
