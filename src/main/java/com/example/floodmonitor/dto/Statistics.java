package com.example.floodmonitor.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Statistik der Messwerte einer Station.
 * Ohne Messungen sind min/max/Durchschnitt null.
 */
@JacksonXmlRootElement(localName = "statistics")
public record Statistics(
        String stationId,
        int measurementCount,
        Double minWaterLevel,
        Double maxWaterLevel,
        Double avgWaterLevel,
        Double avgFlowRate,
        double totalRainfall,
        int warningCount,      // Messungen mit WARNING oder CRITICAL
        int criticalCount) {   // Messungen mit CRITICAL
}
