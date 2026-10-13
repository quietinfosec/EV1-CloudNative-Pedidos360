#!/usr/bin/env bash
# rabbitmq-status.sh
# Muestra el estado del broker RabbitMQ local. Devuelve error si NO esta healthy.
# Uso:  ./scripts/rabbitmq-status.sh
#
# Salida:
#   exit 0 -> el contenedor existe y reporta healthy
#   exit 1 -> el contenedor no existe, o no esta healthy, o el ping falla

set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

CONTAINER="pedidos360-rabbitmq"

echo "========================================"
echo " Pedidos360 - RabbitMQ Status"
echo "========================================"

if ! command -v docker >/dev/null 2>&1; then
  echo "  [FALLO] Docker no esta instalado o no esta en PATH."
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "  [FALLO] El daemon de Docker no esta corriendo."
  exit 1
fi

echo ""
echo "[1/2] Estado de los contenedores:"
docker compose ps

echo ""
echo "[2/2] Diagnostico interno del broker:"
if ! docker inspect "$CONTAINER" >/dev/null 2>&1; then
  echo "  [FALLO] El contenedor $CONTAINER no existe."
  echo "          Ejecuta: ./scripts/rabbitmq-up.sh"
  exit 1
fi

HEALTH="$(docker inspect -f '{{.State.Health.Status}}' "$CONTAINER")"
if [ "$HEALTH" != "healthy" ]; then
  echo "  [FALLO] Estado de healthcheck: $HEALTH (se esperaba healthy)"
  exit 1
fi

if ! docker exec "$CONTAINER" rabbitmq-diagnostics ping; then
  echo "  [FALLO] rabbitmq-diagnostics ping respondio con error."
  exit 1
fi

echo ""
echo "  [OK] RabbitMQ healthy. AMQP localhost:5672 / UI http://localhost:15672"
echo ""