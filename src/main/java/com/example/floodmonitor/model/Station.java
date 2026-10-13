package com.example.floodmonitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Eine Messstation. Wasserstaende in cm, Durchfluss in m³/s.
 */
@JacksonXmlRootElement(localName = "station")
public class Station {

    private final String id;
    private final String name;
    private final String river;
    private final Location location;
    private final double normalWaterLevel;
    private final double warningWaterLevel;
    private final double criticalWaterLevel;
    private final double meanFlowRate;      // Durchfluss bei normalem Wasserstand (fuer die Simulation)

    @JsonProperty("isActive")               // sonst wuerde Jackson das Feld "active" nennen
    private final boolean active;

    private StationStatus status;
    private WarningLevel currentWarningLevel = WarningLevel.UNKNOWN;

    public Station(String id, String name, String river, Location location,
                   double normalWaterLevel, double warningWaterLevel, double criticalWaterLevel,
                   double meanFlowRate, boolean active) {
        this.id = id;
        this.name = name;
        this.river = river;
        this.location = location;
        this.normalWaterLevel = normalWaterLevel;
        this.warningWaterLevel = warningWaterLevel;
        this.criticalWaterLevel = criticalWaterLevel;
        this.meanFlowRate = meanFlowRate;
        this.active = active;
        this.status = active ? StationStatus.ONLINE : StationStatus.OFFLINE;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRiver() { return river; }
    public Location getLocation() { return location; }
    public double getNormalWaterLevel() { return normalWaterLevel; }
    public double getWarningWaterLevel() { return warningWaterLevel; }
    public double getCriticalWaterLevel() { return criticalWaterLevel; }
    public double getMeanFlowRate() { return meanFlowRate; }
    public boolean isActive() { return active; }
    public StationStatus getStatus() { return status; }
    public WarningLevel getCurrentWarningLevel() { return currentWarningLevel; }

    public void setStatus(StationStatus status) { this.status = status; }
    public void setCurrentWarningLevel(WarningLevel level) { this.currentWarningLevel = level; }
}
