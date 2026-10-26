# EP3 — Guion de Presentación (5 a 10 minutos)

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Proyecto:** Pedidos360 · rama `main` · commit base `4175b1c`
- **Formato:** 10 diapositivas · duración objetivo **8 min** (rango 5–10) · demo del flujo de pedidos incluida.
- **Regla:** todo dato citado aquí es **verificable** con el código. No se declara probado lo que no se ejecutó (ver §5 de `06-evidencias-rubrica.md`).

> **Nota de honestidad técnica para el evaluador:** evidencia ejecutada el 2026-10-25: BFF **22/22**, Admin **28/28**, Pedidos **69/69**, Productos **69/69** y Frontend **9/9**, con el clúster RabbitMQ de 2 nodos y Keycloak local arriba; DLQ E2E con `x-death`, login OIDC local (PKCE + claims + 401/200 + CORS) y build Angular OK. El único límite funcional es el login productivo contra Entra ID, por ausencia de tenant/Client ID.

---

## Diapositiva 1 — Portada

**Título:** Pedidos360 — Mensajería Cloud Native con RabbitMQ
**Subtítulo:** DSY1107 · EP3 · Integrantes · Fecha 2026-10-25

**Guion oral (20 s):**
> "Buenos días. Somos [integrantes] y presentamos Pedidos360, la consolidación de la EP3 de Desarrollo Cloud Native I. En esta entrega evolucionamos la mensajería del sistema hacia un modelo de eventos con RabbitMQ —Topic Exchange, tres colas de dominio, Dead Letter Queues, un microservicio de administración y un adaptador SQS— sin romper lo que ya funcionaba. La implementación vive en `main` sobre el commit base `4175b1c`, con pruebas unitarias y de controller verificadas."

---

## Diapositiva 2 — Objetivo de Pedidos360

**Contenido:**
- Plataforma de gestión de **productos** y **pedidos** con arquitectura de microservicios cloud-native.
- Desacoplamiento de servicios, aislamiento de red y autenticación federada (Keycloak local / Entra ID en producción).
- **Objetivo EP3:** robustecer la mensajería asíncrona entre dominios mediante eventos de dominio confiables.

**Guion oral (30 s):**
> "Pedidos360 resuelve dos dominios: catálogo de productos y órdenes de compra. La comunicación interna entra por un BFF y baja a microservicios de dominio. En la EP1 y EP2 ya existía mensajería con RabbitMQ; el objetivo de la EP3 fue **consolidarla**: rutas por patrón, colas especializadas, manejo de fallos con DLQ, observabilidad con un servicio de admin y conservación de la alternativa SQS/ElasticMQ. Todo de forma **incremental**: no reescribimos ni eliminamos lo existente."

---

## Diapositiva 3 — Arquitectura de microservicios

**Contenido (tabla real de componentes):**

| Componente | Tecnología | Puerto |
|---|---|---|
| Frontend Angular | Angular 22 + oidc-client-ts | 4200 |
| Keycloak local | Keycloak 26.2 | 8180 |
| BFF Service | Spring Boot 3.3.4 / Java 21 | 8080 |
| Pedidos Service | Spring Boot 3.3.4 + JPA | 8081 |
| Productos Service | Spring Boot 3.3.4 + JPA | 8082 |
| RabbitMQ Admin Service | Spring Boot 3.3.4 | 8083 |
| RabbitMQ nodo 1 | rabbitmq:4.2-management | 5672 / 15672 |
| RabbitMQ nodo 2 (clúster) | rabbitmq:4.2-management | 5673 / 15673 |
| PostgreSQL / ElasticMQ | Docker Compose | 55432 / 9324 |

**Guion oral (40 s):**
> "Tenemos cuatro microservicios Spring Boot 3.3.4 sobre Java 21 y un frontend Angular 22. El BFF expone la API y propaga el token; pedidos y productos son los dominios; y **agregamos en esta entrega** un cuarto servicio, `rabbitmq-admin-service` en el puerto 8083, que da observabilidad y gestión del broker. La infraestructura local incluye **un clúster RabbitMQ de dos nodos**, PostgreSQL y ElasticMQ, gestionados con Docker Compose."

---

## Diapositiva 4 — Infraestructura Docker y PostgreSQL

**Contenido:**
- `docker-compose.yml` → `rabbitmq` + `rabbitmq2` (rabbitmq:4.2-management), `postgres` (postgres:16-alpine), `elasticmq` (softwaremill/elasticmq-native:1.6.11) y `keycloak` (quay.io/keycloak/keycloak:26.2).
- Clúster RabbitMQ: nodo 1 `pedidos360-rabbitmq` (`5672`/`15672`) y nodo 2 `pedidos360-rabbitmq2` (`5673`/`15673`), cookie Erlang compartida y contenedor `rabbitmq2-join` idempotente.
- PostgreSQL publicado en **`127.0.0.1:55432:5432`** (remapeado para convivir con un PostgreSQL nativo en 5432).
- Persistencia real verificada previamente: tablas `pedidos` y `productos` con `ddl-auto=update`.

**Guion oral (40 s):**
> "La infraestructura local se levanta con `docker compose up -d`. Levantamos **dos nodos RabbitMQ** con sus UIs, PostgreSQL 16 y ElasticMQ. El nodo 2 se une al clúster del nodo 1 mediante un contenedor efímero y una cookie Erlang compartida; el script de arranque es idempotente y valida `cluster_status`. Publicamos PostgreSQL en **55432** para no chocar con una instalación nativa en 5432. El clúster quedó **validado en vivo**: ambos nodos aparecen Running, sin alarmas ni particiones."

**Comando demo:**
```powershell
.\scripts\rabbitmq-up.ps1
.\scripts\rabbitmq-status.ps1
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
- Topología declarada **solo** desde `RabbitMQConfig`; Compose no duplica exchanges/colas/bindings.
- Clúster de 2 nodos con nombre de nodo estable y volumen por nodo.

**Guion oral (50 s):**
> "Esta es la topología objetivo. Mantenemos el *direct exchange* original para comandos y errores, y **añadimos un topic exchange** para ruteo por patrón. Publicamos en tres colas de dominio: pedidos, productos y auditoría. Cada cola tiene su propia Dead Letter Queue. Fíjense en los bindings: pedidos escucha `pedidos.pedido.#`, productos escucha `productos.producto.#`, y auditoría escucha `#`, es decir, **todos** los eventos. La routing key se construye en un único método, `topicRoutingKey`, para que no haya derivas entre publicadores. La topología vive en Java, no en Compose, y el clúster replica la definición entre ambos nodos."

**Comando demo:**
```powershell
docker exec pedidos360-rabbitmq rabbitmqctl -q list_exchanges name type durable
docker exec pedidos360-rabbitmq rabbitmqctl -q list_bindings source_name destination_name routing_key
docker exec pedidos360-rabbitmq rabbitmqctl -q cluster_status
```

---

## Diapositiva 6 — Productores y consumidores

**Contenido:**
- **Productores:** los servicios publican tras el *commit* de la transacción (`@TransactionalEventListener(AFTER_COMMIT)`).
- **Puertos:** pedidos usa `EventPublisher` (genérico) + fachada `PedidoEventPublisher`; productos usa `ProductoPublisher` (dominio).
- **Adaptadores intercambiables:** `RabbitEventPublisher` / `SqsEventPublisher` detrás del mismo puerto (`messaging.provider=rabbitmq|sqs`).
- **Consumidores:** `PedidosColaConsumer` (`pedidos360.pedidos.cola`), `ProductosColaConsumer` (`pedidos360.productos.cola`) y `AuditoriaEventConsumer` (`pedidos360.auditoria.cola`); consumidores legados también con ACK/NACK manual.

**Guion oral (45 s):**
> "El patrón de publicación es importante: los eventos se emiten **solo después del commit** de la transacción, así nunca publicamos un evento de un pedido que luego hizo rollback. El dominio depende de un **puerto**, no de RabbitMQ directamente; detrás hay dos adaptadores: uno RabbitMQ y uno SQS. Eso nos permite cambiar de broker con una propiedad, `messaging.provider`. Del lado de consumo, cada cola objetivo tiene su propio `@RabbitListener` que delega en el manejador de ACK/NACK; auditoría recibe todos los eventos por su binding `#`. Los consumidores validan el payload antes de confirmar."

---

## Diapositiva 7 — ACK, NACK, reintentos y DLQ

**Contenido — configuración real:**
- `acknowledge-mode=manual`
- `spring.rabbitmq.listener.simple.retry.enabled=false` (la política vive en el consumidor)
- `RetryTemplate`: 3 intentos con backoff fijo de 500 ms
- Éxito → `basicAck`; fallo tras 3 intentos → `basicNack(requeue=false)` → DLX `pedidos360.dlx` → DLQ del dominio.

**Guion oral (50 s):**
> "Aquí está el corazón de la robustez. El consumidor confirma o rechaza **manualmente**. Delega en `ManualAckRabbitHandler`, que ejecuta la acción dentro de un `RetryTemplate` de tres intentos con backoff fijo de 500 ms. Si todo va bien, hace `basicAck`. Si falla los tres intentos, hace `basicNack` con `requeue=false`, lo que evita el *loop* infinito y envía el mensaje a la DLX y de ahí a la DLQ del dominio. Desactivamos el retry automático del listener para no duplicar políticas. Los tests unitarios verifican ACK, los tres intentos y NACK; además, la prueba end-to-end contra el broker real confirmó el mensaje en la DLQ con cabecera `x-death`."

**Comando demo:**
```powershell
cd backend/pedidos-service
.\mvnw.cmd -B "-Dtest=ManualAckRabbitHandlerTests,PedidosColaConsumerTests" test
```

---

## Diapositiva 8 — RabbitMQ Admin

**Contenido:**
- Microservicio `rabbitmq-admin-service` (puerto 8083).
- **Lectura:** `GET /admin/queues|exchanges|bindings|dlq`.
- **CRUD:** `POST/DELETE /admin/queues`, `POST/DELETE /admin/exchanges`, `POST/DELETE /admin/bindings`, `DELETE /admin/queues/{name}/messages` (purge) y `POST /admin/messages` (publicar).
- `RabbitAdminService` centraliza los casos de uso; el controlador solo traduce HTTP.
- DTOs con Jakarta Validation; errores 400 por campo y 502 si la Management API falla.
- Seguridad: `/admin/**` exige `ROLE_ADMIN`; credenciales del broker **solo server-side**.

**Guion oral (40 s):**
> "Para la rúbrica construimos un microservicio de administración. Ya no es solo lectura: permite crear y borrar colas, exchanges y bindings, purgar colas y publicar mensajes de prueba. Toda la lógica está en `RabbitAdminService`; el controlador solo traduce HTTP y los DTOs se validan con Jakarta Validation. Está protegido: sin token da 401, sin rol ADMIN da 403, y con ADMIN responde 200 o 201. La suite del admin tiene **28 pruebas en verde**, incluyendo validación de nombres reservados, payload y broker no disponible."

**Comando demo:**
```powershell
cd backend/rabbitmq-admin-service
.\mvnw.cmd -B test
```

---

## Diapositiva 9 — Pruebas y evidencias

**Contenido — matriz real:**

| Servicio / suite | Proveedor | Tests | Fallos |
|---|---|---:|---:|
| bff-service | — | 22 | 0 |
| rabbitmq-admin-service | — | 28 | 0 |
| pedidos — suite completa (unitaria + integración) | rabbitmq | 69 | 0 |
| productos — suite completa (unitaria + integración) | rabbitmq | 69 | 0 |
| pedidos — consumidores + ACK/NACK (subconjunto) | rabbitmq (unitario) | 8 | 0 |
| productos — consumidores + ACK/NACK (subconjunto) | rabbitmq (unitario) | 8 | 0 |
| Clúster RabbitMQ 2 nodos | rabbitmq | 2 nodos Running | 0 |
| Frontend Angular — Vitest | — | 9 | 0 |
| Frontend Angular — build | — | build | SUCCESS |
| OIDC local Keycloak (PKCE + claims) | keycloak | smoke OK | 0 |
| OIDC productivo Entra ID | — | pendiente | — |

**Guion oral (40 s):**
> "El resumen de evidencias ejecutadas en esta sesión: 22 pruebas del BFF, 28 del admin, y las suites completas de pedidos y productos con 69 pruebas cada una contra el clúster RabbitMQ real. Esas suites incluyen 10 y 11 pruebas de integración respectivamente, con DLQ end-to-end y `x-death`; además, 8 y 8 cubren específicamente consumidores y ACK/NACK. El clúster de dos nodos quedó validado en vivo, el login OIDC local con Keycloak emitió tokens con los claims correctos y protegió BFF/Admin, el frontend tiene 9 pruebas Vitest en verde y el build Angular es correcto. El único pendiente es el login productivo contra Entra ID por ausencia de tenant; no lo declaramos como probado. El detalle está en `docs/ep3/06-evidencias-rubrica.md`."

---

## Diapositiva 10 — Conclusiones

**Contenido:**
- Mensajería **consolidada e incremental**: topic + 3 colas + DLQ + ACK/NACK manual + clúster 2 nodos + admin CRUD + SQS.
- **Trazabilidad:** commit base `4175b1c`; los cambios de esta actualización están en el árbol local.
- **Pendiente declarado:** login productivo con Entra ID (sin tenant/Client ID). Clúster, DLQ y OIDC local ya validados en vivo.

**Guion oral (35 s):**
> "En conclusión: consolidamos la mensajería de Pedidos360 de forma incremental. La rúbrica está cubierta en código y con pruebas unitarias, de integración y E2E en verde. Validamos el clúster de dos nodos y la DLQ con `x-death` contra el broker real, y el login OIDC local con Keycloak. Declaramos un solo pendiente con honestidad: no probamos el login productivo contra un tenant real de Entra ID por falta de tenant/Client ID. Todo lo demás está implementado y verificable. Gracias —quedamos atentos a sus preguntas."

---

## Demostración real del flujo de pedidos (guion de demo en vivo)

**Precondiciones:** contenedores arriba (`docker compose up -d`), servicios locales levantados (`.\scripts\run-local.ps1` o por terminal), `npm start` en el frontend.

**Guion de la demo (2–3 min):**

1. **Frontend** — abrir `http://localhost:4200`, ir a `/productos` (público), mostrar el catálogo.
2. **Registrar un pedido** — el botón *Crear Pedido* está en una ruta protegida (`/pedidos`, `authGuard`). Iniciar sesión con Keycloak local (`demo` / `Demo1234!`), completar el callback PKCE y crear el pedido autenticado.
3. **Comprobar el evento en RabbitMQ** — tras crear un pedido (o el evento `pedido.creado` emitido por el publisher), verificar ruteo del topic:
   ```powershell
   # Routing key del evento
   # pedido.creado -> topic "pedidos.pedido.creado" -> pedidos360.pedidos.cola + auditoria.cola (#)
   docker exec pedidos360-rabbitmq rabbitmqctl -q list_queues name messages
   ```
4. **Comprobar ACK/NACK** (unitario) — verificar ACK, reintentos y NACK sin requeue:
   ```powershell
   cd backend/pedidos-service
   .\mvnw.cmd -B "-Dtest=ManualAckRabbitHandlerTests,PedidosColaConsumerTests" test
   ```
5. **Clúster y Management UI** — `http://localhost:15672` y `http://localhost:15673` (`guest`/`guest`, loopback): mostrar exchanges, colas, bindings y `cluster_status`.

> **Advertencia de la demo:** NO ejecutar `docker compose down -v` ni borrar colas/mensajes. El broker conserva mensajes de pruebas previas (por ejemplo, colas legacy `pedidos.eventos.queue` con mensajes), lo cual es evidencia válida y **no debe limpiarse**.

---

## Anexo — Comandos de la demo (copiables)

```powershell
# 1. Infraestructura (clúster 2 nodos + postgres + elasticmq + keycloak)
.\scripts\rabbitmq-up.ps1
.\scripts\rabbitmq-status.ps1
docker compose up -d keycloak
docker ps --format "{{.Names}}`t{{.Status}}"

# 2. Backend (JAVA_HOME jdk-21.0.8)
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.8'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\scripts\run-local.ps1                     # levanta 8080 / 8081 / 8082

# 3. Admin service (8083) — nueva terminal
cd backend\rabbitmq-admin-service; .\mvnw.cmd spring-boot:run

# 4. Frontend
cd frontend\pedidos360-web; npm start       # http://localhost:4200

# 5. Login OIDC local y topología en vivo
# http://localhost:8180/realms/pedidos360 (demo / Demo1234!)
docker exec pedidos360-rabbitmq rabbitmqctl -q cluster_status
docker exec pedidos360-rabbitmq rabbitmqctl -q list_exchanges name type durable
docker exec pedidos360-rabbitmq rabbitmqctl -q list_bindings source_name destination_name routing_key
docker exec pedidos360-rabbitmq rabbitmqctl -q list_queues name messages consumers

# 6. Pruebas (secuencial)
cd backend\bff-service;            .\mvnw.cmd -B test
cd backend\pedidos-service;        .\mvnw.cmd -B test
cd backend\productos-service;      .\mvnw.cmd -B test
cd backend\rabbitmq-admin-service; .\mvnw.cmd -B test

# 6b. Evidencia unitaria ya ejecutada (sin broker)
cd backend\pedidos-service
.\mvnw.cmd -B "-Dtest=PedidosColaConsumerTests,PedidoEventConsumerTests,ManualAckRabbitHandlerTests" test
cd ..\productos-service
.\mvnw.cmd -B "-Dtest=ProductosColaConsumerTests,ProductoEventConsumerTests,ManualAckRabbitHandlerTests" test

# 7. SQS (adaptador alterno)
$env:MESSAGING_PROVIDER='sqs'; .\mvnw.cmd -B test    # en pedidos y productos

# 8. PostgreSQL real
.\mvnw.cmd -B test "-Dsurefire.excludedGroups=" "-Dgroups=postgres"
```
