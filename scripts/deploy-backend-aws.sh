#!/usr/bin/env bash
# Pedidos360 - Preparación de artefactos para AWS (Bash)
# Ejecuta mvn clean package en los tres módulos, copia JAR y systemd a dist/,
# muestra SHA256 y se detiene. No incluye secretos ni sube nada.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST_ROOT="${REPO_ROOT}/dist"
DIST_BACKEND="${DIST_ROOT}/backend"
DIST_SYSTEMD="${DIST_ROOT}/systemd"

JAVA_HOME="${JAVA_HOME:-/tmp/opencode/corretto21/jdk21.0.12_9}"
MAVEN_BIN="${MAVEN_BIN:-/tmp/opencode/maven399/apache-maven-3.9.9/bin}"

echo "=== Pedidos360 - Preparación de artefactos para AWS ==="
echo "Repo: ${REPO_ROOT}"
echo "Dist: ${DIST_ROOT}"
echo ""

if [[ ! -d "${JAVA_HOME}" ]]; then
    echo "ERROR: JAVA_HOME no encontrado en ${JAVA_HOME}" >&2
    exit 1
fi
if [[ ! -x "${MAVEN_BIN}/mvn" ]]; then
    echo "ERROR: Maven no encontrado en ${MAVEN_BIN}" >&2
    exit 1
fi

export JAVA_HOME
export PATH="${JAVA_HOME}/bin:${MAVEN_BIN}:${PATH}"
export MAVEN_SKIP_RC=1

echo "Java: $(java -version 2>&1 | head -1)"
echo "Maven: $(mvn -version 2>&1 | head -1)"
echo ""

# Módulos a compilar
MODULES=(
    "bff-service:bff-service-*.jar:bff-service.jar"
    "pedidos-service:pedidos-service-*.jar:pedidos-service.jar"
    "productos-service:productos-service-*.jar:productos-service.jar"
)

# 1. Build
for entry in "${MODULES[@]}"; do
    IFS=':' read -r NAME PATTERN TARGET_JAR <<< "${entry}"
    MODULE_PATH="${REPO_ROOT}/backend/${NAME}"
    if [[ ! -d "${MODULE_PATH}" ]]; then
        echo "ERROR: Módulo no encontrado: ${MODULE_PATH}" >&2
        exit 1
    fi
    echo "--- Building ${NAME} ---"
    (cd "${MODULE_PATH}" && mvn clean package)
    if [[ $? -ne 0 ]]; then
        echo "ERROR: Fallo el build de ${NAME}" >&2
        exit 1
    fi
done

# 2. Preparar dist/
mkdir -p "${DIST_BACKEND}" "${DIST_SYSTEMD}"

JAR_PATHS=()
for entry in "${MODULES[@]}"; do
    IFS=':' read -r NAME PATTERN TARGET_JAR <<< "${entry}"
    TARGET_DIR="${REPO_ROOT}/backend/${NAME}/target"
    JAR_FILE=$(find "${TARGET_DIR}" -maxdepth 1 -name "${PATTERN}" ! -name '*.original' | head -1)
    if [[ -z "${JAR_FILE}" ]]; then
        echo "ERROR: No se encontró JAR en ${TARGET_DIR} con patrón ${PATTERN}" >&2
        exit 1
    fi
    DEST="${DIST_BACKEND}/${TARGET_JAR}"
    cp "${JAR_FILE}" "${DEST}"
    echo "Copiado: $(basename "${JAR_FILE}") -> ${DEST}"
    JAR_PATHS+=("${DEST}")
done

# 3. Copiar systemd
SYSTEMD_SRC="${REPO_ROOT}/infra/aws/ec2/systemd"
if [[ -d "${SYSTEMD_SRC}" ]]; then
    cp "${SYSTEMD_SRC}"/*.service "${DIST_SYSTEMD}/" 2>/dev/null || true
    for f in "${DIST_SYSTEMD}"/*.service; do
        [[ -e "$f" ]] && echo "Copiado systemd: $(basename "$f")"
    done
else
    echo "WARN: Directorio systemd no encontrado: ${SYSTEMD_SRC}" >&2
fi

# 4. Mostrar JARs y SHA256
echo ""
echo "=== Artefactos listos en dist/ ==="
for JAR_PATH in "${JAR_PATHS[@]}"; do
    SHA256=$(sha256sum "${JAR_PATH}" | awk '{print $1}')
    SIZE_MB=$(du -m "${JAR_PATH}" | awk '{print $1}')
    echo "${JAR_PATH}"
    echo "  SHA256: ${SHA256}"
    echo "  Size: ${SIZE_MB} MB"
done

# 5. Siguiente paso
echo ""
echo "=== SIGUIENTE PASO ==="
echo "Los artefactos están en dist/ listos para transferencia."
echo "Usar un mecanismo AWS seguro (SSM Run Command, S3 presigned URL, EC2 Instance Connect, etc.)."
echo "No subir nada con este script; se detiene aquí por diseño."