package com.example.cw1.dto;

import java.util.List;

public class DeliveryFlightDto {

    private int deliveryId;
    private List<Position> flightPath;

    public int getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(int deliveryId) {
        this.deliveryId = deliveryId;
    }

    public List<Position> getFlightPath() {
        return flightPath;
    }

    public void setFlightPath(List<Position> flightPath) {
        this.flightPath = flightPath;
    }
}
