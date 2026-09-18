# run-local.ps1
# Inicia los tres microservicios backend en perfil local.
# Uso:  .\scripts\run-local.ps1
#
# bff-service      :8080
# pedidos-service  :8081
# productos-service:8082

$ErrorActionPreference = "Stop"
$root = Resolve-Path "$PSScriptRoot\.."

# Detectar JAVA_HOME si no esta configurado
if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem "C:\Program Files\Java" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) {
        $env:JAVA_HOME = $jdk.FullName
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    }
}

$env:SPRING_PROFILES_ACTIVE = "local"

$services = @(
    @{ Name = "bff-service";       Port = 8080 },
    @{ Name = "pedidos-service";   Port = 8081 },
    @{ Name = "productos-service"; Port = 8082 }
)

$jobs = @()
foreach ($svc in $services) {
    $svcPath = Join-Path $root "backend\$($svc.Name)"
    if (-not (Test-Path $svcPath)) { throw "No se encontro $svcPath" }
    Write-Host "Iniciando $($svc.Name) en puerto $($svc.Port)..." -ForegroundColor Yellow
    $job = Start-Job -ScriptBlock {
        param($p)
        Set-Location $p
        & .\mvnw.cmd spring-boot:run
    } -ArgumentList $svcPath
    $jobs += $job
    Start-Sleep -Seconds 2
}

Write-Host "`nMicroservicios iniciados:" -ForegroundColor Green
Write-Host "  bff-service       -> http://localhost:8080" -ForegroundColor Cyan
Write-Host "  pedidos-service   -> http://localhost:8081" -ForegroundColor Cyan
Write-Host "  productos-service -> http://localhost:8082" -ForegroundColor Cyan
Write-Host "`nPresiona Ctrl+C para detener todos.`n" -ForegroundColor DarkGray

try {
    while ($true) { Start-Sleep -Seconds 5 }
} finally {
    Write-Host "`nDeteniendo microservicios..." -ForegroundColor Yellow
    foreach ($job in $jobs) {
        Stop-Job -Job $job -ErrorAction SilentlyContinue
        Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
    }
    Write-Host "Todos detenidos." -ForegroundColor Green
}
