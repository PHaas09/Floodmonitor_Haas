package com.example.floodmonitor.dto;

import com.example.floodmonitor.model.WarningLevel;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.Instant;

/** Eine aktuelle Warnung: Station mit Warnstufe WARNING oder CRITICAL. */
@JacksonXmlRootElement(localName = "alert")
public record Alert(
        String stationId,
        String stationName,
        String river,
        WarningLevel warningLevel,
        double waterLevel,
        Instant timestamp) {
}
