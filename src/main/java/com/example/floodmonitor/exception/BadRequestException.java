package com.example.floodmonitor.exception;

/** Ungueltige Eingabe (Datum, Zeitraum, Warnstufe, limit) -> 400 Bad Request */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
