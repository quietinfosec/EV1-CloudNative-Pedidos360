# EP3 — Auditoría Inicial del Proyecto Pedidos360

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Rol asumido:** Arquitecto de software (Java 17+ / Spring Boot / RabbitMQ / Docker / Amazon SQS / Angular / microservicios)
- **Fecha de auditoría:** 2026-10-20
- **Rama de trabajo:** `ep3-rabbitmq` (creada desde `main` = `b35855f`)
- **Repositorio auditado:** `quietinfosec/EV1-CloudNative-Pedidos360`
- **Alcance:** reconocimiento del estado real del código, sin modificarlo.

> **Nota de alcance (importante).** La pauta EP3 describe el proyecto "MiniPedidos" (Java 17, Spring Boot 4.1.1, RabbitMQ 4, Amazon SQS, ElasticMQ). El repositorio real **no se llama MiniPedidos** y **no es una copia de esa descripción**. Conforme a la decisión del usuario, este informe **adapta la rúbrica EP3 al repositorio real (Pedidos360)** y documenta explícitamente qué elementos de la pauta **NO existen** aquí (SQS/ElasticMQ, `SqsConfig`, `PedidoProducer`, Topic Exchange, microservicio RabbitMQ Admin). No se inventan evidencias.

---

## 1. Resumen ejecutivo

El repositorio `Pedidos360` es un sistema **cloud-native** funcional con **3 microservicios Spring Boot** (BFF, Pedidos, Productos) + **frontend Angular**, comunicación HTTP interna vía BFF y **mensajería asíncrona con RabbitMQ** ya implementada mediante un patrón de eventos de dominio (`@TransactionalEventListener(AFTER_COMMIT)` → `RabbitTemplate`).

- **Verificación real ejecutada:** `115/115` pruebas unitarias e **integración contra broker RabbitMQ real** en verde, con `BUILD SUCCESS` en los 3 módulos.
- **Topología RabbitMQ real (verificada en el broker):** 2 exchanges directos durables, **8 colas durables** (4 productivas + 4 DLQ), 16 bindings.
- **Brechas principales frente a la rúbrica EP3:** falta el microservicio **RabbitMQ Admin**, falta **Topic Exchange**, no existe **SQS/ElasticMQ**, seguridad JWT sólo en el BFF, y el perfil local no define datasource.

---

## 2. Metodología

1. **Reconocimiento:** lectura del árbol de directorios, `pom.xml` de cada servicio, `application*.properties`, `docker-compose.yml`, `.env.example`, `package.json` del frontend y scripts de `scripts/`.
2. **Lectura de código:** 49 clases Java `main` + 29 clases de test; foco en `config/RabbitMQConfig`, `event/*`, `controller/*`, `service/*`, `security`.
3. **Verificación empírica:** levantamiento del broker vía Docker y ejecución real de las suites de test de los 3 servicios.
4. **Inspección del broker:** `rabbitmqctl list_queues / list_exchanges / list_bindings`.
5. **Contraste contra la rúbrica EP3** y registro de brechas, riesgos y dependencias.

---

## 3. Inventario real del repositorio

### 3.1 Módulos

| Módulo | Ruta | Puerto | Stack | Propósito |
|---|---|---|---|---|
| `bff-service` | `backend/bff-service` | 8080 | Spring Boot 3.3.4 / Java 21 | API Gateway / BFF, seguridad OAuth2, orquestación |
| `pedidos-service` | `backend/pedidos-service` | 8081 | Spring Boot 3.3.4 / Java 21 | Dominio Pedidos + eventos RabbitMQ |
| `productos-service` | `backend/productos-service` | 8082 | Spring Boot 3.3.4 / Java 21 | Dominio Productos + eventos RabbitMQ |
| `pedidos360-web` | `frontend/pedidos360-web` | 4200 | Angular 22.1 + MSAL | Frontend SPA |

### 3.2 Volumetría de código

| Métrica | Valor |
|---|---|
| Clases Java `main` | 49 |
| Clases Java `test` | 29 |
| Archivos frontend (`src/`) | 20 |
| Módulos Maven | 3 |

### 3.3 Versiones reales vs. pauta EP3

| Elemento | Pauta EP3 | Repo real | Estado |
|---|---|---|---|
| Java | 17 | 21 (`<java.version>21</java.version>`) | Diferente (compatible, superador) |
| Spring Boot | 4.1.1 | **3.3.4** | Diferente |
| RabbitMQ | 4 | 4.2-management (Docker) | **Cumple** |
| Amazon SQS | Requerido | No existe | **Brecha** |
| Angular | Requerido | 22.1 | **Cumple** |

> Spring Boot **4.1.1** y Java 17 no existen en este repo. No se debe degradar el stack funcional para "calzar" la pauta; se documenta la diferencia.

---

## 4. Arquitectura actual (verificada)

```
                       ┌───────────────────────────┐
   Angular (4200) ───▶ │  bff-service (8080)       │  OAuth2 Resource Server (Entra ID)
   MSAL / OIDC         │  Spring Security + JWT    │
                       │  RestClient (timeout 3s/5s)│
                       └───────┬───────────┬───────┘
                               │ HTTP      │ HTTP
                     ┌─────────▼──┐   ┌────▼─────────┐
                     │ pedidos-   │   │ productos-   │
                     │ service    │   │ service      │
                     │ (8081)     │   │ (8082)       │
                     │ JPA/Postgre│   │ JPA/Postgre  │
                     └─────┬──────┘   └──────┬───────┘
                           │ AMQP            │ AMQP
                    ┌──────▼─────────────────▼────────┐
                    │  RabbitMQ 4.2 (Docker, 5672)    │
                    │  pedidos360.direct.exchange     │
                    │  pedidos360.dlx                 │
                    └─────────────────────────────────┘
```

**Patrón de publicación de eventos (real):**
`PedidoServiceImpl` (`@Transactional`) → `ApplicationEventPublisher` → `PedidoDomainEventListener` (`@TransactionalEventListener(phase = AFTER_COMMIT)`) → `PedidoEventPublisher` → `RabbitTemplate` → exchange directo. Simétrico en `productos-service`.

---

## 5. Estado real de la mensajería RabbitMQ

### 5.1 Topología verificada en el broker (`rabbitmqctl`)

**Exchanges (no default):**

| name | type | durable |
|---|---|---|
| `pedidos360.direct.exchange` | direct | true |
| `pedidos360.dlx` | direct | true |

**Colas (8, todas `durable=true`, `messages=0`, `consumers=0` en reposo):**

| Cola | Rol |
|---|---|
| `pedidos.eventos.queue` | eventos de dominio Pedidos |
| `pedidos.errores.queue` | errores Pedidos |
| `pedidos.eventos.dlq` | DLQ eventos Pedidos |
| `pedidos.errores.dlq` | DLQ errores Pedidos |
| `productos.eventos.queue` | eventos de dominio Productos |
| `productos.errores.queue` | errores Productos |
| `productos.eventos.dlq` | DLQ eventos Productos |
| `productos.errores.dlq` | DLQ errores Productos |

**Bindings productivos (12 al exchange directo):**

| Exchange | Cola | Routing key |
|---|---|---|
| `pedidos360.direct.exchange` | `pedidos.eventos.queue` | `pedido.creado` |
| `pedidos360.direct.exchange` | `pedidos.eventos.queue` | `pedido.actualizado` |
| `pedidos360.direct.exchange` | `pedidos.eventos.queue` | `pedido.cancelado` |
| `pedidos360.direct.exchange` | `pedidos.errores.queue` | `pedido.error` |
| `pedidos360.direct.exchange` | `productos.eventos.queue` | `producto.creado` |
| `pedidos360.direct.exchange` | `productos.eventos.queue` | `producto.actualizado` |
| `pedidos360.direct.exchange` | `productos.eventos.queue` | `producto.stock.actualizado` |
| `pedidos360.direct.exchange` | `productos.errores.queue` | `producto.error` |

**Bindings DLX (4, hacia las DLQ):** `pedidos360.dlx → {pedidos,productos}.{eventos,errores}.dlq` con routing key igual al nombre de la cola.

### 5.2 Reintentos / ACK-NACK (config real)

En `application.properties` de pedidos y productos:

```
spring.rabbitmq.listener.simple.acknowledge-mode=auto
spring.rabbitmq.listener.simple.retry.enabled=true
spring.rabbitmq.listener.simple.retry.max-attempts=3
spring.rabbitmq.listener.simple.retry.initial-interval=1000
spring.rabbitmq.listener.simple.retry.multiplier=2.0
spring.rabbitmq.listener.simple.retry.max-interval=5000
spring.rabbitmq.listener.simple.default-requeue-rejected=false
```

Comportamiento efectivo: ACK automático; si el consumidor lanza excepción, reintenta hasta 3 veces (backoff 1s→2s→…), y al agotarse **rechaza sin reencolar** (`default-requeue-rejected=false`) → el mensaje cae por el DLX a la DLQ correspondiente.

### 5.3 Consumidores

4 métodos `@RabbitListener` (2 por dominio): eventos + errores. Los consumidores **validan** el payload (`validate(event)`) y registran log; no persisten estado (correcto para el nivel actual, documentado como no-objetivo).

---

## 6. Estado de seguridad (JWT / IDaaS)

| Aspecto | Realidad |
|---|---|
| BFF | `spring-boot-starter-security` + `oauth2-resource-server`; `JwtDecoder` condicional por `jwk-set-uri` (Microsoft Entra ID) |
| Pedidos / Productos | **Sin seguridad** (confían en red interna) |
| Frontend | MSAL Angular 6.2 + `angular-auth-oidc-client` |
| Propagación de token BFF→downstream | **No implementada** (RestClient sin interceptor de token) |

**Hallazgo de riesgo:** en el perfil `local` no se configura `jwk-set-uri`; el `SecurityFilterChain` usa `.oauth2ResourceServer(jwt)` y podría fallar al arrancar sin un `JwtDecoder` disponible. Verifíquese antes de correr BFF local sin Entra ID.

---

## 7. Estado de base de datos

| Perfil | Datasource |
|---|---|
| `aws` (pedidos/productos) | PostgreSQL/RDS por variables `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD` → **cloud configurable** ✔ |
| `local` (pedidos/productos) | **No define datasource** → el arranque local requiere proveer uno (BD externa o ajuste del perfil) |
| `test` | H2 en memoria (MODE=PostgreSQL) |

`docker-compose.yml` sólo levanta **RabbitMQ**; no hay PostgreSQL contenedorizado (se asume BD externa).

---

## 8. Estado del frontend

- Angular **22.1**, `@azure/msal-angular` 6.2, `angular-auth-oidc-client` 22.
- `node_modules/` y `dist/` **no están presentes** (requiere `npm install` para compilar). `target/` sí existe en los 3 servicios.

---

## 9. Matriz de estado frente a la rúbrica EP3

| # | Criterio EP3 | Estado | Evidencia / archivo |
|---|---|---|---|
| 1 | Servicio principal de pedidos | **Cumple** | `pedidos-service`, `PedidoController` (`/api/pedidos`) |
| 2 | Módulos/servicios de procesamiento por dominio | **Parcial** | Hay 2 dominios (pedidos/productos); no hay servicios dedicados de procesamiento (notificación/inventario) |
| 3 | Microservicio RabbitMQ Admin | **No cumple** | No existe módulo admin |
| 4 | Productores y consumidores RabbitMQ | **Cumple** | `*EventPublisher`, `*EventConsumer`, `RabbitMQConfig` |
| 5 | Tres colas principales | **Parcial** | Hay 4 colas productivas + 4 DLQ (no "tres"; sin cola de monitoreo/admin) |
| 6 | Direct Exchange | **Cumple** | `pedidos360.direct.exchange` (direct, durable) |
| 7 | Topic Exchange | **No cumple** | 0 referencias a `TopicExchange` en el código |
| 8 | DLX + DLQ por dominio | **Cumple** | `pedidos360.dlx` + 4 DLQ por dominio/tipo |
| 9 | ACK/NACK con reintentos limitados | **Cumple** | retry `max-attempts=3` + `default-requeue-rejected=false` → DLQ |
| 10 | Conservar ElasticMQ/SQS | **No aplica / Brecha** | No existe SQS/ElasticMQ en el repo |
| 11 | Frontend Angular | **Cumple** | `frontend/pedidos360-web` (Angular 22) |
| 12 | Seguridad JWT / IDaaS | **Parcial** | Sólo BFF; downstream sin auth; sin propagación de token |
| 13 | Base de datos cloud configurable | **Parcial** | Perfil `aws` (RDS) OK; perfil `local` sin datasource |
| 14 | Migración incremental sin romper lo existente | **Cumple** | Rama `ep3-rabbitmq`; 115/115 tests verdes |
| 15 | Entregables `docs/ep3/*.md` | **En curso** | Este documento + 02 y 03 |

Resumen: **7 Cumple · 4 Parcial · 3 No cumple · 1 No aplica** (sobre 15).

---

## 10. Brechas (gaps) priorizadas

1. **Microservicio RabbitMQ Admin** (no existe) — alto peso en la pauta.
2. **Topic Exchange** (no existe) — requerido explícitamente.
3. **Amazon SQS / ElasticMQ** (no existe) — requerido conservar/comparar.
4. **Seguridad downstream + propagación de token** — sólo BFF autentica.
5. **Perfil `local` sin datasource** — fricción de arranque.
6. **Cola/rol de monitoreo** — la pauta pide "tres colas principales" + admin.
7. **`jwk-set-uri` ausente en local** — riesgo de arranque de BFF.

---

## 11. Riesgos

| Riesgo | Prob. | Impacto | Mitigación |
|---|---|---|---|
| Degradar stack para calzar la pauta | Media | Alto | Documentar diferencias; no tocar lo funcional |
| Doble declaración de exchange/DLX en 2 servicios divergiendo | Media | Medio | Centralizar constantes de topología (ya idénticas hoy) |
| BFF no arranca en local sin Entra ID | Media | Medio | Perfil local de seguridad permisivo + test |
| Mensajes a DLQ sin observabilidad | Media | Medio | Consumidor/endpoint admin de inspección |
| Romper mensajería al añadir Topic/SQS | Media | Alto | Migración incremental + tests por fase |

---

## 12. Dependencias

- **Infra:** Docker Desktop (broker `rabbitmq:4.2-management`), JDK 21, Maven wrapper.
- **Externas:** Microsoft Entra ID (JWKS), Amazon RDS/PostgreSQL (perfil aws).
- **Futuras (EP3):** SDK AWS SQS v2, ElasticMQ (dev), Angular (`npm install`).

---

## 13. Trabajo pendiente (alto nivel)

1. Microservicio **RabbitMQ Admin** (gestión/observabilidad de colas).
2. **Topic Exchange** para ruteo por patrón, coexistiendo con el directo.
3. Incorporar **adaptador SQS/ElasticMQ** conservando RabbitMQ.
4. Propagación de token JWT BFF→servicios + seguridad downstream.
5. Completar datasource del perfil local.
6. Entregables 02 y 03 + verificación incremental.

---

## 14. Comandos ejecutados y resultados reales

```powershell
# Rama de trabajo
git switch -c ep3-rabbitmq           # -> Switched to a new branch 'ep3-rabbitmq'

# Broker
docker compose up -d rabbitmq        # -> Container pedidos360-rabbitmq Running
docker inspect --format '{{.State.Health.Status}}' pedidos360-rabbitmq  # -> healthy
# docker ServerVersion=29.6.1 ; imagen rabbitmq:4.2-management

# Pruebas (JAVA_HOME = jdk-21)
# bff-service
.\mvnw.cmd -B test   # Tests run: 18, Failures: 0, Errors: 0, Skipped: 0  -> BUILD SUCCESS
# pedidos-service
.\mvnw.cmd -B test   # Tests run: 49, Failures: 0, Errors: 0, Skipped: 0  -> BUILD SUCCESS
# productos-service
.\mvnw.cmd -B test   # Tests run: 48, Failures: 0, Errors: 0, Skipped: 0  -> BUILD SUCCESS

# Topología del broker
docker exec pedidos360-rabbitmq rabbitmqctl -q list_queues name messages consumers durable
docker exec pedidos360-rabbitmq rabbitmqctl -q list_exchanges name type durable
docker exec pedidos360-rabbitmq rabbitmqctl -q list_bindings source_name destination_name routing_key
```

| Verificación | Resultado real |
|---|---|
| Suites de test (3 módulos) | **115/115 en verde** |
| Integración RabbitMQ (consumidor/publicador/servicio) | **OK** contra broker real |
| Broker saludable | **healthy** |
| Exchanges | 2 directos durables |
| Colas | 8 durables (4 + 4 DLQ) |
| Bindings | 16 (12 productivos + 4 DLX) |
| `TopicExchange` en código | **0** |
| `@RabbitListener` | **4** |
| SQS/ElasticMQ | **ausente** |

**Estado del árbol Git al auditar:** `main` limpio, `origin/main` = `b35855f`; rama de trabajo `ep3-rabbitmq` creada. **No se subió nada a GitHub.**

---

## 15. Conclusiones

- El sistema Pedidos360 **ya cumple el núcleo** de mensajería cloud-native que la EP3 evalúa (productores, consumidores, Direct Exchange, DLX/DLQ por dominio, reintentos limitados).
- Las brechas son **aditivas** (RabbitMQ Admin, Topic Exchange, SQS/ElasticMQ, seguridad downstream), lo que permite una **migración incremental** sin reescribir lo existente.
- La estrategia adoptada es **adaptar la rúbrica al repo real**, documentar las diferencias de stack y **no eliminar código funcional**.
