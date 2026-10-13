package com.example.floodmonitor;

import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.service.WarningLevelCalculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Grenzwerte der Warnstufen (Warnwert 300 cm, kritischer Wert 400 cm). */
class WarningLevelCalculatorTest {

    private final Station station = new Station("ST-T", "Test", "Testfluss",
            new Location(48, 16), 200, 300, 400, 100, true);
    private final WarningLevelCalculator calculator = new WarningLevelCalculator();

    @Test
    void normal() {
        assertEquals(WarningLevel.NORMAL, calculator.calculate(station, 299.9));
    }

    @Test
    void warning() {
        assertEquals(WarningLevel.WARNING, calculator.calculate(station, 300));
        assertEquals(WarningLevel.WARNING, calculator.calculate(station, 399.9));
    }

    @Test
    void critical() {
        assertEquals(WarningLevel.CRITICAL, calculator.calculate(station, 400));
    }

    @Test
    void unknown() {
        assertEquals(WarningLevel.UNKNOWN, calculator.calculate(station, -1));
        assertEquals(WarningLevel.UNKNOWN, calculator.current(station, null));
    }
}
