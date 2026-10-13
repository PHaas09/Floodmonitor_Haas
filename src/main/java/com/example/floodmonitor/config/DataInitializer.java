package com.example.floodmonitor.config;

import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.repository.StationRepository;
import com.example.floodmonitor.service.SimulationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Legt beim Start die Messstationen an und erzeugt eine Messhistorie. */
@Component
public class DataInitializer implements ApplicationRunner {

    private final StationRepository stationRepository;
    private final SimulationService simulationService;
    private final int historyHours;

    public DataInitializer(StationRepository stationRepository, SimulationService simulationService,
                           @Value("${simulation.history-hours:12}") int historyHours) {
        this.stationRepository = stationRepository;
        this.simulationService = simulationService;
        this.historyHours = historyHours;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Wasserstaende in cm: normal, Warnwert, kritischer Wert; Durchfluss in m³/s
        stationRepository.save(new Station("ST-001", "Donaubrücke Linz", "Donau",
                new Location(48.3069, 14.2858), 350, 550, 700, 1500, true));
        stationRepository.save(new Station("ST-002", "Reichsbrücke Wien", "Donau",
                new Location(48.2296, 16.4100), 300, 500, 650, 1900, true));
        stationRepository.save(new Station("ST-003", "Staatsbrücke Salzburg", "Salzach",
                new Location(47.8017, 13.0464), 250, 290, 340, 180, true));
        stationRepository.save(new Station("ST-004", "Innbrücke Innsbruck", "Inn",
                new Location(47.2692, 11.3933), 220, 260, 310, 160, true));
        // deaktiviert: bekommt keine Messungen, Warnstufe UNKNOWN
        stationRepository.save(new Station("ST-005", "Hauptbrücke Graz", "Mur",
                new Location(47.0717, 15.4342), 200, 300, 380, 120, false));

        if (historyHours > 0) {
            simulationService.createHistory(historyHours);
        }
    }
}
