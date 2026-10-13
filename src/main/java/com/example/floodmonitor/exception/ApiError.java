package com.example.floodmonitor.exception;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Einheitliches Fehlerformat fuer alle Fehler (JSON oder XML, nie mit Stacktrace). */
@JacksonXmlRootElement(localName = "apiError")
public record ApiError(Instant timestamp, int status, String error, String message, String path) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(), status.getReasonPhrase(), message, path);
    }
}
