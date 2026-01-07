#!/usr/bin/env python3
"""
Simple test script to verify the MCP server can connect to the maintenance API.
Run this after starting the Spring Boot service.
"""

import asyncio
import os
import sys

import httpx

API_BASE_URL = os.getenv("ILP_MAINTENANCE_API_URL", "http://localhost:8080/api/v1")


async def test_api_connection():
    """Test if the maintenance API is accessible."""
    print("Testing connection to maintenance API...")
    print(f"API URL: {API_BASE_URL}\n")
    
    async with httpx.AsyncClient(timeout=5.0) as client:
        # Test 1: Fleet summary
        try:
            response = await client.get(f"{API_BASE_URL}/maintenance/summary")
            response.raise_for_status()
            data = response.json()
            print("[OK] GET /maintenance/summary - OK")
            print(f"  Fleet size: {data.get('insight', {}).get('fleetSize', 0)}")
        except Exception as e:
            print(f"[FAIL] GET /maintenance/summary - FAILED: {e}")
            return False
        
        # Test 2: Log a test event
        try:
            test_log = {
                "droneId": "test-drone-001",
                "flightHours": 5.0,
                "missions": 2,
                "batteryHealth": 0.85
            }
            response = await client.post(f"{API_BASE_URL}/maintenance/log", json=test_log)
            response.raise_for_status()
            print("[OK] POST /maintenance/log - OK")
        except Exception as e:
            print(f"[FAIL] POST /maintenance/log - FAILED: {e}")
            return False
        
        # Test 3: Get drone snapshot
        try:
            response = await client.get(f"{API_BASE_URL}/maintenance/test-drone-001")
            response.raise_for_status()
            data = response.json()
            print("[OK] GET /maintenance/{droneId} - OK")
            print(f"  Risk level: {data.get('riskLevel', 'N/A')}")
        except Exception as e:
            print(f"[FAIL] GET /maintenance/{{droneId}} - FAILED: {e}")
            return False
    
    print("\n[SUCCESS] All API tests passed! MCP server should work correctly.")
    return True


if __name__ == "__main__":
    success = asyncio.run(test_api_connection())
    sys.exit(0 if success else 1)

