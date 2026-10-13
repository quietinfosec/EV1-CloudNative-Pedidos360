# rabbitmq-down.ps1
# Detiene el broker RabbitMQ local (Docker Compose) conservando el volumen.
# Uso:  .\scripts\rabbitmq-down.ps1
#
# NO usa 'docker compose down -v': el volumen rabbitmq_data se conserva para no
# perder la persistencia local. Para un reset completo y consciente usa:
#   docker compose down -v

$ErrorActionPreference = "Stop"
$root = Resolve-Path "$PSScriptRoot\.."

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Pedidos360 - RabbitMQ Down" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "  [FALLO] Docker no esta instalado o no esta en PATH." -ForegroundColor Red
    exit 1
}

Push-Location $root
try {
    Write-Host "`n[1/1] Deteniendo contenedores del compose (sin -v)..." -ForegroundColor Yellow
    docker compose down
    if ($LASTEXITCODE -ne 0) { throw "docker compose down fallo" }
} finally {
    Pop-Location
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " RabbitMQ detenido." -ForegroundColor Cyan
Write-Host " El volumen rabbitmq_data se conserva." -ForegroundColor Yellow
Write-Host " Reset completo: docker compose down -v" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""