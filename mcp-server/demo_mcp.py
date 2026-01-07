#!/usr/bin/env python3
"""
Simple demo showing how MCP tools would be called.
This simulates what an LLM would do when you ask questions.
"""

import asyncio
import os
import httpx

API_BASE_URL = os.getenv("ILP_MAINTENANCE_API_URL", "http://localhost:8080/api/v1")


async def demo_mcp_tools():
    """Demonstrate MCP tool functionality."""
    print("=" * 60)
    print("MCP Server Demo - Simulating LLM Tool Calls")
    print("=" * 60)
    print()
    
    async with httpx.AsyncClient(timeout=10.0) as client:
        # Demo 1: Get fleet summary (like asking "What's the fleet status?")
        print("Demo 1: LLM asks 'What's the maintenance status of the fleet?'")
        print("-" * 60)
        response = await client.get(f"{API_BASE_URL}/maintenance/summary")
        data = response.json()
        insight = data.get("insight", {})
        print(f"Answer: Fleet has {insight.get('fleetSize', 0)} drones")
        print(f"  - Average Risk: {insight.get('averageRisk', 0)}/100")
        print(f"  - High Risk: {insight.get('highRisk', 0)}")
        print(f"  - Readiness: {insight.get('readinessPercent', 0)}%")
        print()
        
        # Demo 2: Get specific drone (like asking "What's drn-101's status?")
        print("Demo 2: LLM asks 'What's the maintenance status of drone drn-101?'")
        print("-" * 60)
        try:
            response = await client.get(f"{API_BASE_URL}/maintenance/drn-101")
            plan = response.json()
            print(f"Answer: Drone drn-101 status:")
            print(f"  - Risk Level: {plan.get('riskLevel', 'N/A')}")
            print(f"  - Risk Score: {plan.get('riskScore', 0)}/100")
            print(f"  - Hours Until Service: {plan.get('hoursUntilService', 0)}")
            print(f"  - Recommendation: {plan.get('recommendation', 'N/A')}")
        except httpx.HTTPStatusError as e:
            if e.response.status_code == 404:
                print("Answer: Drone drn-101 not found in maintenance records")
            else:
                print(f"Error: {e}")
        print()
        
        # Demo 3: Get high-risk drones (like asking "Show me high-risk drones")
        print("Demo 3: LLM asks 'Show me all high-risk drones'")
        print("-" * 60)
        response = await client.get(f"{API_BASE_URL}/maintenance/summary")
        data = response.json()
        plans = data.get("plans", [])
        high_risk = [p for p in plans if p.get("riskLevel") == "HIGH"]
        if high_risk:
            print(f"Answer: Found {len(high_risk)} high-risk drone(s):")
            for plan in high_risk[:3]:  # Show first 3
                print(f"  - {plan.get('droneId')}: Risk {plan.get('riskScore')}/100")
        else:
            print("Answer: No high-risk drones found. Fleet is healthy.")
        print()
        
        # Demo 4: Log maintenance event (like asking to record data)
        print("Demo 4: LLM asks 'Log maintenance: drone drn-101, 10 flight hours, battery 0.80'")
        print("-" * 60)
        log_data = {
            "droneId": "drn-101",
            "flightHours": 10.0,
            "missions": 5,
            "batteryHealth": 0.80
        }
        response = await client.post(f"{API_BASE_URL}/maintenance/log", json=log_data)
        result = response.json()
        print(f"Answer: Maintenance log recorded for drn-101")
        print(f"  - Updated Risk Level: {result.get('riskLevel', 'N/A')}")
        print(f"  - Updated Risk Score: {result.get('riskScore', 0)}/100")
        print()
    
    print("=" * 60)
    print("These are the exact tool calls an LLM would make!")
    print("When you configure MCP in your LLM client, you can ask")
    print("these questions naturally and get the same answers.")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(demo_mcp_tools())




