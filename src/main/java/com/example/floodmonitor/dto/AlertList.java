package com.example.floodmonitor.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/** Liste der aktuellen Warnungen. */
@JacksonXmlRootElement(localName = "alerts")
public record AlertList(
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "alert")
        List<Alert> alerts) {

    public int getCount() {
        return alerts.size();
    }
}
