# aws-check.ps1
# Verifica que AWS CLI este instalado y configurado.
# NO crea ningun recurso. NO genera costos.
# Uso:  .\scripts\aws-check.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Pedidos360 - AWS Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. aws --version
Write-Host "`n[1/2] Verificando AWS CLI..." -ForegroundColor Yellow
$aws = Get-Command aws -ErrorAction SilentlyContinue
if (-not $aws) {
    Write-Host "  [FALLO] AWS CLI no esta instalado o no esta en PATH." -ForegroundColor Red
    Write-Host "          Instala con: pip install awscli  o  winget install Amazon.AWSCLI" -ForegroundColor DarkYellow
    exit 1
}
$version = aws --version 2>&1
Write-Host "  [OK] AWS CLI: $version" -ForegroundColor Green

# 2. aws sts get-caller-identity
Write-Host "`n[2/2] Verificando credenciales (get-caller-identity)..." -ForegroundColor Yellow
try {
    $identity = aws sts get-caller-identity 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] Credenciales validas:" -ForegroundColor Green
        Write-Host "  $identity" -ForegroundColor Gray
    } else {
        Write-Host "  [FALLO] No hay credenciales configuradas." -ForegroundColor Red
        Write-Host "          Ejecuta: aws configure" -ForegroundColor DarkYellow
        exit 1
    }
} catch {
    Write-Host "  [FALLO] Error al verificar credenciales: $_" -ForegroundColor Red
    exit 1
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " AWS Check OK. No se crearon recursos." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
