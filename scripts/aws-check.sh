#!/usr/bin/env bash
# aws-check.sh
# Verifica que AWS CLI este instalado y configurado.
# NO crea ningun recurso. NO genera costos.
# Uso:  ./scripts/aws-check.sh

echo "========================================"
echo " Pedidos360 - AWS Check"
echo "========================================"

# 1. aws --version
echo ""
echo "[1/2] Verificando AWS CLI..."
if ! command -v aws >/dev/null 2>&1; then
  echo "  [FALLO] AWS CLI no esta instalado o no esta en PATH."
  echo "          Instala con: pip install awscli  o  brew install awscli"
  exit 1
fi
VERSION=$(aws --version 2>&1)
echo "  [OK] AWS CLI: $VERSION"

# 2. aws sts get-caller-identity
echo ""
echo "[2/2] Verificando credenciales (get-caller-identity)..."
IDENTITY=$(aws sts get-caller-identity 2>&1)
if [ $? -eq 0 ]; then
  echo "  [OK] Credenciales validas:"
  echo "  $IDENTITY"
else
  echo "  [FALLO] No hay credenciales configuradas."
  echo "          Ejecuta: aws configure"
  exit 1
fi

echo ""
echo "========================================"
echo " AWS Check OK. No se crearon recursos."
echo "========================================"
