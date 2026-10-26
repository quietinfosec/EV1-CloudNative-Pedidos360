# rabbitmq-status.ps1
# Muestra el estado del cluster RabbitMQ local. Devuelve error si algun nodo no
# esta healthy o si el nodo 2 no pertenece al cluster.
# Uso:  .\scripts\rabbitmq-status.ps1
#
# Salida:
#   exit 0 -> ambos nodos healthy y el cluster tiene 2 nodos
#   exit 1 -> contenedor ausente, unhealthy, ping fallido o cluster incompleto

$ErrorActionPreference = "Stop"
$root = Resolve-Path "$PSScriptRoot\.."

$container = "pedidos360-rabbitmq"
$container2 = "pedidos360-rabbitmq2"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Pedidos360 - RabbitMQ Cluster Status" -ForegroundColor Cyan
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
    Write-Host "`n[1/4] Estado de los contenedores:" -ForegroundColor Yellow
    docker compose ps

    Write-Host "`n[2/4] Healthcheck de ambos nodos:" -ForegroundColor Yellow
    foreach ($name in @($container, $container2)) {
        docker inspect $name *> $null
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  [FALLO] El contenedor $name no existe." -ForegroundColor Red
            Write-Host "          Ejecuta: .\scripts\rabbitmq-up.ps1"
            exit 1
        }
        $health = (docker inspect -f '{{.State.Health.Status}}' $name)
        if ($health -ne "healthy") {
            Write-Host "  [FALLO] $name estado: $health (se esperaba healthy)" -ForegroundColor Red
            exit 1
        }
        Write-Host "  [OK] $name healthy" -ForegroundColor Green
    }

    Write-Host "`n[3/4] Diagnostico interno de ambos brokers:" -ForegroundColor Yellow
    foreach ($name in @($container, $container2)) {
        docker exec $name rabbitmq-diagnostics ping
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  [FALLO] rabbitmq-diagnostics ping fallo en $name." -ForegroundColor Red
            exit 1
        }
    }

    Write-Host "`n[4/4] Miembros del cluster:" -ForegroundColor Yellow
    $status = (docker exec $container rabbitmqctl cluster_status 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FALLO] No se pudo consultar cluster_status." -ForegroundColor Red
        exit 1
    }
    $status
    if ($status -notmatch 'rabbit@pedidos360-rabbitmq2') {
        Write-Host "  [FALLO] El nodo 2 no aparece en el cluster." -ForegroundColor Red
        Write-Host "          Ejecuta: .\scripts\rabbitmq-up.ps1"
        exit 1
    }
} finally {
    Pop-Location
}

Write-Host "  [OK] Cluster RabbitMQ de 2 nodos operativo." -ForegroundColor Green
Write-Host "       Nodo 1: AMQP 5672 / UI http://localhost:15672" -ForegroundColor Green
Write-Host "       Nodo 2: AMQP 5673 / UI http://localhost:15673" -ForegroundColor Green
Write-Host ""
