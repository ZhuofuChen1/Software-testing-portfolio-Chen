# Cursor MCP 自动配置脚本
# 运行此脚本将自动配置 Cursor 的 MCP 设置

$CursorConfigPath = "$env:APPDATA\Cursor\User\settings.json"
$ProjectRoot = "C:\Users\32936\Desktop\ILP\CW1"
$McpScriptPath = "$ProjectRoot\mcp-server\maintenance_mcp_server.py"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Cursor MCP 配置脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Cursor 配置目录
if (-not (Test-Path "$env:APPDATA\Cursor\User")) {
    Write-Host "[错误] Cursor 配置目录不存在: $env:APPDATA\Cursor\User" -ForegroundColor Red
    Write-Host "请先安装并运行一次 Cursor" -ForegroundColor Yellow
    exit 1
}

# 检查 MCP 脚本是否存在
if (-not (Test-Path $McpScriptPath)) {
    Write-Host "[错误] MCP 服务器脚本不存在: $McpScriptPath" -ForegroundColor Red
    exit 1
}

Write-Host "[信息] 找到 MCP 服务器脚本: $McpScriptPath" -ForegroundColor Green

# 读取现有配置
$configJson = "{}"
if (Test-Path $CursorConfigPath) {
    try {
        $configJson = Get-Content $CursorConfigPath -Raw
        Write-Host "[信息] 读取现有 Cursor 配置" -ForegroundColor Green
    } catch {
        Write-Host "[警告] 无法读取现有配置文件，将创建新配置" -ForegroundColor Yellow
    }
} else {
    Write-Host "[信息] 配置文件不存在，将创建新配置" -ForegroundColor Yellow
}

# 解析 JSON
$config = $configJson | ConvertFrom-Json

# 确保 mcpServers 对象存在
if (-not $config.mcpServers) {
    $config | Add-Member -MemberType NoteProperty -Name "mcpServers" -Value (New-Object PSObject) -Force
}

# 创建 MCP 服务器配置对象
$mcpServer = New-Object PSObject
$mcpServer | Add-Member -MemberType NoteProperty -Name "command" -Value "python"
$mcpServer | Add-Member -MemberType NoteProperty -Name "args" -Value @($McpScriptPath)
$mcpServer | Add-Member -MemberType NoteProperty -Name "cwd" -Value $ProjectRoot

# 添加到配置
$config.mcpServers | Add-Member -MemberType NoteProperty -Name "ilp-maintenance" -Value $mcpServer -Force

# 备份原配置
if (Test-Path $CursorConfigPath) {
    $backupPath = "$CursorConfigPath.backup.$(Get-Date -Format 'yyyyMMddHHmmss')"
    Copy-Item $CursorConfigPath $backupPath
    Write-Host "[信息] 已备份原配置到: $backupPath" -ForegroundColor Green
}

# 保存配置
try {
    $config | ConvertTo-Json -Depth 10 | Set-Content $CursorConfigPath -Encoding UTF8
    Write-Host "[成功] MCP 配置已添加到: $CursorConfigPath" -ForegroundColor Green
    Write-Host ""
    Write-Host "配置内容:" -ForegroundColor Cyan
    $mcpConfig = @{
        mcpServers = @{
            "ilp-maintenance" = @{
                command = "python"
                args = @($McpScriptPath)
                cwd = $ProjectRoot
            }
        }
    }
    $mcpConfig | ConvertTo-Json -Depth 10 | Write-Host -ForegroundColor Gray
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "下一步操作:" -ForegroundColor Yellow
    Write-Host "1. 确保 Spring Boot 服务正在运行" -ForegroundColor White
    Write-Host "2. 重启 Cursor" -ForegroundColor White
    Write-Host "3. 在 Cursor 中测试: 'What's the maintenance status of the fleet?'" -ForegroundColor White
    Write-Host "========================================" -ForegroundColor Cyan
} catch {
    Write-Host "[错误] 无法保存配置: $_" -ForegroundColor Red
    exit 1
}

