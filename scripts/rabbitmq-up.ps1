# rabbitmq-up.ps1
# Levanta el cluster RabbitMQ local de 2 nodos (Docker Compose) y espera a que
# ambos nodos reporten healthy y el nodo 2 se una al cluster.
# Uso:  .\scripts\rabbitmq-up.ps1
#
# - No modifica ni reinicia bff-service, pedidos-service, productos-service ni admin-service.
# - No declara topologia (exchange/colas/bindings): eso lo crea Spring Boot.
#
# Nodo 1 AMQP:      localhost:5672
# Nodo 1 UI:        http://localhost:15672
# Nodo 2 AMQP:      localhost:5673
# Nodo 2 UI:        http://localhost:15673

$ErrorActionPreference = "Stop"
$root = Resolve-Path "$PSScriptRoot\.."

$container = "pedidos360-rabbitmq"
$container2 = "pedidos360-rabbitmq2"
$joinContainer = "pedidos360-rabbitmq2-join"

function Wait-Healthy([string]$name, [int]$attempts = 60) {
    $health = "unknown"
    for ($i = 0; $i -lt $attempts; $i++) {
        $health = (docker inspect -f '{{.State.Health.Status}}' $name 2>$null)
        if ($LASTEXITCODE -ne 0) { $health = "unknown" }
        if ($health -eq "healthy") { return $health }
        if ($health -eq "unhealthy") { return $health }
        Start-Sleep -Seconds 2
    }
    return $health
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Pedidos360 - RabbitMQ Cluster Up" -ForegroundColor Cyan
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

# 1. Verificar que los puertos de ambos nodos estan libres.
# Si el cluster ya tiene contenedores activos, los puertos pertenecen a nuestros
# nodos y el script debe poder re-ejecutarse sin fallar (idempotencia).
$runningNodes = docker ps --filter "name=pedidos360-rabbitmq" --format "{{.Names}}" 2>$null
if ($runningNodes) {
    Write-Host "`n[0/5] Cluster ya tiene contenedores activos; se omite el chequeo de puertos." -ForegroundColor Yellow
} else {
    Write-Host "`n[0/5] Verificando puertos (5672, 15672, 5673, 15673)..." -ForegroundColor Yellow
    foreach ($port in @(5672, 15672, 5673, 15673)) {
        $inUse = Test-NetConnection -ComputerName 127.0.0.1 -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue
        if ($inUse -eq "True") {
            Write-Host "  [FALLO] El puerto $port ya esta ocupado." -ForegroundColor Red
            Write-Host "          Deten el proceso que lo usa o libera el puerto."
            exit 1
        }
        Write-Host "  [OK] Puerto $port libre"
    }
}

Push-Location $root
try {
    # 2. Validar la configuracion del compose antes de aplicarla
    Write-Host "`n[1/5] Validando docker-compose.yml..." -ForegroundColor Yellow
    docker compose config *> $null
    if ($LASTEXITCODE -ne 0) { throw "docker compose config fallo" }
    Write-Host "  [OK] Configuracion valida" -ForegroundColor Green

    # 3. Levantar los dos nodos del cluster
    Write-Host "`n[2/5] Levantando nodos rabbitmq y rabbitmq2..." -ForegroundColor Yellow
    docker compose up -d rabbitmq rabbitmq2
    if ($LASTEXITCODE -ne 0) { throw "docker compose up fallo" }
    Write-Host "  [OK] Contenedores creados/iniciados" -ForegroundColor Green

    # 4. Esperar a que ambos healthchecks reporten healthy
    Write-Host "`n[3/5] Esperando estado healthy en ambos nodos..." -ForegroundColor Yellow
    $health1 = Wait-Healthy $container
    $health2 = Wait-Healthy $container2
    if ($health1 -ne "healthy" -or $health2 -ne "healthy") {
        Write-Host "  [FALLO] Nodo 1: $health1 | Nodo 2: $health2" -ForegroundColor Red
        docker compose ps
        exit 1
    }
    Write-Host "  [OK] Nodo 1 healthy | Nodo 2 healthy" -ForegroundColor Green

    # 5. Unir el nodo 2 al cluster (contenedor efimero e idempotente)
    Write-Host "`n[4/5] Incorporando rabbitmq2 al cluster..." -ForegroundColor Yellow
    docker compose up -d --force-recreate rabbitmq2-join
    if ($LASTEXITCODE -ne 0) { throw "No se pudo iniciar rabbitmq2-join" }

    $joinExit = $null
    for ($i = 0; $i -lt 60; $i++) {
        $status = (docker inspect -f '{{.State.Status}} {{.State.ExitCode}}' $joinContainer 2>$null)
        if ($LASTEXITCODE -ne 0) { Start-Sleep -Seconds 1; continue }
        if ($status -match '^exited\s+(\d+)$') {
            $joinExit = [int]$Matches[1]
            break
        }
        Start-Sleep -Seconds 2
    }

    if ($null -eq $joinExit) {
        Write-Host "  [FALLO] rabbitmq2-join no termino a tiempo." -ForegroundColor Red
        docker logs $joinContainer
        exit 1
    }
    if ($joinExit -ne 0) {
        Write-Host "  [FALLO] rabbitmq2-join termino con codigo $joinExit." -ForegroundColor Red
        docker logs $joinContainer
        exit 1
    }
    Write-Host "  [OK] Nodo 2 unido al cluster" -ForegroundColor Green

    # 6. Verificar cluster desde ambos nodos
    Write-Host "`n[5/5] Verificando cluster_status..." -ForegroundColor Yellow
    docker exec $container rabbitmqctl cluster_status
    if ($LASTEXITCODE -ne 0) { throw "cluster_status fallo en nodo 1" }
    docker exec $container2 rabbitmqctl cluster_status
    if ($LASTEXITCODE -ne 0) { throw "cluster_status fallo en nodo 2" }
} finally {
    Pop-Location
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " Cluster RabbitMQ listo (2 nodos)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Nodo 1 AMQP:      localhost:5672" -ForegroundColor Green
Write-Host "Nodo 1 UI:        http://localhost:15672" -ForegroundColor Green
Write-Host "Nodo 2 AMQP:      localhost:5673" -ForegroundColor Green
Write-Host "Nodo 2 UI:        http://localhost:15673" -ForegroundColor Green
Write-Host ""
$rabbitUser = if ($env:RABBITMQ_USER) { $env:RABBITMQ_USER } else { "guest" }
$rabbitPass = if ($env:RABBITMQ_PASSWORD) { $env:RABBITMQ_PASSWORD } else { "guest" }
Write-Host "Credenciales locales: $rabbitUser / $rabbitPass"
Write-Host ""
Write-Host "Nota: bff-service (:8080), pedidos-service (:8081), productos-service" -ForegroundColor DarkGray
Write-Host "      (:8082) y admin-service (:8083) no fueron afectados por este script." -ForegroundColor DarkGray
Write-Host ""
