# Amazon RDS PostgreSQL — Pedidos360

Este directorio documenta la infraestructura de base de datos relacional para el proyecto **Pedidos360** en la región `us-east-1`.

---

## 1. Alcance y Arquitectura de Red

El despliegue de base de datos utiliza una arquitectura **100% privada** dentro de la VPC existente del proyecto:

```text
               VPC vpc-091400dfac7e47849 (172.31.0.0/16)
 ┌────────────────────────────────────────────────────────────────────────┐
 │                                                                        │
 │   EC2 (i-003c43af8dfb9a3c5)                                            │
 │   Security Group: sg-000542deedef64065 (pedidos360-backend-sg)         │
 │   ├── bff-service       (:8080)                                        │
 │   ├── pedidos-service   (127.0.0.1:8081) ───────┐                      │
 │   └── productos-service (127.0.0.1:8082) ───────┤                      │
 │                                                 │                      │
 │                                                 │ TCP 5432             │
 │                                                 ▼                      │
 │   Amazon RDS PostgreSQL (pedidos360-db)                                │
 │   Security Group: pedidos360-rds-sg                                    │
 │   ├── DB Name: pedidos360                                              │
 │   ├── PubliclyAccessible: false                                        │
 │   └── StorageEncrypted: true                                           │
 │                                                                        │
 └────────────────────────────────────────────────────────────────────────┘
```

- **VPC ID**: `vpc-091400dfac7e47849` (VPC del proyecto reutilizada).
- **Subnet Group**: `pedidos360-db-subnet-group` (subredes en al menos dos zonas de disponibilidad, ej. `us-east-1a` y `us-east-1b`).
- **Instancia EC2 Backend**: `i-003c43af8dfb9a3c5` asociada a `sg-000542deedef64065`.

---

## 2. Security Group (`pedidos360-rds-sg`)

### 2.1 Especificación
- **Nombre**: `pedidos360-rds-sg`
- **Descripción**: `Security Group privado para PostgreSQL de Pedidos360`
- **VPC**: `vpc-091400dfac7e47849`

### 2.2 Regla Inbound
| Protocolo | Puerto | Origen Autorizado | Descripción |
|---|---|---|---|
| TCP | `5432` | `sg-000542deedef64065` (`pedidos360-backend-sg`) | Acceso exclusivo desde el backend EC2 |

### 2.3 Restricciones de Seguridad
- **NO** existe regla hacia `0.0.0.0/0` ni `::/0`.
- **NO** se autorizan direcciones IP públicas individuales.
- La comunicación se autoriza estrictamente por referencia de Security Group de origen.

---

## 3. Especificación de la Instancia RDS

| Parámetro | Valor |
|---|---|
| **Identificador** | `pedidos360-db` |
| **Motor** | `postgres` (versión 16.x / 16.3) |
| **Clase de instancia** | `db.t3.micro` |
| **Nombre de BD inicial** | `pedidos360` |
| **Almacenamiento** | 20 GiB `gp3` (mínimo razonable para laboratorio) |
| **Cifrado (StorageEncrypted)** | `true` (KMS default `aws/rds`) |
| **Acceso público** | `false` (`PubliclyAccessible: false`) |
| **Multi-AZ** | `false` (Single-AZ para laboratorio) |
| **Backup Retention** | `1 día` |
| **Deletion Protection** | `false` (documentado para gestión del ciclo de vida del laboratorio) |

---

## 4. Procedimiento de Creación y Comandos AWS CLI

> **Nota de Seguridad**: Ejecutar con credenciales activas del laboratorio en la terminal.  
> No colocar contraseñas en scripts versionados ni en Git.

### 4.1 Paso 1: Comprobar o Crear Security Group
```bash
# 1. Comprobar si existe pedidos360-rds-sg en la VPC
SG_ID=$(aws ec2 describe-security-groups --region us-east-1 \
  --filters "Name=vpc-id,Values=vpc-091400dfac7e47849" "Name=group-name,Values=pedidos360-rds-sg" \
  --query "SecurityGroups[0].GroupId" --output text)

# 2. Crear si no existe
if [ "$SG_ID" = "None" ] || [ -z "$SG_ID" ]; then
  SG_ID=$(aws ec2 create-security-group --region us-east-1 \
    --group-name pedidos360-rds-sg \
    --description "Security Group privado para PostgreSQL de Pedidos360" \
    --vpc-id vpc-091400dfac7e47849 \
    --query "GroupId" --output text)
fi

# 3. Autorizar regla inbound TCP 5432 desde sg-000542deedef64065
aws ec2 authorize-security-group-ingress --region us-east-1 \
  --group-id "$SG_ID" \
  --protocol tcp \
  --port 5432 \
  --source-group sg-000542deedef64065
```

### 4.2 Paso 2: Crear DB Subnet Group
```bash
# Obtener subnets de la VPC
SUBNET_IDS=$(aws ec2 describe-subnets --region us-east-1 \
  --filters "Name=vpc-id,Values=vpc-091400dfac7e47849" \
  --query "Subnets[].SubnetId" --output text)

# Crear DB Subnet Group
aws rds create-db-subnet-group --region us-east-1 \
  --db-subnet-group-name pedidos360-db-subnet-group \
  --db-subnet-group-description "Subnet Group para RDS Pedidos360" \
  --subnet-ids $SUBNET_IDS
```

### 4.3 Paso 3: Crear Instancia RDS PostgreSQL
```bash
# Generar password seguro temporal en memoria (no persistido en archivos)
# Reemplazar <DB_MASTER_PASSWORD> por la contraseña generada
aws rds create-db-instance --region us-east-1 \
  --db-instance-identifier pedidos360-db \
  --db-name pedidos360 \
  --engine postgres \
  --engine-version 16.3 \
  --db-instance-class db.t3.micro \
  --allocated-storage 20 \
  --storage-type gp3 \
  --storage-encrypted \
  --master-username dbadmin \
  --master-user-password '<DB_MASTER_PASSWORD>' \
  --vpc-security-group-ids "$SG_ID" \
  --db-subnet-group-name pedidos360-db-subnet-group \
  --no-publicly-accessible \
  --no-multi-az \
  --backup-retention-period 1 \
  --no-deletion-protection
```

### 4.4 Paso 4: Esperar Disponibilidad
```bash
aws rds wait db-instance-available --region us-east-1 \
  --db-instance-identifier pedidos360-db
```

### 4.5 Paso 5: Obtener Endpoint Privado
```bash
RDS_ENDPOINT=$(aws rds describe-db-instances --region us-east-1 \
  --db-instance-identifier pedidos360-db \
  --query "DBInstances[0].Endpoint.Address" --output text)

echo "Endpoint RDS: $RDS_ENDPOINT"
```

---

## 5. Verificación de Conectividad desde EC2 (vía SSM)

La prueba de red se ejecuta desde la instancia EC2 `i-003c43af8dfb9a3c5` hacia el endpoint privado de RDS en el puerto `5432`:

```bash
# Ejecutar verificación TCP desde EC2 con SSM Run Command
aws ssm send-command --region us-east-1 \
  --instance-ids i-003c43af8dfb9a3c5 \
  --document-name "AWS-RunShellScript" \
  --parameters '{"commands":["nc -z -v -w5 '"$RDS_ENDPOINT"' 5432 || pg_isready -h '"$RDS_ENDPOINT"' -p 5432"]}' \
  --query "Command.CommandId" --output text
```

Resultado esperado desde EC2:
- Conexión TCP exitosa hacia `<RDS_ENDPOINT>:5432`.
- `0.0.0.0/0` NO tiene acceso desde Internet.

---

## 6. Variables de Entorno de Aplicación

En EC2, las credenciales y configuración se inyectan en `/etc/pedidos360/pedidos.env` y `/etc/pedidos360/productos.env`:

```ini
SPRING_PROFILES_ACTIVE=aws
DB_HOST=<RDS_ENDPOINT>
DB_PORT=5432
DB_NAME=pedidos360
DB_USERNAME=dbadmin
DB_PASSWORD=<VALOR_SEGURO_EN_RUNTIME>
```

> **Regla de Seguridad**: Ninguna contraseña ni secreto se commitea en Git ni se expone públicamente.
