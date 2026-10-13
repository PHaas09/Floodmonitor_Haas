package com.example.floodmonitor.dto;

import com.example.floodmonitor.model.Measurement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/** Historische Messwerte einer Station (neueste zuerst). */
@JacksonXmlRootElement(localName = "measurements")
public record MeasurementList(
        String stationId,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "measurement")
        List<Measurement> measurements) {

    public int getCount() {
        return measurements.size();
    }
}
