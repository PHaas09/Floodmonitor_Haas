package com.example.floodmonitor.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.Instant;

/**
 * Eine Messung einer Station.
 * timestamp: ISO-8601 (UTC), waterLevel: cm, flowRate: m³/s, rainfall: mm/h,
 * temperature: °C, batteryLevel: %.
 */
@JacksonXmlRootElement(localName = "measurement")
public record Measurement(
        String stationId,
        Instant timestamp,
        double waterLevel,
        double flowRate,
        double rainfall,
        double temperature,
        int batteryLevel,
        StationStatus status,
        WarningLevel warningLevel) {

    /** Kopie dieser Messung mit einer (vom Server berechneten) Warnstufe. */
    public Measurement withWarningLevel(WarningLevel level) {
        return new Measurement(stationId, timestamp, waterLevel, flowRate, rainfall,
                temperature, batteryLevel, status, level);
    }
}
