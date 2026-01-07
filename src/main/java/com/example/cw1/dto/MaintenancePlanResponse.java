package com.example.cw1.dto;

import java.util.ArrayList;
import java.util.List;

public class MaintenancePlanResponse {

    private List<MaintenancePlan> plans = new ArrayList<>();
    private MaintenanceInsight insight;

    public List<MaintenancePlan> getPlans() {
        return plans;
    }

    public void setPlans(List<MaintenancePlan> plans) {
        this.plans = plans;
    }

    public MaintenanceInsight getInsight() {
        return insight;
    }

    public void setInsight(MaintenanceInsight insight) {
        this.insight = insight;
    }
}

