# EP3 — Consolidación de la Implementación (Fases 1–8)

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Fecha:** 2026-10-20
- **Base:** `docs/ep3/01-auditoria-inicial.md` · `02-arquitectura-objetivo.md` · `03-plan-de-implementacion.md`
- **Rama:** `ep3-rabbitmq` (sin `git push`)
- **Alcance:** trabajo de consolidación y verificación sobre la implementación incremental 4.1–4.6, sin eliminar funcionalidad y sin inventar resultados.

> Las suites se ejecutan **secuencialmente**: pedidos y productos comparten el broker RabbitMQ local y la cola `pedidos360.auditoria.cola` está bindeada con `#`, por lo que correrlas en paralelo contamina los asserts. Además, dos procesos Maven en paralelo colisionan en el lock de `.m2`.

---

## 1. Fase 2 — Routing keys y bindings (commit `2497abe`)

**Problema:** las routing keys del topic se construían de forma dispersa (`prefix + eventType` repetido en cada publisher), con riesgo de deriva.

**Solución:**
- Se añadió `RabbitMQConfig.topicRoutingKey(String eventType)` en pedidos y productos: **única fuente de verdad** de la routing key (`TOPIC_ROUTING_PREFIX + eventType`, p. ej. `pedidos.pedido.creado`, `productos.producto.stock.actualizado`).
- `RabbitEventPublisher` (pedidos) y `ProductoEventPublisher` (productos) derivan la routing key vía el helper, eliminando la duplicación.
- **Decisión de bindings:** se mantiene el patrón **específico** `pedidos.pedido.#` / `productos.producto.#` (más preciso que el `pedidos.#` del plan original). Cada cola recibe su patrón; **no** hay bindings superpuestos sobre una misma cola ⇒ **no hay entregas duplicadas**.
- Los eventos de **error** no se publican al topic, sólo al direct exchange.

**Tests nuevos:** `TopicRoutingKeysTests.java` (pedidos y productos) con un matcher de topic wildcard (`*`/`#`) que valida:
- routing keys de dominio válidas contra `pedidos.pedido.#` / `productos.producto.#`;
- separación dominio vs auditoría (`#`).

**Resultado:** `Tests run: 4, Failures: 0, Errors: 0` por servicio.

---

## 2. Fase 3 — Unificación de adaptadores RabbitMQ/SQS (commit `c9b5ae2`)

**Contexto:** los dos servicios modelaban la mensajería con diseños distintos:
- **Pedidos:** puerto genérico `EventPublisher` + fachada de dominio `PedidoEventPublisher`.
- **Productos:** puerto de dominio `ProductoPublisher` (el publisher implementa el puerto directamente).

**Decisión (enfoque mínimo):** *conservar* el diseño propio de cada servicio —refactorizar ambos a un mismo shape no aporta valor y arriesga regresiones— y **corregir la única inconsistencia aislada**: `SqsProductoEventPublisher` no propagaba la routing key. Se homogeneizó para que publique la routing key como atributo SQS (`ROUTING_KEY_ATTRIBUTE = "routingKey"`), reutilizando `topicRoutingKey`, en paridad con `SqsEventPublisher` de pedidos.

**Asimetría documentada (hexagonal AMQP vs SQS):**

| Aspecto | Pedidos | Productos |
|---|---|---|
| Puerto | `EventPublisher` (genérico) | `ProductoPublisher` (dominio) |
| Fachada | `PedidoEventPublisher` | — |
| Adaptador Rabbit | `RabbitEventPublisher` | `ProductoEventPublisher` |
| Adaptador SQS | `SqsEventPublisher` | `SqsProductoEventPublisher` |
| Routing key en SQS | atributo `routingKey` | atributo `routingKey` (homogeneizado) |

**Verificación bajo `MESSAGING_PROVIDER=sqs`:** Pedidos 60 (9 *skipped*) · Productos 60 (10 *skipped*), 0 fallos, contextos SQS sin beans duplicados (los tests de integración Rabbit quedan *gateados* off).

---

## 3. Fase 4 — ACK/NACK y DLQ (commit `a4b3fbc`)

**Configuración efectiva (verificada contra el broker):**
- `spring.rabbitmq.listener.simple.acknowledge-mode=auto`
- `retry.enabled=true`, `retry.max-attempts=3`, backoff exponencial (`initial-interval=1000`, `multiplier=2.0`, `max-interval=5000`)
- `default-requeue-rejected=false`

**Comportamiento:** tras agotar los 3 reintentos, `ConditionalRejectingErrorHandler` emite *"Retry Policy Exhausted"*, rechaza **sin requeue** ⇒ el mensaje va al **DLX** (`pedidos360.dlx`) y de allí a la **DLQ** de la cola (`pedidos360.pedidos.dlq` / `pedidos360.productos.dlq`), con el header `x-death`.

**Hallazgo:** **no** existe un `MessageRecoverer` explícito en el código. El comportamiento por defecto de Spring AMQP es correcto y suficiente; se documenta en lugar de introducir una clase innecesaria.

**Tests nuevos:** `PedidoDlqIntegrationTests.java` y `ProductoDlqIntegrationTests.java`. Arrancan manualmente el contenedor de listener (`RabbitListenerEndpointRegistry` + `waitForConsumer`), publican un evento al topic y verifican la llegada a la DLQ con `x-death`. Ambos **1/1 verde**.

---

## 4. Fase 5 — Auditoría de `rabbitmq-admin-service`

**Alcance revisado:** servicio de **observabilidad de sólo lectura** (`GET /admin/queues|exchanges|bindings|dlq`), puerto 8083. El plan **restringió `/admin/dlq` a listado** (el `RabbitManagementHttpClient.listDeadLetterQueues()` filtra por sufijo `.dlq`); no se exige CRUD de creación/borrado.

**Seguridad:** `/admin/**` exige `ROLE_ADMIN`; actuator `health`/`info` públicos; ante fallo de la Management API responde `502` con `rabbitmq_management_unavailable`. Credenciales del broker sólo server-side.

**Cobertura (15/15 verde):**
- `AdminSecurityTests` (3): sin token → 401; token sin rol ADMIN → 403; token con `ROLE_ADMIN` → 200.
- `AdminControllerTests` (5), `RabbitManagementClientTests` (5), `RabbitmqAdminServiceApplicationTests` (2).

**Decisión:** sin cambios de código — el módulo ya cumple la rúbrica aplicable.

---

## 5. Fase 7 — PostgreSQL (persistencia real, commit `94a54ab`)

**Situación inicial:** los tests usaban H2 en modo PostgreSQL; el datasource de producción (perfil `local`/`aws`) ya apuntaba a PostgreSQL con `ddl-auto=update`, pero **no había verificación real** contra un PostgreSQL.

**Hallazgo de entorno:** el puerto `5432` estaba ocupado por un **PostgreSQL 18 nativo** (servicio `postgresql-x64-18`). Para no interferir con esa instalación, el contenedor `pedidos360-postgres` del compose publica en **`127.0.0.1:55432:5432`**.

**Verificación:** se añadieron `PedidoPostgresPersistenceTests` y `ProductoPostgresPersistenceTests` (`@Tag("postgres")`, `@DataJpaTest` con `Replace.NONE`, dialecto PostgreSQL, `ddl-auto=update`). Hacen *round-trip* real (persistir → leer → borrar) contra el contenedor.
- Resultado: **1/1 verde** por servicio.
- Evidencia: las tablas `pedidos` y `productos` fueron creadas en la base `pedidos360` del contenedor (consultadas vía `psql`).

**Aislamiento en CI/suite estándar:** los POMs configuran `maven-surefire-plugin` con `<excludedGroups>postgres</excludedGroups>` (property `surefire.excludedGroups`), de modo que la suite estándar **no** exige el contenedor. Para ejecutarlos:

```powershell
.\mvnw.cmd -B test "-Dsurefire.excludedGroups=" "-Dgroups=postgres"
```

---

## 6. Secretos y `.gitignore`

- Sólo **placeholders con defaults de desarrollo**: `${DB_PASSWORD:dbadmin}`, `${RABBITMQ_PASSWORD:guest}`, `${SQS_ACCESS_KEY:elasticmq}`. El perfil `aws` exige `${DB_PASSWORD:}` sin default.
- `.gitignore` raíz robusto: `.env`, `*.pem`, `*.key`, `target/`, `node_modules/`, `dist/`.
- Escaneo de secretos limpio; `git diff --cached` sin `.env/.key/.pem`.

---

## 7. Resumen de commits (sin `git push`)

| Commit | Mensaje | Fase |
|---|---|---|
| `89d80ed` | chore(ep3): respaldo de implementación incremental fases 4.1–4.6 | 1 |
| `2497abe` | feat(ep3): centralizar routing keys del topic | 2 |
| `c9b5ae2` | refactor(ep3): homogeneizar adaptador SQS (routing key) | 3 |
| `a4b3fbc` | test(ep3): verificar dead-letter a DLQ con x-death | 4 |
| `388bcc9` | test(ep3): verificar seguridad JWT extremo a extremo | 6 |
| `94a54ab` | test(ep3): verificar persistencia real contra PostgreSQL | 7 |
