package com.example.cw1.service;

import com.example.cw1.dto.Drone;
import com.example.cw1.dto.DroneCapability;
import com.example.cw1.dto.MaintenanceLog;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
public class MaintenanceTelemetrySimulator {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceTelemetrySimulator.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "maintenance-simulator");
        t.setDaemon(true);
        return t;
    });

    private final MaintenanceService maintenanceService;
    private final IlpDataService ilpDataService;
    private final boolean enabled;
    private final long intervalMs;

    public MaintenanceTelemetrySimulator(MaintenanceService maintenanceService,
                                         IlpDataService ilpDataService,
                                         @Value("${maintenance.simulator.enabled:false}") boolean enabled,
                                         @Value("${maintenance.simulator.interval-ms:60000}") long intervalMs) {
        this.maintenanceService = maintenanceService;
        this.ilpDataService = ilpDataService;
        this.enabled = enabled;
        this.intervalMs = intervalMs;
    }

    @PostConstruct
    void start() {
        if (!enabled) {
            LOG.info("Maintenance telemetry simulator disabled.");
            return;
        }
        LOG.info("Starting maintenance telemetry simulator (interval={} ms)", intervalMs);
        executor.scheduleWithFixedDelay(this::emitTelemetry, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }

    private void emitTelemetry() {
        try {
            Drone[] drones = ilpDataService.getDrones();
            if (drones == null || drones.length == 0) {
                return;
            }
            List<Drone> sample = Arrays.stream(drones)
                    .filter(Objects::nonNull)
                    .limit(5)
                    .collect(Collectors.toList());

            for (Drone drone : sample) {
                MaintenanceLog log = buildLog(drone);
                maintenanceService.recordLog(log);
            }
            LOG.debug("Simulated telemetry for {} drones at {}", sample.size(), Instant.now());
        } catch (Exception e) {
            LOG.warn("Failed to emit simulated telemetry", e);
        }
    }

    private MaintenanceLog buildLog(Drone drone) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId(drone.getId());
        DroneCapability cap = drone.getCapability();
        double capacity = (cap == null || cap.getCapacity() <= 0) ? 20.0 : cap.getCapacity();

        log.setFlightHours(round(random.nextDouble(0.5, 3.5)));
        log.setMissions(random.nextInt(1, 5));
        log.setEmergencyDiversions(random.nextDouble() > 0.85 ? 1 : 0);
        log.setAvgPayloadKg(round(random.nextDouble(capacity * 0.3, capacity * 0.9)));
        log.setBatteryHealth(round(random.nextDouble(0.6, 0.95)));
        log.setTemperatureAlerts(random.nextDouble() > 0.9);
        log.setCommunicationIssues(random.nextDouble() > 0.92);
        log.setNote("Simulated telemetry tick");
        return log;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

