# rabbitmq-up.ps1
# Levanta el broker RabbitMQ local (Docker Compose) y espera a que reporte healthy.
# Uso:  .\scripts\rabbitmq-up.ps1
#
# - No modifica ni reinicia bff-service, pedidos-service ni productos-service.
# - No declara topologia (exchange/colas/bindings): eso lo crea Spring Boot.
#
# AMQP:            localhost:5672
# Management UI:   http://localhost:15672

$ErrorActionPreference = "Stop"
$root = Resolve-Path "$PSScriptRoot\.."

$container = "pedidos360-rabbitmq"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Pedidos360 - RabbitMQ Up" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 0. Preflight: docker disponible y daemon corriendo
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "  [FALLO] Docker no esta instalado o no esta en PATH." -ForegroundColor Red
    exit 1
}
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FALLO] El daemon de Docker no esta corriendo." -ForegroundColor Red
    Write-Host "          Inicia Docker Desktop o el servicio de Docker y reintenta."
    exit 1
}

# 1. Verificar que los puertos de RabbitMQ estan libres
Write-Host "`n[0/4] Verificando puertos de RabbitMQ (5672, 15672)..." -ForegroundColor Yellow
foreach ($port in @(5672, 15672)) {
    $inUse = Test-NetConnection -ComputerName 127.0.0.1 -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($inUse -eq "True") {
        Write-Host "  [FALLO] El puerto $port ya esta ocupado." -ForegroundColor Red
        Write-Host "          Deten el proceso que lo usa o libera el puerto."
        exit 1
    }
    Write-Host "  [OK] Puerto $port libre"

}

Push-Location $root
try {
    # 2. Validar la configuracion del compose antes de aplicarla
    Write-Host "`n[1/4] Validando docker-compose.yml..." -ForegroundColor Yellow
    docker compose config *> $null
    if ($LASTEXITCODE -ne 0) { throw "docker compose config fallo" }
    Write-Host "  [OK] Configuracion valida" -ForegroundColor Green

    # 3. Levantar solo el servicio rabbitmq
    Write-Host "`n[2/4] Levantando rabbitmq..." -ForegroundColor Yellow
    docker compose up -d rabbitmq
    if ($LASTEXITCODE -ne 0) { throw "docker compose up fallo" }
    Write-Host "  [OK] Contenedor creado/iniciado" -ForegroundColor Green

    # 4. Esperar a que el healthcheck reporte healthy
    Write-Host "`n[3/4] Esperando estado healthy..." -ForegroundColor Yellow
    $health = "unknown"
    for ($i = 0; $i -lt 60; $i++) {
        $health = (docker inspect -f '{{.State.Health.Status}}' $container 2>$null)
        if ($LASTEXITCODE -ne 0) { $health = "unknown" }
        if ($health -eq "healthy") { break }
        if ($health -eq "unhealthy") { break }
        Start-Sleep -Seconds 2
    }

    if ($health -ne "healthy") {
        Write-Host "  [FALLO] Estado de healthcheck: $health (se esperaba healthy)" -ForegroundColor Red
        docker compose ps
        exit 1
    }
    Write-Host "  [OK] Estado: healthy" -ForegroundColor Green

    # 5. Diagnostico interno
    Write-Host "`n[4/4] Diagnostico interno..." -ForegroundColor Yellow
    docker exec $container rabbitmq-diagnostics ping
    if ($LASTEXITCODE -ne 0) { throw "rabbitmq-diagnostics ping fallo" }
} finally {
    Pop-Location
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " RabbitMQ listo" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RabbitMQ AMQP:" -ForegroundColor Green
Write-Host "localhost:5672"
Write-Host ""
Write-Host "RabbitMQ Management:" -ForegroundColor Green
Write-Host "http://localhost:15672"
Write-Host ""
$rabbitUser = if ($env:RABBITMQ_USER) { $env:RABBITMQ_USER } else { "guest" }
$rabbitPass = if ($env:RABBITMQ_PASSWORD) { $env:RABBITMQ_PASSWORD } else { "guest" }
Write-Host "Credenciales locales: $rabbitUser / $rabbitPass"
Write-Host ""
Write-Host "Nota: bff-service (:8080), pedidos-service (:8081) y productos-service" -ForegroundColor DarkGray
Write-Host "      (:8082) no fueron afectados por este script." -ForegroundColor DarkGray
Write-Host ""