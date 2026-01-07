package com.example.cw1.dto;

import java.util.ArrayList;
import java.util.List;

public class MaintenancePlanRequest {

    private List<String> droneIds = new ArrayList<>();
    private List<MaintenanceLog> newLogs = new ArrayList<>();
    private boolean includeFleetInsight = true;

    public List<String> getDroneIds() {
        return droneIds;
    }

    public void setDroneIds(List<String> droneIds) {
        this.droneIds = droneIds;
    }

    public List<MaintenanceLog> getNewLogs() {
        return newLogs;
    }

    public void setNewLogs(List<MaintenanceLog> newLogs) {
        this.newLogs = newLogs;
    }

    public boolean isIncludeFleetInsight() {
        return includeFleetInsight;
    }

    public void setIncludeFleetInsight(boolean includeFleetInsight) {
        this.includeFleetInsight = includeFleetInsight;
    }
}

