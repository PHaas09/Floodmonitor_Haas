package com.example.floodmonitor.service;

import com.example.floodmonitor.exception.BadRequestException;
import com.example.floodmonitor.model.WarningLevel;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/** Prueft die Query-Parameter. Fehler werden zu 400 Bad Request. */
public final class QueryParams {

    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 1000;

    private QueryParams() {
    }

    /** ISO-8601, z.B. 2026-05-01T08:00:00Z. Leer -> null (kein Filter). */
    public static Instant parseTime(String name, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim()).toInstant();
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Ungültiges Datumsformat für '" + name + "': " + value
                    + " (Beispiel: 2026-05-01T08:00:00Z)");
        }
    }

    public static void checkRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Ungültiger Zeitraum: 'from' liegt nach 'to'");
        }
    }

    /** NORMAL, WARNING, CRITICAL oder UNKNOWN. Leer -> null (kein Filter). */
    public static WarningLevel parseWarningLevel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return WarningLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Ungültige Warnstufe: " + value
                    + " (erlaubt: NORMAL, WARNING, CRITICAL, UNKNOWN)");
        }
    }

    /** limit muss zwischen 1 und 1000 liegen. Leer -> 100. */
    public static int checkLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BadRequestException("Ungültiges limit: " + limit + " (erlaubt: 1 bis " + MAX_LIMIT + ")");
        }
        return limit;
    }
}
