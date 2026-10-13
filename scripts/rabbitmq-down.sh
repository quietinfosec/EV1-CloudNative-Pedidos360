#!/usr/bin/env bash
# rabbitmq-down.sh
# Detiene el broker RabbitMQ local (Docker Compose) conservando el volumen.
# Uso:  ./scripts/rabbitmq-down.sh
#
# NO usa 'docker compose down -v': el volumen rabbitmq_data se conserva para no
# perder la persistencia local. Para un reset completo y consciente usa:
#   docker compose down -v

set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "========================================"
echo " Pedidos360 - RabbitMQ Down"
echo "========================================"

if ! command -v docker >/dev/null 2>&1; then
  echo "  [FALLO] Docker no esta instalado o no esta en PATH."
  exit 1
fi

echo ""
echo "[1/1] Deteniendo contenedores del compose (sin -v)..."
docker compose down

echo ""
echo "========================================"
echo " RabbitMQ detenido."
echo " El volumen rabbitmq_data se conserva."
echo " Reset completo: docker compose down -v"
echo "========================================"
echo ""