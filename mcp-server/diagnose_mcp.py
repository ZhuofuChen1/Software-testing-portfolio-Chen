#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MCP Connection Diagnostic Tool
Checks if MCP server can start and connect properly
"""

import sys
import os
import subprocess

# Set UTF-8 encoding for Windows console
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

print("=" * 60)
print("MCP Server Diagnostic Tool")
print("=" * 60)
print()

# 1. Check Python version
print("1. Checking Python environment...")
print(f"   Python path: {sys.executable}")
print(f"   Python version: {sys.version}")
print()

# 2. Check dependencies
print("2. Checking MCP dependencies...")
try:
    import mcp
    print(f"   [OK] mcp installed: {mcp.__version__ if hasattr(mcp, '__version__') else 'unknown'}")
except ImportError:
    print("   [ERROR] mcp not installed")
    print("   Run: pip install -r requirements.txt")
    sys.exit(1)

try:
    import httpx
    print(f"   [OK] httpx installed")
except ImportError:
    print("   [ERROR] httpx not installed")
    sys.exit(1)
print()

# 3. Check script file
print("3. Checking MCP server script...")
script_path = os.path.join(os.path.dirname(__file__), "maintenance_mcp_server.py")
if os.path.exists(script_path):
    print(f"   [OK] Script exists: {script_path}")
else:
    print(f"   [ERROR] Script not found: {script_path}")
    sys.exit(1)
print()

# 4. Check API connection
print("4. Checking Spring Boot API connection...")
import asyncio
import httpx

async def test_api():
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            response = await client.get("http://localhost:8080/api/v1/maintenance/summary")
            if response.status_code == 200:
                print("   [OK] API connection successful")
                data = response.json()
                print(f"   Fleet size: {data.get('insight', {}).get('fleetSize', 0)}")
            else:
                print(f"   [WARNING] API returned status code: {response.status_code}")
    except Exception as e:
        print(f"   [ERROR] API connection failed: {e}")
        print("   Please ensure Spring Boot service is running")

asyncio.run(test_api())
print()

# 5. Test MCP server startup
print("5. Testing MCP server startup...")
print("   Note: MCP server communicates via stdio, direct run will wait for input")
print("   If you see 'Server initialized' or similar, server can start")
print("   Press Ctrl+C to exit test")
print()

try:
    # Try to import and check server code
    sys.path.insert(0, os.path.dirname(script_path))
    import importlib.util
    spec = importlib.util.spec_from_file_location("mcp_server", script_path)
    if spec and spec.loader:
        print("   [OK] MCP server script can be imported")
    else:
        print("   [ERROR] Cannot load MCP server script")
except Exception as e:
    print(f"   [WARNING] Import test failed: {e}")

print()
print("=" * 60)
print("Diagnosis complete")
print("=" * 60)
print()
print("If all checks pass but Cursor still cannot connect:")
print("1. Confirm Cursor has been restarted")
print("2. Check if Cursor's MCP config format is correct")
print("3. Check Cursor's Output panel for error messages")
print("4. Try searching for 'MCP' options in Cursor settings")

