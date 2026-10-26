# EP3 — Pruebas e Integración (Evidencia Real)

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Fecha:** 2026-10-20
- **Entorno:** Windows · PowerShell · Java 21 (`C:\Program Files\Microsoft\jdk-21.0.8`) · Spring Boot 3.3.4 · Angular 22 · Docker Desktop
- **Regla:** todas las cifras de este documento son **salida real** de las suites; ninguna es estimada.

> **Documento histórico (2026-10-20).** La matriz de §2 refleja la ejecución anterior al ACK/NACK manual y al CRUD admin. La evidencia vigente (2026-10-25) está en `06-evidencias-rubrica.md` §2: BFF **22/22**, Admin **28/28**, Pedidos **69/69** (59 unitarios + 10 de integración), Productos **69/69** (58 unitarios + 11 de integración), Frontend **9/9** (Vitest), clúster de 2 nodos validado en vivo, DLQ E2E con `x-death`, smoke OIDC local con Keycloak y build Angular OK.

---

## 1. Cómo se ejecutan las suites

Cada servicio tiene su propio wrapper (no hay POM agregador en `backend/`). Ejecutar **secuencialmente** (broker RabbitMQ compartido + lock de `.m2`):

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.8'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"
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
| bff-service | — | 22 | 0 | 0 | 0 | ✅ |
| pedidos-service | rabbitmq (default) | 69 | 0 | 0 | 0 | ✅ |
| productos-service | rabbitmq (default) | 69 | 0 | 0 | 0 | ✅ |
| rabbitmq-admin-service | — | 28 | 0 | 0 | 0 | ✅ |

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
Initial total | 432.56 kB | 109.85 kB (estimated transfer)
Application bundle generation complete.
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
| `AdminControllerTests` | MockMvc | endpoints `/admin/*` (15) |
| `RabbitManagementClientTests` | unit | cliente Management API (8) |
| `RabbitmqAdminServiceApplicationTests` | `@SpringBootTest` | contexto (2) |

### bff-service (`backend/bff-service`)
Suite 22/22: contexto, propagación de token (`BearerTokenPropagationInterceptor`), clientes y CORS.

---

## 4. Pruebas de integración con infraestructura real

### RabbitMQ (clúster `pedidos360-rabbitmq` + `pedidos360-rabbitmq2`)
- **Validado en vivo 2026-10-24:** `rabbitmqctl cluster_status` muestra ambos nodos
  `Running`, sin alarmas ni particiones; RabbitMQ 4.2.9 / Erlang 27.3.4.19.
- Suites completas contra el clúster (2026-10-25): Pedidos **69/69** y Productos **69/69**.
- Topología verificada contra el broker:
  - Exchange `pedidos360.direct.exchange` (direct) y `pedidos360.topic.exchange` (topic).
  - DLX `pedidos360.dlx`; colas principales `pedidos360.{pedidos,productos,auditoria}.cola` + DLQ `.dlq` de dominio.
  - Bindings topic: `pedidos.pedido.#` → `pedidos360.pedidos.cola`; `productos.producto.#` → `pedidos360.productos.cola`; `#` → `pedidos360.auditoria.cola`.
  - Cada cola principal con `x-dead-letter-exchange=pedidos360.dlx`.

### ElasticMQ (`pedidos360-elasticmq`, `softwaremill/elasticmq-native:1.6.11`)
- *Healthy* en `127.0.0.1:9324`, cola `pedidos360-eventos`.
- **Smoke real** (no mock): petición `SendMessage` a la cola → `ApproximateNumberOfMessages` pasó de **4 a 5**, confirmando entrega real por el endpoint SQS.

### Keycloak (`pedidos360-keycloak`, `quay.io/keycloak/keycloak:26.2`)
- *Running* en `127.0.0.1:8180`, issuer `http://localhost:8180/realms/pedidos360`, realm importado desde `infra/keycloak/realm-pedidos360.json`.
- Authorization Code + PKCE validado: `id_token`, `access_token` y `refresh_token`; claims `aud=pedidos360-api`, `azp=pedidos360-web`, `roles=ADMIN,USER`.
- BFF y Admin responden 401 sin token y 200 con token; preflight CORS desde `:4200` responde 200.

### PostgreSQL (`pedidos360-postgres`, `postgres:16-alpine`)
- *Healthy* en `127.0.0.1:55432` (remapeado para convivir con un PostgreSQL 18 nativo en `5432`).
- Tablas `pedidos` y `productos` creadas por los tests de persistencia (`ddl-auto=update`), verificadas con `psql`.

---

## 5. Notas y límites declarados

- **JWT / OIDC:** los tests de seguridad validan la cadena de validación (token inválido → 401, expirado → 401, válido → 200). Además, el 2026-10-25 se ejecutó un smoke real con **Keycloak local**: Authorization Code + PKCE, tokens con `aud=pedidos360-api`, `azp=pedidos360-web`, `roles=ADMIN,USER`, BFF/Admin 401→200 y preflight CORS 200. **No** se ejecutó el login productivo contra un tenant de Entra ID real; ese extremo no se declara probado.
- **Suite estándar** excluye el grupo `postgres` para no exigir el contenedor; se ejecuta aparte con `-Dgroups=postgres`.
- **Sin paralelismo:** los recuentos anteriores se obtuvieron con ejecución **secuencial**; correr módulos en paralelo produce falsos fallos por contaminación del broker.
