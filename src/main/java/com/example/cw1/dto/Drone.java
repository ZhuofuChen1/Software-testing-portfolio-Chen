package com.example.cw1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Drone {

    private String id;
    private String name;

    @JsonProperty("capability")
    private DroneCapability capability;

    @JsonProperty("weeklyAvailabilities")
    private List<DroneWeeklyAvailability> weeklyAvailabilities;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public DroneCapability getCapability() {
        return capability;
    }
    public void setCapability(DroneCapability capability) {
        this.capability = capability;
    }

    public List<DroneWeeklyAvailability> getWeeklyAvailabilities() {
        return weeklyAvailabilities;
    }
    public void setWeeklyAvailabilities(List<DroneWeeklyAvailability> weeklyAvailabilities) {
        this.weeklyAvailabilities = weeklyAvailabilities;
    }
}

