package com.example.floodmonitor;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.service.MeasurementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * Tests der REST-Schnittstelle. Die Simulation ist ausgeschaltet,
 * jeder Test legt seine Messungen selbst an -> die Ergebnisse sind immer gleich.
 */
@SpringBootTest(properties = {"simulation.enabled=false", "simulation.history-hours=0"})
@AutoConfigureMockMvc
class FloodMonitorApiTest {

    private static final RequestPostProcessor LOGIN = httpBasic("viewer", "viewer123");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MeasurementService measurementService;

    @Autowired
    private MeasurementRepository measurementRepository;

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    @BeforeEach
    void setUp() {
        measurementRepository.deleteAll();
    }

    /** Legt eine Messung an. Die Warnstufe berechnet der Server. */
    private void addMeasurement(String stationId, Instant time, double waterLevel) {
        measurementService.save(new Measurement(stationId, time, waterLevel, 1500, 2.0, 14.0, 90,
                StationStatus.ONLINE, null));
    }

    // 1. Abruf aller Stationen als JSON
    @Test
    void allStationsAsJson() throws Exception {
        mvc.perform(get("/api/v1/stations").accept(MediaType.APPLICATION_JSON).with(LOGIN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.count").value(5))
                .andExpect(jsonPath("$.stations[0].id").value("ST-001"));
    }

    // 2. Abruf aller Stationen als XML
    @Test
    void allStationsAsXml() throws Exception {
        mvc.perform(get("/api/v1/stations").accept(MediaType.APPLICATION_XML).with(LOGIN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("count(/stations/station)").number(5.0))
                .andExpect(xpath("/stations/station[1]/id").string("ST-001"));
    }

    // 3. Abruf einer existierenden Station
    @Test
    void existingStation() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001").with(LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ST-001"))
                .andExpect(jsonPath("$.river").value("Donau"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    // 4. Abruf einer unbekannten Station
    @Test
    void unknownStation() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-999").with(LOGIN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Station ST-999 wurde nicht gefunden"))
                .andExpect(jsonPath("$.path").value("/api/v1/stations/ST-999"));
    }

    // 5. Abruf des aktuellsten Messwerts
    @Test
    void latestMeasurement() throws Exception {
        addMeasurement("ST-001", now.minus(20, ChronoUnit.MINUTES), 300.0);
        addMeasurement("ST-001", now.minus(10, ChronoUnit.MINUTES), 360.0);

        mvc.perform(get("/api/v1/stations/ST-001/measurements/latest").with(LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waterLevel").value(360.0))
                .andExpect(jsonPath("$.warningLevel").value("NORMAL"));
    }

    // 6. Filterung nach Zeitraum
    @Test
    void filterByTimeRange() throws Exception {
        addMeasurement("ST-002", now.minus(3, ChronoUnit.HOURS), 301.0);
        addMeasurement("ST-002", now.minus(2, ChronoUnit.HOURS), 302.0);
        addMeasurement("ST-002", now.minus(1, ChronoUnit.HOURS), 303.0);

        mvc.perform(get("/api/v1/stations/ST-002/measurements")
                        .param("from", now.minus(150, ChronoUnit.MINUTES).toString())
                        .param("to", now.minus(30, ChronoUnit.MINUTES).toString())
                        .with(LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.measurements[0].waterLevel").value(303.0))
                .andExpect(jsonPath("$.measurements[1].waterLevel").value(302.0));
    }

    // 7. Ungueltiger Zeitraum
    @Test
    void invalidTimeRange() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001/measurements")
                        .param("from", "2026-05-01T12:00:00Z")
                        .param("to", "2026-05-01T08:00:00Z")
                        .with(LOGIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Ungültiger Zeitraum")));

        mvc.perform(get("/api/v1/stations/ST-001/measurements").param("from", "gestern").with(LOGIN))
                .andExpect(status().isBadRequest());
    }

    // 8. Warnstufe NORMAL (Warnwert ST-001 = 550 cm)
    @Test
    void warningLevelNormal() throws Exception {
        addMeasurement("ST-001", now, 549.9);
        mvc.perform(get("/api/v1/stations/ST-001/measurements/latest").with(LOGIN))
                .andExpect(jsonPath("$.warningLevel").value("NORMAL"));
    }

    // 9. Warnstufe WARNING (genau der Warnwert)
    @Test
    void warningLevelWarning() throws Exception {
        addMeasurement("ST-001", now, 550.0);
        mvc.perform(get("/api/v1/stations/ST-001/measurements/latest").with(LOGIN))
                .andExpect(jsonPath("$.warningLevel").value("WARNING"));
        mvc.perform(get("/api/v1/alerts").with(LOGIN))
                .andExpect(jsonPath("$.alerts[*].stationId", hasItem("ST-001")));
    }

    // 10. Warnstufe CRITICAL (kritischer Wert ST-002 = 650 cm)
    @Test
    void warningLevelCritical() throws Exception {
        addMeasurement("ST-002", now, 650.0);
        mvc.perform(get("/api/v1/stations/ST-002/measurements/latest").with(LOGIN))
                .andExpect(jsonPath("$.warningLevel").value("CRITICAL"));
        mvc.perform(get("/api/v1/stations").param("warningLevel", "CRITICAL").with(LOGIN))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.stations[0].id").value("ST-002"));
    }

    // 11. Nicht unterstuetztes Format
    @Test
    void unsupportedFormat() throws Exception {
        mvc.perform(get("/api/v1/stations").accept(MediaType.TEXT_PLAIN).with(LOGIN))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.status").value(406));
    }

    // 12. Maximales Limit
    @Test
    void maximumLimit() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "1000").with(LOGIN))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "1001").with(LOGIN))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "-1").with(LOGIN))
                .andExpect(status().isBadRequest());
    }

    // zusaetzlich: ohne Login -> 401
    @Test
    void withoutLogin() throws Exception {
        mvc.perform(get("/api/v1/stations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
