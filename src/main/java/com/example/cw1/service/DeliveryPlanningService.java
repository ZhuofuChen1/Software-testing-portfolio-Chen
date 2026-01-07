package com.example.cw1.service;

import com.example.cw1.dto.DeliveryFlightDto;
import com.example.cw1.dto.DeliveryPathResponse;
import com.example.cw1.dto.Drone;
import com.example.cw1.dto.DroneCapability;
import com.example.cw1.dto.DronePathDto;
import com.example.cw1.dto.MaintenancePlan;
import com.example.cw1.dto.MedDispatchRec;
import com.example.cw1.dto.MedDispatchRequirements;
import com.example.cw1.dto.Position;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeliveryPlanningService {

    @Autowired
    private IlpDataService ilpDataService;

    @Autowired
    private MaintenanceService maintenanceService;

    public DeliveryPathResponse calcDeliveryPath(List<MedDispatchRec> dispatches) {
        DeliveryPathResponse response = new DeliveryPathResponse();
        response.setTotalCost(0.0);
        response.setTotalMoves(0);
        response.setDronePaths(new ArrayList<>());

        if (dispatches == null || dispatches.isEmpty()) {
            return response;
        }

        Drone[] drones = ilpDataService.getDrones();
        if (drones == null || drones.length == 0) {
            return response;
        }

        DroneSelection selection = chooseDroneFor(dispatches, drones);
        if (selection == null || selection.drone().getCapability() == null) {
            return response;
        }

        Drone chosen = selection.drone();
        MaintenancePlan plan = selection.plan();
        DroneCapability cap = chosen.getCapability();

        Position servicePoint = createPosition(-3.186874, 55.944494);

        List<Position> deliveryPositions = new ArrayList<>();
        for (int i = 0; i < dispatches.size(); i++) {
            double offset = 0.0003 * (i + 1);
            double lng = servicePoint.getLng() + offset;
            double lat = servicePoint.getLat() + offset;
            deliveryPositions.add(createPosition(lng, lat));
        }

        List<DeliveryFlightDto> deliveries = new ArrayList<>();
        List<Integer> movesPerDelivery = new ArrayList<>();
        int totalMoves = 0;

        for (int i = 0; i < dispatches.size(); i++) {
            MedDispatchRec rec = dispatches.get(i);
            Position startPos = (i == 0) ? servicePoint : deliveryPositions.get(i - 1);
            Position targetPos = deliveryPositions.get(i);

            List<Position> segment = buildSegment(startPos, targetPos);
            if (segment.isEmpty()) {
                segment.add(createPosition(startPos.getLng(), startPos.getLat()));
                segment.add(createPosition(targetPos.getLng(), targetPos.getLat()));
            }

            Position last = segment.get(segment.size() - 1);
            if (last.getLng() != targetPos.getLng() || last.getLat() != targetPos.getLat()) {
                segment.add(createPosition(targetPos.getLng(), targetPos.getLat()));
            }

            segment.add(createPosition(targetPos.getLng(), targetPos.getLat()));

            if (i == dispatches.size() - 1) {
                List<Position> back = buildSegment(targetPos, servicePoint);
                if (!back.isEmpty()) {
                    back.remove(0);
                    segment.addAll(back);
                }
            }

            int moves = segment.size() - 1;
            totalMoves += moves;
            movesPerDelivery.add(moves);

            DeliveryFlightDto d = new DeliveryFlightDto();
            d.setDeliveryId(rec.getId());
            d.setFlightPath(segment);
            deliveries.add(d);
        }

        if (totalMoves == 0) {
            return response;
        }

        double baseCost = cap.getCostInitial()
                + cap.getCostFinal()
                + cap.getCostPerMove() * totalMoves;

        for (int i = 0; i < dispatches.size(); i++) {
            MedDispatchRec rec = dispatches.get(i);
            MedDispatchRequirements req = rec.getRequirements();
            if (req == null) {
                continue;
            }
            Double maxCost = req.getMaxCost();
            if (maxCost == null) {
                continue;
            }
            int movesForThis = movesPerDelivery.get(i);
            double share = baseCost * ((double) movesForThis / (double) totalMoves);
            if (share - maxCost > 1e-9) {
                return response;
            }
        }

        response.setTotalMoves(totalMoves);
        response.setTotalCost(baseCost);
        response.setMaintenancePlan(plan);

        DronePathDto path = new DronePathDto();
        path.setDroneId(String.valueOf(chosen.getId()));
        path.setDeliveries(deliveries);

        List<DronePathDto> paths = new ArrayList<>();
        paths.add(path);
        response.setDronePaths(paths);

        return response;
    }

    public String calcDeliveryPathAsGeoJson(List<MedDispatchRec> dispatches) {
        DeliveryPathResponse resp = calcDeliveryPath(dispatches);
        if (resp.getDronePaths() == null
                || resp.getDronePaths().isEmpty()
                || resp.getDronePaths().get(0).getDeliveries() == null
                || resp.getDronePaths().get(0).getDeliveries().isEmpty()) {
            return "{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[]},\"properties\":{}}";
        }

        List<Position> coords = resp.getDronePaths()
                .get(0)
                .getDeliveries()
                .get(0)
                .getFlightPath();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"Feature\",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[");
        for (int i = 0; i < coords.size(); i++) {
            Position p = coords.get(i);
            sb.append("[")
                    .append(p.getLng())
                    .append(",")
                    .append(p.getLat())
                    .append("]");
            if (i < coords.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]},\"properties\":{}}");
        return sb.toString();
    }

    private DroneSelection chooseDroneFor(List<MedDispatchRec> dispatches, Drone[] drones) {
        double totalCapacityNeeded = 0.0;
        boolean coolingNeeded = false;
        boolean heatingNeeded = false;

        for (MedDispatchRec rec : dispatches) {
            if (rec.getRequirements() == null) {
                return null;
            }
            totalCapacityNeeded += rec.getRequirements().getCapacity();
            if (rec.getRequirements().isCooling()) {
                coolingNeeded = true;
            }
            if (rec.getRequirements().isHeating()) {
                heatingNeeded = true;
            }
        }

        DroneSelection withoutHighRisk = chooseDroneCandidate(drones, totalCapacityNeeded, coolingNeeded, heatingNeeded, false);
        if (withoutHighRisk != null) {
            return withoutHighRisk;
        }
        return chooseDroneCandidate(drones, totalCapacityNeeded, coolingNeeded, heatingNeeded, true);
    }

    private DroneSelection chooseDroneCandidate(Drone[] drones,
                                                double capacityNeeded,
                                                boolean coolingNeeded,
                                                boolean heatingNeeded,
                                                boolean allowHighRiskFallback) {
        if (drones == null || drones.length == 0) {
            return null;
        }
        DroneSelection best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Drone d : drones) {
            if (d == null || d.getCapability() == null) {
                continue;
            }
            DroneCapability cap = d.getCapability();
            if (cap.getCapacity() < capacityNeeded) {
                continue;
            }
            if (coolingNeeded && !cap.isCooling()) {
                continue;
            }
            if (heatingNeeded && !cap.isHeating()) {
                continue;
            }

            MaintenancePlan plan = maintenanceService.snapshot(d.getId());
            if (!allowHighRiskFallback && plan != null && "HIGH".equalsIgnoreCase(plan.getRiskLevel())) {
                continue;
            }

            double healthScore = plan == null ? 50.0 : 100.0 - plan.getRiskScore();
            double bufferScore = plan == null ? 0.0 : plan.getMissionBuffer() * 2.0;
            double capacityScore = cap.getCapacity() * 0.1;
            double candidateScore = healthScore + bufferScore + capacityScore;

            if (candidateScore > bestScore) {
                bestScore = candidateScore;
                best = new DroneSelection(d, plan);
            }
        }
        return best;
    }

    private record DroneSelection(Drone drone, MaintenancePlan plan) {
    }

    private Position createPosition(double lng, double lat) {
        Position p = new Position();
        p.setLng(lng);
        p.setLat(lat);
        return p;
    }

    private List<Position> buildSegment(Position from, Position to) {
        List<Position> result = new ArrayList<>();
        if (from == null || to == null) {
            return result;
        }

        double dx = to.getLng() - from.getLng();
        double dy = to.getLat() - from.getLat();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance == 0) {
            result.add(createPosition(from.getLng(), from.getLat()));
            return result;
        }

        double step = 0.00015;
        int steps = (int) Math.ceil(distance / step);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / (double) steps;
            double lng = from.getLng() + dx * t;
            double lat = from.getLat() + dy * t;
            result.add(createPosition(lng, lat));
        }
        return result;
    }
}

