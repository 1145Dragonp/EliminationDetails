# LittleSkin 认证助手
# 通过用户名和密码自动获取 UUID 和 Token

param(
    [Parameter(Mandatory=$true)]
    [string]$username,
    
    [Parameter(Mandatory=$true)]
    [string]$password
)

$authServer = "https://littleskin.cn/api/yggdrasil"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  LittleSkin 认证助手" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Authenticate
Write-Host "正在认证..." -ForegroundColor Yellow

$authBody = @{
    agent = @{
        name = "Minecraft"
        version = 1
    }
    username = $username
    password = $password
    clientToken = [guid]::NewGuid().ToString()
    requestUser = $true
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$authServer/authenticate" -Method Post -ContentType "application/json" -Body $authBody
    
    $accessToken = $response.accessToken
    $selectedProfile = $response.selectedProfile
    
    if ($selectedProfile) {
        $uuid = $selectedProfile.id
        $playerName = $selectedProfile.name
        
        Write-Host "认证成功!" -ForegroundColor Green
        Write-Host ""
        Write-Host "用户信息:" -ForegroundColor Cyan
        Write-Host "  用户名: $playerName" -ForegroundColor White
        Write-Host "  UUID: $uuid" -ForegroundColor White
        Write-Host "  Token: $accessToken" -ForegroundColor White
        Write-Host ""
        Write-Host "=========================================" -ForegroundColor Cyan
        Write-Host "  使用以下命令启动游戏:" -ForegroundColor Cyan
        Write-Host "=========================================" -ForegroundColor Cyan
        Write-Host ""
        Write-Host ".\gradlew starmc -Pusername=$playerName -Puuid=$uuid -Ptoken=$accessToken -Pauthserver=$authServer" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "提示: 点击上面的命令可以复制" -ForegroundColor Gray
    } else {
        Write-Host "错误: 未找到角色配置文件" -ForegroundColor Red
        Write-Host "请确保你的账号有 Minecraft 角色" -ForegroundColor Yellow
    }
} catch {
    Write-Host "认证失败!" -ForegroundColor Red
    Write-Host ""
    Write-Host "错误信息: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "可能的原因:" -ForegroundColor Yellow
    Write-Host "  1. 用户名或密码错误" -ForegroundColor White
    Write-Host "  2. 网络连接问题" -ForegroundColor White
    Write-Host "  3. LittleSkin 服务器暂时不可用" -ForegroundColor White
}

Write-Host ""
Write-Host "按任意键退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
