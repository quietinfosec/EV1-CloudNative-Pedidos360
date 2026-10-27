#!/bin/bash
# Incorpora el nodo 2 al cluster RabbitMQ local.
set -eu

COOKIE="${RABBITMQ_ERLANG_COOKIE:?RABBITMQ_ERLANG_COOKIE no esta definida}"
TARGET="${RABBITMQ_TARGET_NODE:-rabbit@pedidos360-rabbitmq2}"
PRIMARY="${RABBITMQ_PRIMARY_NODE:-rabbit@pedidos360-rabbitmq}"

mkdir -p /var/lib/rabbitmq
printf '%s' "$COOKIE" > /var/lib/rabbitmq/.erlang.cookie
chmod 400 /var/lib/rabbitmq/.erlang.cookie

echo "[join] Esperando a que $TARGET este disponible..."
until rabbitmqctl -n "$TARGET" await_startup >/dev/null 2>&1; do
  sleep 2
done

if rabbitmqctl -n "$TARGET" cluster_status 2>/dev/null | grep -q "$PRIMARY"; then
  echo "[join] $TARGET ya es parte del cluster de $PRIMARY. Nada que hacer."
  rabbitmqctl -n "$TARGET" cluster_status
  exit 0
fi

echo "[join] Deteniendo la aplicacion del nodo 2..."
rabbitmqctl -n "$TARGET" stop_app

echo "[join] Reseteando el nodo 2..."
rabbitmqctl -n "$TARGET" reset

echo "[join] Uniendo $TARGET al cluster de $PRIMARY..."
rabbitmqctl -n "$TARGET" join_cluster "$PRIMARY"
echo "[join] Arrancando la aplicacion del nodo 2..."
rabbitmqctl -n "$TARGET" start_app
echo "[join] Estado del cluster:"
rabbitmqctl -n "$TARGET" cluster_status
