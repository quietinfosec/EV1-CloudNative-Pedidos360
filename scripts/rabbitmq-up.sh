#!/usr/bin/env bash
# rabbitmq-up.sh
# Levanta el broker RabbitMQ local (Docker Compose) y espera a que reporte healthy.
# Uso:  ./scripts/rabbitmq-up.sh
#
# - No modifica ni reinicia bff-service, pedidos-service ni productos-service.
# - No declara topologia (exchange/colas/bindings): eso lo crea Spring Boot.
#
# AMQP:            localhost:5672
# Management UI:   http://localhost:15672

set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

CONTAINER="pedidos360-rabbitmq"

echo "========================================"
echo " Pedidos360 - RabbitMQ Up"
echo "========================================"

# 0. Preflight: docker disponible y daemon corriendo
if ! command -v docker >/dev/null 2>&1; then
  echo "  [FALLO] Docker no esta instalado o no esta en PATH."
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "  [FALLO] El daemon de Docker no esta corriendo."
  echo "          Inicia Docker Desktop o el servicio de Docker y reintenta."
  exit 1
fi

# 1. Verificar que los puertos de RabbitMQ estan libres
for PORT in 5672 15672; do
  if command -v nc >/dev/null 2>&1 && nc -z 127.0.0.1 "$PORT" >/dev/null 2>&1; then
    echo "  [FALLO] El puerto $PORT ya esta ocupado."
    echo "          Deten el proceso que lo usa o libera el puerto."
    exit 1
  fi
done

# 2. Validar la configuracion del compose antes de aplicarla
echo ""
echo "[1/3] Validando docker-compose.yml..."
docker compose config >/dev/null
echo "  [OK] Configuracion valida"

# 3. Levantar solo el servicio rabbitmq
echo ""
echo "[2/3] Levantando rabbitmq..."
docker compose up -d rabbitmq
echo "  [OK] Contenedor creado/iniciado"

# 4. Esperar a que el healthcheck reporte healthy
echo ""
echo "[3/3] Esperando estado healthy..."
STATUS=""
for _ in $(seq 1 60); do
  STATUS="$(docker inspect -f '{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || echo "unknown")"
  case "$STATUS" in
    healthy) echo "  [OK] Estado: healthy"; break ;;
    unhealthy) echo "  [FALLO] El contenedor quedo unhealthy."; docker compose ps; exit 1 ;;
  esac
  sleep 2
done

if [ "$STATUS" != "healthy" ]; then
  echo "  [FALLO] Timeout esperando 'healthy' (estado actual: $STATUS)."
  docker compose ps
  exit 1
fi

docker exec "$CONTAINER" rabbitmq-diagnostics ping

echo ""
echo "========================================"
echo " RabbitMQ listo"
echo "========================================"
echo "RabbitMQ AMQP:"
echo "localhost:5672"
echo ""
echo "RabbitMQ Management:"
echo "http://localhost:15672"
echo ""
echo "Credenciales locales: ${RABBITMQ_USER:-guest} / ${RABBITMQ_PASSWORD:-guest}"
echo ""
echo "Nota: bff-service (:8080), pedidos-service (:8081) y productos-service"
echo "      (:8082) no fueron afectados por este script."
echo ""