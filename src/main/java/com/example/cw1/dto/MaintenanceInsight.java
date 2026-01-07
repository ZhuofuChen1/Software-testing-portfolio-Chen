package com.example.cw1.dto;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceInsight {

    private double averageRisk;
    private int fleetSize;
    private int highRisk;
    private double readinessPercent;
    private List<String> narrative = new ArrayList<>();

    public double getAverageRisk() {
        return averageRisk;
    }

    public void setAverageRisk(double averageRisk) {
        this.averageRisk = averageRisk;
    }

    public int getFleetSize() {
        return fleetSize;
    }

    public void setFleetSize(int fleetSize) {
        this.fleetSize = fleetSize;
    }

    public int getHighRisk() {
        return highRisk;
    }

    public void setHighRisk(int highRisk) {
        this.highRisk = highRisk;
    }

    public double getReadinessPercent() {
        return readinessPercent;
    }

    public void setReadinessPercent(double readinessPercent) {
        this.readinessPercent = readinessPercent;
    }

    public List<String> getNarrative() {
        return narrative;
    }

    public void setNarrative(List<String> narrative) {
        this.narrative = narrative;
    }
}

