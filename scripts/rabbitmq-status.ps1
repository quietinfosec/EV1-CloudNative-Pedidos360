# rabbitmq-status.ps1
# Muestra el estado del broker RabbitMQ local. Devuelve error si NO esta healthy.
# Uso:  .\scripts\rabbitmq-status.ps1
#
# Salida:
#   exit 0 -> el contenedor existe y reporta healthy
#   exit 1 -> el contenedor no existe, o no esta healthy, o el ping falla

$ErrorActionPreference = "Stop"
$root = Resolve-Path "$PSScriptRoot\.."

$container = "pedidos360-rabbitmq"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Pedidos360 - RabbitMQ Status" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "  [FALLO] Docker no esta instalado o no esta en PATH." -ForegroundColor Red
    exit 1
}
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FALLO] El daemon de Docker no esta corriendo." -ForegroundColor Red
    exit 1
}

Push-Location $root
try {
    Write-Host "`n[1/2] Estado de los contenedores:" -ForegroundColor Yellow
    docker compose ps

    Write-Host "`n[2/2] Diagnostico interno del broker:" -ForegroundColor Yellow
    docker inspect $container *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FALLO] El contenedor $container no existe." -ForegroundColor Red
        Write-Host "          Ejecuta: .\scripts\rabbitmq-up.ps1"
        exit 1
    }

    $health = (docker inspect -f '{{.State.Health.Status}}' $container)
    if ($health -ne "healthy") {
        Write-Host "  [FALLO] Estado de healthcheck: $health (se esperaba healthy)" -ForegroundColor Red
        exit 1
    }

    docker exec $container rabbitmq-diagnostics ping
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FALLO] rabbitmq-diagnostics ping respondio con error." -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

Write-Host "`n  [OK] RabbitMQ healthy. AMQP localhost:5672 / UI http://localhost:15672" -ForegroundColor Green
Write-Host ""