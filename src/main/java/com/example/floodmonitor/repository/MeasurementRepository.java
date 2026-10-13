package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Measurement;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Speichert die Messungen im Arbeitsspeicher, pro Station eine Liste (aelteste zuerst).
 * synchronized, weil die Simulation und die HTTP-Anfragen gleichzeitig zugreifen.
 */
@Repository
public class MeasurementRepository {

    private final Map<String, List<Measurement>> measurements = new HashMap<>();

    public synchronized void save(Measurement measurement) {
        measurements.computeIfAbsent(measurement.stationId(), id -> new ArrayList<>()).add(measurement);
    }

    /** Kopie der Liste, damit niemand die Originaldaten veraendert. */
    public synchronized List<Measurement> findByStation(String stationId) {
        return new ArrayList<>(measurements.getOrDefault(stationId, List.of()));
    }

    public synchronized Optional<Measurement> findLatest(String stationId) {
        List<Measurement> list = measurements.get(stationId);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(list.size() - 1));
    }

    public synchronized void deleteAll() {
        measurements.clear();
    }
}
