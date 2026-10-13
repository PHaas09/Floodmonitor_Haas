package com.example.floodmonitor.dto;

import com.example.floodmonitor.model.Station;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/**
 * Liste von Stationen.
 * JSON: {"stations":[...],"count":5}
 * XML:  <stations><station>...</station><station>...</station><count>5</count></stations>
 */
@JacksonXmlRootElement(localName = "stations")
public record StationList(
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "station")
        List<Station> stations) {

    public int getCount() {
        return stations.size();
    }
}
