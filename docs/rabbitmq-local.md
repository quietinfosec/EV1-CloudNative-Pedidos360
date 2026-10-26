# RabbitMQ local

Infraestructura local del **clúster RabbitMQ de 2 nodos** para Pedidos360,
levantada con Docker Compose. Los microservicios Spring Boot (`bff-service`,
`pedidos-service`, `productos-service`, `rabbitmq-admin-service`) siguen
ejecutándose desde el host, sin contenedores.

La **topología** (exchange, colas, bindings, routing keys, DLX/DLQ) **no** se
declara en Docker: se declara desde Spring Boot mediante `RabbitMQConfig`, para
no mantener definiciones duplicadas entre `definitions.json` y el código Java.

> **Actualización 2026-10-24:** el broker local es un clúster de 2 nodos
> (`pedidos360-rabbitmq` y `pedidos360-rabbitmq2`) formado por un contenedor
> efímero `rabbitmq2-join`. Los comandos de una sola instancia que aparezcan más
> abajo deben leerse sobre el nodo 1; el estado del clúster se valida con
> `.\scripts\rabbitmq-status.ps1`.

## Requisitos

Docker Desktop o Docker Engine + Compose v2 (`docker compose`).

Verificar:

```bash
docker version
docker compose version
```

## Levantar

```bash
.\scripts\rabbitmq-up.ps1          # Windows PowerShell (recomendado)
```

El script valida `docker-compose.yml`, levanta ambos nodos, espera los
healthchecks, ejecuta el join idempotente y verifica `cluster_status`.

Equivalente manual:

```bash
docker compose up -d rabbitmq rabbitmq2
docker compose up -d --force-recreate rabbitmq2-join
```

## Estado

```bash
docker compose ps
```

O:

```bash
.\scripts\rabbitmq-status.ps1
```

Devuelve **código de salida 1** si un nodo no existe, no está `healthy`, el
diagnóstico interno falla o el nodo 2 no aparece en `cluster_status`.

## Diagnóstico

```bash
docker exec pedidos360-rabbitmq rabbitmq-diagnostics ping
```

Resultado esperado:

```text
Ping succeeded
```

## Management UI

- Nodo 1: <http://localhost:15672>
- Nodo 2: <http://localhost:15673>

El plugin de management viene incluido en la imagen `rabbitmq:4.2-management`.

Las interfaces están publicadas **solo en loopback**, por lo que no son
accesibles desde otros equipos de la red local.

## AMQP

```text
Nodo 1: localhost:5672
Nodo 2: localhost:5673
```

Desde Spring Boot (perfil `local`) la futura configuración usará:

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
```

> El hostname `pedidos360-rabbitmq` solo es resoluble desde otros contenedores
> Docker attached a la red `pedidos360-network`. Las aplicaciones que corren en
> el host deben usar `localhost:5672`.

## Credenciales locales

```text
guest / guest
```

**Estas credenciales son exclusivamente para desarrollo local.** No deben usarse
en AWS ni en ningún entorno compartido o productivo. El compose las toma de:

```yaml
RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-guest}
RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:-guest}
```

y los valores por defecto están declarados en `.env.example`.

## Puertos

| Puerto | Servicio      | Publicado en          | Nota |
|--------|---------------|-----------------------|------|
| `5672` | AMQP         | `127.0.0.1:5672`      | Uso de los microservicios |
| `15672`| Management UI | `127.0.0.1:15672`    | Solo loopback |

Estos puertos son independientes de los del backend, que **no se modifican**:

| Puerto | Servicio            |
|--------|---------------------|
| `8080` | `bff-service`       |
| `8081` | `pedidos-service`   |
| `8082` | `productos-service` |

## Red

```text
pedidos360-network  (driver: bridge)
```

Declarada explícitamente en `docker-compose.yml` para que las fases futuras
puedan attachar otros contenedores sin cambiar la red.

## Volumen

```text
rabbitmq_data -> /var/lib/rabbitmq
```

Volumen nombrado y persistente: los datos sobreviven a `docker compose down`.

## Detener

```bash
docker compose down
```

O:

```bash
./scripts/rabbitmq-down.sh
.\scripts\rabbitmq-down.ps1
```

El volumen se conserva. Los scripts nunca ejecutan `down -v`.

## Reset completo

```bash
docker compose down -v
```

> **Advertencia:** `down -v` elimina el volumen `rabbitmq_data` y con él toda la
> persistencia local de RabbitMQ (colas, mensajes, usuarios y permisos). Úsalo
> solo como reset manual consciente, no en el flujo normal de trabajo.

## Topología

La topología se declara desde Spring Boot mediante `RabbitMQConfig`:

- `backend/pedidos-service/.../pedidos/config/RabbitMQConfig.java`
- `backend/productos-service/.../productos/config/RabbitMQConfig.java`

No hay definiciones en `definitions.json` ni en Docker: el broker recibe las
declaraciones al iniciar el `ApplicationContext` de cada servicio.

### Exchange

| Nombre | Tipo | Durable | Auto-delete |
|---|---|---|---|
| `pedidos360.direct.exchange` | `direct` | `true` | `false` |

Es un **exchange común** para ambos microservicios. Ambas clases declaran el
mismo exchange con exactamente las mismas propiedades; las declaraciones de
RabbitMQ son idempotentes cuando los argumentos coinciden.

**DirectExchange utiliza coincidencia EXACTA de Routing Key** (no hay
wildcards: `pedido.creado` nunca coincide con `pedido.*`).

### Queues

| Queue | Durable | Servicio |
|---|---|---|
| `pedidos.eventos.queue` | `true` | pedidos-service |
| `pedidos.errores.queue` | `true` | pedidos-service |
| `productos.eventos.queue` | `true` | productos-service |
| `productos.errores.queue` | `true` | productos-service |

Todas son durables, no `exclusive` ni `autoDelete`: sobreviven reinicios del
broker.

### Routing Keys y Bindings

```text
                    pedidos360.direct.exchange   (direct, durable)
                                  │
     ┌────────────────────────────┼────────────────────────────┐
     │                            │                            │
pedido.creado                     │                  producto.creado
pedido.actualizado                │                  producto.actualizado
pedido.cancelado                  │                  producto.stock.actualizado
     │                            │                            │
     ▼                            ▼                            ▼
pedidos.eventos.queue   pedido.error ──► pedidos.errores.queue   productos.eventos.queue

                                  producto.error
                                       │
                                       ▼
                              productos.errores.queue
```

| Exchange | Routing Key | Queue |
|---|---|---|
| `pedidos360.direct.exchange` | `pedido.creado` | `pedidos.eventos.queue` |
| `pedidos360.direct.exchange` | `pedido.actualizado` | `pedidos.eventos.queue` |
| `pedidos360.direct.exchange` | `pedido.cancelado` | `pedidos.eventos.queue` |
| `pedidos360.direct.exchange` | `pedido.error` | `pedidos.errores.queue` |
| `pedidos360.direct.exchange` | `producto.creado` | `productos.eventos.queue` |
| `pedidos360.direct.exchange` | `producto.actualizado` | `productos.eventos.queue` |
| `pedidos360.direct.exchange` | `producto.stock.actualizado` | `productos.eventos.queue` |
| `pedidos360.direct.exchange` | `producto.error` | `productos.errores.queue` |

Cada routing key tiene su propio binding independiente. Las colas `*.errores.queue`
reciben exclusivamente su routing key de error.

### Verificación en el broker

```bash
docker exec pedidos360-rabbitmq rabbitmqctl list_exchanges name type durable auto_delete
docker exec pedidos360-rabbitmq rabbitmqctl list_queues name durable
docker exec pedidos360-rabbitmq rabbitmqctl list_bindings --no-table-headers
```

## Formato de eventos

Los eventos se serializan con el `ObjectMapper` administrado por Spring Boot y
`Jackson2JsonMessageConverter`. El converter soporta `UUID`, `LocalDateTime` y
`BigDecimal`, y publica mensajes con `content_type=application/json`.

### PedidoEvent

| Campo | Tipo |
|---|---|
| `eventId` | `UUID` |
| `eventType` | `String` |
| `eventVersion` | `Integer` |
| `occurredAt` | `LocalDateTime` |
| `pedidoId` | `Long` |
| `estado` | `String` |
| `usuarioId` | `String` |
| `total` | `BigDecimal` |
| `correlationId` | `String` opcional |

`eventId` se genera con UUID, `eventVersion` comienza en `1` y `occurredAt` se
genera al crear el evento.

### ProductoEvent

| Campo | Tipo |
|---|---|
| `eventId` | `UUID` |
| `eventType` | `String` |
| `eventVersion` | `Integer` |
| `occurredAt` | `LocalDateTime` |
| `productoId` | `Long` |
| `nombre` | `String` |
| `stock` | `Integer` |
| `precio` | `BigDecimal` |
| `correlationId` | `String` opcional |

`eventId`, `eventVersion` y `occurredAt` siguen las mismas reglas de
`PedidoEvent`.

## Publicadores

`PedidoEventPublisher` implementa:

- `publishPedidoCreado`
- `publishPedidoActualizado`
- `publishPedidoCancelado`
- `publishPedidoError`

`ProductoEventPublisher` implementa:

- `publishProductoCreado`
- `publishProductoActualizado`
- `publishStockActualizado`
- `publishProductoError`

Los publicadores usan `RabbitTemplate` configurado con JSON y las constantes de
`RabbitMQConfig` para el exchange y las routing keys.

## Integración con dominio

Los publishers están conectados a las operaciones transaccionales reales sin
cambiar los contratos REST:

```text
REST request
    ↓
Service (@Transactional)
    ↓
Repository
    ↓
ApplicationEventPublisher
    ↓
Transaction commit
    ↓
@TransactionalEventListener(AFTER_COMMIT)
    ↓
RabbitMQ Publisher
    ↓
pedidos360.direct.exchange
    ↓
Queue
```

Mapeo actual:

- `PedidoServiceImpl.crear` → `pedido.creado`
- `PedidoServiceImpl.actualizar` → `pedido.actualizado`
- `PedidoServiceImpl.cancelar` → `pedido.cancelado`
- `PedidoServiceImpl.eliminar` no publica: elimina físicamente y no equivale a cancelar
- `ProductoServiceImpl.crear` → `producto.creado`
- `ProductoServiceImpl.actualizar` → `producto.actualizado` si el stock no cambia
- `ProductoServiceImpl.actualizar` → `producto.stock.actualizado` si el stock cambia
- `ProductoServiceImpl.eliminar` no publica: desactiva el producto y no existe un evento de desactivación

`correlationId` permanece `null` porque el proyecto no tiene un correlation ID
distribuido real. No se genera un identificador artificial.

## Consistencia

El evento interno se registra dentro de la transacción, pero el publisher de
RabbitMQ solo se invoca mediante `@TransactionalEventListener` con fase
`AFTER_COMMIT`:

- un commit exitoso habilita la publicación;
- un rollback no genera evento RabbitMQ;
- si RabbitMQ está caído después del commit, el listener registra el error y no
  puede revertir la operación de base de datos ya confirmada;
- la publicación actual es **best-effort**.

Existe una limitación conocida de dual-write: un commit de BD exitoso combinado
con RabbitMQ caído puede dejar el dato persistido sin su evento publicado.
**Para garantía fuerte de consistencia entre BD y RabbitMQ se recomienda
Transactional Outbox en una evolución posterior.** Outbox no está implementado
en esta fase.

## Consumers

Consumidores vigentes:

- `PedidosColaConsumer` escucha `pedidos360.pedidos.cola`.
- `ProductosColaConsumer` escucha `pedidos360.productos.cola`.
- `AuditoriaEventConsumer` escucha `pedidos360.auditoria.cola`.
- `PedidoEventConsumer` escucha `pedidos.eventos.queue` (INFO) y `pedidos.errores.queue` (WARN).
- `ProductoEventConsumer` escucha `productos.eventos.queue` (INFO) y `productos.errores.queue` (WARN).

Los consumers validan `eventId`, `eventType` y el ID de la entidad. Son
observacionales: no llaman repositories, no cambian stock ni modifican la base
de datos.

El modo de confirmación es **manual** (`acknowledge-mode=manual`). Cada listener
delega en `ManualAckRabbitHandler`, que hace `basicAck` si la acción termina bien
y `basicNack(requeue=false)` si se agotan los reintentos.

```text
Message
   │
ManualAckRabbitHandler → 3 intentos con RetryTemplate
   │
OK → basicAck → mensaje removido
Fallo → basicNack(requeue=false) → DLX → DLQ
```

## Retry

El retry vive en el consumidor, no en el listener de Spring AMQP:

```properties
spring.rabbitmq.listener.simple.acknowledge-mode=manual
spring.rabbitmq.listener.simple.retry.enabled=false
spring.rabbitmq.listener.simple.default-requeue-rejected=false
```

`ManualAckRabbitHandler` ejecuta la acción dentro de un `RetryTemplate` con
`SimpleRetryPolicy(3)` y `FixedBackOffPolicy(500 ms)`. Los tres intentos incluyen
la ejecución inicial; al agotarse, el mensaje se rechaza sin requeue, evitando
ciclos infinitos.

## Dead Letter Queue

El exchange común de dead-letter es:

| Nombre | Tipo | Durable | Auto-delete |
|---|---|---|---|
| `pedidos360.dlx` | `direct` | `true` | `false` |

| Queue original | DLQ | Routing key DLQ |
|---|---|---|
| `pedidos360.pedidos.cola` | `pedidos360.pedidos.dlq` | `pedidos360.pedidos.dlq` |
| `pedidos360.productos.cola` | `pedidos360.productos.dlq` | `pedidos360.productos.dlq` |
| `pedidos360.auditoria.cola` | `pedidos360.auditoria.dlq` | `pedidos360.auditoria.dlq` |
| `pedidos.eventos.queue` | `pedidos.eventos.dlq` | `pedidos.eventos.dlq` |
| `pedidos.errores.queue` | `pedidos.errores.dlq` | `pedidos.errores.dlq` |
| `productos.eventos.queue` | `productos.eventos.dlq` | `productos.eventos.dlq` |
| `productos.errores.queue` | `productos.errores.dlq` | `productos.errores.dlq` |

Las queues productivas declaran `x-dead-letter-exchange=pedidos360.dlx` y su
`x-dead-letter-routing-key` específico. Las DLQ son durables y deliberadamente
no tienen `@RabbitListener`.

```text
Message
   ↓
Consumer exception
   ↓
Intento 1
   ↓
Intento 2
   ↓
Intento 3
   ↓
Rejected (sin requeue)
   ↓
pedidos360.dlx
   ↓
DLQ
```

### Migración local de queues

RabbitMQ no permite redeclarar una queue existente con argumentos diferentes.
En desarrollo local, antes de introducir el DLX se verificó que las cuatro
queues productivas tenían `messages=0` y `consumers=0`; luego se eliminaron con
las condiciones `if-empty=true` e `if-unused=true` y Spring las recreó con los
argumentos dead-letter. Este procedimiento no debe ejecutarse automáticamente
en AWS ni sobre queues con mensajes.

## Limitaciones de consumo

1. Los consumers son solo observacionales.
2. RabbitMQ ofrece entrega at-least-once y pueden existir duplicados.
3. No existe idempotencia persistente ni almacenamiento de `eventId`.
4. Las DLQ no tienen consumer.
5. No existe replay automático desde DLQ.
6. No existe Transactional Outbox.
7. El reproceso de DLQ es manual (inspección y reencolado desde el panel admin), no automático.

## Arquitectura resumida

```text
REST
 │
 ▼
Service
 │
 ▼
Database
 │
 ▼
COMMIT
 │
 ▼
AFTER_COMMIT
 │
 ▼
Publisher
 │
 ▼
DirectExchange
 │
 ▼
Queue
 │
 ▼
Consumer
 ├── OK → ACK
 │
 └── ERROR
      ↓
    Retry x3
      ↓
     DLX
      ↓
     DLQ
```

| Capa | Clase | Responsabilidad |
|---|---|---|
| Servicio | `PedidoServiceImpl`, `ProductoServiceImpl` | Persistencia y `ApplicationEventPublisher` |
| Evento interno | `PedidoDomainEvent`, `ProductoDomainEvent` | Snapshot inmutable del cambio |
| Listener transaccional | `PedidoDomainEventListener`, `ProductoDomainEventListener` | `@TransactionalEventListener(AFTER_COMMIT)` |
| Publicador | `PedidoEventPublisher`, `ProductoEventPublisher` | `convertAndSend` al exchange |
| Consumidor | `PedidoEventConsumer`, `ProductoEventConsumer` | Validación y logging observacional |

## Troubleshooting

| Síntoma | Causa probable | Solución |
|---|---|---|
| `AmqpConnectException` al iniciar o al publicar | Broker apagado | `docker compose up -d rabbitmq` y `rabbitmq-diagnostics ping` |
| Healthcheck `DOWN` en `/actuator/health` | Indicador `rabbit` sin broker | Igual que arriba |
| `PRECONDITION_FAILED ... cannot redeclare queue` | La queue existe con otros argumentos | Eliminar la queue vacía y dejar que Spring la recree; solo en local |
| `ACCESS_REFUSED` en la Management UI | Credenciales distintas a `guest/guest` | Revisar `RABBITMQ_USER` / `RABBITMQ_PASSWORD` de `.env` |
| `vhost not found` | `RABBITMQ_VHOST` no coincide con el declarado | El compose declara `RABBITMQ_DEFAULT_VHOST` con el mismo valor |
| Topología vacía tras `down` + `up` | Broker recreado sin Spring | Levantar Spring o levantar el broker con el mismo `hostname` fijo |
| Tests fallan al conectar | Broker no levantado antes de `mvnw test` | `docker compose up -d rabbitmq` |

## Comandos de prueba

```bash
# Broker
docker compose up -d rabbitmq
docker compose ps
docker exec pedidos360-rabbitmq rabbitmq-diagnostics ping

# Topología
docker exec pedidos360-rabbitmq rabbitmqctl list_exchanges name type durable auto_delete
docker exec pedidos360-rabbitmq rabbitmqctl list_queues name durable messages
docker exec pedidos360-rabbitmq rabbitmqctl list_bindings --no-table-headers

# Suites (Java 21, con el broker arriba)
cd backend/bff-service      && ./mvnw clean test
cd backend/pedidos-service  && ./mvnw clean test
cd backend/productos-service && ./mvnw clean test

# Builds
cd backend/bff-service      && ./mvnw clean package
cd backend/pedidos-service  && ./mvnw clean package
cd backend/productos-service && ./mvnw clean package

# Pruebas de mensajería aisladas
cd backend/pedidos-service   && ./mvnw -Dtest=PedidoConsumerRabbitIntegrationTests test
cd backend/productos-service && ./mvnw -Dtest=ProductoConsumerRabbitIntegrationTests test
```

## Tabla de componentes

| Componente | Implementado |
|---|---|
| RabbitMQ Docker | SI |
| DirectExchange | SI |
| Routing Keys | SI |
| Queues | SI |
| Publishers | SI |
| JSON | SI |
| AFTER_COMMIT | SI |
| Consumers | SI |
| Manual ACK/NACK | SI |
| Retry (`RetryTemplate`) | SI |
| DLX | SI |
| DLQ | SI |
| Topic Exchange | SI |
| Clúster 2 nodos | SI |
| Idempotencia persistente | NO |
| Transactional Outbox | NO |
| Replay DLQ automático | NO |

Idempotencia persistente, Transactional Outbox y replay automático de DLQ
**no forman parte del alcance actual** de esta actividad.

## Evidencias para evaluación

Orden recomendado:

1. `docker compose ps`
2. RabbitMQ Management Overview
3. Exchange `pedidos360.direct.exchange`
4. Bindings del exchange
5. Queues normales
6. DLX `pedidos360.dlx`
7. DLQs
8. Test `pedido.creado`
9. Test `producto.creado`
10. Test inválido mostrando retry
11. Mensaje llegando a DLQ
12. `x-death`
13. `mvnw clean test`
14. `mvnw clean package`

La guía detallada de cada evidencia está en
[`evidencias-rabbitmq.md`](evidencias-rabbitmq.md).

## Persistencia y recreación del broker

El broker declara **nombre de nodo estable** (`rabbit@pedidos360-rabbitmq`) y
`RABBITMQ_DEFAULT_VHOST` explícito. Esto es necesario porque, sin un `hostname`
fijo, RabbitMQ deriva el directorio de datos del ID del contenedor: al
recrear el contenedor con `docker compose down` + `up` cambiaba el directorio y
la topología y los mensajes se perdían pese a estar montado `rabbitmq_data`.

Qué persiste y qué no:

| Elemento | Persiste tras `down`/`up` |
|---|---|
| Exchanges `pedidos360.direct.exchange`, `pedidos360.topic.exchange` y `pedidos360.dlx` | SI (durable, volumen) |
| Colas objetivo `pedidos360.{pedidos,productos,auditoria}.cola` + `.dlq` | SI (durable, volumen) |
| Colas legacy `*.eventos.queue` / `*.errores.queue` + sus DLQ | SI (durable, volumen) |
| Bindings (direct, topic y DLX) | SI |
| Mensajes encolados | SI |
| Topología ausente | Spring la recrea al iniciar el contexto |

## Evidencia de persistencia verificada

Ciclo ejecutado en esta fase:

```text
docker compose down            # sin -v
docker compose up -d rabbitmq
docker exec pedidos360-rabbitmq rabbitmq-diagnostics ping
→ Ping succeeded

Topología completa presente SIN ejecutar Spring (exchanges direct/topic/DLX, colas objetivo y legacy)
```

## Idempotencia

**Idempotencia persistente no es necesaria mientras los consumers sean
observacionales y sin side effects.** Será obligatoria antes de introducir
mutaciones de estado desde consumidores.

## Alcance

**No** existen todavía: consumers de DLQ, replay automático, idempotencia
persistente ni Transactional Outbox. El ACK/NACK manual ya está implementado.

## Estado actual de la integración

| Componente | Estado |
|---|---|
| Broker RabbitMQ local | Disponible |
| `spring-boot-starter-amqp` en los `pom.xml` | **Agregado** (pedidos y productos; BFF no) |
| `RabbitMQConfig` (exchange/queues/bindings) | **Creado** en pedidos y productos |
| Topología (exchange, DLX, queues, DLQ y bindings) | **Declarada** |
| `PedidoEvent` / `ProductoEvent` | **Creados** |
| JSON `MessageConverter` / `RabbitTemplate` | **Configurados** |
| Publishers | **Integrados AFTER_COMMIT** con pedidos y productos |
| Consumers de eventos y errores | **Implementados**, observacionales, ACK/NACK manual |
| Consumers de colas objetivo | `pedidos360.pedidos.cola`, `pedidos360.productos.cola`, `pedidos360.auditoria.cola` |
| Retry | **3 intentos** con `RetryTemplate`, backoff fijo 500 ms y sin requeue infinito |
| DLQ | DLQ durables por dominio; sin consumer automático |
| Clúster | **2 nodos validados en vivo** (`cluster_status`, sin alarmas ni particiones) |
| Panel admin | `rabbitmq-admin-service` con lectura, CRUD, purge y publicación |
| API REST existente | **Sin cambios** |