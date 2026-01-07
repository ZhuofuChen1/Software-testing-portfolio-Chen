package com.example.cw1.controller;

import com.example.cw1.dto.MaintenanceLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

        MvcResult result = mockMvc.perform(post("/api/v1/maintenance/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(log)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.droneId").exists())
                .andExpect(jsonPath("$.riskScore").exists())
                .andExpect(jsonPath("$.riskLevel").exists())
                .andExpect(jsonPath("$.hoursUntilService").exists())
                .andExpect(jsonPath("$.missionBuffer").exists())
                .andExpect(jsonPath("$.recommendation").exists())
                .andReturn();
    }
}

