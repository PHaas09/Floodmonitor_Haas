package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

/**
 * Simuliert regelmaessig neue Messwerte fuer alle aktiven Stationen.
 *
 * Regeln (jeder neue Wert baut auf dem vorherigen auf):
 *  - Regen: geht pro Messung auf 70 % zurueck, mit 2 % Wahrscheinlichkeit beginnt Starkregen (+15 bis +40 mm/h)
 *  - Wasserstand: + Regen * 0.5, - 5 % der Abweichung vom Normalwert, + kleines Rauschen;
 *                 hoechstens 15 cm Aenderung pro Messung, nie negativ
 *  - Durchfluss: steigt mit dem Wasserstand (meanFlowRate * (Wasserstand / Normalwert)^1.6)
 *  - Akku: sinkt langsam; bei 10 % oder weniger -> MAINTENANCE, wird geladen, ab 80 % wieder ONLINE
 *  - deaktivierte oder OFFLINE-Stationen bekommen keine Messungen
 */
@Service
public class SimulationService {

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final MeasurementService measurementService;
    private final boolean enabled;
    private final Random random = new Random();

    public SimulationService(StationRepository stationRepository, MeasurementRepository measurementRepository,
                             MeasurementService measurementService,
                             @Value("${simulation.enabled:true}") boolean enabled) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        this.measurementService = measurementService;
        this.enabled = enabled;
    }

    /** Wird alle simulation.interval Millisekunden aufgerufen. */
    @Scheduled(fixedRateString = "${simulation.interval:10000}", initialDelayString = "${simulation.interval:10000}")
    public void simulate() {
        if (!enabled) {
            return;   // in den Tests ist die Simulation ausgeschaltet
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        for (Station station : stationRepository.findAll()) {
            simulateStation(station, now);
        }
    }

    /** Erzeugt beim Start Messwerte fuer die letzten Stunden (alle 10 Minuten eine). */
    public void createHistory(int hours) {
        Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(Duration.ofHours(hours));
        for (int i = 0; i <= hours * 6; i++) {
            Instant time = start.plus(Duration.ofMinutes(10L * i));
            for (Station station : stationRepository.findAll()) {
                simulateStation(station, time);
            }
        }
    }

    private void simulateStation(Station station, Instant time) {
        if (!station.isActive() || station.getStatus() == StationStatus.OFFLINE) {
            return;
        }
        Measurement previous = measurementRepository.findLatest(station.getId()).orElse(null);
        measurementService.save(nextMeasurement(station, previous, time, random));
    }

    /** Berechnet aus der vorherigen Messung die naechste. Die Warnstufe setzt danach der MeasurementService. */
    public static Measurement nextMeasurement(Station station, Measurement previous, Instant time, Random random) {
        // Startwerte, falls es noch keine Messung gibt
        double oldLevel = previous != null ? previous.waterLevel() : station.getNormalWaterLevel();
        double oldRain = previous != null ? previous.rainfall() : 0;
        double oldTemperature = previous != null ? previous.temperature() : 15;
        int battery = previous != null ? previous.batteryLevel() : 100;
        StationStatus status = previous != null ? previous.status() : StationStatus.ONLINE;

        // Regen
        double rain = oldRain * 0.7;
        if (rain < 0.5) {
            rain = 0;                                // leichter Regen hoert auf
        }
        if (random.nextDouble() < 0.02) {
            rain += 15 + random.nextDouble() * 25;   // Starkregen beginnt
        }
        rain = Math.min(rain, 80);

        // Wasserstand
        double change = rain * 0.5
                - (oldLevel - station.getNormalWaterLevel()) * 0.05
                + random.nextGaussian();
        change = Math.max(-15, Math.min(15, change));
        double level = Math.max(0, oldLevel + change);

        // Durchfluss
        double flow = station.getMeanFlowRate() * Math.pow(level / station.getNormalWaterLevel(), 1.6);

        // Temperatur aendert sich nur leicht
        double temperature = oldTemperature + random.nextGaussian() * 0.2;
        temperature = Math.max(-20, Math.min(40, temperature));

        // Akku und Status
        if (status == StationStatus.MAINTENANCE) {
            battery = Math.min(100, battery + 5);
            if (battery >= 80) {
                status = StationStatus.ONLINE;
            }
        } else {
            if (random.nextDouble() < 0.3) {
                battery = Math.max(0, battery - 1);
            }
            if (battery <= 10) {
                status = StationStatus.MAINTENANCE;
            }
        }

        return new Measurement(station.getId(), time, round(level), round(flow), round(rain),
                round(temperature), battery, status, null);
    }

    private static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
