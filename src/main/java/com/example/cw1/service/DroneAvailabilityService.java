package com.example.cw1.service;

import com.example.cw1.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DroneAvailabilityService {

    @Autowired
    private IlpDataService ilpDataService;

    public List<String> findAvailableDrones(List<MedDispatchRec> dispatches) {

        List<String> result = new ArrayList<>();
        Drone[] drones = ilpDataService.getDrones();

        if (drones == null) return result;

        for (Drone drone : drones) {
            boolean ok = canHandleAll(dispatches, drone);
            if (ok) {
                result.add(drone.getId());
            }
        }
        return result;
    }

    private boolean canHandleAll(List<MedDispatchRec> dispatches, Drone drone) {
        for (MedDispatchRec rec : dispatches) {
            if (!canHandleOne(rec, drone)) return false;
        }
        return true;
    }

    private boolean canHandleOne(MedDispatchRec rec, Drone drone) {

        DroneCapability cap = drone.getCapability();
        if (cap == null) return false;

        if (!cap.supportsCapacity(rec.requiredCapacity())) return false;

        if (!cap.supportsTemperature(rec.needCooling(), rec.needHeating())) return false;

        LocalDate d = rec.getDateAsLocalDate();
        LocalTime t = rec.getTimeAsLocalTime();

        return matchesWeekly(drone, d, t);
    }

    private boolean matchesWeekly(Drone drone, LocalDate date, LocalTime time) {
        List<DroneWeeklyAvailability> wa = drone.getWeeklyAvailabilities();

        if (wa == null || wa.isEmpty()) return true;

        for (DroneWeeklyAvailability slot : wa) {
            if (slot.getDay() == date.getDayOfWeek()) {
                LocalTime from = slot.getFromAsLocalTime();
                LocalTime to = slot.getToAsLocalTime();

                if ((time.equals(from) || time.isAfter(from)) && time.isBefore(to)) {
                    return true;
                }
            }
        }
        return false;
    }
}



