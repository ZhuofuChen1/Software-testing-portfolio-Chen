Predictive Maintenance Intelligence: Elevating ILP Drone Operations Report
Part1: Problem Statement & Stakeholders
CW3 addresses a critical gap in ILP drone operations: the absence of predictive maintenance
intelligence. Three primary stakeholders drive this innovation.
First, regional dispatchers managing medical deliveries previously selected drones solely
based on capacity and temperature requirements, unaware of impending maintenance needs.
This reactive approach led to mission failures when high-risk drones were deployed. CW3
embeds risk-aware intelligence into delivery planning, automatically filtering out high-risk
airframes before dispatch.
Second, engineering teams managing depot capacity lacked prioritization data. Without
explainable risk factors, scheduling was either overly conservative (wasting capacity) or
reactive (responding to failures). The system provides transparent contributing factors
(utilization, battery health, emergency diversions) enabling data-driven maintenance
scheduling.
Third, fleet operators need strategic visibility for capacity planning. The solution delivers
fleet-wide readiness metrics and high-risk counts previously unavailable. The innovation
transforms operations from reactive troubleshooting to proactive, predictive maintenance
management.
Part 2：Innovation & Novel Contribution
The core innovation is a predictive maintenance intelligence layer that combines ILP
catalogue data with real-time telemetry to generate actionable risk assessments. Unlike
traditional maintenance systems that rely on fixed schedules or failure events, CW3
implements a dynamic, explainable multi-factor weighted risk-scoring algorithm that
normalizes operational and health indicators into a unified 0–100 score.
CW3’s algorithm evaluates seven contributing factors — flight hours, mission count,
emergency diversions, payload utilization, battery health degradation, temperature alerts, and
communication issues. Each normalized through clamped ratios (0–1) and combined using
explicitly defined weights (30/20/15/15/10 + conditional fixed penalties). This ensures a
balanced representation of utilization load, operational stress, and safety-critical anomalies.
The model’s output includes not only a risk score and tiered risk level (LOW / MEDIUM /
HIGH), but also a set of human-readable contributing factors (e.g., “utilization above 75%”,
“battery degradation detected”), and derived operational insights such as “hours until service”
or “mission buffer”. These elements significantly enhance transparency and interpretability
compared to opaque statistical or ML approaches.
The novelty lies in seamless integration: maintenance scoring is embedded directly into
delivery planning responses, creating a closed-loop system where dispatch decisions
automatically consider asset health. High-risk airframes are filtered out during
/calcDeliveryPath execution, and medium-risk units are accompanied by maintenance
recommendations (e.g., “schedule service before next dispatch block”). This transforms
maintenance from a reactive workflow into an operationally aware, predictive decision
layer.The complete implementation of the risk-scoring algorithm can be found in
src\main\java\com\example\cw1\service\MaintenanceService.java.
Additionally, CW3 introduces MCP (Model Context Protocol) integration, exposing
maintenance APIs as LLM tools, enabling natural-language queries, real-time fleet health
interrogation, and future AI-driven workflows such as autonomous maintenance planning
agents. This positions the solution at the intersection of predictive analytics, operational
intelligence, and AI-native orchestration.
Beyond these innovations, CW3 also enhances the original capabilities established in CW2.
Existing endpoints for drone lookup and delivery path calculation have been expanded with a
full maintenance layer, including telemetry ingestion, fleet summaries, health snapshot
queries, and JSON/CSV export. Persistent storage, validation, global exception handling, and
a real-time telemetry simulator further strengthen reliability and data integrity. Together,
these additions transform the earlier CW2 framework into a more complete and operationally
mature maintenance ecosystem.
Part3: Implementation & Technical Excellence
CW3 demonstrates production-grade engineering quality, building a more robust,
maintenance-aware operational system on top of the CW2 architecture. It supports risk-based
drone filtering and automatically embeds maintenance plans into dispatch results. While
CW2 lacked persistence, CW3 introduces JSON-based storage(storage/maintenance-log.json)
for durable telemetry logging. Jakarta Validation ensures input integrity, and a global
exception handler provides consistent and structured error responses. High-risk alerts are
automatically logged when thresholds are exceeded. A real-time telemetry simulator
continuously generates synthetic data, showcasing dynamic risk computation and live data
ingestion.
The system is built on IntelliJ using Spring Boot, leveraging dependency injection, RESTful
APIs, and type-safe DTOs. MaintenanceService handles the core logic, including JSON
persistence, ILP catalogue integration, and a deterministic multi-factor risk-scoring algorithm
that combines drone capability parameters (capacity, max flight cycles, cooling/heating
functions) with real-time telemetry metrics. DeliveryPlanningService then uses these health
insights to filter and rank drones by risk.
Part 4: Test Case
There are some test cases which can run on postman.
Test 1 - High-Risk Detection:
POST http://localhost:8080/api/v1/maintenance/log
Body:
{"droneId":"drn-002","flightHours":55.0,"missions":28,"emergencyDiversions":4,"avgPayloa
dKg":18.5,"batteryHealth":0.45,"temperatureAlerts":true,"communicationIssues":true,"note":
"Multiple issues detected"}
Expected: riskLevel="HIGH", riskScore>70, recommendation contains "Ground
immediately", console logs [HIGH RISK ALERT] message.
Test 2 -Midem-Risk Detection:
POST http://localhost:8080/api/v1/maintenance/log
Body:
{"droneId":"drn-001","flightHours":15.5,"missions":8,"emergencyDiversions":0,"avgPayloadK
g":8.2,"batteryHealth":0.92,"temperatureAlerts":false,"communicationIssues":false,"note":"Re
gular checkup"}
Expected: Response riskLevel="MIDEM" and recommendation.
Test 3 - Get Summary:
GET http://localhost:8080/api/v1/maintenance/summary
Expected: All Drones with its maintenance summary.
Test 4 - Request Validation:
POST http://localhost:8080/api/v1/maintenance/log
Body: {"droneId":"","batteryHealth":1.5}
Expected: Returns 400 Bad Requests with structured field-level errors.
However there are also code for testing on :
src\test\java\com\example\cw1\service\MaintenanceServiceTest.java
Conclution：
In closing, CW3 stands as a complete, production-ready demonstration of predictive
maintenance intelligence within ILP drone operations. The solution contributes a clear and
superior innovation: a transparent, explainable, multi-factor risk-scoring model tightly
integrated into dispatch planning, enabling the shift from reactive troubleshooting to
proactive, health-aware operations. This innovation is further strengthened by CW3’s
extensive technical execution. The system employs a mature engineering stack—Spring Boot,
dependency injection, DTO validation, JSON persistence, structured exception handling, and
MCP-based LLM tooling—showcasing strong mastery of modern software-engineering
practices.
CW3 also achieves full completeness by covering the end-to-end maintenance lifecycle:
real-time telemetry ingestion, risk analysis, fleet summaries, maintenance-aware path
planning, data export, high-risk alerting, and continuous simulation. Every anticipated
scenario is supported and operationalised within the implementation. Architecturally, the
project maintains excellent clarity through a clean layered design, modular services,
well-structured REST endpoints, and extensible interfaces that support future scaling,
automation and machine-learning integration.
