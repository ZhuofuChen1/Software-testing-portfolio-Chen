package com.example.cw1.dto;

import java.time.LocalTime;

public class DroneAvailability {

    private int droneId;
    private String availableFrom; // "HH:mm"
    private String availableTo;   // "HH:mm"

    public int getDroneId() {
        return droneId;
    }

    public void setDroneId(int droneId) {
        this.droneId = droneId;
    }

    public String getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(String availableFrom) {
        this.availableFrom = availableFrom;
    }

    public String getAvailableTo() {
        return availableTo;
    }

    public void setAvailableTo(String availableTo) {
        this.availableTo = availableTo;
    }


    public LocalTime getAvailableFromAsLocalTime() {
        if (availableFrom == null) return null;
        return LocalTime.parse(availableFrom);
    }

    public LocalTime getAvailableToAsLocalTime() {
        if (availableTo == null) return null;
        return LocalTime.parse(availableTo);
    }
}
