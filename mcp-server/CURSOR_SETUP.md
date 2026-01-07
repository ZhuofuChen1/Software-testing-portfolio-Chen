# Cursor MCP 配置指南

## 方法 1: 通过 Cursor 设置界面（推荐）

1. **打开 Cursor 设置**
   - 按 `Ctrl + ,` 或点击 `File` → `Preferences` → `Settings`

2. **搜索 MCP**
   - 在设置搜索框中输入 "MCP" 或 "Model Context Protocol"

3. **添加 MCP 服务器配置**
   - 找到 "MCP Servers" 或类似选项
   - 点击 "Edit in settings.json" 或直接编辑配置文件

4. **添加以下配置**：
```json
{
  "mcpServers": {
    "ilp-maintenance": {
      "command": "python",
      "args": [
        "C:\\Users\\32936\\Desktop\\ILP\\CW1\\mcp-server\\maintenance_mcp_server.py"
      ],
      "cwd": "C:\\Users\\32936\\Desktop\\ILP\\CW1"
    }
  }
}
```

## 方法 2: 直接编辑配置文件

1. **找到 Cursor 配置文件位置**：
   - Windows: `C:\Users\32936\AppData\Roaming\Cursor\User\settings.json`
   - 或者：`%APPDATA%\Cursor\User\settings.json`

2. **打开配置文件**，添加或合并以下内容：
```json
{
  "mcpServers": {
    "ilp-maintenance": {
      "command": "python",
      "args": [
        "C:\\Users\\32936\\Desktop\\ILP\\CW1\\mcp-server\\maintenance_mcp_server.py"
      ],
      "cwd": "C:\\Users\\32936\\Desktop\\ILP\\CW1"
    }
  }
}
```

3. **保存文件**

## 方法 3: 使用项目级配置（如果支持）

在项目根目录创建 `.cursor/mcp.json` 或 `.vscode/mcp.json`：
```json
{
  "mcpServers": {
    "ilp-maintenance": {
      "command": "python",
      "args": [
        "mcp-server/maintenance_mcp_server.py"
      ],
      "cwd": "${workspaceFolder}"
    }
  }
}
```

## 验证配置

1. **重启 Cursor**（必须！）

2. **确保 Spring Boot 服务正在运行**
   ```bash
   # 在项目目录运行
   .\mvnw.cmd spring-boot:run
   ```

3. **在 Cursor 中测试**
   - 打开 Cursor 的 AI 聊天窗口
   - 尝试提问：
     - "What's the maintenance status of the drone fleet?"
     - "Show me all high-risk drones"
     - "Get maintenance details for drone drn-101"

4. **检查 MCP 连接**
   - 查看 Cursor 的输出面板（View → Output）
   - 选择 "MCP" 或 "Model Context Protocol" 频道
   - 应该能看到 MCP 服务器的连接日志

## 故障排除

### 问题 1: MCP 服务器无法启动
- **检查 Python 路径**：确保 `python` 命令在 PATH 中
- **检查文件路径**：确保 `maintenance_mcp_server.py` 路径正确
- **检查依赖**：运行 `pip install -r mcp-server/requirements.txt`

### 问题 2: 工具调用失败
- **检查 Spring Boot 服务**：确保 `http://localhost:8080` 可访问
- **检查 API 连接**：运行 `python mcp-server/test_mcp_server.py`

### 问题 3: Cursor 找不到 MCP 配置
- **确认配置文件位置**：检查 `%APPDATA%\Cursor\User\settings.json`
- **检查 JSON 格式**：确保 JSON 格式正确，没有语法错误
- **重启 Cursor**：配置更改后必须重启

## 测试命令

在 Cursor 的 AI 聊天中尝试这些命令：

```
What's the maintenance status of the drone fleet?
```

```
Show me all high-risk drones that need immediate attention
```

```
Get detailed maintenance plan for drone drn-101
```

```
Log a maintenance event: drone drn-101, 15 flight hours, 8 missions, battery health 0.75
```

## 注意事项

- MCP 服务器需要 Spring Boot 服务运行在 `http://localhost:8080`
- 如果 Spring Boot 运行在不同端口，设置环境变量：
  ```bash
  set ILP_MAINTENANCE_API_URL=http://localhost:YOUR_PORT/api/v1
  ```
- 配置更改后必须重启 Cursor 才能生效




