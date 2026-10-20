# EP3 — Evidencia frente a la Rúbrica ("Hecho" verificable)

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Fecha:** 2026-10-20
- **Base:** Matriz §9 de `01-auditoria-inicial.md` (estado inicial) contra la implementación consolidada.
- **Regla:** cada fila cita **artefactos y salida real** de la suite. El criterio "Hecho" exige código + prueba en verde + evidencia reproducible.

---

## 1. Matriz de cumplimiento (inicial → final)

| # | Criterio EP3 | Inicial (§9 doc 01) | Final | Evidencia |
|---|---|---|---|---|
| 1 | Servicio principal de pedidos | Cumple | **Cumple** | `pedidos-service`, `PedidoController` (`/api/pedidos`), suite 65/65 |
| 2 | Servicios de procesamiento por dominio | Parcial | **Cumple** | 2 dominios (pedidos/productos) + consumidor de **auditoría**; colas `pedidos360.{pedidos,productos,auditoria}.cola` |
| 3 | Microservicio RabbitMQ Admin | No cumple | **Cumple** | `backend/rabbitmq-admin-service` (8083), `/admin/queues|exchanges|bindings|dlq`, 15/15 verde |
| 4 | Productores y consumidores | Cumple | **Cumple** | `*EventPublisher`, `*EventConsumer`, `RabbitMQConfig`; tests de publisher/consumer |
| 5 | Tres colas principales | Parcial | **Cumple** | 3 colas principales + DLQ de dominio (`pedidos360.{pedidos,productos,auditoria}.cola` + `.dlq`) |
| 6 | Direct Exchange | Cumple | **Cumple** | `pedidos360.direct.exchange` (direct, durable) |
| 7 | Topic Exchange | No cumple | **Cumple** | `pedidos360.topic.exchange` (topic, durable) + bindings `pedidos.pedido.#` / `productos.producto.#` / `#`; `TopicRoutingKeysTests` 4+4 |
| 8 | DLX + DLQ por dominio | Cumple | **Cumple** | `pedidos360.dlx` + DLQ de dominio; `*DlqIntegrationTests` (1+1) con `x-death` |
| 9 | ACK/NACK con reintentos limitados | Cumple | **Cumple** | retry `max-attempts=3` + `default-requeue-rejected=false` → DLQ; sin `MessageRecoverer` explícito (documentado) |
| 10 | Conservar ElasticMQ / SQS | No aplica | **Cumple** | `messaging.provider=rabbitmq|sqs`; `SqsConfig`, `SqsEventPublisher`/`SqsProductoEventPublisher`, ElasticMQ en compose; suites SQS 65+65; smoke real 4→5 |
| 11 | Frontend Angular | Cumple | **Cumple** | Angular 22.1; `npm run build` → **BUILD SUCCESS** |
| 12 | Seguridad JWT / IDaaS | Parcial | **Cumple** | `SecurityConfig` en pedidos/productos/admin; `BearerTokenPropagationInterceptor` (BFF); `JwtSecurityTests` (3), `PermissiveSecurityProfileTests` (1), `AdminSecurityTests` (3) |
| 13 | Base de datos cloud configurable | Parcial | **Cumple** | perfiles `local`/`aws`; PostgreSQL real (`*PostgresPersistenceTests` 1+1) sobre contenedor `55432` |
| 14 | Migración incremental sin romper | Cumple | **Cumple** | rama `ep3-rabbitmq`; suites finales 21/65/65/15 (rabbit) sin regresiones |
| 15 | Entregables `docs/ep3/*.md` | En curso | **Cumple** | `01`–`06` presentes y con evidencia real |

**Resumen:** inicial `7 Cumple · 4 Parcial · 3 No cumple · 1 No aplica` → final **15 Cumple** (según la rúbrica adaptada al repo real Pedidos360).

---

## 2. Criterios originalmente "No cumple" — cómo se cerraron

### Criterio 3 — Microservicio RabbitMQ Admin
- **Qué se hizo:** módulo nuevo `backend/rabbitmq-admin-service` (SB 3.3.4 / Java 21), puerto 8083.
- **Endpoints:** `/admin/queues`, `/admin/exchanges`, `/admin/bindings`, `/admin/dlq` (sólo lectura).
- **Seguridad:** `/admin/**` exige `ROLE_ADMIN`; actuator `health`/`info` públicos; 502 `rabbitmq_management_unavailable` ante fallo de la Management API.
- **Evidencia:** `AdminSecurityTests` (401/403/200), `AdminControllerTests` (5), `RabbitManagementClientTests` (5), `RabbitmqAdminServiceApplicationTests` (2) → **15/15**.

### Criterio 7 — Topic Exchange
- **Qué se hizo:** `pedidos360.topic.exchange` (topic, durable), aditivo sobre el direct existente.
- **Routing keys:** `topicRoutingKey(eventType)` = `TOPIC_ROUTING_PREFIX + eventType`.
- **Bindings:** `pedidos.pedido.#` → `pedidos360.pedidos.cola`; `productos.producto.#` → `pedidos360.productos.cola`; `#` → `pedidos360.auditoria.cola`.
- **Evidencia:** `TopicRoutingKeysTests` (4+4) validando matcher de topic; topología verificada con `rabbitmqctl`.

### Criterio 10 — Conservar ElasticMQ / SQS
- **Qué se hizo:** adaptador SQS seleccionable por `messaging.provider=rabbitmq|sqs` (default `rabbitmq`), sin romper Rabbit.
- **Componentes:** `SqsConfig` (SDK `software.amazon.awssdk:sqs:2.31.26`), `SqsEventPublisher`, `SqsProductoEventPublisher` (routing key como atributo `routingKey`); `elasticmq` en `docker-compose.yml`.
- **Evidencia:** `MESSAGING_PROVIDER=sqs` → Pedidos 65 (10 *skipped*) · Productos 65 (11 *skipped*); smoke real `SendMessage` sobre `pedidos360-eventos` (cola pasó de 4 a 5).

---

## 3. Criterios originalmente "Parcial" — cómo se cerraron

### Criterio 2 — Servicios de procesamiento por dominio
`auditoria.cola` bindeada a `#` sobre el topic recibe todos los eventos de dominio; se documenta como el rol de procesamiento transversal (auditoría) además de pedidos/productos.

### Criterio 5 — Tres colas principales
Se materializó la topología objetivo: `pedidos360.pedidos.cola`, `pedidos360.productos.cola`, `pedidos360.auditoria.cola`, cada una con su DLQ de dominio y `x-dead-letter-exchange=pedidos360.dlx`.

### Criterio 12 — Seguridad JWT / IDaaS
- **Downstream:** `SecurityConfig` con `oauth2-resource-server` en pedidos/productos/admin; propiedad `pedidos360.security.require-auth` (base `true` con `matchIfMissing`; `local`/`test` = `false` permisivo; `aws` = `true`).
- **Propagación:** `BearerTokenPropagationInterceptor` en el BFF añade `Authorization: Bearer` a las llamadas downstream.
- **Evidencia:** `JwtSecurityTests` (inválido→401, expirado→401, válido→200), `PermissiveSecurityProfileTests` (sin token→200 en perfil permisivo), `AdminSecurityTests`.
- **Límite declarado:** los tests validan la cadena de validación con un `JwtDecoder` de test; **no** se probó contra un Entra ID real (no disponible).

### Criterio 13 — Base de datos cloud configurable
- **Perfiles:** `local` y `aws` apuntan a PostgreSQL (`jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/...`); perfil `aws` sin default de password.
- **Verificación real:** `*PostgresPersistenceTests` (`@Tag("postgres")`, `Replace.NONE`, `ddl-auto=update`) → round-trip real 1+1; tablas `pedidos`/`productos` confirmadas. Contenedor publica en `55432` (convivencia con PostgreSQL nativo).

---

## 4. Criterio transversal — Migración incremental (14)

- Toda adición fue **aditiva**: el direct exchange, los publishers/consumers y las colas originales permanecen operativos; el topic y SQS se superponen sin eliminar código funcional.
- **Sin regresiones:** suites finales BFF 21 · Pedidos 65 · Productos 65 · Admin 15 (rabbit), más 65+65 (sqs) y 1+1 (postgres).
- **Sin `git push`:** todos los cambios viven en la rama `ep3-rabbitmq` locale.

---

## 5. Trazabilidad commit ↔ criterio

| Commit | Criterio(s) EP3 que respaldan |
|---|---|
| `89d80ed` | #14 (protección de línea base) |
| `2497abe` | #7 (Topic Exchange / routing keys) |
| `c9b5ae2` | #10 (adaptador SQS) |
| `a4b3fbc` | #8, #9 (DLQ / reintentos) |
| `388bcc9` | #12 (seguridad JWT E2E) |
| `94a54ab` | #13 (PostgreSQL real) |

---

## 6. Verificación reproducible (resumen de comandos)

```powershell
# Rabbit (default), secuencial
.\mvnw.cmd -B test          # en backend/{bff,pedidos,productos,rabbitmq-admin}-service

# SQS
$env:MESSAGING_PROVIDER='sqs'; .\mvnw.cmd -B test   # pedidos, productos

# PostgreSQL real
.\mvnw.cmd -B test "-Dsurefire.excludedGroups=" "-Dgroups=postgres"

# Frontend
cd frontend/pedidos360-web; npm ci; npm run build
```

**Criterio de cierre EP3:** los 4 módulos backend compilan y prueban en verde; el frontend compila; la topología objetivo (topic + 3 colas + DLQ + admin + SQS/ElasticMQ + PostgreSQL + JWT) es observable y verificada. Rúbrica adaptada: **15/15 Cumple**.
