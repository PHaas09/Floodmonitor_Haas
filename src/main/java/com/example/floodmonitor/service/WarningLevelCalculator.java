package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Berechnet die Warnstufe (nur am Server).
 *
 *   Wasserstand >= kritischer Wert  -> CRITICAL
 *   Wasserstand >= Warnwert         -> WARNING
 *   sonst                           -> NORMAL
 *   ungueltiger Wert (negativ)      -> UNKNOWN
 */
@Component
public class WarningLevelCalculator {

    /** Ist die letzte Messung aelter, gilt sie nicht mehr als aktuell. */
    private static final Duration MAX_AGE = Duration.ofMinutes(5);

    public WarningLevel calculate(Station station, double waterLevel) {
        if (Double.isNaN(waterLevel) || waterLevel < 0) {
            return WarningLevel.UNKNOWN;
        }
        if (waterLevel >= station.getCriticalWaterLevel()) {
            return WarningLevel.CRITICAL;
        }
        if (waterLevel >= station.getWarningWaterLevel()) {
            return WarningLevel.WARNING;
        }
        return WarningLevel.NORMAL;
    }

    /** Aktuelle Warnstufe einer Station: UNKNOWN, wenn keine aktuelle Messung vorhanden ist. */
    public WarningLevel current(Station station, Measurement latest) {
        if (!station.isActive() || station.getStatus() == StationStatus.OFFLINE || latest == null) {
            return WarningLevel.UNKNOWN;
        }
        if (latest.timestamp().isBefore(Instant.now().minus(MAX_AGE))) {
            return WarningLevel.UNKNOWN;
        }
        return latest.warningLevel();
    }
}
