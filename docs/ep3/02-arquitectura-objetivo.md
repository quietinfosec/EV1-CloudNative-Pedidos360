# EP3 — Arquitectura Objetivo (Pedidos360 → EP3)

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Fecha:** 2026-10-20
- **Basado en:** `docs/ep3/01-auditoria-inicial.md` (estado real verificado)
- **Principio rector:** **migración incremental**. Se conserva todo lo funcional (115/115 tests verdes) y se **añade** lo que la rúbrica pide. No se reescribe ni se elimina código existente sin justificación.

---

## 1. Visión de la arquitectura objetivo

```
                                   ┌─────────────────────────────────────────────┐
   Angular 22 (4200)  ──OIDC/JWT──▶│             bff-service (8080)              │
   MSAL + angular-auth-oidc-client  │  API Gateway + BFF                          │
                                   │  - Valida JWT (Entra ID / IDaaS)            │
                                   │  - Propaga Bearer a servicios internos      │
                                   │  - RestClient + Resilience                  │
                                   └───────┬───────────────┬─────────────────────┘
                                           │ HTTP (JWT)    │ HTTP (JWT)
                              ┌────────────▼───┐    ┌──────▼──────────┐
                              │ pedidos-service│    │ productos-      │
                              │ (8081)         │    │ service (8082)  │
                              │ Dominio Pedidos│    │ Dominio Productos│
                              └───────┬────────┘    └──────┬──────────┘
                                      │ AMQP/SQS           │ AMQP/SQS
                     ┌────────────────▼────────────────────▼───────────────────┐
                     │                 Capa de mensajería (abstracción)         │
                     │  EventPublisher (puerto) ──▶ RabbitMQ  |  SQS/ElasticMQ  │
                     └───────────────┬───────────────────────┬─────────────────┘
                                     │                       │
                     ┌───────────────▼───────────────────────▼─────────────────┐
                     │        RabbitMQ 4.2 (Docker / Amazon MQ-ready)          │
                     │  direct: pedidos360.direct.exchange                     │
                     │  topic : pedidos360.topic.exchange                      │
                     │  dlx   : pedidos360.dlx                                 │
                     └───────────────────────────┬─────────────────────────────┘
                                                 │ AMQP (admin API)
                     ┌───────────────────────────▼─────────────────────────────┐
                     │        rabbitmq-admin-service (8083)  [NUEVO]           │
                     │  Gestión/observabilidad: colas, exchanges, bindings,     │
                     │  DLQ, profundidad, purge, requeue de DLQ. Sin negocio.   │
                     └─────────────────────────────────────────────────────────┘
```

**Servicios objetivo:** 4 microservicios backend (`bff`, `pedidos`, `productos`, `rabbitmq-admin`) + frontend Angular.
**Módulos de procesamiento por dominio:** consumidores de eventos desacoplados por dominio (`pedidos`, `productos`, `auditoría`).

---

## 2. Estrategia de mensajería objetivo

### 2.1 Puertos y adaptadores (permite conservar SQS)

Se introduce una **abstracción de mensajería** para no acoplar el dominio a RabbitMQ:

- **Puerto:** `EventPublisher` (interfaz de dominio, ya existe el patrón en `*EventPublisher`).
- **Adaptadores:**
  - `RabbitEventPublisher` (actual, por defecto).
  - `SqsEventPublisher` (**nuevo**) — AWS SQS en la nube, **ElasticMQ** en local.
- **Selección por configuración:** `messaging.provider=rabbitmq|sqs` (env `MESSAGING_PROVIDER`).

Así se **conserva y se añade SQS** sin eliminar RabbitMQ, y la misma arquitectura de eventos sirve en local (RabbitMQ/ElasticMQ) y en AWS (Amazon MQ / Amazon SQS).

### 2.2 Exchanges objetivo

| Exchange | Tipo | Rol | Estado |
|---|---|---|---|
| `pedidos360.direct.exchange` | direct | comandos y errores con routing key exacta | **existe** (se conserva) |
| `pedidos360.topic.exchange` | **topic** | fan-out de eventos por patrón | **nuevo** |
| `pedidos360.dlx` | direct | dead-lettering | **existe** (se conserva) |

**Routing keys (topic):** `pedidos.pedido.creado`, `pedidos.pedido.actualizado`, `pedidos.pedido.cancelado`, `productos.producto.creado`, `productos.producto.actualizado`, `productos.stock.actualizado`, `*.error`.
**Patrones de suscripción (topic):** `pedidos.#`, `productos.#`, `#.error`.

### 2.3 Tres colas principales

| # | Cola principal | Suscripción (topic) | Consumidor |
|---|---|---|---|
| 1 | `pedidos360.pedidos.cola` | `pedidos.#` | procesamiento de pedidos |
| 2 | `pedidos360.productos.cola` | `productos.#` | procesamiento de productos/inventario |
| 3 | `pedidos360.auditoria.cola` | `#` (o `*.creado`, `*.actualizado`) | auditoría / notificaciones |

### 2.4 DLX + DLQ por dominio

- **DLX:** `pedidos360.dlx` (direct, compartido).
- **DLQ por dominio (objetivo):**

| Cola origen | DLQ |
|---|---|
| `pedidos360.pedidos.cola` | `pedidos360.pedidos.dlq` |
| `pedidos360.productos.cola` | `pedidos360.productos.dlq` |
| `pedidos360.auditoria.cola` | `pedidos360.auditoria.dlq` |

> Las DLQ existentes (`pedidos.*.dlq`, `productos.*.dlq`) **se conservan** durante la transición; la nueva nomenclatura convive hasta el corte.

### 2.5 ACK/NACK y reintentos

- `acknowledge-mode=auto` con `retry.max-attempts=3`, backoff `1s → 2s → 4s` (multiplier 2.0, max 5s).
- Al agotar reintentos: **rechazo sin reencolar** → DLX → DLQ.
- Endurecimiento: configurar explícitamente un `MessageRecoverer` (`RejectAndDontRequeueRecoverer`) para que el comportamiento sea declarativo y testeable.
- **Reproceso controlado:** endpoint en `rabbitmq-admin-service` para **republicar** desde DLQ a la cola principal (requeue), previa inspección.

---

## 3. Microservicio RabbitMQ Admin (nuevo)

**Propósito:** gestión y observabilidad de la infraestructura de mensajería, sin procesar mensajes de negocio.

- **Stack:** Spring Boot 3.3.4 / Java 21, `spring-boot-starter-amqp` + `spring-boot-starter-actuator` + cliente HTTP a la **Management API** de RabbitMQ (`15672`).
- **Puerto:** `8083` (interno).
- **Autenticación:** Resource Server JWT (rol `ADMIN`), consumido a través del BFF.
- **Endpoints propuestos:**

| Método | Ruta | Función |
|---|---|---|
| GET | `/admin/queues` | listar colas (mensajes, consumidores, durable) |
| GET | `/admin/queues/{name}` | detalle de cola |
| GET | `/admin/exchanges` | listar exchanges |
| GET | `/admin/bindings` | listar bindings |
| GET | `/admin/dlq` | resumen de DLQ por dominio |
| POST | `/admin/dlq/{name}/purge` | purgar DLQ (auditado) |
| POST | `/admin/dlq/{name}/requeue` | republicar mensajes desde DLQ |
| GET | `/admin/health` | salud del broker |

**Seguridad:** credenciales de management **nunca en el frontend**; el BFF media el acceso y exige rol `ADMIN`.

---

## 4. Seguridad objetivo (JWT / IDaaS)

- **Identidad:** Microsoft Entra ID (IDaaS) emite los JWT.
- **Frontend:** MSAL Angular adquiere token y lo envía al BFF.
- **BFF:** valida firma/expiración/audiencia y **propaga** el `Authorization: Bearer` a pedidos/productos/admin mediante un `ClientHttpRequestInterceptor`.
- **Servicios internos:** configurados como **Resource Server** (validan el token propagado; rechazan 401 si falta).
- **Autorización:** roles/scopes por endpoint (p. ej. `ADMIN` para `/admin/**`).
- **Local:** perfil de seguridad permisivo **explícito** (no por accidente) para desarrollo sin Entra ID.

---

## 5. Base de datos objetivo (cloud configurable)

| Entorno | Fuente |
|---|---|
| Local | PostgreSQL en Docker (`docker-compose`) **o** datasource explícito en `application-local.properties` |
| AWS | Amazon RDS PostgreSQL vía `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD` (ya implementado) |
| Test | H2 (MODE=PostgreSQL) |

Cambios: **añadir datasource al perfil local** (brecha detectada) y agregar PostgreSQL al `docker-compose.yml` para paridad local/cloud.

---

## 6. Mapeo explícito: estado actual → objetivo

| Elemento | Actual | Objetivo | Tipo de cambio |
|---|---|---|---|
| Microservicios | 3 (bff, pedidos, productos) | 4 (+ rabbitmq-admin) | **nuevo** |
| Direct Exchange | `pedidos360.direct.exchange` | se mantiene | sin cambio |
| Topic Exchange | — | `pedidos360.topic.exchange` | **nuevo** |
| Colas productivas | 4 (eventos/errores por dominio) | 3 principales + DLQ por dominio | **refactor** |
| DLQ | `pedidos/productos.{eventos,errores}.dlq` | `{dominio}.dlq` (convive) | **aditivo** |
| Reintentos | retry 3 + no-requeue | igual + `MessageRecoverer` explícito | endurecer |
| SQS/ElasticMQ | ausente | adaptador `SqsEventPublisher` + ElasticMQ local | **nuevo** |
| Seguridad | solo BFF | BFF + propagación + downstream Resource Server | **extender** |
| Frontend | Angular | se mantiene | sin cambio |
| BD local | sin datasource | datasource + Postgres Docker | corregir |

---

## 7. Atributos de calidad objetivo

| Atributo | Cómo se logra |
|---|---|
| **Escalabilidad** | Colas por dominio; consumidores con `concurrency`/`prefetch` ajustables |
| **Resiliencia** | DLX/DLQ + reintentos limitados + requeue controlado |
| **Observabilidad** | Actuator + `rabbitmq-admin-service` (profundidad, consumidores, DLQ) |
| **Portabilidad cloud** | Abstracción `EventPublisher` (RabbitMQ ↔ SQS) + perfiles `local`/`aws` |
| **Seguridad** | JWT extremo a extremo; credenciales broker sólo server-side |
| **Mantenibilidad** | Topología centralizada y versionada; tests por capa |

---

## 8. Riesgos de la arquitectura objetivo y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Duplicar topología (direct + topic) confunde ruteo | Documentar frontera: **direct = comandos/errores**, **topic = eventos** |
| Migrar a 3 colas rompe consumidores actuales | Convivencia de topologías; corte por fase con tests verdes |
| Broker gestionado (Amazon MQ) vs Docker local difieren | Topología declarada por código; prueba en ambas |
| Añadir SQS desvía de RabbitMQ | SQS es **adaptador opcional**; RabbitMQ sigue por defecto |
| Sobrecargar `rabbitmq-admin` con lógica de negocio | Regla: **sólo** gestión/observabilidad |

---

## 9. Conclusión

La arquitectura objetivo **reutiliza el núcleo existente** (eventos de dominio, Direct Exchange, DLX/DLQ, reintentos) y **añade** las capacidades exigidas (RabbitMQ Admin, Topic Exchange, SQS/ElasticMQ, seguridad extremo a extremo, tres colas principales). El diseño habilita una **transición por fases** detallada en `docs/ep3/03-plan-de-implementacion.md`.
