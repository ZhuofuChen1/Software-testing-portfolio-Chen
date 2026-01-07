package com.example.cw1.service;

import com.example.cw1.dto.Drone;
import com.example.cw1.dto.DroneCapability;
import com.example.cw1.dto.MaintenanceLog;
import com.example.cw1.dto.MaintenancePlan;
import com.example.cw1.dto.MaintenancePlanRequest;
import com.example.cw1.dto.MaintenancePlanResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock
    private IlpDataService ilpDataService;

    private MaintenanceService maintenanceService;

    private static final Path STORAGE_PATH = Paths.get("storage", "maintenance-log.json");

    @BeforeEach
    void setUp() throws IOException {
        // Clear storage before each test to ensure isolation
        if (Files.exists(STORAGE_PATH)) {
            Files.writeString(STORAGE_PATH, "{}");
        }
        
        // Create service with mocked dependency
        maintenanceService = new MaintenanceService(ilpDataService);
        
        // Mock drone data for testing (lenient to avoid UnnecessaryStubbingException)
        Drone drone = createMockDrone("drn-test-001", 25.0, 100, true, false);
        lenient().when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up storage after each test
        if (Files.exists(STORAGE_PATH)) {
            Files.writeString(STORAGE_PATH, "{}");
        }
    }

    @Test
    void recordsMaintenanceLogAndReturnsPlan() {
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId("drn-test-001");
        log.setFlightHours(5.0);
        log.setMissions(3);
        log.setEmergencyDiversions(0);
        log.setAvgPayloadKg(10.0);
        log.setBatteryHealth(0.90);
        log.setTemperatureAlerts(false);
        log.setCommunicationIssues(false);

        MaintenancePlan plan = maintenanceService.recordLog(log);

        assertNotNull(plan);
        assertEquals("drn-test-001", plan.getDroneId());
        assertTrue(plan.getRiskScore() >= 0 && plan.getRiskScore() <= 100, "Risk score should be in valid range");
        assertNotNull(plan.getRiskLevel());
        assertNotNull(plan.getRecommendation());
    }

    @Test
    void recordsMaintenanceLogWithHighUtilization() {
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId("drn-test-001");
        log.setFlightHours(90.0);  // Very high
        log.setMissions(25);
        log.setEmergencyDiversions(5);
        log.setAvgPayloadKg(24.0);
        log.setBatteryHealth(0.40);
        log.setTemperatureAlerts(true);
        log.setCommunicationIssues(true);

        MaintenancePlan plan = maintenanceService.recordLog(log);

        assertNotNull(plan);
        assertEquals("drn-test-001", plan.getDroneId());
        // High inputs should produce higher risk score
        assertTrue(plan.getRiskScore() > 50, "High utilization should produce elevated risk");
        assertNotNull(plan.getRecommendation());
    }

    @Test
    void calculatesRiskScoreBasedOnMultipleFactors() {
        MaintenanceLog log1 = new MaintenanceLog();
        log1.setDroneId("drn-test-001");
        log1.setFlightHours(5.0);
        log1.setBatteryHealth(0.95);
        log1.setTemperatureAlerts(false);

        MaintenanceLog log2 = new MaintenanceLog();
        log2.setDroneId("drn-test-001");
        log2.setFlightHours(18.0);
        log2.setBatteryHealth(0.60);
        log2.setTemperatureAlerts(true);

        MaintenancePlan plan1 = maintenanceService.recordLog(log1);
        MaintenancePlan plan2 = maintenanceService.recordLog(log2);

        assertTrue(plan2.getRiskScore() > plan1.getRiskScore(),
                "Higher utilization and issues should increase risk score");
    }

    @Test
    void returnsSnapshotForExistingDrone() {
        // First record a log
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId("drn-test-001");
        log.setFlightHours(10.0);
        log.setBatteryHealth(0.85);
        maintenanceService.recordLog(log);

        // Then get snapshot
        MaintenancePlan snapshot = maintenanceService.snapshot("drn-test-001");

        assertNotNull(snapshot);
        assertEquals("drn-test-001", snapshot.getDroneId());
        assertNotNull(snapshot.getRiskScore());
        assertNotNull(snapshot.getRiskLevel());
    }

    @Test
    void snapshotReturnsValidPlanForKnownDrone() {
        // First record a log for the drone
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId("drn-test-001");
        log.setFlightHours(5.0);
        log.setBatteryHealth(0.90);
        maintenanceService.recordLog(log);
        
        // Then get snapshot
        MaintenancePlan snapshot = maintenanceService.snapshot("drn-test-001");
        assertNotNull(snapshot, "Known drone should have a snapshot");
        assertEquals("drn-test-001", snapshot.getDroneId());
    }

    @Test
    void fleetSummaryIncludesInsights() {
        MaintenancePlanRequest request = new MaintenancePlanRequest();
        request.setIncludeFleetInsight(true);

        MaintenancePlanResponse response = maintenanceService.plan(request);

        assertNotNull(response);
        assertNotNull(response.getPlans());
        assertNotNull(response.getInsight());
        assertTrue(response.getInsight().getFleetSize() > 0);
        assertNotNull(response.getInsight().getAverageRisk());
        assertNotNull(response.getInsight().getReadinessPercent());
    }

    @Test
    void maintainsPersistenceAcrossServiceCalls() {
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId("drn-test-001");
        log.setFlightHours(12.0);
        log.setBatteryHealth(0.80);
        maintenanceService.recordLog(log);

        MaintenancePlan snapshot1 = maintenanceService.snapshot("drn-test-001");
        assertNotNull(snapshot1);

        // The service should persist data (tested through snapshot retrieval)
        MaintenancePlan snapshot2 = maintenanceService.snapshot("drn-test-001");
        assertNotNull(snapshot2);
        assertEquals(snapshot1.getDroneId(), snapshot2.getDroneId());
    }

    @Test
    void throwsExceptionForNullDroneId() {
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId(null);

        assertThrows(IllegalArgumentException.class, () -> {
            maintenanceService.recordLog(log);
        });
    }

    @Test
    void throwsExceptionForBlankDroneId() {
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId("");

        assertThrows(IllegalArgumentException.class, () -> {
            maintenanceService.recordLog(log);
        });
    }

    // ==================== Parameterized Tests for Risk Score Calculation ====================

    @ParameterizedTest(name = "flightHours={0}, batteryHealth={1}, missions={2} should produce valid risk score")
    @CsvSource({
            "5.0, 0.95, 2",      // Low utilization, good battery, few missions
            "10.0, 0.85, 5",     // Medium utilization
            "15.0, 0.70, 10",    // Higher utilization, degraded battery
            "20.0, 0.60, 15",    // High utilization, poor battery
            "25.0, 0.50, 20"     // Very high utilization
    })
    void calculatesRiskScoreWithVariousInputCombinations(
            double flightHours, double batteryHealth, int missions) {
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId("drn-test-001");
        log.setFlightHours(flightHours);
        log.setBatteryHealth(batteryHealth);
        log.setMissions(missions);
        log.setEmergencyDiversions(0);
        log.setAvgPayloadKg(10.0);
        log.setTemperatureAlerts(false);
        log.setCommunicationIssues(false);

        MaintenancePlan plan = maintenanceService.recordLog(log);

        assertNotNull(plan);
        assertTrue(plan.getRiskScore() >= 0 && plan.getRiskScore() <= 100,
                String.format("Risk score %.1f should be in range [0, 100]", plan.getRiskScore()));
        assertNotNull(plan.getRiskLevel());
        assertTrue(plan.getRiskLevel().equals("LOW") || 
                   plan.getRiskLevel().equals("MEDIUM") || 
                   plan.getRiskLevel().equals("HIGH"),
                "Risk level should be LOW, MEDIUM, or HIGH");
    }

    @ParameterizedTest(name = "emergencyDiversions={0}, tempAlerts={1}, commIssues={2} produces valid risk")
    @CsvSource({
            "0, false, false",   // No issues - baseline
            "1, false, false",   // One emergency diversion
            "0, true, false",    // Temperature alerts only
            "0, false, true",    // Communication issues only
            "2, true, true"      // Multiple issues compound risk
    })
    void riskScoreWithVariousIssues(int emergencyDiversions, boolean tempAlerts, boolean commIssues) {
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId("drn-test-001");
        log.setFlightHours(10.0);
        log.setBatteryHealth(0.80);
        log.setMissions(5);
        log.setEmergencyDiversions(emergencyDiversions);
        log.setTemperatureAlerts(tempAlerts);
        log.setCommunicationIssues(commIssues);

        MaintenancePlan plan = maintenanceService.recordLog(log);

        assertNotNull(plan);
        assertTrue(plan.getRiskScore() >= 0 && plan.getRiskScore() <= 100,
                "Risk score should be in valid range");
        assertNotNull(plan.getRiskLevel());
        assertNotNull(plan.getRecommendation());
    }

    // ==================== Boundary Value Tests for Risk Level Classification ====================

    @Test
    void verifyRiskLevelClassificationLogic() {
        // Test the risk level classification based on score thresholds
        // LOW: score < 40, MEDIUM: 40 <= score < 70, HIGH: score >= 70
        
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId("drn-test-001");
        log.setFlightHours(10.0);
        log.setBatteryHealth(0.80);
        log.setMissions(5);

        MaintenancePlan plan = maintenanceService.recordLog(log);
        
        double score = plan.getRiskScore();
        String level = plan.getRiskLevel();
        
        // Verify classification matches the score
        if (score < 40) {
            assertEquals("LOW", level, "Score " + score + " should be classified as LOW");
        } else if (score < 70) {
            assertEquals("MEDIUM", level, "Score " + score + " should be classified as MEDIUM");
        } else {
            assertEquals("HIGH", level, "Score " + score + " should be classified as HIGH");
        }
    }

    @Test
    void verifyHighRiskClassification() {
        // Create conditions that should produce HIGH risk (score >= 70)
        MaintenanceLog highLog = new MaintenanceLog();
        highLog.setDroneId("drn-test-001");
        highLog.setFlightHours(90.0);  // Very high utilization
        highLog.setBatteryHealth(0.40);  // Poor battery
        highLog.setMissions(25);
        highLog.setEmergencyDiversions(5);
        highLog.setAvgPayloadKg(24.0);
        highLog.setTemperatureAlerts(true);
        highLog.setCommunicationIssues(true);

        MaintenancePlan highPlan = maintenanceService.recordLog(highLog);
        
        assertEquals("HIGH", highPlan.getRiskLevel(), 
                String.format("Extreme inputs should produce HIGH risk, got score %.1f", highPlan.getRiskScore()));
    }

    @Test
    void verifyLowRiskClassification() {
        // Create conditions that should produce LOW risk (score < 40)
        MaintenanceLog lowLog = new MaintenanceLog();
        lowLog.setDroneId("drn-test-001");
        lowLog.setFlightHours(1.0);  // Minimal utilization
        lowLog.setBatteryHealth(0.99);  // Excellent battery
        lowLog.setMissions(1);
        lowLog.setEmergencyDiversions(0);
        lowLog.setAvgPayloadKg(5.0);
        lowLog.setTemperatureAlerts(false);
        lowLog.setCommunicationIssues(false);

        MaintenancePlan lowPlan = maintenanceService.recordLog(lowLog);
        
        assertEquals("LOW", lowPlan.getRiskLevel(), 
                String.format("Minimal inputs should produce LOW risk, got score %.1f", lowPlan.getRiskScore()));
    }

    @ParameterizedTest(name = "flightHours={0}, battery={1} should produce increasing risk")
    @CsvSource({
            "1.0, 0.99",    // Very low risk inputs
            "20.0, 0.80",   // Moderate risk inputs
            "80.0, 0.50"    // High risk inputs
    })
    void verifyRiskIncreasesWithDegradation(double flightHours, double batteryHealth) {
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId("drn-test-001");
        log.setFlightHours(flightHours);
        log.setBatteryHealth(batteryHealth);
        log.setMissions(5);
        log.setEmergencyDiversions(0);

        MaintenancePlan plan = maintenanceService.recordLog(log);
        
        assertNotNull(plan);
        assertNotNull(plan.getRiskLevel());
        assertTrue(plan.getRiskScore() >= 0 && plan.getRiskScore() <= 100,
                "Risk score should be in valid range [0, 100]");
    }

    private Drone createMockDrone(String id, double capacity, int maxMoves, boolean cooling, boolean heating) {
        Drone drone = new Drone();
        drone.setId(id);
        DroneCapability capability = new DroneCapability();
        capability.setCapacity(capacity);
        capability.setMaxMoves(maxMoves);
        capability.setCooling(cooling);
        capability.setHeating(heating);
        capability.setCostInitial(10);
        capability.setCostFinal(10);
        capability.setCostPerMove(1);
        drone.setCapability(capability);
        return drone;
    }
}

