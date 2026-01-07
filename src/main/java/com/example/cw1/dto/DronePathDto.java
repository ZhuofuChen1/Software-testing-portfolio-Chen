package com.example.cw1.dto;

import java.util.List;

public class DronePathDto {

    private String droneId;
    private List<DeliveryFlightDto> deliveries;

    public String getDroneId() {
        return droneId;
    }

    public void setDroneId(String droneId) {
        this.droneId = droneId;
    }

    public List<DeliveryFlightDto> getDeliveries() {
        return deliveries;
    }

    public void setDeliveries(List<DeliveryFlightDto> deliveries) {
        this.deliveries = deliveries;
    }
}
