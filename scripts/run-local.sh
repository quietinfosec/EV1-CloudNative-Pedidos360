#!/usr/bin/env bash
# run-local.sh
# Inicia los tres microservicios backend en perfil local.
# Uso:  ./scripts/run-local.sh
#
# bff-service      :8080
# pedidos-service  :8081
# productos-service:8082

set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

export SPRING_PROFILES_ACTIVE=local

PIDS=()

cleanup() {
  echo ""
  echo "Deteniendo microservicios..."
  for PID in "${PIDS[@]}"; do
    kill "$PID" 2>/dev/null || true
  done
  echo "Todos detenidos."
}
trap cleanup EXIT INT TERM

start_service() {
  local name="$1"
  local port="$2"
  local path="$ROOT/backend/$name"
  if [ ! -d "$path" ]; then echo "ERROR: No se encontro $path"; exit 1; fi
  echo "Iniciando $name en puerto $port..."
  cd "$path"
  if [ -f "./mvnw" ]; then
    chmod +x ./mvnw
    ./mvnw spring-boot:run &
  elif command -v mvn >/dev/null 2>&1; then
    mvn spring-boot:run &
  else
    echo "ERROR: No se encontro mvnw ni mvn"; exit 1
  fi
  PIDS+=($!)
  sleep 2
}

start_service "bff-service"       8080
start_service "pedidos-service"   8081
start_service "productos-service" 8082

echo ""
echo "Microservicios iniciados:"
echo "  bff-service       -> http://localhost:8080"
echo "  pedidos-service   -> http://localhost:8081"
echo "  productos-service -> http://localhost:8082"
echo ""
echo "Presiona Ctrl+C para detener todos."
echo ""

wait
