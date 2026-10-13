package com.example.floodmonitor;

import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.service.SimulationService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueft die Simulationsregeln. Fester Seed (42) -> immer dieselben Zufallszahlen. */
class SimulationTest {

    private final Station station = new Station("ST-T", "Test", "Testfluss",
            new Location(48, 16), 250, 290, 340, 180, true);

    @Test
    void valuesStayRealistic() {
        Random random = new Random(42);
        Measurement m = null;
        for (int i = 0; i < 5000; i++) {
            Measurement next = SimulationService.nextMeasurement(station, m, Instant.now(), random);
            assertTrue(next.waterLevel() >= 0);
            assertTrue(next.batteryLevel() >= 0 && next.batteryLevel() <= 100);
            if (m != null) {
                assertTrue(Math.abs(next.waterLevel() - m.waterLevel()) <= 15.1);   // kein Sprung
            }
            m = next;
        }
    }

    @Test
    void heavyRainRaisesWaterLevel() {
        Measurement before = new Measurement("ST-T", Instant.now(), 250, 180, 40, 15, 90, StationStatus.ONLINE, null);
        Measurement after = SimulationService.nextMeasurement(station, before, Instant.now(), new Random(42));
        assertTrue(after.waterLevel() > before.waterLevel());
    }

    @Test
    void lowBatteryMeansMaintenance() {
        Measurement before = new Measurement("ST-T", Instant.now(), 250, 180, 0, 15, 10, StationStatus.ONLINE, null);
        Measurement after = SimulationService.nextMeasurement(station, before, Instant.now(), new Random(42));
        assertEquals(StationStatus.MAINTENANCE, after.status());
    }
}
