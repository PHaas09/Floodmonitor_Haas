package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Station;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Speichert die Stationen im Arbeitsspeicher. */
@Repository
public class StationRepository {

    private final Map<String, Station> stations = new LinkedHashMap<>();

    public synchronized void save(Station station) {
        stations.put(station.getId(), station);
    }

    public synchronized List<Station> findAll() {
        return new ArrayList<>(stations.values());
    }

    public synchronized Optional<Station> findById(String id) {
        return Optional.ofNullable(stations.get(id));
    }
}
