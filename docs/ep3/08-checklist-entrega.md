# EP3 — Checklist de Entrega

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Proyecto:** Pedidos360
- **Rama:** `ep3-rabbitmq` · **Último commit:** `9d0ff1e`
- **Fecha:** 2026-10-20
- **Regla transversal:** sin `git push`, sin despliegue AWS, sin eliminar recursos, sin declarar probado lo no probado.

---

## A. Estado de entrega (resumen)

| Área | Estado | Evidencia |
|---|---|---|
| Repositorio Git limpio | ✅ | `git status` = "nothing to commit", rama `ep3-rabbitmq` |
| Commits convencionales Fases 1–9 | ✅ | 7 commits (`89d80ed`…`9d0ff1e`) |
| Documentación `docs/ep3/01–08` | ✅ | 8 archivos presentes |
| Pruebas backend (RabbitMQ) | ✅ | 21 + 65 + 65 + 15 = **166/166** |
| Pruebas adaptador SQS | ✅ | 65 + 65 = **130** (skips por gate, 0 fallos) |
| Persistencia PostgreSQL real | ✅ | 1 + 1 round-trip verde |
| Build frontend Angular | ✅ | `ng build` → BUILD SUCCESS |
| Infraestructura Docker | ✅ | 3 contenedores *healthy* |
| Secretos en repositorio | ✅ | Solo placeholders de desarrollo |

---

## B. Matriz definitiva — Ocho criterios EP3

> Los **ocho criterios** agrupan los requisitos centrales de mensajería de la pauta EP3 (los 15 subcriterios completos están en `06-evidencias-rubrica.md`).

| # | Criterio EP3 | Estado | Implementación | Prueba / comando | Evidencia |
|---|---|---|---|---|---|
| 1 | **Topic Exchange** (ruteo por patrón sobre el direct existente) | ✅ Cumple | `RabbitMQConfig`: `pedidos360.topic.exchange` (topic, durable), bindings `pedidos.pedido.#`, `productos.producto.#`, `#`; helper `topicRoutingKey(eventType)` | `TopicRoutingKeysTests` (4+4) · `mvnw -B test` | broker: exchange topic presente |
| 2 | **Tres colas principales de dominio** | ✅ Cumple | `pedidos360.pedidos.cola`, `pedidos360.productos.cola`, `pedidos360.auditoria.cola` (durables) | `TopicExchangeIntegrationTests` · `list_queues` | 3 colas + 3 DLQ en broker |
| 3 | **Productores y consumidores** | ✅ Cumple | `@TransactionalEventListener(AFTER_COMMIT)` → puerto `EventPublisher`/`ProductoPublisher`; `@RabbitListener` por cola | `*EventPublisherTests`, `*EventConsumerTests`, integración Rabbit | eventos consumidos en broker |
| 4 | **DLX + DLQ por dominio** | ✅ Cumple | `pedidos360.dlx` (direct) + `.dlq` por cola con `x-dead-letter-exchange` | `PedidoDlqIntegrationTests` / `ProductoDlqIntegrationTests` (1+1) | header `x-death` verificado |
| 5 | **ACK/NACK con reintentos limitados** | ✅ Cumple | `acknowledge-mode=auto`, retry `max-attempts=3` + backoff, `default-requeue-rejected=false` → DLQ | integración DLQ (agotamiento → DLQ) | rechazo sin requeue confirmado |
| 6 | **RabbitMQ Admin** (observabilidad) | ✅ Cumple | `rabbitmq-admin-service` (8083): `/admin/queues\|exchanges\|bindings\|dlq`, `ROLE_ADMIN`, 502 ante fallo | `AdminSecurityTests` (3), `AdminControllerTests` (5) → **15/15** | suite admin verde |
| 7 | **Conservar ElasticMQ / SQS** | ✅ Cumple | `messaging.provider=rabbitmq\|sqs`; `SqsConfig`, `SqsEventPublisher`, `SqsProductoEventPublisher`; `elasticmq` en compose | `MESSAGING_PROVIDER=sqs` → 65+65 · smoke real `SendMessage` (cola 4→5) | ElasticMQ *healthy* en 9324 |
| 8 | **Seguridad JWT extremo a extremo** | ⚠️ Cumple con límite declarado | `SecurityConfig` (resource server) en pedidos/productos/admin; `BearerTokenPropagationInterceptor` en BFF; `pedidos360.security.require-auth` | `JwtSecurityTests` (3), `PermissiveSecurityProfileTests` (1), `AdminSecurityTests` (3) | 401/403/200 con `JwtDecoder` de test |

**Resumen:** 8/8 criterios implementados; el criterio 8 se marca **"Cumple con límite declarado"** porque la validación OIDC se verificó con un `JwtDecoder` de test, **no** contra un tenant real de Microsoft Entra ID.

---

## C. Checklist pre-entrega (marcar antes de subir)

### Código y pruebas
- [x] Los 4 módulos backend compilan (`mvnw -B test` → BUILD SUCCESS).
- [x] Suites RabbitMQ: BFF 21 · Pedidos 65 · Productos 65 · Admin 15 (0 fallos).
- [x] Suites SQS: Pedidos 65 · Productos 65 (0 fallos, skips gated).
- [x] PostgreSQL real: 1 + 1 (0 fallos).
- [x] Frontend: `npm ci && npm run build` → BUILD SUCCESS.
- [x] Ejecución **secuencial** de suites (documentado para evitar contaminación del broker).

### Infraestructura
- [x] `docker-compose.yml` define rabbitmq, postgres (55432), elasticmq.
- [x] `infra/elasticmq/elasticmq.conf` declara la cola `pedidos360-eventos`.
- [x] Topología verificada con `rabbitmqctl` (exchanges, colas, bindings).
- [x] **No** se ejecutó `docker compose down -v` ni se borraron colas/volúmenes.

### Documentación
- [x] `README.md` cubre levantar y probar todos los componentes (RabbitMQ, admin, SQS, PostgreSQL, frontend).
- [x] `docs/ep3/01`–`06` completos y consistentes.
- [x] `docs/ep3/07-guion-presentacion.md` (10 diapositivas + demo).
- [x] `docs/ep3/08-checklist-entrega.md` (este documento).

### Seguridad
- [x] Sin credenciales reales: solo placeholders (`guest`, `dbadmin`, `elasticmq`) y `.env.example`.
- [x] `.gitignore` cubre `.env`, `*.pem`, `*.key`, `target/`, `node_modules/`, `dist/`.
- [x] `git grep` de secretos → solo valores de desarrollo/test.
- [x] Perfil `aws` exige `${DB_PASSWORD:}` sin default.

### Git
- [x] Árbol limpio, rama `ep3-rabbitmq`.
- [x] Commits convencionales por fase.
- [ ] **PUSH a GitHub** — **NO ejecutado** (requiere autorización del usuario; ver `09-informe-final.md` §4).
- [ ] Merge a `main` — **NO ejecutado** (a decisión del usuario).

---

## D. Comandos de verificación rápida (para el evaluador)

```powershell
# Git
git status
git log --oneline -8
git branch -vv

# Infraestructura
docker ps --format "{{.Names}}`t{{.Status}}`t{{.Ports}}"
docker exec pedidos360-rabbitmq rabbitmqctl -q list_exchanges name type durable
docker exec pedidos360-rabbitmq rabbitmqctl -q list_queues name messages consumers
docker exec pedidos360-rabbitmq rabbitmqctl -q list_bindings source_name destination_name routing_key

# Pruebas (desde cada backend\<servicio>, con JAVA_HOME=jdk-21)
.\mvnw.cmd -B test                              # rabbitmq (default)
$env:MESSAGING_PROVIDER='sqs'; .\mvnw.cmd -B test   # sqs
.\mvnw.cmd -B test "-Dsurefire.excludedGroups=" "-Dgroups=postgres"   # postgres

# Frontend
cd frontend\pedidos360-web; npm ci; npm run build
```

---

## E. Bloqueadores / pendientes declarados

| # | Pendiente | Impacto | Acción sugerida |
|---|---|---|---|
| 1 | Validación OIDC contra Entra ID real | Medio | Configurar `AZURE_TENANT_ID`/`AZURE_CLIENT_ID` y probar login real; hoy solo `JwtDecoder` de test |
| 2 | `frontend` usa Client ID placeholder | Medio | Reemplazar `00000000-…` en `environment.ts`/`environment.prod.ts` |
| 3 | Push / merge a GitHub | Alto (entrega) | Requiere autorización explícita del usuario |
| 4 | README histórico menciona "Evaluación Parcial 1" y Angular 19 | Bajo | Corregido/actualizado en esta entrega (ver `09-informe-final.md` §2) |

**Ningún bloqueador impide la compilación ni las pruebas; son condiciones de entorno/entrega.**
