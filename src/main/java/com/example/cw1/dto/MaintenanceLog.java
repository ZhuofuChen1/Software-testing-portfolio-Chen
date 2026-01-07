package com.example.cw1.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

public class MaintenanceLog {

    @NotBlank(message = "droneId is required")
    private String droneId;
    
    @jakarta.validation.constraints.Min(value = 0, message = "flightHours must be non-negative")
    private Double flightHours;
    
    @jakarta.validation.constraints.Min(value = 0, message = "missions must be non-negative")
    private Integer missions;
    
    @jakarta.validation.constraints.Min(value = 0, message = "emergencyDiversions must be non-negative")
    private Integer emergencyDiversions;
    
    @jakarta.validation.constraints.Min(value = 0, message = "avgPayloadKg must be non-negative")
    private Double avgPayloadKg;
    
    @jakarta.validation.constraints.DecimalMin(value = "0.0", message = "batteryHealth must be between 0 and 1")
    @jakarta.validation.constraints.DecimalMax(value = "1.0", message = "batteryHealth must be between 0 and 1")
    private Double batteryHealth; // 0..1
    
    private Boolean temperatureAlerts;
    private Boolean communicationIssues;
    private String note;
    private String recordedAt;

    public String getDroneId() {
        return droneId;
    }

    public void setDroneId(String droneId) {
        this.droneId = droneId;
    }

    public Double getFlightHours() {
        return flightHours;
    }

    public void setFlightHours(Double flightHours) {
        this.flightHours = flightHours;
    }

    public Integer getMissions() {
        return missions;
    }

    public void setMissions(Integer missions) {
        this.missions = missions;
    }

    public Integer getEmergencyDiversions() {
        return emergencyDiversions;
    }

    public void setEmergencyDiversions(Integer emergencyDiversions) {
        this.emergencyDiversions = emergencyDiversions;
    }

    public Double getAvgPayloadKg() {
        return avgPayloadKg;
    }

    public void setAvgPayloadKg(Double avgPayloadKg) {
        this.avgPayloadKg = avgPayloadKg;
    }

    public Double getBatteryHealth() {
        return batteryHealth;
    }

    public void setBatteryHealth(Double batteryHealth) {
        if (batteryHealth != null) {
            this.batteryHealth = Math.max(0.0, Math.min(1.0, batteryHealth));
        } else {
            this.batteryHealth = null;
        }
    }

    public Boolean getTemperatureAlerts() {
        return temperatureAlerts;
    }

    public void setTemperatureAlerts(Boolean temperatureAlerts) {
        this.temperatureAlerts = temperatureAlerts;
    }

    public Boolean getCommunicationIssues() {
        return communicationIssues;
    }

    public void setCommunicationIssues(Boolean communicationIssues) {
        this.communicationIssues = communicationIssues;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(String recordedAt) {
        this.recordedAt = recordedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaintenanceLog that = (MaintenanceLog) o;
        return Objects.equals(droneId, that.droneId) &&
                Objects.equals(recordedAt, that.recordedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(droneId, recordedAt);
    }
}

