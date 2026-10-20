# EP3 — Guion de Presentación (5 a 10 minutos)

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Proyecto:** Pedidos360 · rama `ep3-rabbitmq` · commit `9d0ff1e`
- **Formato:** 10 diapositivas · duración objetivo **8 min** (rango 5–10) · demo real del flujo de pedidos incluida.
- **Regla:** todo dato citado aquí es **verificable** con el código y la infraestructura de la rama. No se declara probado lo que no lo está (ver §11 de `08-checklist-entrega.md`).

> **Nota de honestidad técnica para el evaluador:** las cifras de pruebas son las **reales** de esta rama: BFF 21 · Pedidos 65 · Productos 65 · Admin 15 (RabbitMQ) + 65/65 (SQS) + 1/1 (PostgreSQL) + build Angular OK. La integración OIDC se probó con un `JwtDecoder` de test (no contra Entra ID real); ver §Limitaciones.

---

## Diapositiva 1 — Portada

**Título:** Pedidos360 — Mensajería Cloud Native con RabbitMQ
**Subtítulo:** DSY1107 · EP3 · Integrantes · Fecha 2026-10-20

**Guion oral (20 s):**
> "Buenos días. Somos [integrantes] y presentamos Pedidos360, la consolidación de la EP3 de Desarrollo Cloud Native I. En esta entrega evolucionamos la mensajería del sistema hacia un modelo de eventos con RabbitMQ —Topic Exchange, tres colas de dominio, Dead Letter Queues, un microservicio de administración y un adaptador SQS— sin romper lo que ya funcionaba. La implementación vive en la rama `ep3-rabbitmq`, con pruebas verificadas."

---

## Diapositiva 2 — Objetivo de Pedidos360

**Contenido:**
- Plataforma de gestión de **productos** y **pedidos** con arquitectura de microservicios cloud-native.
- Desacoplamiento de servicios, aislamiento de red y autenticación federada (Entra ID).
- **Objetivo EP3:** robustecer la mensajería asíncrona entre dominios mediante eventos de dominio confiables.

**Guion oral (30 s):**
> "Pedidos360 resuelve dos dominios: catálogo de productos y órdenes de compra. La comunicación interna entra por un BFF y baja a microservicios de dominio. En la EP1 y EP2 ya existía mensajería con RabbitMQ; el objetivo de la EP3 fue **consolidarla**: rutas por patrón, colas especializadas, manejo de fallos con DLQ, observabilidad con un servicio de admin y conservación de la alternativa SQS/ElasticMQ. Todo de forma **incremental**: no reescribimos ni eliminamos lo existente."

---

## Diapositiva 3 — Arquitectura de microservicios

**Contenido (tabla real de componentes):**

| Componente | Tecnología | Puerto |
|---|---|---|
| Frontend Angular | Angular 22 + MSAL | 4200 |
| BFF Service | Spring Boot 3.3.4 / Java 21 | 8080 |
| Pedidos Service | Spring Boot 3.3.4 + JPA | 8081 |
| Productos Service | Spring Boot 3.3.4 + JPA | 8082 |
| RabbitMQ Admin Service | Spring Boot 3.3.4 | 8083 |
| RabbitMQ / PostgreSQL / ElasticMQ | Docker Compose | 5672+15672 / 55432 / 9324 |

**Guion oral (40 s):**
> "Tenemos cuatro microservicios Spring Boot 3.3.4 sobre Java 21 y un frontend Angular 22. El BFF expone la API y propaga el token; pedidos y productos son los dominios; y **agregamos en esta entrega** un cuarto servicio, `rabbitmq-admin-service` en el puerto 8083, que da observabilidad del broker. La infraestructura local son tres contenedores: RabbitMQ, PostgreSQL y ElasticMQ, todos gestionados con Docker Compose y verificados como *healthy*."

---

## Diapositiva 4 — Infraestructura Docker y PostgreSQL

**Contenido:**
- `docker-compose.yml` → `rabbitmq` (rabbitmq:4.2-management), `postgres` (postgres:16-alpine), `elasticmq` (softwaremill/elasticmq-native:1.6.11).
- PostgreSQL publicado en **`127.0.0.1:55432:5432`** (remapeado para convivir con un PostgreSQL nativo en 5432).
- Persistencia real verificada: tablas `pedidos` y `productos` creadas con `ddl-auto=update`.

**Guion oral (40 s):**
> "La infraestructura local se levanta con un solo `docker compose up -d`. Levantamos RabbitMQ con su UI de administración, PostgreSQL 16 y ElasticMQ como broker compatible con SQS. Un detalle de entorno que documentamos: el puerto 5432 estaba ocupado por un PostgreSQL nativo de la máquina, así que publicamos el contenedor en **55432** y los servicios se conectan con `DB_PORT=55432`. Verificamos la persistencia **real** contra ese PostgreSQL, no solo contra H2."

**Comando demo:**
```powershell
docker compose up -d
docker ps --format "{{.Names}}`t{{.Status}}`t{{.Ports}}"
```

---

## Diapositiva 5 — Exchanges, colas y bindings

**Contenido — topología real verificada con `rabbitmqctl`:**

| Exchange | Tipo | Durable |
|---|---|---|
| `pedidos360.direct.exchange` | direct | ✅ |
| `pedidos360.topic.exchange` | topic | ✅ |
| `pedidos360.dlx` | direct | ✅ |

| Cola principal | DLQ | Binding topic |
|---|---|---|
| `pedidos360.pedidos.cola` | `pedidos360.pedidos.dlq` | `pedidos.pedido.#` |
| `pedidos360.productos.cola` | `pedidos360.productos.dlq` | `productos.producto.#` |
| `pedidos360.auditoria.cola` | `pedidos360.auditoria.dlq` | `#` |

- **Direct** = comandos/eventos puntuales y errores. **Topic** = eventos de dominio por patrón.
- Routing key centralizada: `topicRoutingKey(eventType)` = `"pedidos." + eventType`.

**Guion oral (50 s):**
> "Esta es la topología objetivo, **verificada directamente en el broker**. Mantenemos el *direct exchange* original para comandos y errores, y **añadimos un topic exchange** para ruteo por patrón. Publicamos en tres colas de dominio: pedidos, productos y auditoría. Cada cola tiene su propia Dead Letter Queue. Fíjense en los bindings: pedidos escucha `pedidos.pedido.#`, productos escucha `productos.producto.#`, y auditoría escucha `#`, es decir, **todos** los eventos. La routing key se construye en un único método, `topicRoutingKey`, para que no haya derivas entre publicadores."

**Comando demo:**
```powershell
docker exec pedidos360-rabbitmq rabbitmqctl -q list_exchanges name type durable
docker exec pedidos360-rabbitmq rabbitmqctl -q list_bindings source_name destination_name routing_key
```

---

## Diapositiva 6 — Productores y consumidores

**Contenido:**
- **Productores:** los servicios publican tras el *commit* de la transacción (`@TransactionalEventListener(AFTER_COMMIT)`).
- **Puertos:** pedidos usa `EventPublisher` (genérico) + fachada `PedidoEventPublisher`; productos usa `ProductoPublisher` (dominio).
- **Adaptadores intercambiables:** `RabbitEventPublisher` / `SqsEventPublisher` detrás del mismo puerto (`messaging.provider=rabbitmq|sqs`).
- **Consumidores:** `@RabbitListener` por cola; la cola de auditoría registra todos los eventos de dominio.

**Guion oral (45 s):**
> "El patrón de publicación es importante: los eventos se emiten **solo después del commit** de la transacción, así nunca publicamos un evento de un pedido que luego hizo rollback. El dominio depende de un **puerto**, no de RabbitMQ directamente; detrás hay dos adaptadores: uno RabbitMQ y uno SQS. Eso nos permite cambiar de broker con una propiedad, `messaging.provider`. Del lado de consumo, cada cola tiene su `@RabbitListener`, y auditoría recibe todos los eventos por su binding `#`. Documentamos que ambos dominios mantienen su propio diseño de puerto —es una asimetría consciente que preserva lo ya probado."

---

## Diapositiva 7 — ACK, NACK, reintentos y DLQ

**Contenido — configuración real:**
- `acknowledge-mode=auto`
- `retry.max-attempts=3` + backoff exponencial (1s → 5s)
- `default-requeue-rejected=false`
- Al agotar reintentos → `ConditionalRejectingErrorHandler` rechaza **sin requeue** → DLX → DLQ con header `x-death`.

**Guion oral (50 s):**
> "Aquí está el corazón de la robustez. Si un consumidor falla, RabbitMQ reintenta **hasta tres veces** con backoff exponencial. Si sigue fallando, con `default-requeue-rejected=false` el mensaje **no** vuelve a la cola —lo que evita el *loop* infinito que tumbaría el servicio— y va a la Dead Letter Exchange, y de ahí a la DLQ de su dominio, con el historial en el header `x-death`. Verificamos este flujo **end-to-end** con pruebas de integración que comprueban la llegada real a la DLQ. Un hallazgo honesto: no hay un `MessageRecoverer` explícito; el comportamiento por defecto de Spring AMQP es suficiente y así lo documentamos."

**Comando demo:**
```powershell
cd backend/pedidos-service
.\mvnw.cmd -B test "-Dtest=PedidoDlqIntegrationTests"
```

---

## Diapositiva 8 — RabbitMQ Admin

**Contenido:**
- Nuevo microservicio `rabbitmq-admin-service` (puerto 8083).
- Endpoints **read-only**: `GET /admin/queues|exchanges|bindings|dlq`.
- Seguridad: `/admin/**` exige `ROLE_ADMIN`; actuator `health`/`info` públicos; ante fallo de la Management API → `502 rabbitmq_management_unavailable`.
- Credenciales del broker **solo server-side**.

**Guion oral (40 s):**
> "Para la rúbrica de observabilidad construimos un microservicio de administración. Expone de forma **solo de lectura** las colas, exchanges, bindings y DLQs, consultando la Management API del broker del lado del servidor —las credenciales nunca llegan al cliente. Está protegido: sin token da 401, con token pero sin rol ADMIN da 403, y con ADMIN responde 200. Está cubierto con quince pruebas, todas en verde, incluyendo el caso de broker no disponible, que devuelve 502."

**Comando demo:**
```powershell
cd backend/rabbitmq-admin-service
.\mvnw.cmd -B test
```

---

## Diapositiva 9 — Pruebas y evidencias

**Contenido — matriz real:**

| Servicio | Proveedor | Tests | Fallos |
|---|---|---:|---:|
| bff-service | — | 21 | 0 |
| pedidos-service | rabbitmq | 65 | 0 |
| productos-service | rabbitmq | 65 | 0 |
| rabbitmq-admin-service | — | 15 | 0 |
| pedidos-service | sqs | 65 | 0 |
| productos-service | sqs | 65 | 0 |
| PostgreSQL real | — | 1+1 | 0 |
| Frontend Angular | — | build | SUCCESS |

**Guion oral (40 s):**
> "El resumen de evidencias. Todas las suites backend están en verde: 21 en el BFF, 65 en pedidos, 65 en productos y 15 en el admin. Corremos además las suites completas bajo el adaptador **SQS** contra ElasticMQ, también en verde. Verificamos la persistencia contra PostgreSQL **real** y compilamos el frontend Angular sin errores. Las cifras son las que reporta Maven; el detalle está en `docs/ep3/05-pruebas-integracion.md` y el mapeo a la rúbrica en `docs/ep3/06-evidencias-rubrica.md`."

---

## Diapositiva 10 — Conclusiones

**Contenido:**
- Mensajería **consolidada e incremental**: Topic Exchange + 3 colas + DLQ + admin + SQS, sin eliminar lo funcional.
- **Trazabilidad:** cada cambio en su commit convencional; sin *push* ni despliegue AWS.
- **Pendiente declarado:** verificación OIDC contra Entra ID real (solo probado con `JwtDecoder` de test).

**Guion oral (35 s):**
> "En conclusión: consolidamos la mensajería de Pedidos360 de forma incremental y verificable. Todo lo que la rúbrica pide está implementado y probado, salvo un punto que declaramos con honestidad: la validación de seguridad se probó con un decodificador de test, **no contra un tenant real de Entra ID**, porque no disponemos de uno en el entorno. Con eso cerramos. Gracias —quedamos atentos a sus preguntas."

---

## Demostración real del flujo de pedidos (guion de demo en vivo)

**Precondiciones:** contenedores arriba (`docker compose up -d`), servicios locales levantados (`.\scripts\run-local.ps1` o por terminal), `npm start` en el frontend.

**Guion de la demo (2–3 min):**

1. **Frontend** — abrir `http://localhost:4200`, ir a `/productos` (público), mostrar el catálogo.
2. **Registrar un pedido** — el botón *Crear Pedido* está en una ruta protegida (`/pedidos`, `MsalGuard`). Sin sesión Entra ID real redirige al login (limitación declarada). **Alternativa verificable para la demo de mensajería** sin OIDC real: usar el flujo por API/evento y comprobar la topología.
3. **Comprobar el evento en RabbitMQ** — tras crear un pedido (o el evento `pedido.creado` emitido por el publisher), verificar ruteo del topic:
   ```powershell
   # Routing key del evento
   # pedido.creado -> topic "pedidos.pedido.creado" -> pedidos360.pedidos.cola + auditoria.cola (#)
   docker exec pedidos360-rabbitmq rabbitmqctl -q list_queues name messages
   ```
4. **Comprobar la DLQ** (opcional) — inducir un fallo de consumidor en una prueba y observar `x-death` en la DLQ:
   ```powershell
   cd backend/pedidos-service
   .\mvnw.cmd -B test "-Dtest=PedidoDlqIntegrationTests"
   ```
5. **RabbitMQ Management UI** — `http://localhost:15672` (`guest`/`guest`, loopback): mostrar exchanges, colas y bindings iguales que en la diapositiva 5.

> **Advertencia de la demo:** NO ejecutar `docker compose down -v` ni borrar colas/mensajes. El broker conserva mensajes de pruebas previas (por ejemplo, colas legacy `pedidos.eventos.queue` con mensajes), lo cual es evidencia válida y **no debe limpiarse**.

---

## Anexo — Comandos de la demo (copiables)

```powershell
# 1. Infraestructura
docker compose up -d
docker ps --format "{{.Names}}`t{{.Status}}"

# 2. Backend (JAVA_HOME jdk-21)
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\scripts\run-local.ps1                     # levanta 8080 / 8081 / 8082

# 3. Admin service (8083) — nueva terminal
cd backend\rabbitmq-admin-service; .\mvnw.cmd spring-boot:run

# 4. Frontend
cd frontend\pedidos360-web; npm start       # http://localhost:4200

# 5. Topología en vivo
docker exec pedidos360-rabbitmq rabbitmqctl -q list_exchanges name type durable
docker exec pedidos360-rabbitmq rabbitmqctl -q list_bindings source_name destination_name routing_key
docker exec pedidos360-rabbitmq rabbitmqctl -q list_queues name messages consumers

# 6. Pruebas (secuencial)
cd backend\bff-service;            .\mvnw.cmd -B test
cd backend\pedidos-service;        .\mvnw.cmd -B test
cd backend\productos-service;      .\mvnw.cmd -B test
cd backend\rabbitmq-admin-service; .\mvnw.cmd -B test

# 7. SQS (adaptador alterno)
$env:MESSAGING_PROVIDER='sqs'; .\mvnw.cmd -B test    # en pedidos y productos

# 8. PostgreSQL real
.\mvnw.cmd -B test "-Dsurefire.excludedGroups=" "-Dgroups=postgres"
```
