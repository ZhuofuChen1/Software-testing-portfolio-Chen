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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

