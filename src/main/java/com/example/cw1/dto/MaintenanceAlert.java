package com.example.cw1.dto;

import java.time.Instant;

public class MaintenanceAlert {
    private String droneId;
    private String riskLevel;
    private double riskScore;
    private String recommendation;
    private String timestamp;
    private String message;

    public MaintenanceAlert() {
    }

    public MaintenanceAlert(String droneId, String riskLevel, double riskScore, String recommendation) {
        this.droneId = droneId;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.recommendation = recommendation;
        this.timestamp = Instant.now().toString();
        this.message = String.format("HIGH RISK ALERT: Drone %s requires immediate attention. Risk Score: %.1f/100. %s",
                droneId, riskScore, recommendation);
    }

    public String getDroneId() {
        return droneId;
    }

    public void setDroneId(String droneId) {
        this.droneId = droneId;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

