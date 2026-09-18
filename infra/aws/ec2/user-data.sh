#!/bin/bash
set -euo pipefail
umask 027

# Cloud-init prepara el host como root; las aplicaciones usaran pedidos360.
if (( EUID != 0 )); then
  printf '%s\n' 'Este User Data requiere root para preparar el sistema.' >&2
  exit 1
fi

# Actualiza los paquetes dentro de la release AL2023 fijada por la AMI.
dnf upgrade -y

# Los tres pom.xml requieren Java 21. El runtime headless basta para sus JAR.
dnf install -y java-21-amazon-corretto-headless unzip shadow-utils

# curl-minimal proporciona curl en AL2023 sin reemplazar una instalacion existente.
if ! command -v curl >/dev/null 2>&1; then
  dnf install -y curl-minimal
fi

# AL2023 normalmente incluye AWS CLI v2; instalarlo solo si falta.
if ! command -v aws >/dev/null 2>&1; then
  dnf install -y awscli-2
fi

# Selecciona Corretto 21 aunque exista otro Java; admite x86_64 y aarch64.
alternatives --set java "/usr/lib/jvm/java-21-amazon-corretto.$(uname -m)/bin/java"
java -version

# Cuenta y grupo dedicados, sin home nuevo ni acceso interactivo.
if ! getent group pedidos360 >/dev/null; then
  groupadd --system pedidos360
fi

if ! id -u pedidos360 >/dev/null 2>&1; then
  useradd --system --gid pedidos360 --home-dir /opt/pedidos360 \
    --no-create-home --shell /sbin/nologin pedidos360
fi

# No reutilizar una cuenta privilegiada o con grupos ajenos al servicio.
if [[ "$(id -u pedidos360)" -eq 0 || "$(id -g pedidos360)" -eq 0 ||
      "$(id -gn pedidos360)" != pedidos360 ||
      "$(id -G pedidos360)" != "$(id -g pedidos360)" ]]; then
  printf '%s\n' 'La cuenta pedidos360 existente no cumple los requisitos de aislamiento.' >&2
  exit 1
fi
usermod --lock --shell /sbin/nologin pedidos360

# Codigo y configuracion: root administra; pedidos360 puede leer, no modificar.
# Los futuros archivos de entorno deberan ser root:pedidos360 con modo 0640.
install -d -o root -g pedidos360 -m 0750 \
  /opt/pedidos360 \
  /opt/pedidos360/bff \
  /opt/pedidos360/pedidos \
  /opt/pedidos360/productos \
  /etc/pedidos360

# Solo los logs requieren escritura del usuario de las aplicaciones.
install -d -o pedidos360 -g pedidos360 -m 0750 \
  /var/log/pedidos360 \
  /var/log/pedidos360/bff \
  /var/log/pedidos360/pedidos \
  /var/log/pedidos360/productos

# No se despliegan ni arrancan aplicaciones en esta etapa.
# Las futuras unidades systemd deben usar User=pedidos360 y Group=pedidos360.
