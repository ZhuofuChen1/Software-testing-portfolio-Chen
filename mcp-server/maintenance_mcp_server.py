#!/usr/bin/env python3
"""
MCP Server for ILP Maintenance Intelligence Kit

This server exposes maintenance API endpoints as MCP tools,
allowing LLMs to query drone maintenance status and insights.
"""

import asyncio
import json
import os
import sys
from typing import Any, Sequence

import httpx

try:
    from mcp.server import Server
    from mcp.server.stdio import stdio_server
    from mcp.types import Tool, TextContent
except ImportError:
    # Fallback for older MCP versions
    try:
        from mcp.server.models import Server
        from mcp.server.stdio import stdio_server
        from mcp.types import Tool, TextContent
    except ImportError:
        print("Error: MCP library not found. Install with: pip install mcp", file=sys.stderr)
        sys.exit(1)

# Default API base URL (can be overridden via environment variable)
def get_api_base_url():
    """Get API base URL with validation."""
    url = os.getenv("ILP_MAINTENANCE_API_URL", "http://localhost:8080/api/v1")
    if not url or url.strip() == "":
        url = "http://localhost:8080/api/v1"
    # Ensure URL doesn't end with slash (endpoints start with /)
    url = url.rstrip("/")
    # Ensure URL has protocol
    if not url.startswith("http://") and not url.startswith("https://"):
        url = f"http://{url}"
    return url

API_BASE_URL = get_api_base_url()

# Initialize MCP server
server = Server("ilp-maintenance")


@server.list_tools()
async def list_tools() -> list[Tool]:
    """List available MCP tools."""
    return [
        Tool(
            name="get_fleet_summary",
            description="Get a summary of the entire drone fleet maintenance status, including average risk, high-risk count, and readiness percentage.",
            inputSchema={
                "type": "object",
                "properties": {},
                "required": []
            }
        ),
        Tool(
            name="get_drone_maintenance",
            description="Get detailed maintenance plan for a specific drone by ID. Returns risk score, risk level, hours until service, and recommendations.",
            inputSchema={
                "type": "object",
                "properties": {
                    "droneId": {
                        "type": "string",
                        "description": "The drone ID (e.g., 'drn-101')"
                    }
                },
                "required": ["droneId"]
            }
        ),
        Tool(
            name="get_high_risk_drones",
            description="Get list of all drones with HIGH risk level that need immediate attention.",
            inputSchema={
                "type": "object",
                "properties": {},
                "required": []
            }
        ),
        Tool(
            name="log_maintenance_event",
            description="Record a maintenance/telemetry event for a drone. Automatically updates the maintenance plan.",
            inputSchema={
                "type": "object",
                "properties": {
                    "droneId": {
                        "type": "string",
                        "description": "The drone ID"
                    },
                    "flightHours": {
                        "type": "number",
                        "description": "Flight hours logged (optional)"
                    },
                    "missions": {
                        "type": "integer",
                        "description": "Number of missions completed (optional)"
                    },
                    "emergencyDiversions": {
                        "type": "integer",
                        "description": "Number of emergency diversions (optional)"
                    },
                    "avgPayloadKg": {
                        "type": "number",
                        "description": "Average payload in kg (optional)"
                    },
                    "batteryHealth": {
                        "type": "number",
                        "description": "Battery health (0.0-1.0, optional)"
                    },
                    "temperatureAlerts": {
                        "type": "boolean",
                        "description": "Whether temperature alerts occurred (optional)"
                    },
                    "communicationIssues": {
                        "type": "boolean",
                        "description": "Whether communication issues occurred (optional)"
                    },
                    "note": {
                        "type": "string",
                        "description": "Optional note about the event"
                    }
                },
                "required": ["droneId"]
            }
        ),
        Tool(
            name="plan_maintenance_batch",
            description="Get maintenance plans for multiple drones at once. Can optionally include new log entries.",
            inputSchema={
                "type": "object",
                "properties": {
                    "droneIds": {
                        "type": "array",
                        "items": {"type": "string"},
                        "description": "List of drone IDs to query (optional, queries all if omitted)"
                    },
                    "includeFleetInsight": {
                        "type": "boolean",
                        "description": "Whether to include fleet-level insight (default: true)"
                    }
                },
                "required": []
            }
        )
    ]


async def call_api(method: str, endpoint: str, data: dict | None = None) -> dict[str, Any]:
    """Make HTTP request to the maintenance API."""
    # Ensure endpoint starts with /
    if not endpoint.startswith("/"):
        endpoint = "/" + endpoint
    url = f"{API_BASE_URL}{endpoint}"
    async with httpx.AsyncClient(timeout=10.0) as client:
        if method == "GET":
            response = await client.get(url)
        elif method == "POST":
            response = await client.post(url, json=data)
        else:
            raise ValueError(f"Unsupported method: {method}")
        
        response.raise_for_status()
        return response.json()


@server.call_tool()
async def call_tool(name: str, arguments: dict[str, Any]) -> Sequence[TextContent]:
    """Handle tool calls."""
    try:
        if name == "get_fleet_summary":
            result = await call_api("GET", "/maintenance/summary")
            insight = result.get("insight", {})
            plans = result.get("plans", [])
            
            summary = f"""Fleet Maintenance Summary:
- Fleet Size: {insight.get('fleetSize', 0)}
- Average Risk Score: {insight.get('averageRisk', 0)}/100
- High Risk Drones: {insight.get('highRisk', 0)}
- Readiness: {insight.get('readinessPercent', 0)}%

Narrative:
"""
            for line in insight.get("narrative", []):
                summary += f"  - {line}\n"
            
            if plans:
                summary += "\nIndividual Drone Status:\n"
                for plan in plans[:5]:  # Show first 5
                    summary += f"  - {plan.get('droneId')}: {plan.get('riskLevel')} risk ({plan.get('riskScore')}/100)\n"
            
            return [TextContent(type="text", text=summary)]
        
        elif name == "get_drone_maintenance":
            drone_id = arguments.get("droneId")
            if not drone_id:
                return [TextContent(type="text", text="Error: droneId is required")]
            
            result = await call_api("GET", f"/maintenance/{drone_id}")
            
            plan_text = f"""Maintenance Plan for {drone_id}:
- Risk Score: {result.get('riskScore', 0)}/100
- Risk Level: {result.get('riskLevel', 'UNKNOWN')}
- Hours Until Service: {result.get('hoursUntilService', 0)}
- Mission Buffer: {result.get('missionBuffer', 0)}
- Recommendation: {result.get('recommendation', 'N/A')}

Contributing Factors:
"""
            for factor in result.get("contributingFactors", []):
                plan_text += f"  - {factor}\n"
            
            return [TextContent(type="text", text=plan_text)]
        
        elif name == "get_high_risk_drones":
            result = await call_api("GET", "/maintenance/summary")
            plans = result.get("plans", [])
            
            high_risk = [p for p in plans if p.get("riskLevel") == "HIGH"]
            
            if not high_risk:
                return [TextContent(type="text", text="No high-risk drones found. Fleet is healthy.")]
            
            report = f"Found {len(high_risk)} high-risk drone(s):\n\n"
            for plan in high_risk:
                report += f"Drone: {plan.get('droneId')}\n"
                report += f"  Risk Score: {plan.get('riskScore')}/100\n"
                report += f"  Hours Until Service: {plan.get('hoursUntilService')}\n"
                report += f"  Recommendation: {plan.get('recommendation')}\n"
                report += f"  Factors: {', '.join(plan.get('contributingFactors', []))}\n\n"
            
            return [TextContent(type="text", text=report)]
        
        elif name == "log_maintenance_event":
            log_data = {
                "droneId": arguments.get("droneId"),
                "flightHours": arguments.get("flightHours"),
                "missions": arguments.get("missions"),
                "emergencyDiversions": arguments.get("emergencyDiversions"),
                "avgPayloadKg": arguments.get("avgPayloadKg"),
                "batteryHealth": arguments.get("batteryHealth"),
                "temperatureAlerts": arguments.get("temperatureAlerts"),
                "communicationIssues": arguments.get("communicationIssues"),
                "note": arguments.get("note")
            }
            # Remove None values
            log_data = {k: v for k, v in log_data.items() if v is not None}
            
            result = await call_api("POST", "/maintenance/log", log_data)
            
            response_text = f"""Maintenance log recorded for {log_data['droneId']}.

Updated Plan:
- Risk Score: {result.get('riskScore', 0)}/100
- Risk Level: {result.get('riskLevel', 'UNKNOWN')}
- Hours Until Service: {result.get('hoursUntilService', 0)}
- Recommendation: {result.get('recommendation', 'N/A')}
"""
            return [TextContent(type="text", text=response_text)]
        
        elif name == "plan_maintenance_batch":
            request_data = {}
            if "droneIds" in arguments:
                request_data["droneIds"] = arguments["droneIds"]
            if "includeFleetInsight" in arguments:
                request_data["includeFleetInsight"] = arguments["includeFleetInsight"]
            
            result = await call_api("POST", "/maintenance/plan", request_data if request_data else None)
            
            plans = result.get("plans", [])
            insight = result.get("insight")
            
            report = f"Maintenance Plans for {len(plans)} drone(s):\n\n"
            for plan in plans:
                report += f"{plan.get('droneId')}: {plan.get('riskLevel')} risk ({plan.get('riskScore')}/100)\n"
            
            if insight:
                report += f"\nFleet Insight:\n"
                report += f"- Average Risk: {insight.get('averageRisk', 0)}/100\n"
                report += f"- High Risk Count: {insight.get('highRisk', 0)}\n"
                report += f"- Readiness: {insight.get('readinessPercent', 0)}%\n"
            
            return [TextContent(type="text", text=report)]
        
        else:
            return [TextContent(type="text", text=f"Unknown tool: {name}")]
    
    except httpx.HTTPStatusError as e:
        error_msg = f"API Error ({e.response.status_code}): {e.response.text}"
        return [TextContent(type="text", text=error_msg)]
    except Exception as e:
        return [TextContent(type="text", text=f"Error: {str(e)}")]


async def main():
    """Run the MCP server."""
    async with stdio_server() as (read_stream, write_stream):
        await server.run(
            read_stream,
            write_stream,
            server.create_initialization_options()
        )


if __name__ == "__main__":
    asyncio.run(main())

