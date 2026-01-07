package com.example.cw1.dto;

import java.util.List;

public class DeliveryPathResponse {

    private double totalCost;
    private int totalMoves;
    private List<DronePathDto> dronePaths;
    private MaintenancePlan maintenancePlan;

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public int getTotalMoves() {
        return totalMoves;
    }

    public void setTotalMoves(int totalMoves) {
        this.totalMoves = totalMoves;
    }

    public List<DronePathDto> getDronePaths() {
        return dronePaths;
    }

    public void setDronePaths(List<DronePathDto> dronePaths) {
        this.dronePaths = dronePaths;
    }

    public MaintenancePlan getMaintenancePlan() {
        return maintenancePlan;
    }

    public void setMaintenancePlan(MaintenancePlan maintenancePlan) {
        this.maintenancePlan = maintenancePlan;
    }
}
