package com.example.cw1.controller;

import com.example.cw1.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * System-level API tests using MockMvc.
 * These tests verify the REST endpoints respond correctly to HTTP requests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== GET Endpoint Tests ====================

    @Test
    void getUidReturnsStudentId() throws Exception {
        mockMvc.perform(get("/api/v1/uid"))
                .andExpect(status().isOk())
                .andExpect(content().string("s2322251"));
    }

    @Test
    void getMaintenanceSummaryReturnsValidResponse() throws Exception {
        mockMvc.perform(get("/api/v1/maintenance/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.plans").isArray())
                .andExpect(jsonPath("$.insight").exists());
    }

    @Test
    void getMaintenanceExportJsonReturnsValidJson() throws Exception {
        mockMvc.perform(get("/api/v1/maintenance/export/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void getMaintenanceExportCsvReturnsCsv() throws Exception {
        mockMvc.perform(get("/api/v1/maintenance/export/csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"));
    }

    // ==================== POST Endpoint Tests ====================

    @Test
    void postMaintenanceLogWithValidDataReturnsOk() throws Exception {
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId("drn-test-api");
        log.setFlightHours(10.0);
        log.setBatteryHealth(0.85);
        log.setMissions(5);

        mockMvc.perform(post("/api/v1/maintenance/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(log)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.droneId").value("drn-test-api"))
                .andExpect(jsonPath("$.riskScore").isNumber())
                .andExpect(jsonPath("$.riskLevel").isString());
    }

    @Test
    void postMaintenanceLogWithMissingDroneIdReturnsBadRequest() throws Exception {
        MaintenanceLog log = new MaintenanceLog();
        log.setFlightHours(10.0);
        // droneId is missing

        mockMvc.perform(post("/api/v1/maintenance/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(log)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postMaintenancePlanReturnsValidResponse() throws Exception {
        mockMvc.perform(post("/api/v1/maintenance/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.plans").isArray());
    }

    // ==================== Error Handling Tests ====================

    @Test
    void postMaintenanceLogWithEmptyDroneIdReturnsBadRequest() throws Exception {
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId("");  // Empty string
        log.setFlightHours(10.0);

        mockMvc.perform(post("/api/v1/maintenance/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(log)))
                .andExpect(status().isBadRequest());
    }

    // ==================== Response Structure Tests ====================

    @Test
    void maintenancePlanResponseContainsExpectedFields() throws Exception {
        MaintenanceLog log = new MaintenanceLog();
        log.setDroneId("drn-structure-test");
        log.setFlightHours(15.0);
        log.setBatteryHealth(0.70);
        log.setMissions(10);
        log.setEmergencyDiversions(1);
        log.setTemperatureAlerts(true);

        mockMvc.perform(post("/api/v1/maintenance/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(log)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.droneId").exists())
                .andExpect(jsonPath("$.riskScore").exists())
                .andExpect(jsonPath("$.riskLevel").exists())
                .andExpect(jsonPath("$.hoursUntilService").exists())
                .andExpect(jsonPath("$.missionBuffer").exists())
                .andExpect(jsonPath("$.recommendation").exists());
    }

    // ==================== Geometry Endpoint Tests ====================

    @Test
    @DisplayName("POST /distanceTo calculates Euclidean distance correctly")
    void postDistanceToCalculatesCorrectly() throws Exception {
        String requestJson = """
            {
                "position1": {"lng": 0.0, "lat": 0.0},
                "position2": {"lng": 3.0, "lat": 4.0}
            }
            """;

        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("5.0"));  // 3-4-5 triangle
    }

    @Test
    @DisplayName("POST /distanceTo returns zero for same positions")
    void postDistanceToSamePositionReturnsZero() throws Exception {
        String requestJson = """
            {
                "position1": {"lng": 1.5, "lat": 2.5},
                "position2": {"lng": 1.5, "lat": 2.5}
            }
            """;

        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("0.0"));
    }

    @Test
    @DisplayName("POST /isCloseTo returns true for nearby positions")
    void postIsCloseToReturnsTrueForNearby() throws Exception {
        String requestJson = """
            {
                "position1": {"lng": 0.0, "lat": 0.0},
                "position2": {"lng": 0.00001, "lat": 0.00001}
            }
            """;

        mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("POST /isCloseTo returns false for distant positions")
    void postIsCloseToReturnsFalseForDistant() throws Exception {
        String requestJson = """
            {
                "position1": {"lng": 0.0, "lat": 0.0},
                "position2": {"lng": 1.0, "lat": 1.0}
            }
            """;

        mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("POST /nextPosition calculates next position at 0 degrees (East)")
    void postNextPositionEast() throws Exception {
        String requestJson = """
            {
                "start": {"lng": 0.0, "lat": 0.0},
                "angle": 0.0
            }
            """;

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lng").value(closeTo(0.0001, 0.00001)))
                .andExpect(jsonPath("$.lat").value(closeTo(0.0, 0.00001)));
    }

    @Test
    @DisplayName("POST /nextPosition calculates next position at 90 degrees (North)")
    void postNextPositionNorth() throws Exception {
        String requestJson = """
            {
                "start": {"lng": 0.0, "lat": 0.0},
                "angle": 90.0
            }
            """;

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lng").exists())
                .andExpect(jsonPath("$.lat").exists());
    }

    @Test
    @DisplayName("POST /isInRegion returns true for point inside polygon")
    void postIsInRegionInsidePolygon() throws Exception {
        // Square region with vertices at (0,0), (10,0), (10,10), (0,10)
        String requestJson = """
            {
                "position": {"lng": 5.0, "lat": 5.0},
                "region": {
                    "name": "TestSquare",
                    "vertices": [
                        {"lng": 0.0, "lat": 0.0},
                        {"lng": 10.0, "lat": 0.0},
                        {"lng": 10.0, "lat": 10.0},
                        {"lng": 0.0, "lat": 10.0}
                    ]
                }
            }
            """;

        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("POST /isInRegion returns false for point outside polygon")
    void postIsInRegionOutsidePolygon() throws Exception {
        String requestJson = """
            {
                "position": {"lng": 15.0, "lat": 15.0},
                "region": {
                    "name": "TestSquare",
                    "vertices": [
                        {"lng": 0.0, "lat": 0.0},
                        {"lng": 10.0, "lat": 0.0},
                        {"lng": 10.0, "lat": 10.0},
                        {"lng": 0.0, "lat": 10.0}
                    ]
                }
            }
            """;

        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // ==================== Query Endpoint Tests ====================

    @Test
    @DisplayName("GET /dronesWithCooling/true returns drones with cooling")
    void getDronesWithCoolingTrue() throws Exception {
        mockMvc.perform(get("/api/v1/dronesWithCooling/true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /dronesWithCooling/false returns drones without cooling")
    void getDronesWithCoolingFalse() throws Exception {
        mockMvc.perform(get("/api/v1/dronesWithCooling/false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /queryAsPath/{attribute}/{value} returns matching drones")
    void getQueryAsPathReturnsMatchingDrones() throws Exception {
        mockMvc.perform(get("/api/v1/queryAsPath/cooling/true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /query with criteria list returns matching drones")
    void postQueryWithCriteriaList() throws Exception {
        String requestJson = """
            [
                {"attribute": "cooling", "operator": "=", "value": "true"}
            ]
            """;

        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /query with multiple criteria returns filtered results")
    void postQueryWithMultipleCriteria() throws Exception {
        String requestJson = """
            [
                {"attribute": "cooling", "operator": "=", "value": "true"},
                {"attribute": "capacity", "operator": ">", "value": "0"}
            ]
            """;

        mockMvc.perform(post("/api/v1/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    // ==================== Delivery Path Tests ====================

    @Test
    @DisplayName("POST /calcDeliveryPath returns valid response")
    void postCalcDeliveryPath() throws Exception {
        String requestJson = """
            [
                {
                    "id": 1,
                    "date": "2026-01-12",
                    "time": "10:00",
                    "requirements": {
                        "capacity": 5.0,
                        "cooling": false,
                        "heating": false
                    }
                }
            ]
            """;

        mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("POST /calcDeliveryPathAsGeoJson returns GeoJSON string")
    void postCalcDeliveryPathAsGeoJson() throws Exception {
        String requestJson = """
            [
                {
                    "id": 1,
                    "date": "2026-01-12",
                    "time": "10:00",
                    "requirements": {
                        "capacity": 5.0,
                        "cooling": false,
                        "heating": false
                    }
                }
            ]
            """;

        mockMvc.perform(post("/api/v1/calcDeliveryPathAsGeoJson")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());
    }

    // ==================== Available Drones Tests ====================

    @Test
    @DisplayName("POST /queryAvailableDrones with empty list returns empty array")
    void postQueryAvailableDronesEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/queryAvailableDrones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("POST /queryAvailableDrones with valid dispatch returns drone IDs")
    void postQueryAvailableDronesWithDispatch() throws Exception {
        String requestJson = """
            [
                {
                    "id": 1,
                    "date": "2026-01-12",
                    "time": "10:00",
                    "requirements": {
                        "capacity": 1.0,
                        "cooling": false,
                        "heating": false
                    }
                }
            ]
            """;

        mockMvc.perform(post("/api/v1/queryAvailableDrones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    // ==================== Drone Details Tests ====================

    @Test
    @DisplayName("GET /droneDetails/{id} returns 404 for non-existent drone")
    void getDroneDetailsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/droneDetails/non-existent-drone-xyz"))
                .andExpect(status().isNotFound());
    }
}

