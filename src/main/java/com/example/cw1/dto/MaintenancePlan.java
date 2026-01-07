package com.example.cw1.dto;

import java.util.ArrayList;
import java.util.List;

public class MaintenancePlan {

    private String droneId;
    private double riskScore;
    private String riskLevel;
    private double hoursUntilService;
    private int missionBuffer;
    private String recommendation;
    private List<String> contributingFactors = new ArrayList<>();

    public String getDroneId() {
        return droneId;
    }

    public void setDroneId(String droneId) {
        this.droneId = droneId;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getHoursUntilService() {
        return hoursUntilService;
    }

    public void setHoursUntilService(double hoursUntilService) {
        this.hoursUntilService = hoursUntilService;
    }

    public int getMissionBuffer() {
        return missionBuffer;
    }

    public void setMissionBuffer(int missionBuffer) {
        this.missionBuffer = missionBuffer;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public List<String> getContributingFactors() {
        return contributingFactors;
    }

    public void setContributingFactors(List<String> contributingFactors) {
        this.contributingFactors = contributingFactors;
    }
}

