# EP3 — Evidencia frente a la Rúbrica

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Proyecto:** Pedidos360
- **Fecha de actualización:** 2026-10-25
- **Rama:** `main` · **Commit base:** `4175b1c`
- **Alcance:** estado real del código y de las pruebas ejecutadas. No se declara probado lo que no se ejecutó.

> **Resumen honesto:** los ocho criterios de la rúbrica están implementados y cuentan con evidencia ejecutada. El clúster RabbitMQ de 2 nodos se validó en vivo, las suites completas de pedidos y productos pasaron contra el broker real (incluyendo DLQ con `x-death`) y las suites de BFF/Admin están en verde. El login OIDC se validó de extremo a extremo con **Keycloak local** (Authorization Code + PKCE, claims, 401/200 y CORS); el único límite restante es el login productivo contra Entra ID, que requiere tenant/Client ID reales (§5).

---

## 1. Matriz de cumplimiento (8 criterios ponderados)

| # | Criterio de la rúbrica | Peso | Estado | Evidencia principal |
|---|---|---:|---|---|
| 1 | **Centralización de la configuración RabbitMQ** | 12% | **Cumple** | `RabbitMQConfig` en pedidos y productos concentra exchanges, colas, bindings, DLX/DLQ, `RetryTemplate` y `MessageConverter`; `topicRoutingKey(...)` es la única fuente de verdad de routing keys; `docker-compose.yml` solo declara infraestructura, no topología. |
| 2 | **Definición de beans de configuración** | 13% | **Cumple** | Beans `DirectExchange`, `TopicExchange`, `DirectExchange` DLX, `Queue`, `Binding`, `Jackson2JsonMessageConverter`, `RabbitTemplate` y `RetryTemplate` en `config/RabbitMQConfig.java` de ambos servicios. |
| 3 | **Separación de responsabilidades** | 10% | **Cumple** | Configuración (`config`), eventos/DTOs (`event`), consumidores (`event`), publicadores (`event`), caso de uso (`service`), adaptador HTTP (`client`), controlador (`controller`) y validación (`exception`). |
| 4 | **Consumidores en las colas objetivo** | 15% | **Cumple** | `PedidosColaConsumer` (`pedidos360.pedidos.cola`), `ProductosColaConsumer` (`pedidos360.productos.cola`) y `AuditoriaEventConsumer` (`pedidos360.auditoria.cola`); consumidores legados también migrados a ACK/NACK manual. Evidencia unitaria y de integración real (`*ConsumerRabbitIntegrationTests`, 2+2). |
| 5 | **ACK/NACK manual con reintentos y DLQ** | 20% | **Cumple** | `acknowledge-mode=manual`; `ManualAckRabbitHandler` ejecuta 3 intentos con backoff fijo de 500 ms; éxito → `basicAck`; fallo → `basicNack(requeue=false)` → DLX `pedidos360.dlx` → DLQ del dominio. Evidencia unitaria (2+2) y E2E (`*DlqIntegrationTests`, 1+1, con `x-death`). |
| 6 | **Endpoints del panel de administración** | 13% | **Cumple** | GET `/admin/queues`, `/exchanges`, `/bindings`, `/dlq`; POST `/admin/queues`, `/exchanges`, `/bindings`, `/messages`; DELETE `/admin/queues/{name}`, `/admin/exchanges/{name}`, `/admin/bindings`; purge `DELETE /admin/queues/{name}/messages`. |
| 7 | **Servicio `RabbitAdminService`** | 10% | **Cumple** | `service/RabbitAdminService.java` centraliza lectura y mutación; el controlador solo traduce HTTP; el cliente solo conoce la Management API; `GlobalAdminExceptionHandler` traduce errores. |
| 8 | **Validación de DTOs** | 7% | **Cumple** | Jakarta Validation en `CreateQueueRequest`, `CreateExchangeRequest`, `CreateBindingRequest`, `PublishMessageRequest`; `@Valid` en el controlador; 400 con mapa de campos en `GlobalAdminExceptionHandler`. |

**Cobertura de rúbrica:** 8/8 criterios implementados (100% del peso), con evidencia unitaria/controller e integración real contra el clúster RabbitMQ.

### Criterio adicional solicitado — Clúster RabbitMQ de 2 nodos

No forma parte del peso de la matriz anterior, pero está implementado como refuerzo de disponibilidad:

- Nodo 1: `pedidos360-rabbitmq` (`rabbit@pedidos360-rabbitmq`), AMQP `5672`, UI `15672`.
- Nodo 2: `pedidos360-rabbitmq2` (`rabbit@pedidos360-rabbitmq2`), AMQP `5673`, UI `15673`.
- Cookie Erlang compartida: `RABBITMQ_ERLANG_COOKIE` (default `pedidos360-local-cluster-cookie`).
- Contenedor efímero `pedidos360-rabbitmq2-join` ejecuta `infra/rabbitmq/join-cluster.sh` y une el nodo 2 al clúster de forma idempotente.
- `scripts/rabbitmq-up.ps1` espera ambos nodos, ejecuta el join y valida `cluster_status`; `scripts/rabbitmq-status.ps1` verifica membresía y puertos.
- **Validado en vivo el 2026-10-24:** ambos nodos `Running` en `cluster_status`, sin alarmas ni particiones (RabbitMQ 4.2.9 / Erlang 27.3.4.19).

---

## 2. Evidencia ejecutada (2026-10-25)

| Suite / verificación | Comando | Resultado |
|---|---|---:|
| BFF service (unidad) | `mvnw.cmd -B test` | **22/22** (0 fallos) |
| Admin service | `mvnw.cmd -B test` | **28/28** (0 fallos) |
| Pedidos — suite unitaria sin integración | `mvnw.cmd -B test "-Dtest=!*IntegrationTests,!*DlqIntegrationTests"` | **59/59** (0 fallos) |
| Productos — suite unitaria sin integración | `mvnw.cmd -B test "-Dtest=!*IntegrationTests,!*DlqIntegrationTests"` | **58/58** (0 fallos) |
| Pedidos — suite completa con broker | `mvnw.cmd -B test` | **69/69** (59 unitarios + 10 integración) |
| Productos — suite completa con broker | `mvnw.cmd -B test` | **69/69** (58 unitarios + 11 integración) |
| Pedidos — DLQ E2E | `PedidoDlqIntegrationTests` | **1/1** (`x-death` en `pedidos360.pedidos.dlq`) |
| Productos — DLQ E2E | `ProductoDlqIntegrationTests` | **1/1** (`x-death` en `pedidos360.productos.dlq`) |
| Pedidos — consumidores objetivo y ACK manual (subconjunto de 59) | `mvnw.cmd -B -Dtest=PedidosColaConsumerTests,PedidoEventConsumerTests,ManualAckRabbitHandlerTests test` | **8/8** (0 fallos) |
| Productos — consumidores objetivo y ACK manual (subconjunto de 58) | `mvnw.cmd -B -Dtest=ProductosColaConsumerTests,ProductoEventConsumerTests,ManualAckRabbitHandlerTests test` | **8/8** (0 fallos) |
| Clúster RabbitMQ 2 nodos | `.\scripts\rabbitmq-status.ps1` + `rabbitmqctl cluster_status` | **2 nodos Running**, sin alarmas ni particiones |
| Frontend Angular — tests | `npm test` | **9/9** (OIDC + servicios HTTP) |
| Frontend Angular — build | `npm run build` | **BUILD SUCCESS** |
| Compose (sintaxis) | `docker compose config --quiet` | **OK** |
| OIDC local — Authorization Code + PKCE | Keycloak `:8180` | **Token emitido** con `id_token`, `access_token`, `refresh_token` |
| OIDC local — claims | Access token | `aud=pedidos360-api`, `azp=pedidos360-web`, `roles=ADMIN,USER` |
| OIDC local — BFF sin/con token | `GET /api/pedidos` | **401 / 200** |
| OIDC local — Admin sin/con token | `GET /admin/queues` | **401 / 200** |
| OIDC local — validación DTO | `POST /admin/queues` con `amq.test` | **400** |
| OIDC local — CORS | Preflight desde `:4200` | **200** |
| OIDC productivo contra Entra ID | login interactivo | **No ejecutado** (sin tenant/Client ID real) |

Los conteos corresponden a la salida de Maven/Surefire de esta sesión, no a estimaciones. Las suites unitarias corren sin broker porque `management.health.rabbit.enabled=false` está activo en `src/test/resources/application.properties`; las suites completas y las clases `*IntegrationTests`/`*DlqIntegrationTests` se ejecutaron con el clúster RabbitMQ real arriba.

---

## 3. Cómo se cierra cada criterio de mayor peso

### 3.1 ACK/NACK y DLQ (20%)

Flujo implementado:

1. `spring.rabbitmq.listener.simple.acknowledge-mode=manual`.
2. Cada `@RabbitListener` recibe `(payload, Message, Channel)` y delega en `ManualAckRabbitHandler`.
3. `ManualAckRabbitHandler` ejecuta la acción dentro de un `RetryTemplate` con `SimpleRetryPolicy(3)` y `FixedBackOffPolicy(500 ms)`.
4. Si la acción termina bien: `channel.basicAck(deliveryTag, false)`.
5. Si falla los 3 intentos: `channel.basicNack(deliveryTag, false, false)`.
6. La cola declara `x-dead-letter-exchange=pedidos360.dlx` y su routing key de DLQ; el mensaje termina en `pedidos360.<dominio>.dlq`.
7. `spring.rabbitmq.listener.simple.retry.enabled=false` evita duplicar la política de reintentos.

Evidencia unitaria: `ManualAckRabbitHandlerTests` (2+2) verifica ACK en éxito, 3 intentos y NACK sin requeue en fallo. `PedidosColaConsumerTests` / `ProductosColaConsumerTests` verifican ACK/NACK desde las colas objetivo.

Evidencia de integración: `PedidoDlqIntegrationTests` y `ProductoDlqIntegrationTests` (1+1) publican un mensaje inválido contra el broker real y confirman que termina en `pedidos360.pedidos.dlq` / `pedidos360.productos.dlq` con cabecera `x-death`. `PedidoConsumerRabbitIntegrationTests` y `ProductoConsumerRabbitIntegrationTests` (2+2) validan consumo con ACK y el agotamiento de reintentos.

### 3.2 Consumidores (15%)

| Consumidor | Cola | Servicio |
|---|---|---|
| `PedidosColaConsumer` | `pedidos360.pedidos.cola` | pedidos |
| `PedidoEventConsumer` | `pedidos.eventos.queue` y `pedidos.errores.queue` | pedidos |
| `AuditoriaEventConsumer` | `pedidos360.auditoria.cola` | pedidos |
| `ProductosColaConsumer` | `pedidos360.productos.cola` | productos |
| `ProductoEventConsumer` | `productos.eventos.queue` y `productos.errores.queue` | productos |

Todos usan ACK/NACK manual y validan el payload antes de confirmar.

### 3.3 Endpoints admin (13%)

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/admin/queues` | lista colas |
| GET | `/admin/exchanges` | lista exchanges |
| GET | `/admin/bindings` | lista bindings con `propertiesKey` |
| GET | `/admin/dlq` | lista colas terminadas en `.dlq` |
| POST | `/admin/queues` | 201 |
| DELETE | `/admin/queues/{name}` | 204 |
| DELETE | `/admin/queues/{name}/messages` | 204 (purge) |
| POST | `/admin/exchanges` | 201 |
| DELETE | `/admin/exchanges/{name}` | 204 |
| POST | `/admin/bindings` | 201 |
| DELETE | `/admin/bindings?exchange=&queue=&routingKey=` | 204 |
| POST | `/admin/messages` | 200 con `{ "routed": true/false }` |

### 3.4 Validación de DTOs (7%)

| DTO | Reglas |
|---|---|
| `CreateQueueRequest` | `name` obligatorio, máximo 255, sin espacios al inicio/fin y sin prefijo reservado `amq.`; defaults `vhost=/`, `durable=true`, `autoDelete=false` |
| `CreateExchangeRequest` | `name` obligatorio, máximo 255, sin prefijos reservados; `type` obligatorio y restringido a `direct|topic|fanout|headers` |
| `CreateBindingRequest` | `exchange` y `queue` obligatorios; `routingKey` default vacío |
| `PublishMessageRequest` | `exchange` y `payload` obligatorios; payload máximo 1 MB; `routingKey` default vacío |

`GlobalAdminExceptionHandler` devuelve `400 validacion_invalida` con el detalle por campo, `400 solicitud_invalida` para argumentos inválidos y `502 rabbitmq_management_unavailable` si falla la Management API.

---

## 4. Comandos reproducibles

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.8'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"

# Clúster y validación en vivo
.\scripts\rabbitmq-up.ps1
.\scripts\rabbitmq-status.ps1
docker exec pedidos360-rabbitmq rabbitmqctl -q cluster_status

# BFF (22 tests) y Admin (28 tests), sin broker
cd backend\bff-service; .\mvnw.cmd -B test
cd ..\rabbitmq-admin-service; .\mvnw.cmd -B test

# Suites completas con broker (Pedidos 69, Productos 69)
cd ..\pedidos-service; .\mvnw.cmd -B test
cd ..\productos-service; .\mvnw.cmd -B test

# Consumidores y ACK/NACK (subconjunto 8 + 8)
cd ..\pedidos-service
.\mvnw.cmd -B "-Dtest=PedidosColaConsumerTests,PedidoEventConsumerTests,ManualAckRabbitHandlerTests" test
cd ..\productos-service
.\mvnw.cmd -B "-Dtest=ProductosColaConsumerTests,ProductoEventConsumerTests,ManualAckRabbitHandlerTests" test

# Frontend (9 tests + build)
cd ..\..\frontend\pedidos360-web; npm test; npm run build

# Compose y Keycloak
cd ..\..; docker compose config --quiet
docker compose up -d keycloak

# Smoke OIDC local: ver docs/authentication.md §5
```

---

## 5. Límites declarados

| # | Límite | Impacto | Mitigación |
|---|---|---|---|
| L1 | OIDC productivo con Entra ID no probado | Keycloak local sí se validó (PKCE, claims, 401/200, CORS), pero no hay tenant/Client ID reales | Configurar `AZURE_TENANT_ID`/`AZURE_CLIENT_ID`, reemplazar placeholders y ejecutar login real |
| L2 | Frontend de producción con placeholders | `environment.prod.ts` apunta a un tenant ficticio | Reemplazar `REPLACE_WITH_*` antes del despliegue |
| L3 | Mensajes previos en colas legacy | No es un fallo; son evidencias de pruebas | No limpiar colas/mensajes (regla de la evaluación) |

**No se declara como probado:** login productivo contra Entra ID. El login local con Keycloak, el clúster, la integración RabbitMQ, el ACK/NACK, la DLQ end-to-end, los tests frontend y el build sí se ejecutaron y quedaron en verde.
