#!/usr/bin/env bash
# build-all.sh
# Compila el frontend y los tres microservicios backend de Pedidos360.
# Uso:  ./scripts/build-all.sh
#
# - Frontend:  npm run build
# - Backend:   mvn clean package  (genera JAR en target/)

set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "========================================"
echo " Pedidos360 - Build All"
echo "========================================"

# --- Frontend ---
echo ""
echo "[1/4] Compilando frontend (Angular)..."
FE="$ROOT/frontend/pedidos360-web"
if [ ! -d "$FE" ]; then echo "ERROR: No se encontro $FE"; exit 1; fi
cd "$FE"
if [ ! -d "node_modules" ]; then
  echo "  node_modules no encontrado. Ejecutando npm install..."
  npm install
fi
npm run build
echo "  [OK] Frontend compilado"

# --- Backend ---
SERVICES=("bff-service" "pedidos-service" "productos-service")
i=2
for SVC in "${SERVICES[@]}"; do
  echo ""
  echo "[$i/4] Compilando $SVC (mvn clean package)..."
  SVC_PATH="$ROOT/backend/$SVC"
  if [ ! -d "$SVC_PATH" ]; then echo "ERROR: No se encontro $SVC_PATH"; exit 1; fi
  cd "$SVC_PATH"
  if [ -f "./mvnw" ]; then
    chmod +x ./mvnw
    ./mvnw clean package
  elif command -v mvn >/dev/null 2>&1; then
    mvn clean package
  else
    echo "ERROR: No se encontro mvnw ni mvn en PATH"; exit 1
  fi
  JAR=$(find target -maxdepth 1 -name "*.jar" ! -name "*-sources*" ! -name "*-javadoc*" 2>/dev/null | head -1)
  if [ -n "$JAR" ]; then
    echo "  [OK] $SVC -> $(basename "$JAR")"
  else
    echo "  [OK] $SVC compilado (sin JAR encontrado)"
  fi
  i=$((i + 1))
done

echo ""
echo "========================================"
echo " Build All completado."
echo "========================================"
