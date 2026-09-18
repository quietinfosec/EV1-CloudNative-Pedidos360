# Plan de Despliegue Amazon RDS PostgreSQL — Pedidos360

Este documento establece la planificación de la base de datos relacional para el proyecto **Pedidos360** en Amazon Web Services (región `us-east-1`).

> **Estado actual**: Fase de planificación e inspección técnica.  
> **Acción**: NO se han creado recursos en AWS en esta etapa.

---

## 1. Arquitectura y Topología de Conexión

La arquitectura del sistema sigue un modelo desacoplado donde únicamente los microservicios de dominio (`pedidos-service` y `productos-service`) interactúan con la base de datos:

```text
               Internet
                  │
                  ▼
          AWS API Gateway
                  │
                  ▼
         BFF Spring Boot (:8080)
                  │
        ┌─────────┴─────────┐
        ▼                   ▼
  Pedidos Service     Productos Service
    (127.0.0.1:8081)    (127.0.0.1:8082)
        │                   │
        └─────────┬─────────┘
                  │  TCP 5432 (PostgreSQL)
                  ▼
       Amazon RDS PostgreSQL
        (pedidos360-rds-sg)
```

- **BFF Service**: Actúa como API Gateway interno / agregador y validador de JWT. No se conecta a RDS ni contiene configuración de base de datos.
- **Pedidos Service & Productos Service**: Microservicios de dominio desplegados en EC2 (`us-east-1a`). Se conectan a RDS mediante TCP puerto `5432` utilizando Spring Data JPA y el driver de PostgreSQL.
- **Amazon RDS**: Instancia gestionada en subredes privadas de la VPC existente.

---

## 2. Red y Subredes (VPC)

- **VPC ID**: `vpc-091400dfac7e47849` (VPC del proyecto Pedidos360, CIDR `172.31.0.0/16`).
- **Reutilización de VPC**: Se reutiliza la VPC existente sin crear VPCs adicionales.
- **DB Subnet Group**:
  - Nombre planificado: `pedidos360-db-subnet-group`
  - Requisito AWS RDS: Requiere subredes en al menos dos Availability Zones (AZ) distintas dentro de `us-east-1` (por ejemplo, subredes en `us-east-1a` y `us-east-1b`).
  - Tipo de acceso: Subredes privadas/aisladas, sin tablas de enrutamiento que expongan la base de datos directamente a Internet.

---

## 3. Seguridad de Red (Security Groups)

### 3.1 Security Group de RDS planificado
- **Nombre**: `pedidos360-rds-sg`
- **Descripción**: `Security Group para RDS PostgreSQL Pedidos360`
- **VPC**: `vpc-091400dfac7e47849`

### 3.2 Regla Inbound
| Protocolo | Puerto | Origen Autorizado | Propósito |
|---|---|---|---|
| TCP | `5432` | `sg-000542deedef64065` (`pedidos360-backend-sg`) | Tráfico exclusivo desde la instancia backend EC2 |

### 3.3 Restricciones de Seguridad
- **NO** se permite tráfico desde `0.0.0.0/0` ni `::/0`.
- **NO** se autorizan IPs públicas individuales.
- La autorización se realiza por **referencia de Security Group a Security Group** (`sg-000542deedef64065` → `pedidos360-rds-sg`).
- **Reglas Outbound**: Predeterminadas (todos los destinos o restringido al ciclo de vida RDS).

---

## 4. Configuración Objetivo de Amazon RDS

| Parámetro | Valor Propuesto | Justificación |
|---|---|---|
| **Motor (Engine)** | `postgres` | Motor relacional estándar del proyecto |
| **Versión (EngineVersion)** | `16.3` (o `16.x` disponible) | Soporte LTS y compatibilidad con Spring Boot 3.3.4 |
| **DB Instance Identifier** | `pedidos360-db` | Identificador unívoco del recurso en AWS |
| **Nombre de BD Inicial** | `pedidos360` | Base de datos principal para las entidades del sistema |
| **Instance Class** | `db.t3.micro` / `db.t4g.micro` | Tamaño adecuado para entorno de laboratorio / pruebas |
| **Almacenamiento** | `20 GiB` tipo `gp3` | Mínimo razonable para laboratorio con rendimiento SSD |
| **Autoscaling Storage** | Habilitado (máx. 50 GiB) | Prevención de falta de espacio |
| **Acceso Público** | `No` (`PubliclyAccessible: false`) | La base de datos es estrictamente privada |
| **Cifrado (Storage Encryption)** | `Habilitado` (`StorageEncrypted: true`) | Cifrado en reposo con clave KMS gestionada por AWS |
| **Multi-AZ** | `No` (Single-AZ) | Optimización de costos y recursos de laboratorio |
| **Retención de Backups** | `1 día` | Ventana mínima operativa para laboratorio |
| **Deletion Protection** | `Deshabilitado` (documentado) | Facilita la limpieza controlada del laboratorio al finalizar |
| **Monitoreo** | CloudWatch Metrics estándar | Métricas de CPU, conexiones y almacenamiento |

---

## 5. Gestión de Credenciales y Variables de Entorno

### 5.1 Reglas de Seguridad
- **Cero secretos en código**: Ninguna contraseña, usuario ni endpoint se almacenará en Git, `application.properties`, scripts ni documentación.
- **Inyección mediante variables de entorno**: Los servicios en EC2 consumirán los datos de conexión mediante `/etc/pedidos360/pedidos.env` y `/etc/pedidos360/productos.env`.

### 5.2 Variables Requeridas por la Aplicación
| Variable | Descripción | Ejemplo de Formato |
|---|---|---|
| `DB_HOST` | Endpoint privado de la instancia RDS | `pedidos360-db.cxxxxxx.us-east-1.rds.amazonaws.com` |
| `DB_PORT` | Puerto de escucha PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos | `pedidos360` |
| `DB_USERNAME` | Usuario administrador de la BD | `dbadmin` / `postgres` |
| `DB_PASSWORD` | Contraseña segura definida en runtime | `[PROTEGIDO - INYECTADO EN RUNTIME]` |

---

## 6. Adaptación Futura en Spring Boot

### 6.1 Dependencias Requeridas (`pom.xml`)
Para la etapa de integración de datos, se agregarán a `pedidos-service` y `productos-service`:

```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- PostgreSQL JDBC Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 6.2 Configuración del Datasource (`application-aws.properties`)
```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

---

## 7. Próximos Pasos (Secuencia Controlada)

1. **Creación del DB Subnet Group** en `us-east-1` sobre `vpc-091400dfac7e47849`.
2. **Creación del Security Group `pedidos360-rds-sg`** con regla inbound TCP 5432 desde `sg-000542deedef64065`.
3. **Aprovisionamiento de la instancia RDS PostgreSQL** con la configuración planificada.
4. **Verificación de conectividad de red** desde la instancia EC2 (`nc -zv <DB_HOST> 5432` o `pg_isready`).
5. **Incorporación de dependencias JPA/PostgreSQL** y modelos de datos en los microservicios backend.
6. **Actualización de archivos de entorno** en EC2 (`pedidos.env` y `productos.env`) con reinicio ordenado de systemd.
