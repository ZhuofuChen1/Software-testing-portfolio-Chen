package com.example.cw1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalTime;

public class MedDispatchRec {

    private int id;
    private String date;
    private String time;

    @JsonProperty("requirements")
    private MedDispatchRequirements requirements;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDate() { return date; }

    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }

    public void setTime(String time) { this.time = time; }

    public MedDispatchRequirements getRequirements() {
        return requirements;
    }

    public void setRequirements(MedDispatchRequirements requirements) {
        this.requirements = requirements;
    }

    public double requiredCapacity() {
        return requirements != null ? requirements.getCapacity() : 0;
    }

    public boolean needCooling() {
        return requirements != null && requirements.isCooling();
    }

    public boolean needHeating() {
        return requirements != null && requirements.isHeating();
    }

    public Double getMaxCost() {
        return requirements != null ? requirements.getMaxCost() : null;
    }

    public LocalDate getDateAsLocalDate() {
        return LocalDate.parse(date);
    }

    public LocalTime getTimeAsLocalTime() {
        return LocalTime.parse(time);
    }
}

