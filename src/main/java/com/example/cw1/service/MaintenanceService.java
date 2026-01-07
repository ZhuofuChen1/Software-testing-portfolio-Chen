package com.example.cw1.service;

import com.example.cw1.dto.Drone;
import com.example.cw1.dto.DroneCapability;
import com.example.cw1.dto.MaintenanceInsight;
import com.example.cw1.dto.MaintenanceLog;
import com.example.cw1.dto.MaintenancePlan;
import com.example.cw1.dto.MaintenancePlanRequest;
import com.example.cw1.dto.MaintenancePlanResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MaintenanceService {

    private final IlpDataService ilpDataService;
    private final ObjectMapper mapper;
    private final Path storePath;

    public MaintenanceService(IlpDataService ilpDataService) {
        this.ilpDataService = ilpDataService;
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();

        try {
            Path storageDir = Paths.get("storage");
            Files.createDirectories(storageDir);
            this.storePath = storageDir.resolve("maintenance-log.json");
            if (!Files.exists(storePath)) {
                Files.writeString(storePath, "{}", StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to prepare maintenance storage", e);
        }
    }

    public synchronized MaintenancePlan recordLog(MaintenanceLog log) {
        if (log == null || log.getDroneId() == null || log.getDroneId().isBlank()) {
            throw new IllegalArgumentException("droneId is required");
        }
        ensureTimestamp(log);

        Map<String, List<MaintenanceLog>> store = loadStore();
        store.computeIfAbsent(log.getDroneId(), k -> new ArrayList<>()).add(log);
        persist(store);

        Map<String, Drone> index = indexDrones(ilpDataService.getDrones());
        MaintenancePlan plan = buildPlan(log.getDroneId(), store.get(log.getDroneId()), index.get(log.getDroneId()));
        
        // Alert mechanism: log high-risk alerts
        if ("HIGH".equalsIgnoreCase(plan.getRiskLevel())) {
            logHighRiskAlert(plan);
        }
        
        return plan;
    }

    private void logHighRiskAlert(MaintenancePlan plan) {
        String alertMessage = String.format(
            "[HIGH RISK ALERT] Drone %s: Risk Score %.1f/100, Hours Until Service: %.1f, Recommendation: %s",
            plan.getDroneId(),
            plan.getRiskScore(),
            plan.getHoursUntilService(),
            plan.getRecommendation()
        );
        System.out.println(alertMessage);
        
        // Log contributing factors
        if (!plan.getContributingFactors().isEmpty()) {
            System.out.println("  Contributing Factors: " + String.join(", ", plan.getContributingFactors()));
        }
    }

    public synchronized MaintenancePlanResponse plan(MaintenancePlanRequest request) {
        Map<String, List<MaintenanceLog>> store = loadStore();

        if (request != null && request.getNewLogs() != null && !request.getNewLogs().isEmpty()) {
            for (MaintenanceLog log : request.getNewLogs()) {
                if (log == null || log.getDroneId() == null || log.getDroneId().isBlank()) {
                    continue;
                }
                ensureTimestamp(log);
                store.computeIfAbsent(log.getDroneId(), k -> new ArrayList<>()).add(log);
            }
            persist(store);
        }

        Map<String, Drone> droneIndex = indexDrones(ilpDataService.getDrones());
        List<String> targets = determineTargets(request, store.keySet(), droneIndex.keySet());

        List<MaintenancePlan> plans = new ArrayList<>();
        for (String droneId : targets) {
            List<MaintenanceLog> logs = store.getOrDefault(droneId, Collections.emptyList());
            MaintenancePlan plan = buildPlan(droneId, logs, droneIndex.get(droneId));
            plans.add(plan);
            
            // Alert mechanism: log high-risk alerts during batch planning
            if ("HIGH".equalsIgnoreCase(plan.getRiskLevel())) {
                logHighRiskAlert(plan);
            }
        }

        MaintenancePlanResponse response = new MaintenancePlanResponse();
        response.setPlans(plans);

        boolean includeInsight = request == null || request.isIncludeFleetInsight();
        if (includeInsight) {
            response.setInsight(buildInsight(plans));
        }

        return response;
    }

    public MaintenancePlanResponse fleetSummary() {
        MaintenancePlanRequest req = new MaintenancePlanRequest();
        req.setIncludeFleetInsight(true);
        return plan(req);
    }

    public synchronized MaintenancePlan snapshot(String droneId) {
        if (droneId == null || droneId.isBlank()) {
            return null;
        }
        Map<String, List<MaintenanceLog>> store = loadStore();
        List<MaintenanceLog> logs = store.get(droneId);
        Map<String, Drone> droneIndex = indexDrones(ilpDataService.getDrones());
        return buildPlan(droneId, logs, droneIndex.get(droneId));
    }

    private MaintenancePlan buildPlan(String droneId, List<MaintenanceLog> logs, Drone drone) {
        MaintenancePlan plan = new MaintenancePlan();
        plan.setDroneId(droneId);

        List<MaintenanceLog> safeLogs = logs == null ? Collections.emptyList() : logs;
        double hours = safeLogs.stream().mapToDouble(l -> safeDouble(l.getFlightHours(), 0)).sum();
        double missions = safeLogs.stream().mapToDouble(l -> safeInt(l.getMissions(), 0)).sum();
        double emergency = safeLogs.stream().mapToDouble(l -> safeInt(l.getEmergencyDiversions(), 0)).sum();

        double payloadAverage = safeLogs.stream()
                .map(MaintenanceLog::getAvgPayloadKg)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double batteryAverage = safeLogs.stream()
                .map(MaintenanceLog::getBatteryHealth)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.85);

        boolean temperatureIssues = safeLogs.stream().anyMatch(l -> Boolean.TRUE.equals(l.getTemperatureAlerts()));
        boolean commsIssues = safeLogs.stream().anyMatch(l -> Boolean.TRUE.equals(l.getCommunicationIssues()));

        double capacity = 25.0;
        double maxMoves = 60.0;
        if (drone != null && drone.getCapability() != null) {
            DroneCapability cap = drone.getCapability();
            capacity = cap.getCapacity();
            maxMoves = cap.getMaxMoves();
        }

        if (safeLogs.isEmpty()) {
            // no telemetry yet - assume light utilization
            hours = maxMoves * 0.15;
            missions = 8;
            payloadAverage = capacity * 0.4;
            plan.getContributingFactors().add("Using inferred utilization because no telemetry logs were found");
        }

        double utilizationFactor = clamp(hours / Math.max(1.0, maxMoves), 0, 1);
        double missionFactor = clamp(missions / 30.0, 0, 1);
        double emergencyFactor = clamp(emergency / 5.0, 0, 1);
        double payloadFactor = clamp(payloadAverage / Math.max(1.0, capacity), 0, 1);
        double batteryStress = clamp(1 - batteryAverage, 0, 1);

        double riskScore = 30 * utilizationFactor
                + 20 * missionFactor
                + 15 * payloadFactor
                + 15 * emergencyFactor
                + 10 * batteryStress
                + (temperatureIssues ? 5 : 0)
                + (commsIssues ? 5 : 0);

        plan.setRiskScore(round(riskScore));
        plan.setRiskLevel(resolveRiskLevel(riskScore));

        double hoursUntilHardLimit = Math.max(0, (maxMoves * 0.9) - hours);
        plan.setHoursUntilService(round(hoursUntilHardLimit));

        int missionBuffer = (int) Math.max(0, Math.round(20 - missions * 0.5));
        plan.setMissionBuffer(missionBuffer);

        plan.setRecommendation(recommendation(plan.getRiskLevel(), hoursUntilHardLimit));
        applyFactors(plan, utilizationFactor, missionFactor, payloadFactor, emergencyFactor, batteryStress,
                temperatureIssues, commsIssues);

        return plan;
    }

    private void applyFactors(MaintenancePlan plan,
                              double utilization,
                              double mission,
                              double payload,
                              double emergency,
                              double battery,
                              boolean tempIssues,
                              boolean commsIssues) {
        if (utilization > 0.75) {
            plan.getContributingFactors().add("Sustained utilization above 75%");
        }
        if (mission > 0.65) {
            plan.getContributingFactors().add("Dense mission schedule in current window");
        }
        if (payload > 0.7) {
            plan.getContributingFactors().add("Payload levels trending near capacity");
        }
        if (emergency > 0.2) {
            plan.getContributingFactors().add("Multiple emergency diversions logged");
        }
        if (battery > 0.4) {
            plan.getContributingFactors().add("Battery health degradation detected");
        }
        if (tempIssues) {
            plan.getContributingFactors().add("Cold-chain / thermal anomaly reported");
        }
        if (commsIssues) {
            plan.getContributingFactors().add("Communication dropouts observed");
        }
    }

    private MaintenanceInsight buildInsight(List<MaintenancePlan> plans) {
        MaintenanceInsight insight = new MaintenanceInsight();
        if (plans == null || plans.isEmpty()) {
            insight.setAverageRisk(0);
            insight.setFleetSize(0);
            insight.setHighRisk(0);
            insight.setReadinessPercent(0);
            insight.getNarrative().add("No maintenance telemetry available yet.");
            return insight;
        }

        insight.setFleetSize(plans.size());

        double avg = plans.stream().mapToDouble(MaintenancePlan::getRiskScore).average().orElse(0);
        insight.setAverageRisk(round(avg));

        long highRisk = plans.stream()
                .filter(p -> "HIGH".equalsIgnoreCase(p.getRiskLevel()))
                .count();
        insight.setHighRisk((int) highRisk);

        double readiness = 100.0 * (plans.size() - highRisk) / plans.size();
        insight.setReadinessPercent(round(readiness));

        plans.stream()
                .sorted(Comparator.comparingDouble(MaintenancePlan::getRiskScore).reversed())
                .limit(3)
                .forEach(plan -> insight.getNarrative().add(
                        plan.getDroneId() + " -> " + plan.getRiskLevel()
                                + " risk, recommend: " + plan.getRecommendation()));

        if (highRisk > 0) {
            insight.getNarrative().add("Ground the flagged drones before the next ILP dispatch cycle.");
        } else {
            insight.getNarrative().add("Fleet ready for the next window; keep logging telemetry.");
        }
        return insight;
    }

    private String resolveRiskLevel(double score) {
        if (score >= 70) {
            return "HIGH";
        }
        if (score >= 40) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String recommendation(String riskLevel, double hoursUntilHardLimit) {
        switch (riskLevel) {
            case "HIGH":
                return "Ground immediately and schedule engineering review.";
            case "MEDIUM":
                return hoursUntilHardLimit < 10
                        ? "Schedule service before the next dispatch block."
                        : "Line up maintenance within the next 2 rotations.";
            default:
                return "Cleared to fly; monitor telemetry after each sortie.";
        }
    }

    private List<String> determineTargets(MaintenancePlanRequest request,
                                          Collection<String> stored,
                                          Collection<String> fleet) {
        Set<String> targets = new HashSet<>();
        if (request != null && request.getDroneIds() != null && !request.getDroneIds().isEmpty()) {
            targets.addAll(request.getDroneIds());
        }
        if (targets.isEmpty()) {
            targets.addAll(stored);
            targets.addAll(fleet);
        }
        return targets.stream()
                .filter(id -> id != null && !id.isBlank())
                .sorted()
                .collect(Collectors.toList());
    }

    private Map<String, Drone> indexDrones(Drone[] drones) {
        if (drones == null) {
            return Collections.emptyMap();
        }
        Map<String, Drone> map = new HashMap<>();
        for (Drone drone : drones) {
            if (drone == null || drone.getId() == null) {
                continue;
            }
            map.put(drone.getId(), drone);
        }
        return map;
    }

    private Map<String, List<MaintenanceLog>> loadStore() {
        if (!Files.exists(storePath)) {
            return new HashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(storePath)) {
            return mapper.readValue(reader, new TypeReference<Map<String, List<MaintenanceLog>>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    private void persist(Map<String, List<MaintenanceLog>> store) {
        try (Writer writer = Files.newBufferedWriter(storePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, store);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist maintenance logs", e);
        }
    }

    private void ensureTimestamp(MaintenanceLog log) {
        if (log.getRecordedAt() == null || log.getRecordedAt().isBlank()) {
            log.setRecordedAt(Instant.now().toString());
        }
    }

    private double safeDouble(Double value, double fallback) {
        return value == null ? fallback : value;
    }

    private double safeInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}

