package com.example.floodmonitor.service;

import com.example.floodmonitor.exception.NotFoundException;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Logik fuer Stationen: abfragen und filtern. */
@Service
public class StationService {

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final WarningLevelCalculator calculator;

    public StationService(StationRepository stationRepository, MeasurementRepository measurementRepository,
                          WarningLevelCalculator calculator) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        this.calculator = calculator;
    }

    /** Alle Stationen. Jeder Filter ist optional (null = nicht filtern). */
    public List<Station> getStations(String river, Boolean isActive, String warningLevel) {
        WarningLevel level = QueryParams.parseWarningLevel(warningLevel);
        List<Station> result = new ArrayList<>();
        for (Station station : stationRepository.findAll()) {
            updateWarningLevel(station);
            if (river != null && !station.getRiver().equalsIgnoreCase(river)) {
                continue;
            }
            if (isActive != null && station.isActive() != isActive) {
                continue;
            }
            if (level != null && station.getCurrentWarningLevel() != level) {
                continue;
            }
            result.add(station);
        }
        return result;
    }

    public Station getStation(String id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Station " + id + " wurde nicht gefunden"));
        updateWarningLevel(station);
        return station;
    }

    /** Setzt die aktuelle Warnstufe der Station anhand ihrer letzten Messung. */
    private void updateWarningLevel(Station station) {
        Measurement latest = measurementRepository.findLatest(station.getId()).orElse(null);
        station.setCurrentWarningLevel(calculator.current(station, latest));
    }
}
