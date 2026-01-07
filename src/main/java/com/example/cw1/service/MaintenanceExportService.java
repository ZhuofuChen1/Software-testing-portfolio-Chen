package com.example.cw1.service;

import com.example.cw1.dto.MaintenanceLog;
import com.example.cw1.dto.MaintenancePlan;
import com.example.cw1.dto.MaintenancePlanResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MaintenanceExportService {

    @Autowired
    private MaintenanceService maintenanceService;

    private final ObjectMapper mapper = new ObjectMapper();

    public String exportAsJson() {
        try {
            MaintenancePlanResponse response = maintenanceService.fleetSummary();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to export JSON", e);
        }
    }

    public String exportAsCsv() {
        MaintenancePlanResponse response = maintenanceService.fleetSummary();
        List<MaintenancePlan> plans = response.getPlans();
        
        if (plans == null || plans.isEmpty()) {
            return "droneId,riskLevel,riskScore,hoursUntilService,missionBuffer,recommendation\n";
        }

        StringBuilder csv = new StringBuilder();
        csv.append("droneId,riskLevel,riskScore,hoursUntilService,missionBuffer,recommendation,contributingFactors\n");

        for (MaintenancePlan plan : plans) {
            csv.append(escapeCsv(plan.getDroneId())).append(",");
            csv.append(escapeCsv(plan.getRiskLevel())).append(",");
            csv.append(plan.getRiskScore()).append(",");
            csv.append(plan.getHoursUntilService()).append(",");
            csv.append(plan.getMissionBuffer()).append(",");
            csv.append(escapeCsv(plan.getRecommendation())).append(",");
            
            String factors = String.join("; ", plan.getContributingFactors());
            csv.append(escapeCsv(factors)).append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

