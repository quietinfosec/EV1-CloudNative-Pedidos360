# build-all.ps1
# Compila el frontend y los tres microservicios backend de Pedidos360.
# Uso:  .\scripts\build-all.ps1
#
# - Frontend:  npm run build
# - Backend:   mvn clean package  (genera JAR en target/)

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

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Pedidos360 - Build All" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# --- Frontend ---
Write-Host "`n[1/4] Compilando frontend (Angular)..." -ForegroundColor Yellow
$fe = Join-Path $root "frontend\pedidos360-web"
if (-not (Test-Path $fe)) { throw "No se encontro $fe" }
Push-Location $fe
try {
    if (-not (Test-Path "node_modules")) {
        Write-Host "  node_modules no encontrado. Ejecutando npm install..." -ForegroundColor DarkYellow
        npm install
        if ($LASTEXITCODE -ne 0) { throw "npm install fallo" }
    }
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "npm run build fallo" }
    Write-Host "  [OK] Frontend compilado" -ForegroundColor Green
} finally { Pop-Location }

# --- Backend ---
$services = @("bff-service", "pedidos-service", "productos-service")
$i = 2
foreach ($svc in $services) {
    Write-Host "`n[$i/4] Compilando $svc (mvn clean package)..." -ForegroundColor Yellow
    $svcPath = Join-Path $root "backend\$svc"
    if (-not (Test-Path $svcPath)) { throw "No se encontro $svcPath" }
    Push-Location $svcPath
    try {
        $mvnw = ".\mvnw.cmd"
        if (-not (Test-Path $mvnw)) { throw "No se encontro mvnw.cmd en $svcPath" }
        & $mvnw clean package
        if ($LASTEXITCODE -ne 0) { throw "mvn clean package fallo para $svc" }
        $jar = Get-ChildItem "target\*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike "*-sources*" -and $_.Name -notlike "*-javadoc*" } | Select-Object -First 1
        if ($jar) {
            Write-Host "  [OK] $svc -> $($jar.Name)" -ForegroundColor Green
        } else {
            Write-Host "  [OK] $svc compilado (sin JAR encontrado)" -ForegroundColor Green
        }
    } finally { Pop-Location }
    $i++
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " Build All completado." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
