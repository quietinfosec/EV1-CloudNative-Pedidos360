# Estrategia de Pruebas y Control de Calidad — Pedidos360

Este documento describe la suite de pruebas automatizadas implementadas para la plataforma **Pedidos360**.

---

## 1. Cobertura de Pruebas Backend

| Módulo | Pruebas | Frameworks | Estrategia de Aislamiento |
|---|---|---|---|
| `bff-service` | 18 | JUnit 5, MockMvc, Spring Security Test, Mockito | Mocks de RestClient para microservicios y JwtDecoder en memoria |
| `pedidos-service` | 49 | JUnit 5, DataJpaTest, MockMvc, Mockito, Spring AMQP | H2 en memoria (modo PostgreSQL) + broker RabbitMQ local |
| `productos-service` | 48 | JUnit 5, DataJpaTest, MockMvc, Mockito, Spring AMQP | H2 en memoria (modo PostgreSQL) + broker RabbitMQ local |
| **Total Backend** | **115** | **100% Pasadas (0 Fallos)** | Sin dependencias de servicios cloud activos |

### 1.1 Requisito previo: broker RabbitMQ

`pedidos-service` y `productos-service` incluyen tests de integración que
consumen y publican mensajes reales en `localhost:5672`. Antes de ejecutar sus
suites hay que levantar el broker:

```bash
docker compose up -d rabbitmq
docker exec pedidos360-rabbitmq rabbitmq-diagnostics ping   # Ping succeeded
```

`bff-service` no usa RabbitMQ y su suite se ejecuta sin broker.

### 1.2 Clasificación de pruebas

**Unitarias (sin broker):** publishers, consumers, listeners de dominio,
serialización JSON, servicios, controllers, repositorios y BFF.

**De integración (requieren broker):**

| Prueba | Cobertura |
|---|---|
| `PedidoEventPublisherIntegrationTests` | Publicación JSON real con `content_type` |
| `PedidoServiceRabbitIntegrationTests` | `crear`/`actualizar`/`cancelar` con commit H2, publicación real y rollback sin evento |
| `PedidoConsumerRabbitIntegrationTests` | Consumo válido con ACK y agotamiento de 3 reintentos hacia DLQ |
| `ProductoEventPublisherIntegrationTests` | Publicación JSON real con `content_type` |
| `ProductoServiceRabbitIntegrationTests` | `crear`/`actualizar` sin y con cambio de stock, publicación real y rollback sin evento |
| `ProductoConsumerRabbitIntegrationTests` | Consumo válido con ACK y agotamiento de 3 reintentos hacia DLQ |
| `PedidosServiceApplicationTests` y `ProductosServiceApplicationTests` | Contexto completo; el indicador `rabbit` de Actuator exige el broker arriba |

Todas las pruebas de integración usan el sufijo `*IntegrationTests`, salvo los
tests de contexto de aplicación.

---

## 2. Ejecución de Pruebas

Para ejecutar la suite completa en local:

```bash
# 0) Broker (requisito de pedidos y productos)
docker compose up -d rabbitmq

# 1) Probar BFF (sin broker)
cd backend/bff-service && ./mvnw test

# 2) Probar Pedidos (requiere broker)
cd backend/pedidos-service && ./mvnw test

# 3) Probar Productos (requiere broker)
cd backend/productos-service && ./mvnw test

# Build completo frontend y backend
powershell -ExecutionPolicy Bypass -File scripts/build-all.ps1
```

> Java 21 es la versión soportada. Java 25 no es compatible con las
> dependencias de testing actuales (Byte Buddy).
