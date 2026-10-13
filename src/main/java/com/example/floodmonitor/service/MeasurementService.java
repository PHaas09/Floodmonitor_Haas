package com.example.floodmonitor.service;

import com.example.floodmonitor.dto.Alert;
import com.example.floodmonitor.dto.Statistics;
import com.example.floodmonitor.exception.NotFoundException;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Logik fuer Messwerte: speichern, Historie, Statistik und Warnungen. */
@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final StationService stationService;
    private final WarningLevelCalculator calculator;

    public MeasurementService(MeasurementRepository measurementRepository, StationService stationService,
                              WarningLevelCalculator calculator) {
        this.measurementRepository = measurementRepository;
        this.stationService = stationService;
        this.calculator = calculator;
    }

    /** Speichert eine Messung. Die Warnstufe wird HIER am Server berechnet. */
    public Measurement save(Measurement measurement) {
        Station station = stationService.getStation(measurement.stationId());
        WarningLevel level = calculator.calculate(station, measurement.waterLevel());
        Measurement saved = measurement.withWarningLevel(level);
        measurementRepository.save(saved);
        station.setStatus(saved.status());
        return saved;
    }

    public Measurement getLatest(String stationId) {
        stationService.getStation(stationId);   // 404, wenn es die Station nicht gibt
        return measurementRepository.findLatest(stationId)
                .orElseThrow(() -> new NotFoundException("Für Station " + stationId + " gibt es noch keine Messung"));
    }

    /** Historische Messwerte, neueste zuerst, mit optionalen Filtern. */
    public List<Measurement> getHistory(String stationId, String from, String to, Integer limit, String warningLevel) {
        stationService.getStation(stationId);
        Instant fromTime = QueryParams.parseTime("from", from);
        Instant toTime = QueryParams.parseTime("to", to);
        QueryParams.checkRange(fromTime, toTime);
        int maxResults = QueryParams.checkLimit(limit);
        WarningLevel level = QueryParams.parseWarningLevel(warningLevel);

        List<Measurement> all = measurementRepository.findByStation(stationId);
        Collections.reverse(all);   // neueste zuerst

        List<Measurement> result = new ArrayList<>();
        for (Measurement m : all) {
            if (isInRange(m, fromTime, toTime) && (level == null || m.warningLevel() == level)) {
                result.add(m);
                if (result.size() == maxResults) {
                    break;
                }
            }
        }
        return result;
    }

    public Statistics getStatistics(String stationId, String from, String to) {
        stationService.getStation(stationId);
        Instant fromTime = QueryParams.parseTime("from", from);
        Instant toTime = QueryParams.parseTime("to", to);
        QueryParams.checkRange(fromTime, toTime);

        int count = 0;
        double min = Double.MAX_VALUE;
        double max = 0;
        double sumWaterLevel = 0;
        double sumFlowRate = 0;
        double sumRainfall = 0;
        int warnings = 0;
        int criticals = 0;

        for (Measurement m : measurementRepository.findByStation(stationId)) {
            if (!isInRange(m, fromTime, toTime)) {
                continue;
            }
            count++;
            min = Math.min(min, m.waterLevel());
            max = Math.max(max, m.waterLevel());
            sumWaterLevel += m.waterLevel();
            sumFlowRate += m.flowRate();
            sumRainfall += m.rainfall();
            if (m.warningLevel() == WarningLevel.WARNING || m.warningLevel() == WarningLevel.CRITICAL) {
                warnings++;
            }
            if (m.warningLevel() == WarningLevel.CRITICAL) {
                criticals++;
            }
        }

        if (count == 0) {
            return new Statistics(stationId, 0, null, null, null, null, 0, 0, 0);
        }
        return new Statistics(stationId, count, min, max, round(sumWaterLevel / count),
                round(sumFlowRate / count), round(sumRainfall), warnings, criticals);
    }

    /** Aktuelle Warnungen: Stationen mit WARNING oder CRITICAL (CRITICAL zuerst). */
    public List<Alert> getAlerts(String warningLevel, String river) {
        WarningLevel filter = QueryParams.parseWarningLevel(warningLevel);
        List<Alert> alerts = new ArrayList<>();
        for (Station station : stationService.getStations(river, null, null)) {
            WarningLevel level = station.getCurrentWarningLevel();
            boolean isWarning = level == WarningLevel.WARNING || level == WarningLevel.CRITICAL;
            if (!isWarning || (filter != null && level != filter)) {
                continue;
            }
            Measurement latest = measurementRepository.findLatest(station.getId()).orElseThrow();
            alerts.add(new Alert(station.getId(), station.getName(), station.getRiver(),
                    level, latest.waterLevel(), latest.timestamp()));
        }
        alerts.sort((a, b) -> Boolean.compare(b.warningLevel() == WarningLevel.CRITICAL,
                a.warningLevel() == WarningLevel.CRITICAL));
        return alerts;
    }

    private boolean isInRange(Measurement m, Instant from, Instant to) {
        return (from == null || !m.timestamp().isBefore(from))
                && (to == null || !m.timestamp().isAfter(to));
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
