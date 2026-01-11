package com.example.cw1.service;

import com.example.cw1.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DroneAvailabilityService.
 * Tests drone filtering based on capacity, temperature requirements, and weekly availability.
 */
class DroneAvailabilityServiceTest {

    @Mock
    private IlpDataService ilpDataService;

    @InjectMocks
    private DroneAvailabilityService droneAvailabilityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ========== Helper Methods ==========

    private Drone createDrone(String id, double capacity, boolean cooling, boolean heating) {
        Drone drone = new Drone();
        drone.setId(id);
        drone.setName("Drone " + id);

        DroneCapability cap = new DroneCapability();
        cap.setCapacity(capacity);
        cap.setCooling(cooling);
        cap.setHeating(heating);
        cap.setMaxMoves(100);
        drone.setCapability(cap);

        return drone;
    }

    private Drone createDroneWithSchedule(String id, double capacity, DayOfWeek day, String from, String to) {
        Drone drone = createDrone(id, capacity, true, true);

        DroneWeeklyAvailability slot = new DroneWeeklyAvailability();
        slot.setDay(day);
        slot.setFrom(from);
        slot.setTo(to);

        List<DroneWeeklyAvailability> schedule = new ArrayList<>();
        schedule.add(slot);
        drone.setWeeklyAvailabilities(schedule);

        return drone;
    }

    private MedDispatchRec createDispatch(double capacity, boolean cooling, boolean heating, String date, String time) {
        MedDispatchRec rec = new MedDispatchRec();
        rec.setId(1);
        rec.setDate(date);
        rec.setTime(time);

        MedDispatchRequirements req = new MedDispatchRequirements();
        req.setCapacity(capacity);
        req.setCooling(cooling);
        req.setHeating(heating);
        rec.setRequirements(req);

        return rec;
    }

    // ========== Null/Empty Input Tests ==========

    @Test
    @DisplayName("Returns empty list when drones array is null")
    void findAvailableDrones_withNullDrones_returnsEmptyList() {
        when(ilpDataService.getDrones()).thenReturn(null);

        List<MedDispatchRec> dispatches = List.of(
                createDispatch(5.0, false, false, "2026-01-12", "10:00")
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Returns empty list when no drones match requirements")
    void findAvailableDrones_withNoMatchingDrones_returnsEmptyList() {
        Drone drone = createDrone("D1", 5.0, false, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});

        // Dispatch requires cooling but drone doesn't have it
        List<MedDispatchRec> dispatches = List.of(
                createDispatch(3.0, true, false, "2026-01-12", "10:00")
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertTrue(result.isEmpty());
    }

    // ========== Capacity Tests ==========

    @Test
    @DisplayName("Returns drone ID when drone has sufficient capacity")
    void findAvailableDrones_withCapableDrone_returnsDroneId() {
        Drone drone = createDrone("D1", 10.0, true, true);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});

        List<MedDispatchRec> dispatches = List.of(
                createDispatch(5.0, false, false, "2026-01-12", "10:00")
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertEquals(1, result.size());
        assertEquals("D1", result.get(0));
    }

    @Test
    @DisplayName("Excludes drone when capacity is insufficient")
    void findAvailableDrones_withInsufficientCapacity_excludesDrone() {
        Drone drone = createDrone("D1", 3.0, true, true);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});

        List<MedDispatchRec> dispatches = List.of(
                createDispatch(10.0, false, false, "2026-01-12", "10:00")
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Capacity boundary testing")
    @CsvSource({
            "10.0, 10.0, true",   // Exact match
            "10.0, 9.9, true",    // Just under
            "10.0, 10.1, false",  // Just over
            "5.0, 0.0, true",     // Zero requirement
            "100.0, 50.0, true"   // Large capacity
    })
    void findAvailableDrones_capacityBoundaryTests(double droneCapacity, double requiredCapacity, boolean shouldMatch) {
        Drone drone = createDrone("D1", droneCapacity, true, true);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});

        List<MedDispatchRec> dispatches = List.of(
                createDispatch(requiredCapacity, false, false, "2026-01-12", "10:00")
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertEquals(shouldMatch, !result.isEmpty(),
                String.format("Drone capacity %.1f vs required %.1f", droneCapacity, requiredCapacity));
    }

    // ========== Temperature Tests ==========

    @Test
    @DisplayName("Returns drone when cooling requirement is met")
    void findAvailableDrones_withCoolingRequirement_filtersCorrectly() {
        Drone droneWithCooling = createDrone("D1", 10.0, true, false);
        Drone droneWithoutCooling = createDrone("D2", 10.0, false, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{droneWithCooling, droneWithoutCooling});

        List<MedDispatchRec> dispatches = List.of(
                createDispatch(5.0, true, false, "2026-01-12", "10:00")
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertEquals(1, result.size());
        assertEquals("D1", result.get(0));
    }

    @Test
    @DisplayName("Returns drone when heating requirement is met")
    void findAvailableDrones_withHeatingRequirement_filtersCorrectly() {
        Drone droneWithHeating = createDrone("D1", 10.0, false, true);
        Drone droneWithoutHeating = createDrone("D2", 10.0, false, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{droneWithHeating, droneWithoutHeating});

        List<MedDispatchRec> dispatches = List.of(
                createDispatch(5.0, false, true, "2026-01-12", "10:00")
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertEquals(1, result.size());
        assertEquals("D1", result.get(0));
    }

    @Test
    @DisplayName("Returns drone when both cooling and heating are required")
    void findAvailableDrones_withBothTempRequirements_filtersCorrectly() {
        Drone fullCapabilityDrone = createDrone("D1", 10.0, true, true);
        Drone partialDrone = createDrone("D2", 10.0, true, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{fullCapabilityDrone, partialDrone});

        List<MedDispatchRec> dispatches = List.of(
                createDispatch(5.0, true, true, "2026-01-12", "10:00")
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertEquals(1, result.size());
        assertEquals("D1", result.get(0));
    }

    // ========== Weekly Schedule Tests ==========

    @Test
    @DisplayName("Returns drone when within scheduled availability window")
    void findAvailableDrones_withinScheduleWindow_returnsDrone() {
        // Monday 09:00-17:00
        Drone drone = createDroneWithSchedule("D1", 10.0, DayOfWeek.MONDAY, "09:00", "17:00");
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});

        // Request for Monday at 10:00
        List<MedDispatchRec> dispatches = List.of(
                createDispatch(5.0, false, false, "2026-01-12", "10:00")  // Monday
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertEquals(1, result.size());
        assertEquals("D1", result.get(0));
    }

    @Test
    @DisplayName("Excludes drone when outside scheduled availability window")
    void findAvailableDrones_outsideScheduleWindow_excludesDrone() {
        // Monday 09:00-17:00
        Drone drone = createDroneWithSchedule("D1", 10.0, DayOfWeek.MONDAY, "09:00", "17:00");
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});

        // Request for Monday at 18:00 (outside window)
        List<MedDispatchRec> dispatches = List.of(
                createDispatch(5.0, false, false, "2026-01-12", "18:00")  // Monday but after hours
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Excludes drone when requested on wrong day")
    void findAvailableDrones_wrongDay_excludesDrone() {
        // Monday 09:00-17:00
        Drone drone = createDroneWithSchedule("D1", 10.0, DayOfWeek.MONDAY, "09:00", "17:00");
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});

        // Request for Tuesday (drone only available Monday)
        List<MedDispatchRec> dispatches = List.of(
                createDispatch(5.0, false, false, "2026-01-13", "10:00")  // Tuesday
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Returns drone with no schedule restrictions (always available)")
    void findAvailableDrones_noSchedule_alwaysAvailable() {
        Drone drone = createDrone("D1", 10.0, true, true);
        // No weekly availability set = always available
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});

        List<MedDispatchRec> dispatches = List.of(
                createDispatch(5.0, false, false, "2026-01-12", "10:00")
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertEquals(1, result.size());
    }

    // ========== Multiple Dispatch Tests ==========

    @Test
    @DisplayName("Drone must satisfy ALL dispatches to be included")
    void findAvailableDrones_multipleDispatches_mustSatisfyAll() {
        Drone drone = createDrone("D1", 10.0, true, false);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});

        // First dispatch needs cooling (OK), second needs heating (FAIL)
        List<MedDispatchRec> dispatches = List.of(
                createDispatch(5.0, true, false, "2026-01-12", "10:00"),
                createDispatch(5.0, false, true, "2026-01-12", "11:00")
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Returns multiple drones when all satisfy requirements")
    void findAvailableDrones_multipleDrones_returnsAllMatching() {
        Drone drone1 = createDrone("D1", 10.0, true, true);
        Drone drone2 = createDrone("D2", 15.0, true, true);
        Drone drone3 = createDrone("D3", 5.0, false, false);  // Won't match
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone1, drone2, drone3});

        List<MedDispatchRec> dispatches = List.of(
                createDispatch(8.0, true, false, "2026-01-12", "10:00")
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertEquals(2, result.size());
        assertTrue(result.contains("D1"));
        assertTrue(result.contains("D2"));
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("Handles drone with null capability")
    void findAvailableDrones_nullCapability_excludesDrone() {
        Drone drone = new Drone();
        drone.setId("D1");
        drone.setCapability(null);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});

        List<MedDispatchRec> dispatches = List.of(
                createDispatch(5.0, false, false, "2026-01-12", "10:00")
        );

        List<String> result = droneAvailabilityService.findAvailableDrones(dispatches);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Handles empty dispatch list")
    void findAvailableDrones_emptyDispatchList_returnsAllDrones() {
        Drone drone = createDrone("D1", 10.0, true, true);
        when(ilpDataService.getDrones()).thenReturn(new Drone[]{drone});

        List<String> result = droneAvailabilityService.findAvailableDrones(Collections.emptyList());

        // Empty list means no requirements = all drones match
        assertEquals(1, result.size());
    }
}

