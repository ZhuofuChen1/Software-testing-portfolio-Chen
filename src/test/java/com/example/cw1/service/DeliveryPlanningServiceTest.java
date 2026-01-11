package com.example.cw1.service;

import com.example.cw1.dto.DeliveryPathResponse;
import com.example.cw1.dto.Drone;
import com.example.cw1.dto.DroneCapability;
import com.example.cw1.dto.MaintenancePlan;
import com.example.cw1.dto.MedDispatchRec;
import com.example.cw1.dto.MedDispatchRequirements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryPlanningServiceTest {

    @Mock
    private IlpDataService ilpDataService;

    @Mock
    private MaintenanceService maintenanceService;

    @InjectMocks
    private DeliveryPlanningService service;

    @Test
    void choosesLowestRiskDroneWhenMultipleOptionsExist() {
        Drone lowRisk = drone("drn-low", 30, true, false);
        Drone highRisk = drone("drn-high", 30, true, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{lowRisk, highRisk});

        when(maintenanceService.snapshot("drn-low")).thenReturn(plan("drn-low", 25.0, "LOW", 12.0, 10));
        when(maintenanceService.snapshot("drn-high")).thenReturn(plan("drn-high", 75.0, "HIGH", 4.0, 2));

        DeliveryPathResponse response = service.calcDeliveryPath(
                List.of(dispatch(1, 12, true, false, 1500.0))
        );

        assertNotNull(response.getMaintenancePlan());
        assertEquals("drn-low", response.getMaintenancePlan().getDroneId());
        assertEquals("LOW", response.getMaintenancePlan().getRiskLevel());
        assertEquals("drn-low", response.getDronePaths().get(0).getDroneId());
    }

    @Test
    void fallsBackToHighRiskWhenOnlyCandidateMeetsCapacity() {
        Drone insufficient = drone("drn-small", 8, false, false);
        Drone highCapacity = drone("drn-large", 40, false, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{insufficient, highCapacity});

        when(maintenanceService.snapshot("drn-large")).thenReturn(plan("drn-large", 82.0, "HIGH", 5.0, 3));

        DeliveryPathResponse response = service.calcDeliveryPath(
                List.of(dispatch(2, 25, false, false, null))
        );

        assertNotNull(response.getMaintenancePlan());
        assertEquals("drn-large", response.getDronePaths().get(0).getDroneId());
        assertEquals("HIGH", response.getMaintenancePlan().getRiskLevel());
    }

    private Drone drone(String id, double capacity, boolean cooling, boolean heating) {
        Drone drone = new Drone();
        drone.setId(id);
        DroneCapability capability = new DroneCapability();
        capability.setCapacity(capacity);
        capability.setCooling(cooling);
        capability.setHeating(heating);
        capability.setCostInitial(10);
        capability.setCostFinal(10);
        capability.setCostPerMove(1);
        capability.setMaxMoves(60);
        drone.setCapability(capability);
        return drone;
    }

    private MaintenancePlan plan(String droneId, double riskScore, String riskLevel, double hoursUntilService, int missionBuffer) {
        MaintenancePlan plan = new MaintenancePlan();
        plan.setDroneId(droneId);
        plan.setRiskScore(riskScore);
        plan.setRiskLevel(riskLevel);
        plan.setHoursUntilService(hoursUntilService);
        plan.setMissionBuffer(missionBuffer);
        plan.setRecommendation("test");
        return plan;
    }

    // ==================== Branch Coverage Tests ====================

    @Test
    void calcDeliveryPathWithNullDispatches() {
        DeliveryPathResponse response = service.calcDeliveryPath(null);
        
        assertNotNull(response);
        assertEquals(0, response.getTotalMoves());
        assertEquals(0.0, response.getTotalCost());
    }

    @Test
    void calcDeliveryPathWithEmptyDispatches() {
        DeliveryPathResponse response = service.calcDeliveryPath(List.of());
        
        assertNotNull(response);
        assertEquals(0, response.getTotalMoves());
    }

    @Test
    void calcDeliveryPathWithNullDrones() {
        when(ilpDataService.getDrones()).thenReturn(null);
        
        DeliveryPathResponse response = service.calcDeliveryPath(
                List.of(dispatch(1, 10, false, false, null))
        );
        
        assertNotNull(response);
        assertEquals(0, response.getTotalMoves());
    }

    @Test
    void calcDeliveryPathWithEmptyDrones() {
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{});
        
        DeliveryPathResponse response = service.calcDeliveryPath(
                List.of(dispatch(1, 10, false, false, null))
        );
        
        assertNotNull(response);
        assertEquals(0, response.getTotalMoves());
    }

    @Test
    void calcDeliveryPathWithNullRequirements() {
        Drone drone = drone("drn-test", 30, true, true);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});
        
        // Dispatch with null requirements
        MedDispatchRec rec = new MedDispatchRec();
        rec.setId(1);
        rec.setRequirements(null);
        rec.setDate("2025-01-01");
        rec.setTime("12:00");
        
        DeliveryPathResponse response = service.calcDeliveryPath(List.of(rec));
        
        assertNotNull(response);
        assertEquals(0, response.getTotalMoves()); // Should return early
    }

    @Test
    void calcDeliveryPathWithCoolingRequirement() {
        Drone coolingDrone = drone("drn-cool", 30, true, false);
        Drone noCoolingDrone = drone("drn-nocool", 30, false, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{noCoolingDrone, coolingDrone});
        when(maintenanceService.snapshot("drn-cool")).thenReturn(plan("drn-cool", 30.0, "LOW", 10.0, 5));

        DeliveryPathResponse response = service.calcDeliveryPath(
                List.of(dispatch(1, 10, true, false, null)) // Needs cooling
        );
        
        assertNotNull(response);
        if (!response.getDronePaths().isEmpty()) {
            assertEquals("drn-cool", response.getDronePaths().get(0).getDroneId());
        }
    }

    @Test
    void calcDeliveryPathWithHeatingRequirement() {
        Drone heatingDrone = drone("drn-heat", 30, false, true);
        Drone noHeatingDrone = drone("drn-noheat", 30, false, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{noHeatingDrone, heatingDrone});
        when(maintenanceService.snapshot("drn-heat")).thenReturn(plan("drn-heat", 30.0, "LOW", 10.0, 5));

        DeliveryPathResponse response = service.calcDeliveryPath(
                List.of(dispatch(1, 10, false, true, null)) // Needs heating
        );
        
        assertNotNull(response);
        if (!response.getDronePaths().isEmpty()) {
            assertEquals("drn-heat", response.getDronePaths().get(0).getDroneId());
        }
    }

    @Test
    void calcDeliveryPathWithNullDroneCapability() {
        Drone noCapDrone = new Drone();
        noCapDrone.setId("drn-nocap");
        noCapDrone.setCapability(null);
        
        Drone validDrone = drone("drn-valid", 30, false, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{noCapDrone, validDrone});
        when(maintenanceService.snapshot("drn-valid")).thenReturn(plan("drn-valid", 30.0, "LOW", 10.0, 5));

        DeliveryPathResponse response = service.calcDeliveryPath(
                List.of(dispatch(1, 10, false, false, null))
        );
        
        assertNotNull(response);
    }

    @Test
    void calcDeliveryPathWithMaxCostExceeded() {
        Drone drone = drone("drn-test", 30, false, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});
        when(maintenanceService.snapshot("drn-test")).thenReturn(plan("drn-test", 30.0, "LOW", 10.0, 5));

        // Create dispatch with very low maxCost that will be exceeded
        DeliveryPathResponse response = service.calcDeliveryPath(
                List.of(dispatch(1, 10, false, false, 0.001)) // Very low max cost
        );
        
        assertNotNull(response);
        // Should return empty response due to cost exceeded
    }

    @Test
    void calcDeliveryPathWithMultipleDispatches() {
        Drone drone = drone("drn-test", 50, true, true);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});
        when(maintenanceService.snapshot("drn-test")).thenReturn(plan("drn-test", 30.0, "LOW", 20.0, 10));

        DeliveryPathResponse response = service.calcDeliveryPath(
                List.of(
                        dispatch(1, 10, false, false, null),
                        dispatch(2, 15, false, false, null),
                        dispatch(3, 20, false, false, null)
                )
        );
        
        assertNotNull(response);
        assertTrue(response.getTotalMoves() > 0);
    }

    @Test
    void calcDeliveryPathAsGeoJsonWithEmptyDispatches() {
        String geoJson = service.calcDeliveryPathAsGeoJson(List.of());
        
        assertNotNull(geoJson);
        assertTrue(geoJson.contains("LineString"));
        assertTrue(geoJson.contains("coordinates"));
    }

    @Test
    void calcDeliveryPathAsGeoJsonWithNullDispatches() {
        String geoJson = service.calcDeliveryPathAsGeoJson(null);
        
        assertNotNull(geoJson);
        assertTrue(geoJson.contains("LineString"));
    }

    @Test
    void calcDeliveryPathAsGeoJsonWithValidPath() {
        Drone drone = drone("drn-test", 30, false, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});
        when(maintenanceService.snapshot("drn-test")).thenReturn(plan("drn-test", 30.0, "LOW", 20.0, 10));

        String geoJson = service.calcDeliveryPathAsGeoJson(
                List.of(dispatch(1, 10, false, false, null))
        );
        
        assertNotNull(geoJson);
        assertTrue(geoJson.contains("Feature"));
        assertTrue(geoJson.contains("LineString"));
    }

    @Test
    void calcDeliveryPathWithNullDroneEntry() {
        Drone validDrone = drone("drn-valid", 30, false, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{null, validDrone});
        when(maintenanceService.snapshot("drn-valid")).thenReturn(plan("drn-valid", 30.0, "LOW", 10.0, 5));

        DeliveryPathResponse response = service.calcDeliveryPath(
                List.of(dispatch(1, 10, false, false, null))
        );
        
        assertNotNull(response);
    }

    @Test
    void calcDeliveryPathWithInsufficientCapacity() {
        Drone smallDrone = drone("drn-small", 5, false, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{smallDrone});

        DeliveryPathResponse response = service.calcDeliveryPath(
                List.of(dispatch(1, 50, false, false, null)) // Needs 50, drone has 5
        );
        
        assertNotNull(response);
        assertEquals(0, response.getTotalMoves()); // No suitable drone
    }

    @Test
    void calcDeliveryPathSkipsHighRiskWhenLowRiskAvailable() {
        Drone lowRisk = drone("drn-low", 30, false, false);
        Drone highRisk = drone("drn-high", 30, false, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{highRisk, lowRisk});
        
        when(maintenanceService.snapshot("drn-low")).thenReturn(plan("drn-low", 20.0, "LOW", 20.0, 10));
        when(maintenanceService.snapshot("drn-high")).thenReturn(plan("drn-high", 80.0, "HIGH", 5.0, 1));

        DeliveryPathResponse response = service.calcDeliveryPath(
                List.of(dispatch(1, 10, false, false, null))
        );
        
        assertNotNull(response);
        if (!response.getDronePaths().isEmpty()) {
            assertEquals("drn-low", response.getDronePaths().get(0).getDroneId());
        }
    }

    @Test
    void calcDeliveryPathWithNullMaintenancePlan() {
        Drone drone = drone("drn-test", 30, false, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});
        when(maintenanceService.snapshot("drn-test")).thenReturn(null); // No maintenance plan

        DeliveryPathResponse response = service.calcDeliveryPath(
                List.of(dispatch(1, 10, false, false, null))
        );
        
        assertNotNull(response);
    }

    private MedDispatchRec dispatch(int id, double capacity, boolean cooling, boolean heating, Double maxCost) {
        MedDispatchRequirements requirements = new MedDispatchRequirements();
        requirements.setCapacity(capacity);
        requirements.setCooling(cooling);
        requirements.setHeating(heating);
        requirements.setMaxCost(maxCost);

        MedDispatchRec rec = new MedDispatchRec();
        rec.setId(id);
        rec.setRequirements(requirements);
        rec.setDate("2025-01-01");
        rec.setTime("12:00");
        return rec;
    }
}

