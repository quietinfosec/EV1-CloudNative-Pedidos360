# Estrategia de Pruebas y Control de Calidad — Pedidos360

Este documento describe la suite de pruebas automatizadas implementadas para la plataforma **Pedidos360**.

> **Evidencia vigente (2026-10-25, clúster RabbitMQ y Keycloak arriba):**
> BFF **22/22**, Admin **28/28**, Pedidos **69/69** (59 unitarios + 10 de integración),
> Productos **69/69** (58 unitarios + 11 de integración), Frontend **9/9** (Vitest),
> smoke OIDC local y build Angular OK. El detalle está en
> [`docs/ep3/06-evidencias-rubrica.md`](ep3/06-evidencias-rubrica.md) §2.

---

## 1. Cobertura de Pruebas

### Backend

| Módulo | Pruebas | Frameworks | Estrategia de Aislamiento |
|---|---|---|---|
| `bff-service` | 22 | JUnit 5, MockMvc, Spring Security Test, Mockito | Mocks de RestClient para microservicios, JwtDecoder en memoria y CORS |
| `rabbitmq-admin-service` | 28 | JUnit 5, MockMvc, Mockito, Spring Security Test | Cliente Management API mockeado; seguridad con JwtDecoder de test y validación de DTOs |
| `pedidos-service` | 69 | JUnit 5, DataJpaTest, MockMvc, Mockito, Spring AMQP | H2 en memoria (modo PostgreSQL) + broker RabbitMQ real |
| `productos-service` | 69 | JUnit 5, DataJpaTest, MockMvc, Mockito, Spring AMQP | H2 en memoria (modo PostgreSQL) + broker RabbitMQ real |
| **Total Backend** | **188** | **100% Pasadas (0 Fallos)** | Sin dependencias de servicios cloud activos |

### Frontend Angular

| Suite | Pruebas | Framework | Cobertura |
|---|---:|---|---|
| `oidc.config.spec.ts` | 2 | Vitest + jsdom | Configuración OIDC, authority, scopes, PKCE y redirect URI |
| `pedido.service.spec.ts` | 4 | Vitest + `HttpTestingController` | GET con/sin filtro, POST, PUT y DELETE |
| `producto.service.spec.ts` | 3 | Vitest + `HttpTestingController` | Listado activo/inactivo, GET por ID, POST, PUT y DELETE |

**Total Frontend:** **9/9** en verde con `npm test`; el build de producción se
valida aparte con `npm run build`.

### 1.1 Requisito previo: broker RabbitMQ

`pedidos-service` y `productos-service` incluyen tests de integración que
consumen y publican mensajes reales en `localhost:5672`. Antes de ejecutar sus
suites hay que levantar y validar el clúster:

```powershell
.\scripts\rabbitmq-up.ps1
.\scripts\rabbitmq-status.ps1
docker exec pedidos360-rabbitmq rabbitmqctl -q cluster_status
```

`bff-service` y `rabbitmq-admin-service` no requieren broker para sus suites
(usan mocks de la Management API); aun así el admin puede levantarse contra el
clúster real para la demo.

### 1.2 Clasificación de pruebas

**Unitarias (sin broker):** publishers, consumers, listeners de dominio,
serialización JSON, servicios, controllers, repositorios y BFF.

**De integración (requieren broker):**

| Prueba | Cobertura |
|---|---|
| `PedidoEventPublisherIntegrationTests` | Publicación JSON real con `content_type` |
| `PedidoServiceRabbitIntegrationTests` | `crear`/`actualizar`/`cancelar` con commit H2, publicación real y rollback sin evento |
| `PedidoConsumerRabbitIntegrationTests` | Consumo válido con ACK y agotamiento de 3 reintentos hacia DLQ |
| `PedidoDlqIntegrationTests` | Mensaje inválido termina en `pedidos360.pedidos.dlq` con `x-death` |
| `PedidoTopicExchangeIntegrationTests` | Bindings y routing keys topic reales |
| `ProductoEventPublisherIntegrationTests` | Publicación JSON real con `content_type` |
| `ProductoServiceRabbitIntegrationTests` | `crear`/`actualizar` sin y con cambio de stock, publicación real y rollback sin evento |
| `ProductoConsumerRabbitIntegrationTests` | Consumo válido con ACK y agotamiento de 3 reintentos hacia DLQ |
| `ProductoDlqIntegrationTests` | Mensaje inválido termina en `pedidos360.productos.dlq` con `x-death` |
| `ProductoTopicExchangeIntegrationTests` | Bindings y routing keys topic reales |
| `PedidosServiceApplicationTests` y `ProductosServiceApplicationTests` | Contexto completo; escuchan en `localhost:5672` cuando el broker está arriba |

Todas las pruebas de integración usan el sufijo `*IntegrationTests`, salvo los
tests de contexto de aplicación.

### 1.3 Smoke test OIDC local

Con `docker compose up -d keycloak` y los servicios en perfil `local`, se validó:

| Verificación | Resultado |
|---|---|
| Authorization Code + PKCE contra Keycloak | Token emitido con `id_token`, `access_token` y `refresh_token` |
| Claims del access token | `iss` Keycloak, `aud=pedidos360-api`, `azp=pedidos360-web`, `roles=ADMIN,USER` |
| BFF `GET /api/pedidos` sin token / con token | **401 / 200** |
| Admin `GET /admin/queues` sin token / con token | **401 / 200** |
| Admin `POST /admin/queues` con `amq.test` | **400** |
| Preflight CORS desde `http://localhost:4200` | **200** |

Este smoke test cubre emisión y consumo de tokens, no reemplaza un login
interactivo contra el tenant productivo de Entra ID.

---

## 2. Ejecución de Pruebas

Para ejecutar la suite completa en local:

```bash
# 0) Clúster RabbitMQ de 2 nodos (requisito de las suites completas)
.\scripts\rabbitmq-up.ps1

# 0.1) Keycloak local (smoke OIDC; no requerido por las suites unitarias)
docker compose up -d keycloak

# 1) BFF y Admin (sin broker)
cd backend/bff-service && .\mvnw.cmd -B test
cd ..\rabbitmq-admin-service && .\mvnw.cmd -B test

# 2) Pedidos y Productos completos (requieren broker)
cd ..\pedidos-service && .\mvnw.cmd -B test
cd ..\productos-service && .\mvnw.cmd -B test

# 3) Frontend (tests Vitest + build de producción)
cd ..\..\frontend\pedidos360-web && npm test
npm run build
```

> Java 21 es la versión soportada. Java 25 no es compatible con las
> dependencias de testing actuales (Byte Buddy).
