# EP3 — Pruebas e Integración (Evidencia Real)

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Fecha:** 2026-10-20
- **Entorno:** Windows · PowerShell · Java 21 (`C:\Program Files\Java\jdk-21`) · Spring Boot 3.3.4 · Angular 22 · Docker Desktop
- **Regla:** todas las cifras de este documento son **salida real** de las suites; ninguna es estimada.

---

## 1. Cómo se ejecutan las suites

Cada servicio tiene su propio wrapper (no hay POM agregador en `backend/`). Ejecutar **secuencialmente** (broker RabbitMQ compartido + lock de `.m2`):

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"
cd backend\<servicio>
.\mvnw.cmd -B test
```

Bajo SQS:

```powershell
$env:MESSAGING_PROVIDER='sqs'
.\mvnw.cmd -B test
```

---

## 2. Matriz de resultados (verificada)

| Servicio | Proveedor | Tests | Fallos | Errores | Skipped | Estado |
|---|---|---:|---:|---:|---:|---|
| bff-service | — | 21 | 0 | 0 | 0 | ✅ |
| pedidos-service | rabbitmq (default) | 65 | 0 | 0 | 0 | ✅ |
| productos-service | rabbitmq (default) | 65 | 0 | 0 | 0 | ✅ |
| rabbitmq-admin-service | — | 15 | 0 | 0 | 0 | ✅ |
| pedidos-service | sqs | 65 | 0 | 0 | 10 | ✅ |
| productos-service | sqs | 65 | 0 | 0 | 11 | ✅ |

Los *skipped* bajo SQS son los tests de **integración Rabbit**, correctamente *gateados* con `@EnabledIf("#{environment.getProperty('messaging.provider','rabbitmq') == 'rabbitmq'}")`; es el comportamiento diseñado, no un fallo.

### Persistencia real PostgreSQL (grupo `postgres`)

| Servicio | Test | Tests | Fallos | Estado |
|---|---|---:|---:|---|
| pedidos-service | `PedidoPostgresPersistenceTests` | 1 | 0 | ✅ |
| productos-service | `ProductoPostgresPersistenceTests` | 1 | 0 | ✅ |

```powershell
.\mvnw.cmd -B test "-Dsurefire.excludedGroups=" "-Dgroups=postgres"
```

### Frontend Angular

```
> ng build
√ Building...
Initial total | 506.09 kB | 123.55 kB (estimated transfer)
Application bundle generation complete. [5.198 seconds]
BUILD SUCCESS
```

---

## 3. Tipos de prueba por servicio

### pedidos-service (`backend/pedidos-service`)
| Clase | Tipo | Qué verifica |
|---|---|---|
| `PedidosServiceApplicationTests` | `@SpringBootTest` | contexto arranca |
| `PedidosServiceAwsProfileTests` | `@SpringBootTest` (perfil aws) | propiedades del perfil (4) |
| `PedidoControllerTests` | MockMvc | capa web (10) |
| `PedidoServiceTests` | unit | lógica de dominio |
| `PedidoRepositoryTests` | `@DataJpaTest` (H2) | repositorio |
| `PedidoPostgresPersistenceTests` | `@DataJpaTest` (PostgreSQL) `@Tag(postgres)` | round-trip real |
| `TopicRoutingKeysTests` | unit | routing keys topic (4) |
| `PedidoDlqIntegrationTests` | integración Rabbit | NACK → DLQ + `x-death` (1) |
| `JwtSecurityTests` | `@SpringBootTest` + MockMvc | 401 inválido / 401 expirado / 200 válido (3) |
| `PermissiveSecurityProfileTests` | `@SpringBootTest` | perfil permisivo 200 sin token (1) |
| `SecurityConfigTests` | unit | beans de seguridad |
| `PedidoEventConsumerTests`, `PedidoEventPublisherTests`, `PedidoEventJsonTests` | unit | consumidor/publisher/JSON |
| `*RabbitIntegrationTests`, `*TopicExchangeIntegrationTests`, `*PublisherIntegrationTests` | integración Rabbit | *gateados* bajo SQS |
| `SqsEventPublisherTests` | unit (Mockito) | forma del `SendMessageRequest` (2) |

### productos-service (`backend/productos-service`)
Análogo: `ProductoControllerTests` (10), `ProductoServiceTests`, `ProductoRepositoryTests`, `ProductoPostgresPersistenceTests`, `TopicRoutingKeysTests` (4), `ProductoDlqIntegrationTests` (1), `JwtSecurityTests` (3), `PermissiveSecurityProfileTests` (1), `SecurityConfigTests`, `ProductoEventConsumerTests/PublisherTests/JsonTests`, `SqsProductoEventPublisherTests` (2) y las clases de integración Rabbit *gateadas*.

### rabbitmq-admin-service (`backend/rabbitmq-admin-service`)
| Clase | Tipo | Qué verifica |
|---|---|---|
| `AdminSecurityTests` | MockMvc + seguridad | 401 / 403 / 200 por rol |
| `AdminControllerTests` | MockMvc | endpoints `/admin/*` (5) |
| `RabbitManagementClientTests` | unit | cliente Management API (5) |
| `RabbitmqAdminServiceApplicationTests` | `@SpringBootTest` | contexto (2) |

### bff-service (`backend/bff-service`)
Suite 21/21: contexto, propagación de token (`BearerTokenPropagationInterceptor`) y clientes.

---

## 4. Pruebas de integración con infraestructura real

### RabbitMQ (broker `pedidos360-rabbitmq`)
- Topología verificada contra el broker:
  - Exchange `pedidos360.direct.exchange` (direct) y `pedidos360.topic.exchange` (topic).
  - DLX `pedidos360.dlx`; colas principales `pedidos360.{pedidos,productos,auditoria}.cola` + DLQ `.dlq` de dominio.
  - Bindings topic: `pedidos.pedido.#` → `pedidos360.pedidos.cola`; `productos.producto.#` → `pedidos360.productos.cola`; `#` → `pedidos360.auditoria.cola`.
  - Cada cola principal con `x-dead-letter-exchange=pedidos360.dlx`.

### ElasticMQ (`pedidos360-elasticmq`, `softwaremill/elasticmq-native:1.6.11`)
- *Healthy* en `127.0.0.1:9324`, cola `pedidos360-eventos`.
- **Smoke real** (no mock): petición `SendMessage` a la cola → `ApproximateNumberOfMessages` pasó de **4 a 5**, confirmando entrega real por el endpoint SQS.

### PostgreSQL (`pedidos360-postgres`, `postgres:16-alpine`)
- *Healthy* en `127.0.0.1:55432` (remapeado para convivir con un PostgreSQL 18 nativo en `5432`).
- Tablas `pedidos` y `productos` creadas por los tests de persistencia (`ddl-auto=update`), verificadas con `psql`.

---

## 5. Notas y límites declarados

- **JWT con proveedor OIDC real:** los tests de seguridad validan la **cadena de validación** (token inválido → 401, expirado → 401, válido → 200) mediante un `JwtDecoder` de test que rechaza/acepta tokens; **no** se ejecutó contra un tenant de Entra ID real (no disponible en el entorno). No se declara ese extremo como probado.
- **Suite estándar** excluye el grupo `postgres` para no exigir el contenedor; se ejecuta aparte con `-Dgroups=postgres`.
- **Sin paralelismo:** los recuentos anteriores se obtuvieron con ejecución **secuencial**; correr módulos en paralelo produce falsos fallos por contaminación del broker.
