import os
import sys
import asyncio
import httpx

# Test URL validation
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

async def test_url():
    api_url = get_api_base_url()
    print(f"API Base URL: {api_url}")
    
    # Test different endpoint formats
    endpoints = [
        "/maintenance/summary",
        "maintenance/summary",  # Without leading slash
    ]
    
    async with httpx.AsyncClient(timeout=5.0) as client:
        for endpoint in endpoints:
            # Ensure endpoint starts with /
            if not endpoint.startswith("/"):
                endpoint = "/" + endpoint
            url = f"{api_url}{endpoint}"
            print(f"\nTesting: {url}")
            try:
                response = await client.get(url)
                print(f"  Status: {response.status_code}")
                if response.status_code == 200:
                    print("  [OK] URL construction is correct")
                else:
                    print(f"  [WARNING] Unexpected status code")
            except Exception as e:
                print(f"  [ERROR] {e}")

if __name__ == "__main__":
    asyncio.run(test_url())

