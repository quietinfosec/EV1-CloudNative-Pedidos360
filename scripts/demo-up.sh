#!/usr/bin/env bash
# demo-up.sh
# Levanta toda la plataforma Pedidos360 para una demostración local.
# Uso: ./scripts/demo-up.sh
# Requisitos: Docker Desktop, Java 21, Node.js/npm y Git Bash/WSL.

set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT/.demo-logs"
PIDS=()

export SPRING_PROFILES_ACTIVE="local"

log() { printf '\n[%s] %s\n' "$(date '+%H:%M:%S')" "$*"; }
fail() { printf '\nERROR: %s\n' "$*" >&2; exit 1; }

cleanup() {
  local code=$?
  trap - EXIT INT TERM
  printf '\n\nDeteniendo servicios de la demostración...\n'
  for pid in "${PIDS[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
  wait 2>/dev/null || true
  printf 'Los contenedores quedan activos para conservar sus datos.\n'
  printf 'Para detener la infraestructura: docker compose down\n'
  exit "$code"
}
trap cleanup EXIT INT TERM

command -v docker >/dev/null 2>&1 || fail "Docker no está instalado o no está en PATH."
command -v npm >/dev/null 2>&1 || fail "npm no está instalado o no está en PATH."
command -v java >/dev/null 2>&1 || fail "Java no está instalado o no está en PATH."

docker compose version >/dev/null 2>&1 || fail "Docker Compose no está disponible. Abre Docker Desktop y vuelve a intentarlo."

mkdir -p "$LOG_DIR"

log "Validando configuración Docker Compose"
cd "$ROOT"
docker compose config --quiet

log "Levantando infraestructura: PostgreSQL, RabbitMQ, Keycloak y ElasticMQ"
docker compose up -d

log "Esperando a que PostgreSQL y RabbitMQ estén disponibles"
for attempt in {1..30}; do
  postgres_ok=0
  rabbit_ok=0
  docker inspect --format '{{.State.Health.Status}}' pedidos360-postgres 2>/dev/null | grep -q healthy && postgres_ok=1 || true
  docker inspect --format '{{.State.Health.Status}}' pedidos360-rabbitmq 2>/dev/null | grep -q healthy && rabbit_ok=1 || true
  if [[ "$postgres_ok" == 1 && "$rabbit_ok" == 1 ]]; then break; fi
  [[ "$attempt" == 30 ]] && fail "La infraestructura no quedó saludable. Revisa: docker compose logs postgres rabbitmq"
  sleep 2
done

start_backend() {
  local name="$1"
  local port="$2"
  local service_dir="$ROOT/backend/$name"
  local log_file="$LOG_DIR/$name.log"

  [[ -d "$service_dir" ]] || fail "No existe backend/$name"
  [[ -f "$service_dir/mvnw" || -f "$service_dir/mvnw.cmd" ]] || fail "No existe Maven Wrapper para $name"

  log "Iniciando $name en http://localhost:$port"
  (
    cd "$service_dir"
    if [[ -x ./mvnw ]]; then
      ./mvnw spring-boot:run
    elif [[ -f ./mvnw ]]; then
      bash ./mvnw spring-boot:run
    else
      ./mvnw.cmd spring-boot:run
    fi
  ) >"$log_file" 2>&1 &
  PIDS+=("$!")
}

start_backend "bff-service" 8080
start_backend "pedidos-service" 8081
start_backend "productos-service" 8082
start_backend "rabbitmq-admin-service" 8083

log "Instalando dependencias del frontend si es necesario"
FRONTEND="$ROOT/frontend/pedidos360-web"
[[ -d "$FRONTEND" ]] || fail "No existe frontend/pedidos360-web"
if [[ ! -d "$FRONTEND/node_modules" ]]; then
  (cd "$FRONTEND" && npm install) 2>&1 | tee "$LOG_DIR/frontend-install.log"
fi

log "Iniciando frontend Angular en http://localhost:4200"
(
  cd "$FRONTEND"
  npm start -- --host 127.0.0.1
) >"$LOG_DIR/frontend.log" 2>&1 &
PIDS+=("$!")

sleep 8

cat <<'EOF'

============================================================
 Pedidos360 listo para demostración
============================================================
 Frontend Angular : http://localhost:4200
 BFF              : http://localhost:8080
 Pedidos Service  : http://localhost:8081
 Productos        : http://localhost:8082
 Admin RabbitMQ   : http://localhost:8083
 Keycloak         : http://localhost:8180
 RabbitMQ UI      : http://localhost:15672   guest / guest
 PostgreSQL       : localhost:55432
 ElasticMQ/SQS    : http://localhost:9324

 Usuario demo Keycloak: demo / Demo1234!

 Logs: .demo-logs/
 Presiona Ctrl+C para detener backend y frontend.
============================================================
EOF

wait
