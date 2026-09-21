# Base de Datos y Persistencia — Pedidos360

Este documento describe el modelo de persistencia relacional implementado en **Pedidos360** utilizando **Spring Data JPA**, **Hibernate ORM** y **Amazon RDS PostgreSQL** (versión 16.x).

---

## 1. Arquitectura de Persistencia

```text
       BFF Service (:8080)
      ┌────────┴────────┐
      ▼                 ▼
Pedidos Service   Productos Service
(:8081)           (:8082)
Spring Data JPA   Spring Data JPA
      │                 │
      └────────┬────────┘
               │  TCP 5432 (PostgreSQL)
               ▼
     Amazon RDS PostgreSQL
```

- **Aislamiento**: El BFF no se conecta directamente a la base de datos. Cada microservicio de dominio gestiona sus propias entidades y tablas.
- **Seguridad**: La base de datos es 100% privada dentro de la VPC (`PubliclyAccessible = false`), con acceso restringido por Security Group (`pedidos360-rds-sg` permite TCP 5432 únicamente desde `pedidos360-backend-sg`).

---

## 2. Modelo de Dominio y Entidades

### 2.1 Entidad `Producto` (Tabla: `productos`)
| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `Long` | PK, Auto-incremental | Identificador único del producto |
| `nombre` | `VARCHAR(100)` | NOT NULL | Nombre descriptivo del producto |
| `descripcion` | `VARCHAR(500)` | Nullable | Detalle y características |
| `precio` | `NUMERIC(12,2)` | NOT NULL, >= 0 | Precio unitario (BigDecimal) |
| `stock` | `INTEGER` | NOT NULL, >= 0 | Cantidad disponible en inventario |
| `activo` | `BOOLEAN` | NOT NULL (default true) | Flag para borrado lógico |
| `created_at` | `TIMESTAMP` | NOT NULL, Updatable=false | Auditoría de creación |
| `updated_at` | `TIMESTAMP` | Nullable | Auditoría de última modificación |

### 2.2 Entidad `Pedido` (Tabla: `pedidos`)
| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `Long` | PK, Auto-incremental | Identificador único del pedido |
| `usuario` | `VARCHAR(150)` | NOT NULL | Identificador / email del usuario |
| `fecha` | `TIMESTAMP` | NOT NULL | Fecha de emisión de la orden |
| `estado` | `VARCHAR(30)` | NOT NULL, Enum | `PENDIENTE`, `CONFIRMADO`, `EN_PROCESO`, `COMPLETADO`, `CANCELADO` |
| `total` | `NUMERIC(12,2)` | NOT NULL, >= 0 | Monto total del pedido (BigDecimal) |
| `created_at` | `TIMESTAMP` | NOT NULL, Updatable=false | Auditoría de creación |
| `updated_at` | `TIMESTAMP` | Nullable | Auditoría de última modificación |

---

## 3. Configuración de Datasource por Variables

En `application-aws.properties`:

```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:pedidos360}
spring.datasource.username=${DB_USERNAME:dbadmin}
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

---

## 4. Estrategia de Testing Aislado (H2 in-memory)

Para garantizar que los tests unitarios y de integración puedan ejecutarse sin conexión a AWS RDS, se utiliza una base de datos **H2 en memoria** (`scope=test`):

```properties
spring.datasource.url=jdbc:h2:mem:pedidosdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
```
