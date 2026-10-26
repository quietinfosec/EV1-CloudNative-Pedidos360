# Evidencias RabbitMQ Pedidos360

Guía de presentación de la integración RabbitMQ de Pedidos360. Cada evidencia
indica qué mostrar, el comando, qué debe verse y qué demuestra.

Prerrequisito general: Java 21 y el **clúster** local levantado.

```powershell
.\scripts\rabbitmq-up.ps1
```

> **Actualización 2026-10-24:** las capturas y salidas de este documento son
> evidencia histórica. El entorno actual usa 2 nodos (`5672/15672` y
> `5673/15673`), ACK/NACK manual y admin service con CRUD. El estado vigente de
> la rúbrica está en `docs/ep3/06-evidencias-rubrica.md`.

---

## Evidencia 1 — RabbitMQ operativo

**Qué mostrar:** el contenedor del broker y su estado de salud.

**Comando:**

```bash
docker compose ps
```

**Debe verse:**

```text
pedidos360-rabbitmq   rabbitmq:4.2-management   Up ... (healthy)
```

**Demuestra:** el broker está disponible y el healthcheck responde.

---

## Evidencia 2 — Diagnóstico interno del broker

**Qué mostrar:** el ping al nodo Erlang.

**Comando:**

```bash
docker exec pedidos360-rabbitmq rabbitmq-diagnostics ping
```

**Debe verse:**

```text
Ping succeeded
```

**Demuestra:** el nodo RabbitMQ está registrado y operativo.

---

## Evidencia 3 — Management UI

**Qué mostrar:** la interfaz de administración en el navegador.

**Comando:** abrir <http://localhost:15672> e iniciar sesión con `guest` / `guest`.

**Debe verse:** la pestaña **Overview** con el nodo `rabbit@pedidos360-rabbitmq` en estado running.

**Demuestra:** acceso administrativo restringido a loopback (`127.0.0.1:15672`).

---

## Evidencia 4 — Exchange principal

**Qué mostrar:** el exchange de integración.

**Comando:**

```bash
docker exec pedidos360-rabbitmq rabbitmqctl list_exchanges name type durable auto_delete
```

**Debe verse:**

```text
pedidos360.direct.exchange   direct   true   false
pedidos360.dlx               direct   true   false
```

**Demuestra:** existe un `DirectExchange` común a pedidos y productos, durable y sin auto-delete. El enrutamiento es por coincidencia exacta de routing key.

---

## Evidencia 5 — Bindings del exchange principal

**Qué mostrar:** los 8 bindings funcionales.

**Comando:**

```bash
docker exec pedidos360-rabbitmq rabbitmqctl list_bindings --no-table-headers
```

**Debe verse:**

```text
pedido.creado              -> pedidos.eventos.queue
pedido.actualizado         -> pedidos.eventos.queue
pedido.cancelado           -> pedidos.eventos.queue
pedido.error               -> pedidos.errores.queue
producto.creado            -> productos.eventos.queue
producto.actualizado       -> productos.eventos.queue
producto.stock.actualizado -> productos.eventos.queue
producto.error             -> productos.errores.queue
```

**Demuestra:** el enrutamiento exacto esperado y el aislamiento entre dominios.

---

## Evidencia 6 — Queues productivas

**Qué mostrar:** las colas objetivo de la topología nueva.

**Comando:**

```bash
docker exec pedidos360-rabbitmq rabbitmqctl list_queues name durable messages
```

**Debe verse:**

```text
pedidos360.pedidos.cola      true
pedidos360.productos.cola    true
pedidos360.auditoria.cola    true
```

Las colas legacy `pedidos.eventos.queue`, `pedidos.errores.queue`,
`productos.eventos.queue` y `productos.errores.queue` siguen existiendo por
compatibilidad y evidencia; no se eliminan.

**Demuestra:** las colas de trabajo son durables y no efímeras.

---

## Evidencia 7 — Dead Letter Exchange y DLQs

**Qué mostrar:** el DLX y las DLQ de dominio (incluidas las de las colas objetivo).

**Comando:**

```bash
docker exec pedidos360-rabbitmq rabbitmqctl list_queues name durable
```

**Debe verse:**

```text
pedidos360.pedidos.dlq     true
pedidos360.productos.dlq   true
pedidos360.auditoria.dlq   true
pedidos.eventos.dlq        true
pedidos.errores.dlq        true
productos.eventos.dlq      true
productos.errores.dlq      true
```

Para ver los argumentos dead-letter de las colas productivas, abrir en la UI la
cola y revisar **Arguments**, o usar:

```bash
docker exec pedidos360-rabbitmq rabbitmqctl list_queues name arguments
```

**Debe verse:** `x-dead-letter-exchange=pedidos360.dlx` y
`x-dead-letter-routing-key=<cola>.dlq`.

**Demuestra:** los mensajes agotados no se pierden: quedan retenidos para inspección.

---

## Evidencia 8 — Topología declarada por Spring

**Qué mostrar:** que la topología proviene del código, no de definiciones en Docker.

**Comando:** revisar `RabbitMQConfig` de ambos servicios.

**Debe verse:** beans `DirectExchange`, `QueueBuilder.durable(...)` y `BindingBuilder`, sin `definitions.json` ni `rabbitmq.config`.

**Demuestra:** la topología es código versionado e idempotente.

---

## Evidencia 9 — Publicación y consumo de `pedido.creado`

**Qué mostrar:** un evento válido traveling del servicio al consumidor.

**Comando:**

```bash
cd backend/pedidos-service
./mvnw -Dtest=PedidoServiceRabbitIntegrationTests#shouldPublishPedidoCreadoAfterServiceTransactionCommit test
```

**Debe verse:** log `Evento RabbitMQ publicado eventId=... eventType=pedido.creado` y el test en verde.

**Demuestra:** `crear` persiste en H2, confirma la transacción, publica solo después del commit y el consumidor hace ACK.

---

## Evidencia 10 — Publicación y consumo de `producto.creado`

**Comando:**

```bash
cd backend/productos-service
./mvnw -Dtest=ProductoServiceRabbitIntegrationTests#shouldPublishProductoCreadoAfterServiceTransactionCommit test
```

**Debe verse:** log `Evento RabbitMQ publicado ... eventType=producto.creado` y el test en verde.

**Demuestra:** mismo flujo para productos, con payload derivado de la entidad persistida.

---

## Evidencia 11 — Retry de 3 intentos

**Qué mostrar:** el agotamiento de reintentos ante un evento inválido.

**Comando:**

```bash
cd backend/pedidos-service
./mvnw -Dtest=PedidoConsumerRabbitIntegrationTests test
```

**Debe verse:** el consumer invocado 3 veces y el log de Spring AMQP:

```text
RejectAndDontRequeueRecoverer : Retries exhausted for message ...
```

**Demuestra:** `max-attempts=3` con backoff y sin requeue infinito.

---

## Evidencia 12 — Mensaje fallido en la DLQ

**Qué mostrar:** el mensaje inválido retenido en la DLQ.

**Comando:** en la UI, abrir **Queues → pedidos.eventos.dlq → Get messages**, o revisar el test que ya valida el mensaje.

**Debe verse:** un mensaje con `eventType` vacío y header `x-death`.

**Demuestra:** el rechazo final enruta hacia `pedidos360.dlx` y queda disponible para inspección. Las DLQ no tienen consumidor.

---

## Evidencia 13 — Rollback sin publicación

**Comando:**

```bash
cd backend/pedidos-service
./mvnw -Dtest=PedidoServiceRabbitIntegrationTests#shouldNotPublishPedidoCreadoWhenOuterTransactionRollsBack test
```

**Debe verse:** excepción de rollback, `count() = 0` y ningún mensaje en `pedidos.eventos.queue`.

**Demuestra:** `AFTER_COMMIT` garantiza que un rollback no genera evento.

---

## Evidencia 14 — Suite completa

**Comando:**

```bash
docker compose up -d rabbitmq
cd backend/bff-service       && ./mvnw clean test
cd backend/pedidos-service   && ./mvnw clean test
cd backend/productos-service && ./mvnw clean test
```

**Debe verse:**

```text
bff-service       Tests run: 18, Failures: 0, Errors: 0
pedidos-service   Tests run: 49, Failures: 0, Errors: 0
productos-service Tests run: 48, Failures: 0, Errors: 0
```

**Demuestra:** 115 pruebas en verde.

---

## Evidencia 15 — Builds finales

**Comando:**

```bash
cd backend/bff-service       && ./mvnw clean package
cd backend/pedidos-service   && ./mvnw clean package
cd backend/productos-service && ./mvnw clean package
```

**Debe verse:** `BUILD SUCCESS` y los tres JAR generados en `target/`.

**Demuestra:** artefactos construibles y ejecutables.

---

## Evidencia 16 — Ambiente limpio al finalizar

**Comando:**

```bash
docker exec pedidos360-rabbitmq rabbitmqctl list_queues name messages consumers
```

**Debe verse:** las colas objetivo y sus DLQ con `messages = 0`. Las colas
legacy `*.eventos.queue` / `*.errores.queue` pueden conservar mensajes de
evidencia de ejecuciones anteriores; **no se deben borrar**.

**Demuestra:** las colas de la topología nueva quedan drenadas y las DLQ no
acumulan mensajes inesperados.

---

## Preguntas frecuentes durante la defensa

- **¿Por qué DirectExchange?** Porque el trabajo académico se centra en
  enrutamiento por coincidencia exacta de routing key.
- **¿Por qué publicar después del commit?** Para que un rollback nunca genere
  un evento con datos que no existen en la base de datos.
- **¿Qué pasa si RabbitMQ está caído?** El commit de BD ya está confirmado; el
  listener registra el error y no lo revierte. Es una limitación conocida de
  dual-write.
- **¿Por qué no hay idempotencia?** Los consumers son observacionales y no
  tienen side effects. Será obligatoria antes de permitir mutaciones.
- **¿Por qué las DLQ no tienen consumidor?** Para que los mensajes fallidos
  queden disponibles para inspección en lugar de perderse automáticamente.