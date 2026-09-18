<# 
.SYNOPSIS
Prepara los artefactos de Pedidos360 para despliegue AWS sin subir nada.

.DESCRIPTION
Ejecuta mvn clean package en los tres modulos, copia los JAR y los archivos
systemd a dist/, muestra sus SHA256 y se detiene.
No incluye secretos, .env reales, tokens ni credenciales.
#>

[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Rutas base
$RepoRoot = (Get-Item -LiteralPath (Join-Path $PSScriptRoot '..')).FullName
$DistRoot = Join-Path $RepoRoot 'dist'
$DistBackend = Join-Path $DistRoot 'backend'
$DistSystemd = Join-Path $DistRoot 'systemd'

Write-Host "=== Pedidos360 - Preparacion de artefactos para AWS ===" -ForegroundColor Cyan
Write-Host "Repo: $RepoRoot"
Write-Host "Dist: $DistRoot"
Write-Host ""

# Herramientas: usamos Corretto 21 y Maven 3.9.9 del directorio temporal
$JavaHome = 'C:\Users\Usuario\AppData\Local\Temp\opencode\corretto21\jdk21.0.12_9'
$MavenBin = 'C:\Users\Usuario\AppData\Local\Temp\opencode\maven399\apache-maven-3.9.9\bin'

if (-not (Test-Path -LiteralPath $JavaHome)) {
    Write-Error "No se encontro JAVA_HOME en $JavaHome. Ejecuta primero la preparacion de herramientas."
    exit 1
}
if (-not (Test-Path -LiteralPath (Join-Path $MavenBin 'mvn.cmd'))) {
    Write-Error "No se encontro Maven en $MavenBin."
    exit 1
}

$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$MavenBin;$env:Path"
$env:MAVEN_SKIP_RC = '1'

# java -version escribe a stderr; mostramos directamente
& java -version
& mvn -version
Write-Host ""

# Modulos a compilar
$Modules = @(
    @{ Name = 'bff-service';      JarPattern = 'bff-service-*.jar';      TargetJar = 'bff-service.jar' },
    @{ Name = 'pedidos-service';  JarPattern = 'pedidos-service-*.jar';  TargetJar = 'pedidos-service.jar' },
    @{ Name = 'productos-service';JarPattern = 'productos-service-*.jar';TargetJar = 'productos-service.jar' }
)

# 1. Build de cada modulo
foreach ($m in $Modules) {
    $ModulePath = Join-Path $RepoRoot "backend\$($m.Name)"
    if (-not (Test-Path -LiteralPath $ModulePath)) {
        Write-Error "Modulo no encontrado: $ModulePath"
        exit 1
    }
    Write-Host "--- Building $($m.Name) ---" -ForegroundColor Yellow
    Set-Location -LiteralPath $ModulePath
    mvn clean package
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Fallo el build de $($m.Name)"
        exit 1
    }
}

# 2. Verificar JAR y preparar dist/backend/
Write-Host ""
Write-Host "--- Preparando dist/backend/ ---" -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path $DistBackend | Out-Null
New-Item -ItemType Directory -Force -Path $DistSystemd | Out-Null

$JarPaths = @()
foreach ($m in $Modules) {
    $TargetDir = Join-Path $RepoRoot "backend\$($m.Name)\target"
    $Jar = Get-ChildItem -LiteralPath $TargetDir -Filter $m.JarPattern | Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
    if (-not $Jar) {
        Write-Error "No se encontro JAR en $TargetDir con patron $($m.JarPattern)"
        exit 1
    }
    $Dest = Join-Path $DistBackend $m.TargetJar
    Copy-Item -LiteralPath $Jar.FullName -Destination $Dest -Force
    Write-Host "Copiado: $($Jar.Name) -> $Dest"
    $JarPaths += $Dest
}

# 3. Copiar systemd
Write-Host ""
Write-Host "--- Preparando dist/systemd/ ---" -ForegroundColor Yellow
$SystemdSrc = Join-Path $RepoRoot 'infra\aws\ec2\systemd'
if (Test-Path -LiteralPath $SystemdSrc) {
    Get-ChildItem -LiteralPath $SystemdSrc -Filter '*.service' | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination $DistSystemd -Force
        Write-Host "Copiado systemd: $($_.Name)"
    }
} else {
    Write-Warning "Directorio systemd no encontrado: $SystemdSrc"
}

# 4. Mostrar JARs y SHA256
Write-Host ""
Write-Host "=== Artefactos listos en dist/ ===" -ForegroundColor Green
$Hasher = [System.Security.Cryptography.SHA256]::Create()
try {
    foreach ($JarPath in $JarPaths) {
        $Bytes = [System.IO.File]::ReadAllBytes($JarPath)
        $Hash = $Hasher.ComputeHash($Bytes)
        $Hex = [BitConverter]::ToString($Hash).Replace('-', '').ToLowerInvariant()
        $SizeMB = [math]::Round((New-Object System.IO.FileInfo($JarPath)).Length / 1MB, 1)
        Write-Host "$JarPath"
        Write-Host "  SHA256: $Hex"
        Write-Host "  Size: $SizeMB MB"
    }
} finally {
    $Hasher.Dispose()
}

# 5. Indicar siguiente paso
Write-Host ""
Write-Host "=== SIGUIENTE PASO ===" -ForegroundColor Cyan
Write-Host "Los artefactos estan en dist/ listos para transferencia."
Write-Host "Usar un mecanismo AWS seguro (SSM Run Command, S3 presigned URL, EC2 Instance Connect, etc.)."
Write-Host "No subir nada con este script; se detiene aqui por diseno."