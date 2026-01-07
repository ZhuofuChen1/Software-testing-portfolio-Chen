package com.example.cw1.controller;

import com.example.cw1.dto.*;
import com.example.cw1.service.DeliveryPlanningService;
import com.example.cw1.service.DroneAvailabilityService;
import com.example.cw1.service.IlpDataService;
import com.example.cw1.service.MaintenanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@org.springframework.validation.annotation.Validated
public class ApiController {

    @Autowired
    private IlpDataService ilpDataService;

    @Autowired
    private DroneAvailabilityService droneAvailabilityService;

    @Autowired
    private DeliveryPlanningService deliveryPlanningService;

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private com.example.cw1.service.MaintenanceExportService maintenanceExportService;

    @GetMapping("/uid")
    public ResponseEntity<String> getUid() {
        return ResponseEntity.ok("s2322251");
    }


    @GetMapping("/dronesWithCooling/{state}")
    public ResponseEntity<List<String>> getDronesWithCooling(@PathVariable boolean state) {

        Drone[] drones = ilpDataService.getDrones();
        List<String> ids = new ArrayList<>();

        if (drones != null) {
            for (Drone d : drones) {
                if (d.getCapability() != null &&
                        d.getCapability().isCooling() == state) {

                    ids.add(d.getId());
                }
            }
        }

        return ResponseEntity.ok(ids);
    }


    @GetMapping("/droneDetails/{id}")
    public ResponseEntity<Drone> getDroneDetails(@PathVariable String id) {

        Drone[] drones = ilpDataService.getDrones();

        if (drones != null) {
            for (Drone d : drones) {
                if (d.getId().equals(id)) {
                    return ResponseEntity.ok(d);
                }
            }
        }

        return ResponseEntity.notFound().build();
    }


    @GetMapping("/queryAsPath/{attribute}/{value}")
    public ResponseEntity<List<String>> queryAsPath(
            @PathVariable String attribute,
            @PathVariable String value) {

        Drone[] drones = ilpDataService.getDrones();
        List<String> ids = new ArrayList<>();

        if (drones != null) {
            for (Drone d : drones) {

                QueryCriteria q = new QueryCriteria();
                q.setAttribute(attribute);
                q.setOperator("=");
                q.setValue(value);

                if (matchCriteria(d, q)) {
                    ids.add(d.getId());
                }
            }
        }

        return ResponseEntity.ok(ids);
    }


    @PostMapping("/query")
    public ResponseEntity<List<String>> queryDrones(
            @RequestBody List<QueryCriteria> criteriaList) {

        Drone[] drones = ilpDataService.getDrones();
        List<String> ids = new ArrayList<>();

        if (drones != null) {

            for (Drone d : drones) {
                boolean ok = true;

                for (QueryCriteria q : criteriaList) {
                    if (!matchCriteria(d, q)) {
                        ok = false;
                        break;
                    }
                }

                if (ok) ids.add(d.getId());
            }
        }

        return ResponseEntity.ok(ids);
    }


    private boolean matchCriteria(Drone drone, QueryCriteria criteria) {

        String attr = criteria.getAttribute().toLowerCase();
        String op = criteria.getOperator();
        String target = criteria.getValue();

        Object actual = null;
        boolean number = false;

        DroneCapability cap = drone.getCapability();

        switch (attr) {
            case "id":
                actual = drone.getId(); // string ID
                break;

            case "name":
                actual = drone.getName();
                break;

            case "capacity":
                actual = cap.getCapacity();
                number = true;
                break;

            case "cooling":
                actual = cap.isCooling();
                break;

            case "heating":
                actual = cap.isHeating();
                break;

            case "maxmoves":
                actual = cap.getMaxMoves();
                number = true;
                break;

            case "costpermove":
                actual = cap.getCostPerMove();
                number = true;
                break;

            case "costinitial":
                actual = cap.getCostInitial();
                number = true;
                break;

            case "costfinal":
                actual = cap.getCostFinal();
                number = true;
                break;

            default:
                return false;
        }

        if (actual == null) return false;

        try {
            if (number) {
                double a = Double.parseDouble(actual.toString());
                double b = Double.parseDouble(target);

                return switch (op) {
                    case "=", "==" -> Math.abs(a - b) < 0.000001;
                    case "!=" -> Math.abs(a - b) > 0.000001;
                    case "<" -> a < b;
                    case ">" -> a > b;
                    case "<=" -> a <= b;
                    case ">=" -> a >= b;
                    default -> false;
                };
            }

            if (actual instanceof Boolean) {
                boolean a = (Boolean) actual;
                boolean b = Boolean.parseBoolean(target);

                return switch (op) {
                    case "=", "==" -> a == b;
                    case "!=" -> a != b;
                    default -> false;
                };
            }

            // STRING compare
            return switch (op) {
                case "=", "==" -> actual.toString().equals(target);
                case "!=" -> !actual.toString().equals(target);
                default -> false;
            };

        } catch (Exception e) {
            return false;
        }
    }


    @PostMapping("/queryAvailableDrones")
    public ResponseEntity<List<String>> queryAvailableDrones(
            @RequestBody List<MedDispatchRec> dispatches) {

        if (dispatches == null || dispatches.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<String> ids = droneAvailabilityService.findAvailableDrones(dispatches);
        return ResponseEntity.ok(ids);
    }


    @PostMapping("/calcDeliveryPath")
    public ResponseEntity<DeliveryPathResponse> calcDeliveryPath(
            @RequestBody List<MedDispatchRec> dispatches) {

        return ResponseEntity.ok(
                deliveryPlanningService.calcDeliveryPath(dispatches)
        );
    }

    @PostMapping("/calcDeliveryPathAsGeoJson")
    public ResponseEntity<String> calcDeliveryPathAsGeoJson(
            @RequestBody List<MedDispatchRec> dispatches) {

        return ResponseEntity.ok(
                deliveryPlanningService.calcDeliveryPathAsGeoJson(dispatches)
        );
    }


    @PostMapping("/distanceTo")
    public ResponseEntity<Double> distanceTo(@RequestBody DistanceRequest req) {
        Position p1 = req.getPosition1();
        Position p2 = req.getPosition2();

        if (p1 == null || p2 == null) return ResponseEntity.badRequest().build();

        double dx = p1.getLng() - p2.getLng();
        double dy = p1.getLat() - p2.getLat();
        return ResponseEntity.ok(Math.sqrt(dx * dx + dy * dy));
    }

    @PostMapping("/isCloseTo")
    public ResponseEntity<Boolean> isCloseTo(@RequestBody CloseToRequest req) {
        Position p1 = req.getPosition1();
        Position p2 = req.getPosition2();

        double dx = p1.getLng() - p2.getLng();
        double dy = p1.getLat() - p2.getLat();

        return ResponseEntity.ok(Math.sqrt(dx * dx + dy * dy) < 0.00015);
    }

    @PostMapping("/nextPosition")
    public ResponseEntity<Position> nextPosition(@RequestBody NextPositionRequest req) {

        Position s = req.getStart();
        double step = 0.0001;
        double rad = Math.toRadians(req.getAngle());

        Position p = new Position(
                s.getLng() + step * Math.cos(rad),
                s.getLat() + step * Math.sin(rad)
        );

        return ResponseEntity.ok(p);
    }

    @PostMapping("/isInRegion")
    public ResponseEntity<Boolean> isInRegion(@RequestBody RegionRequest req) {

        List<Position> v = req.getRegion().getVertices();

        double x = req.getPosition().getLng();
        double y = req.getPosition().getLat();

        boolean inside = false;

        for (int i = 0, j = v.size() - 1; i < v.size(); j = i++) {
            double xi = v.get(i).getLng(), yi = v.get(i).getLat();
            double xj = v.get(j).getLng(), yj = v.get(j).getLat();

            boolean inter = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi + 1e-12) + xi);

            if (inter) inside = !inside;
        }

        return ResponseEntity.ok(inside);
    }

    @PostMapping("/maintenance/log")
    public ResponseEntity<MaintenancePlan> recordMaintenanceLog(
            @jakarta.validation.Valid @RequestBody MaintenanceLog log) {
        return ResponseEntity.ok(maintenanceService.recordLog(log));
    }

    @PostMapping("/maintenance/plan")
    public ResponseEntity<MaintenancePlanResponse> planMaintenance(
            @RequestBody(required = false) MaintenancePlanRequest request) {
        return ResponseEntity.ok(maintenanceService.plan(request));
    }

    @GetMapping("/maintenance/summary")
    public ResponseEntity<MaintenancePlanResponse> maintenanceSummary() {
        return ResponseEntity.ok(maintenanceService.fleetSummary());
    }

    @GetMapping("/maintenance/{droneId}")
    public ResponseEntity<MaintenancePlan> maintenanceSnapshot(@PathVariable String droneId) {
        MaintenancePlan plan = maintenanceService.snapshot(droneId);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(plan);
    }

    @GetMapping(value = "/maintenance/export/json", produces = "application/json")
    public ResponseEntity<String> exportMaintenanceJson() {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=maintenance-export.json")
                .body(maintenanceExportService.exportAsJson());
    }

    @GetMapping(value = "/maintenance/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportMaintenanceCsv() {
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=maintenance-export.csv")
                .body(maintenanceExportService.exportAsCsv());
    }
}
