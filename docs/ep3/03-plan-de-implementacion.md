# EP3 — Plan de Implementación (Incremental)

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Fecha:** 2026-10-20
- **Base:** `docs/ep3/01-auditoria-inicial.md` (estado real) + `docs/ep3/02-arquitectura-objetivo.md` (diseño)
- **Rama:** `ep3-rabbitmq`
- **Reglas de la pauta:** NO subir a GitHub aún · NO eliminar código funcional · NO inventar pruebas · migración incremental.

> **Estado de línea base (verificado al iniciar):** `115/115` tests en verde (BFF 18 · Pedidos 49 · Productos 48), broker `healthy`, árbol Git limpio en `main` = `b35855f`.

---

## 0. Resumen del plan

| Fase | Nombre | Estado | Entregable |
|---|---|---|---|
| 0 | Preparación | ✅ Hecho | rama `ep3-rabbitmq`, broker levantado |
| 1 | Auditoría inicial | ✅ Hecho | `01-auditoria-inicial.md` |
| 2 | Protección (Git) | ✅ Hecho | rama + verificación, sin push |
| 3 | Diseño | ✅ Hecho | `02-arquitectura-objetivo.md` |
| 4 | Implementación incremental | ✅ Hecho (4.1–4.6) | código + tests |
| 5 | Verificación y cierre | ⏳ Pendiente | evidencias + este plan actualizado |

**Regla de oro por tarea:** *cada cambio termina con las 3 suites en verde* antes de pasar a la siguiente.

### Estado de ejecución Fase 4 (verificado)

| Tarea | Estado | Evidencia |
|---|---|---|
| 4.6 Datasource local PostgreSQL | ✅ Hecho | `application-local.properties` (pedidos/productos), `docker-compose.yml` (`postgres`) |
| 4.1 Topic Exchange | ✅ Hecho | `RabbitMQConfig` (topic + bindings), `*TopicExchangeIntegrationTests` |
| 4.2 Tres colas + DLQ | ✅ Hecho | colas `pedidos360.{pedidos,productos,auditoria}.cola` + DLQs |
| 4.3 `rabbitmq-admin-service` | ✅ Hecho | módulo nuevo, `15/15` tests verde |
| 4.5 Seguridad JWT extremo a extremo | ✅ Hecho | `BearerTokenPropagationInterceptor`, `SecurityConfig` (BFF 21 / Pedidos / Productos) |
| 4.4 Adaptador SQS / ElasticMQ | ✅ Hecho | puerto `EventPublisher`/`ProductoPublisher`, `RabbitEventPublisher`/`SqsEventPublisher`, `SqsConfig`, `elasticmq.conf`, `SqsEventPublisherTests` |

**Verificación de suites (secuencial, broker compartido):**
- `rabbitmq` (default): BFF 21 · Pedidos 56 · Productos 56 · Admin 15 — **todos en verde**.
- `MESSAGING_PROVIDER=sqs`: Pedidos 56 (9 skipped) · Productos 56 (10 skipped) — **sin fallos**; los tests de integración Rabbit quedan correctamente *gateados* con `@EnabledIf`.
- ElasticMQ `pedidos360-elasticmq` *healthy*; cola `pedidos360-eventos` recibe mensajes SQS reales.

**Divergencias a reconciliar en Fase 5 (registradas):**
1. **Bindings (4.1):** el plan dice `pedidos.#`, `productos.#`, `#.error`; se implementó `pedidos.pedido.#`, `productos.producto.#`, `#` (los eventos de error no se publican al topic).
2. **Diseño SQS (4.4):** Pedidos usa un puerto genérico `EventPublisher` + fachada `PedidoEventPublisher`; Productos hace que `ProductoEventPublisher` implemente directamente el puerto `ProductoPublisher`. Unificar criterio o documentar la asimetría.
3. **Nombres de paquetes/adaptadores (4.4):** los adaptadores finales viven en el paquete `messaging` (`RabbitEventPublisher`, `SqsEventPublisher`, `SqsConfig`); la fachada de dominio queda en `event` (`PedidoEventPublisher`, `ProductoEventPublisher`). Ajustar las referencias del doc 03 a los nombres reales.

---

## Fase 4 — Implementación incremental

### 4.1 Topic Exchange (aditivo)

- **Objetivo:** añadir `pedidos360.topic.exchange` (topic, durable) sin tocar el directo.
- **Archivos:**
  - **modificar** `backend/{pedidos,productos}-service/.../config/RabbitMQConfig.java`: bean `TopicExchange`, colas/bindings topic.
  - **modificar** `*EventPublisher`: publicar eventos al topic con claves `dominio.entidad.accion`.
- **Criterios de aceptación:**
  - El exchange topic aparece en `list_exchanges` (type=topic, durable=true).
  - Los bindings `pedidos.#`, `productos.#`, `#.error` existen.
  - Tests existentes siguen verdes; se añaden tests de binding/routing.
- **Verificación:** `rabbitmqctl list_exchanges` + `.\mvnw.cmd -B test`.
- **Riesgo:** doble publicación. **Mitigación:** frontera direct=comandos/errores, topic=eventos (doc 02 §2.2).

### 4.2 Tres colas principales (con convivencia)

- **Objetivo:** introducir `pedidos360.pedidos.cola`, `pedidos360.productos.cola`, `pedidos360.auditoria.cola` con sus DLQ de dominio, conservando las colas actuales hasta el corte.
- **Archivos:**
  - **modificar** `RabbitMQConfig` (nuevas colas + DLQ + bindings al topic).
  - **nuevo** consumidor de auditoría `AuditoriaEventConsumer` (registra/almacena eventos).
- **Criterios de aceptación:** 3 colas principales + 3 DLQ visibles; mensaje de cada tipo llega a la cola esperada; tests nuevos por cola.
- **Riesgo:** consumidores duplicados. **Mitigación:** desactivar consumo de colas antiguas sólo tras validar las nuevas (flag por perfil).

### 4.3 Microservicio `rabbitmq-admin-service` (nuevo)

- **Objetivo:** servicio de gestión/observabilidad (doc 02 §3), puerto 8083.
- **Archivos nuevos:**
  - `backend/rabbitmq-admin-service/pom.xml` (SB 3.3.4, Java 21, web, actuator, amqp, security, validation).
  - `.../RabbitmqAdminServiceApplication.java`, `config/` (cliente Management API + seguridad), `controller/AdminController.java`, `service/`, `dto/`.
  - `application.properties`, `application-local.properties`, `application-aws.properties`.
  - Tests: `AdminControllerTests`, `RabbitManagementClientTests`, `AdminSecurityTests`.
- **Criterios de aceptación:** `/admin/queues|exchanges|bindings|dlq` responden; `/admin/**` exige rol `ADMIN`; credenciales broker sólo server-side.
- **Verificación:** `.\mvnw.cmd -B test` del nuevo módulo + smoke HTTP.
- **Dependencia:** Management API `http://<broker>:15672`.

### 4.4 Adaptador SQS / ElasticMQ (conservar SQS)

- **Objetivo:** `messaging.provider=rabbitmq|sqs`; añadir `SqsEventPublisher` y ElasticMQ local.
- **Archivos:**
  - **nuevo** `.../messaging/EventPublisher.java` (puerto) + `RabbitEventPublisher` + `SqsEventPublisher`.
  - **nuevo** `SqsConfig` (SDK AWS SQS v2; endpoint configurable → ElasticMQ en local).
  - **modificar** `docker-compose.yml`: agregar servicio `elasticmq`.
  - **modificar** poms: `software.amazon.awssdk:sqs`.
- **Criterios de aceptación:** con `MESSAGING_PROVIDER=sqs` los eventos se publican en ElasticMQ; con `rabbitmq` (default) todo sigue verde.
- **Riesgo:** duplicar lógica de publicación. **Mitigación:** el dominio depende del puerto `EventPublisher`, no de SDKs.

### 4.5 Seguridad extremo a extremo (JWT/IDaaS)

- **Objetivo:** propagar token BFF→servicios y validar en cada servicio.
- **Archivos:**
  - **modificar** `bff-service/.../config/BffClientConfig.java`: interceptor que añade `Authorization: Bearer` (usando `SecurityContext`).
  - **nuevo** `SecurityConfig` en pedidos/productos/admin: `oauth2-resource-server`.
  - **modificar** perfiles local (seguridad permisiva **explícita**).
- **Criterios de aceptación:** llamada sin token → 401 en servicios internos; con token válido → 200; BFF propaga el token.
- **Riesgo:** romper local sin Entra ID. **Mitigación:** perfil local permisivo declarado + tests de ambos casos.

### 4.6 Datasource local + PostgreSQL en Docker

- **Objetivo:** corregir brecha del perfil local (doc 01 §7).
- **Archivos:** **modificar** `application-local.properties` (pedidos/productos) y `docker-compose.yml` (+ `postgres`).
- **Criterios de aceptación:** arranque local sin proveer BD externa; suites verdes.

---

## 5. Verificación y cierre

| Actividad | Comando / criterio |
|---|---|
| Tests por módulo | `.\mvnw.cmd -B test` (×4) → 0 fallos |
| Topología | `docker exec pedidos360-rabbitmq rabbitmqctl -q list_queues/list_exchanges/list_bindings` |
| Seguridad | smoke 401 sin token / 200 con token |
| SQS | `MESSAGING_PROVIDER=sqs` + ElasticMQ |
| Frontend | `npm install && npm run build` (Angular) |
| Entregables | 01, 02, 03 actualizados con evidencia real |

**Criterio de cierre EP3:** los 4 módulos compilan y prueban en verde; la topología objetivo es observable; la rúbrica (§ doc 01 §9) pasa a **Cumple** en todos los criterios aplicables.

---

## 6. Estado de rúbrica → acción

| Criterio | Estado actual | Acción del plan |
|---|---|---|
| Servicio principal de pedidos | Cumple | — |
| Servicios de procesamiento por dominio | ✅ Cumple | 4.2 (consumidor auditoría) |
| RabbitMQ Admin | ✅ Cumple | **4.3** |
| Productores/consumidores | Cumple | mantener |
| Tres colas principales | ✅ Cumple | **4.2** |
| Direct Exchange | Cumple | mantener |
| Topic Exchange | ✅ Cumple | **4.1** |
| DLX + DLQ por dominio | Cumple | mantener |
| ACK/NACK reintentos limitados | Cumple | 4.1/4.2 (MessageRecoverer explícito) |
| Conservar SQS/ElasticMQ | ✅ Cumple | **4.4** |
| Frontend Angular | Cumple | mantener |
| Seguridad JWT/IDaaS | ✅ Cumple | **4.5** |
| BD cloud configurable | ✅ Cumple | **4.6** |
| Migración incremental | Cumple | mantener |
| Entregables docs/ep3 | En curso | Fase 5 |

---

## 7. Cronograma sugerido (orden de ejecución)

```
4.6 (rápido) → 4.1 (topic) → 4.2 (3 colas) → 4.3 (admin) → 4.5 (seguridad) → 4.4 (sqs) → 5 (cierre)
```

**Razón del orden:** empezar por cambios de bajo riesgo (datasource local) y aditivos (topic), luego las colas y el servicio admin (mayor valor de rúbrica), después seguridad y SQS (mayor riesgo), cerrando con verificación integral.

---

## 8. Riesgos globales

| Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|
| Romper mensajería al refactorizar colas | Media | Alto | Convivencia + tests por fase |
| Falta de tiempo para SQS | Media | Medio | SQS = adaptador opcional, no bloquea el resto |
| Entorno local sin Docker/BD | Baja | Medio | Perfiles + ElasticMQ/Postgres en compose |
| Deriva de credenciales broker | Media | Medio | Credenciales sólo server-side, `.env` ignorado |

---

## 9. Definición de "Hecho" (por tarea)

1. Código compila.
2. Tests del módulo (y los existentes) en verde.
3. Evidencia registrada (comando + salida real) en el informe.
4. Sin regresiones en los otros módulos.
5. Documentación (`docs/ep3`) actualizada.
6. **Sin `git push`** hasta que el usuario lo autorice.
