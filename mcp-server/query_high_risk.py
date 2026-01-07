import asyncio
import httpx

async def get_high_risk():
    async with httpx.AsyncClient(timeout=10.0) as client:
        response = await client.get("http://localhost:8080/api/v1/maintenance/summary")
        data = response.json()
        plans = data.get("plans", [])
        high_risk = [p for p in plans if p.get("riskLevel") == "HIGH"]
        
        if high_risk:
            print(f"Found {len(high_risk)} high-risk drone(s):\n")
            for p in high_risk[:10]:
                print(f"Drone: {p['droneId']}")
                print(f"  - Risk Score: {p.get('riskScore', 0)}/100")
                print(f"  - Hours Until Service: {p.get('hoursUntilService', 0)}")
                print(f"  - Mission Buffer: {p.get('missionBuffer', 0)}")
                print(f"  - Recommendation: {p.get('recommendation', 'N/A')}")
                factors = p.get('contributingFactors', [])
                if factors:
                    print(f"  - Contributing Factors: {', '.join(factors[:3])}")
                print()
        else:
            print("No high-risk drones found. Fleet is healthy!")

asyncio.run(get_high_risk())

