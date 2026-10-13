package com.example.floodmonitor.controller;

import com.example.floodmonitor.dto.AlertList;
import com.example.floodmonitor.dto.MeasurementList;
import com.example.floodmonitor.dto.StationList;
import com.example.floodmonitor.dto.Statistics;
import com.example.floodmonitor.exception.ApiError;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.service.MeasurementService;
import com.example.floodmonitor.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Schnittstelle. Nur HTTP, die Logik steckt in den Services.
 * produces: liefert JSON (Standard) oder XML, je nach Accept-Header.
 */
@RestController
@RequestMapping(value = "/api/v1", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@ApiResponse(responseCode = "400", description = "Ungültiger Parameter",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
@ApiResponse(responseCode = "401", description = "Nicht angemeldet",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
@ApiResponse(responseCode = "404", description = "Station oder Messung nicht gefunden",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
@ApiResponse(responseCode = "406", description = "Format nicht unterstützt (nur JSON oder XML)",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
public class FloodMonitorController {

    private final StationService stationService;
    private final MeasurementService measurementService;

    public FloodMonitorController(StationService stationService, MeasurementService measurementService) {
        this.stationService = stationService;
        this.measurementService = measurementService;
    }

    @Operation(summary = "Alle Messstationen abrufen, optional gefiltert")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping("/stations")
    public StationList getStations(@RequestParam(name = "river", required = false) String river,
                                   @RequestParam(name = "isActive", required = false) Boolean isActive,
                                   @RequestParam(name = "warningLevel", required = false) String warningLevel) {
        return new StationList(stationService.getStations(river, isActive, warningLevel));
    }

    @Operation(summary = "Einzelne Station abrufen")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping("/stations/{stationId}")
    public Station getStation(@PathVariable("stationId") String stationId) {
        return stationService.getStation(stationId);
    }

    @Operation(summary = "Aktuellsten Messwert abrufen")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping("/stations/{stationId}/measurements/latest")
    public Measurement getLatest(@PathVariable("stationId") String stationId) {
        return measurementService.getLatest(stationId);
    }

    @Operation(summary = "Historische Messwerte abrufen (neueste zuerst)")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping("/stations/{stationId}/measurements")
    public MeasurementList getMeasurements(@PathVariable("stationId") String stationId,
                                           @RequestParam(name = "from", required = false) String from,
                                           @RequestParam(name = "to", required = false) String to,
                                           @RequestParam(name = "limit", required = false) Integer limit,
                                           @RequestParam(name = "warningLevel", required = false) String warningLevel) {
        return new MeasurementList(stationId, measurementService.getHistory(stationId, from, to, limit, warningLevel));
    }

    @Operation(summary = "Statistik der Messwerte abrufen")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping("/stations/{stationId}/statistics")
    public Statistics getStatistics(@PathVariable("stationId") String stationId,
                                    @RequestParam(name = "from", required = false) String from,
                                    @RequestParam(name = "to", required = false) String to) {
        return measurementService.getStatistics(stationId, from, to);
    }

    @Operation(summary = "Aktuelle Warnungen abrufen")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping("/alerts")
    public AlertList getAlerts(@RequestParam(name = "warningLevel", required = false) String warningLevel,
                               @RequestParam(name = "river", required = false) String river) {
        return new AlertList(measurementService.getAlerts(warningLevel, river));
    }
}
