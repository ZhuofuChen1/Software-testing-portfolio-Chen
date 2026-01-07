# ILP Maintenance MCP Server

This MCP (Model Context Protocol) server exposes the ILP Maintenance Intelligence Kit as tools that LLMs can use to query and manage drone maintenance data.

## Prerequisites

- Python 3.10+
- Spring Boot service running on `http://localhost:8080` (or configure via `ILP_MAINTENANCE_API_URL`)

## Installation

```bash
pip install -r requirements.txt
```

## Configuration

### For Claude Desktop

Edit `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS) or `%APPDATA%\Claude\claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "ilp-maintenance": {
      "command": "python",
      "args": ["/absolute/path/to/CW1/mcp-server/maintenance_mcp_server.py"]
    }
  }
}
```

### For Cursor

Edit your Cursor MCP settings and add:

```json
{
  "mcpServers": {
    "ilp-maintenance": {
      "command": "python",
      "args": ["mcp-server/maintenance_mcp_server.py"],
      "cwd": "/path/to/CW1"
    }
  }
}
```

## Environment Variables

- `ILP_MAINTENANCE_API_URL`: Override the default API base URL (default: `http://localhost:8080/api/v1`)

## Available Tools

1. **get_fleet_summary** - Get overall fleet maintenance status
2. **get_drone_maintenance** - Get detailed plan for a specific drone
3. **get_high_risk_drones** - List all high-risk drones
4. **log_maintenance_event** - Record maintenance/telemetry data
5. **plan_maintenance_batch** - Batch query multiple drones

## Testing

After configuring, restart your LLM client and try asking:

- "What's the maintenance status of the drone fleet?"
- "Show me all high-risk drones"
- "Get maintenance details for drone drn-101"
- "Log a maintenance event: drone drn-101, 12 flight hours, battery 0.80"

The LLM will automatically use the appropriate MCP tools to answer.




