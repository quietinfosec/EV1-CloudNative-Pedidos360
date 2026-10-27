#!/usr/bin/env bash
# demo-up.sh
# Levanta toda la plataforma Pedidos360 para una demostración local.
# Uso: ./scripts/demo-up.sh
# Requisitos: Docker Desktop, Java 21, Node.js/npm y Git Bash/WSL.

set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT/.demo-logs"
PIDS=()
STARTED_AT="$(date +%s)"

# Nunca continuar con una infraestructura parcialmente levantada.
export COMPOSE_HTTP_TIMEOUT="${COMPOSE_HTTP_TIMEOUT:-120}"

export SPRING_PROFILES_ACTIVE="local"
# Configuración local: PostgreSQL se publica en 55432 para no chocar con 5432.
export DB_HOST="127.0.0.1"
export DB_PORT="55432"
export DB_NAME="pedidos360"
export DB_USERNAME="dbadmin"
export DB_PASSWORD="dbadmin"
export RABBITMQ_HOST="127.0.0.1"
export RABBITMQ_PORT="5672"
export RABBITMQ_MGMT_URL="http://localhost:15672/api"

# Spring Boot/Maven requiere JAVA_HOME. Usar Java 21, la versión del proyecto.
if [[ -z "${JAVA_HOME:-}" ]]; then
  for candidate in \
    "C:/Program Files/Java/jdk-21" \
    "/c/Program Files/Java/jdk-21" \
    "/mnt/c/Program Files/Java/jdk-21"; do
    if [[ -d "$candidate" ]]; then JAVA_HOME="$candidate"; break; fi
  done
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME
  export PATH="$JAVA_HOME/bin:$PATH"
fi

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

command -v npm >/dev/null 2>&1 || fail "npm no está instalado o no está en PATH."
command -v java >/dev/null 2>&1 || fail "Java no está instalado o no está en PATH."

# En WSL puede existir un Docker Linux distinto en /usr/bin/docker.
# Priorizar Docker Desktop de Windows, que sí incluye el plugin Compose.
DOCKER=(docker)
if command -v docker.exe >/dev/null 2>&1 && docker.exe compose version >/dev/null 2>&1; then
  DOCKER=(docker.exe)
elif ! docker compose version >/dev/null 2>&1; then
  fail "Docker Compose no está disponible. Abre Docker Desktop y vuelve a intentarlo."
fi

# Nombre fijo para que Compose no mezcle recursos con clones anteriores.
COMPOSE=("${DOCKER[@]}" compose -p pedidos360)

mkdir -p "$LOG_DIR"

log "Usando ${DOCKER[*]} con proyecto Compose pedidos360"
log "Validando configuración Docker Compose"
cd "$ROOT"
"${COMPOSE[@]}" config --quiet

log "Comprobando contenedores existentes de Pedidos360"
for container in pedidos360-postgres pedidos360-rabbitmq pedidos360-rabbitmq2 pedidos360-rabbitmq2-join pedidos360-keycloak pedidos360-elasticmq; do
  if "${DOCKER[@]}" inspect "$container" >/dev/null 2>&1; then
    project=$("${DOCKER[@]}" inspect --format '{{index .Config.Labels "com.docker.compose.project"}}' "$container" 2>/dev/null || true)
    if [[ "$project" != "pedidos360" ]]; then
      log "Eliminando contenedor antiguo $container (se conservan los volúmenes)"
      "${DOCKER[@]}" rm -f "$container" >/dev/null
    fi
  fi
done

log "Levantando infraestructura: PostgreSQL, RabbitMQ, Keycloak y ElasticMQ"
"${COMPOSE[@]}" up -d --remove-orphans

log "Esperando a que PostgreSQL y RabbitMQ estén disponibles"
for attempt in {1..30}; do
  postgres_ok=0
  rabbit_ok=0
  "${DOCKER[@]}" inspect --format '{{.State.Health.Status}}' pedidos360-postgres 2>/dev/null | grep -q healthy && postgres_ok=1 || true
  "${DOCKER[@]}" inspect --format '{{.State.Health.Status}}' pedidos360-rabbitmq 2>/dev/null | grep -q healthy && rabbit_ok=1 || true
  if [[ "$postgres_ok" == 1 && "$rabbit_ok" == 1 ]]; then break; fi
  if [[ "$attempt" == 30 ]]; then
    "${COMPOSE[@]}" ps >&2 || true
    "${COMPOSE[@]}" logs --tail=80 postgres rabbitmq rabbitmq2 >&2 || true
    fail "La infraestructura no quedó saludable."
  fi
  sleep 2
done

wait_for_url() {
  local url="$1"
  local label="$2"
  local attempts="${3:-45}"
  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if curl -fsS --max-time 2 "$url" >/dev/null 2>&1; then
      log "$label disponible: $url"
      return 0
    fi
    sleep 1
  done
  fail "$label no respondió después de ${attempts}s: $url"
}

open_url() {
  local url="$1"
  if command -v powershell.exe >/dev/null 2>&1; then
    powershell.exe -NoProfile -Command "Start-Process '$url'" >/dev/null 2>&1 || true
  elif command -v explorer.exe >/dev/null 2>&1; then
    explorer.exe "$url" >/dev/null 2>&1 || true
  fi
}

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

# Si un proceso muere al iniciar, mostrar su log inmediatamente.
for pid in "${PIDS[@]}"; do
  kill -0 "$pid" 2>/dev/null || fail "Un backend terminó prematuramente. Revisa .demo-logs/."
done

log "Instalando dependencias del frontend si es necesario"
FRONTEND="$ROOT/frontend/pedidos360-web"
[[ -d "$FRONTEND" ]] || fail "No existe frontend/pedidos360-web"
if [[ ! -d "$FRONTEND/node_modules" ]]; then
  (cd "$FRONTEND" && npm ci --no-audit --no-fund) 2>&1 | tee "$LOG_DIR/frontend-install.log"
fi

log "Iniciando frontend Angular en http://localhost:4200"
(
  cd "$FRONTEND"
  npm start -- --host 127.0.0.1
) >"$LOG_DIR/frontend.log" 2>&1 &
PIDS+=("$!")

wait_for_url "http://127.0.0.1:8180/realms/pedidos360/.well-known/openid-configuration" "Keycloak"
wait_for_url "http://127.0.0.1:8080/actuator/health" "BFF"
wait_for_url "http://127.0.0.1:8081/actuator/health" "Pedidos Service"
wait_for_url "http://127.0.0.1:8082/actuator/health" "Productos Service"
wait_for_url "http://127.0.0.1:8083/actuator/health" "Admin Service"
wait_for_url "http://127.0.0.1:4200" "Frontend Angular"

ELAPSED="$(( $(date +%s) - STARTED_AT ))"
log "Servicios disponibles en ${ELAPSED}s"

cat <<'EOF'

============================================================
 Pedidos360 listo para demostración
============================================================
 Frontend Angular : http://localhost:4200
 BFF/API          : http://localhost:8080/api
 Pedidos API      : http://localhost:8081
 Productos API    : http://localhost:8082
 Admin RabbitMQ   : http://localhost:8083/admin/queues
 Keycloak         : http://localhost:8180
 RabbitMQ UI      : http://localhost:15672   guest / guest
 PostgreSQL       : localhost:55432
 ElasticMQ/SQS    : http://localhost:9324

 Usuario demo Keycloak: demo / Demo1234!

 Logs: .demo-logs/
 Presiona Ctrl+C para detener backend y frontend.
============================================================
EOF

log "Abriendo pantallas necesarias en el navegador"
# El BFF (8080) es una API, no una pantalla. No se abre en el navegador.
# Las únicas pantallas de demostración son la aplicación, Keycloak y RabbitMQ.
open_url "http://localhost:4200/home"
open_url "http://localhost:8180/admin"
open_url "http://localhost:15672"

wait
