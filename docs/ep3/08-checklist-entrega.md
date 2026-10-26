# EP3 — Checklist de Entrega

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Proyecto:** Pedidos360
- **Rama:** `main` · **Commit base:** `4175b1c`
- **Fecha de actualización:** 2026-10-25
- **Regla transversal:** no declarar probado lo que no se ejecutó; no borrar colas, volúmenes ni mensajes de evidencia.

---

## A. Estado de entrega (resumen)

| Área | Estado | Evidencia |
|---|---|---|
| Código backend compila | ✅ | `mvnw.cmd -B test` en BFF, admin y suites unitarias de pedidos/productos |
| BFF service | ✅ | **22/22** tests en verde |
| Admin service | ✅ | **28/28** tests en verde |
| Suites completas pedidos/productos | ✅ | Pedidos **69/69** · Productos **69/69** (59+10 y 58+11) |
| Consumidores + ACK/NACK (unitario) | ✅ | Pedidos **8/8** · Productos **8/8** |
| Frontend Angular | ✅ | `npm test` → **9/9** · `npm run build` → **BUILD SUCCESS** |
| Configuración Compose | ✅ | `docker compose config --quiet` → OK |
| Clúster RabbitMQ 2 nodos | ✅ | `cluster_status`: ambos nodos Running, sin alarmas ni particiones |
| Integración RabbitMQ / DLQ E2E | ✅ | `*ConsumerRabbitIntegrationTests` y `*DlqIntegrationTests` en verde |
| Seguridad OIDC local | ✅ | Keycloak: Authorization Code + PKCE, claims, BFF/Admin 401→200, CORS 200 |
| Seguridad OIDC productiva | ⚠️ Pendiente | Entra ID sin tenant/Client ID real |
| Documentación EP3 | ✅ | `01`–`09` + este checklist actualizados |

---

## B. Matriz de rúbrica (8 criterios ponderados)

| # | Criterio | Peso | Estado | Implementación | Evidencia |
|---|---|---:|---|---|---|
| 1 | Centralización de configuración RabbitMQ | 12% | ✅ | `RabbitMQConfig` en cada servicio concentra topología, retry, converter y template; `topicRoutingKey(...)` como fuente única | Revisión de `config/RabbitMQConfig.java`; Compose sin topología duplicada |
| 2 | Beans de configuración | 13% | ✅ | `DirectExchange`, `TopicExchange`, DLX, `Queue`, `Binding`, `Jackson2JsonMessageConverter`, `RabbitTemplate`, `RetryTemplate` | `RabbitMQConfig` (pedidos y productos) |
| 3 | Separación de responsabilidades | 10% | ✅ | `config` / `event` / `service` / `client` / `controller` / `exception` | Estructura de paquetes de ambos servicios y admin |
| 4 | Consumidores | 15% | ✅ | `PedidosColaConsumer`, `ProductosColaConsumer`, `AuditoriaEventConsumer`, consumidores legados con ACK manual | `*ColaConsumerTests`, `*EventConsumerTests` y `*ConsumerRabbitIntegrationTests` (2+2) |
| 5 | ACK/NACK + reintentos + DLQ | 20% | ✅ | `acknowledge-mode=manual`; `ManualAckRabbitHandler` (3 intentos, 500 ms); `basicNack(requeue=false)` → DLX → DLQ | `ManualAckRabbitHandlerTests` (2+2), tests de consumidores y `*DlqIntegrationTests` (1+1, con `x-death`) |
| 6 | Endpoints admin | 13% | ✅ | GET colas/exchanges/bindings/dlq; POST colas/exchanges/bindings/messages; DELETE colas/exchanges/bindings; purge | `AdminControllerTests` (15) |
| 7 | `RabbitAdminService` | 10% | ✅ | Caso de uso central para lectura y mutación; controlador delgado | `service/RabbitAdminService.java`; tests de controller |
| 8 | Validación de DTOs | 7% | ✅ | Jakarta Validation + `@Valid` + manejador global 400/502 | `Create*Request`, `PublishMessageRequest`, `GlobalAdminExceptionHandler` |

**Resumen:** 8/8 criterios implementados y con evidencia ejecutada; los criterios 4 y 5 cuentan además con validación end-to-end contra el broker real.

### Refuerzo adicional — clúster RabbitMQ

- Nodo 1 `pedidos360-rabbitmq` (`5672` / `15672`) y nodo 2 `pedidos360-rabbitmq2` (`5673` / `15673`).
- Cookie Erlang compartida y contenedor `pedidos360-rabbitmq2-join` idempotente.
- `scripts/rabbitmq-up.ps1` valida ambos nodos y el `cluster_status`; `rabbitmq-status.ps1` reporta membresía.
- **Validado en vivo el 2026-10-24:** ambos nodos Running, sin alarmas ni particiones.

---

## C. Checklist pre-entrega

### Código y pruebas
- [x] BFF service compila y pasa **22/22** tests.
- [x] Admin service compila y pasa **28/28** tests.
- [x] Pedidos: suite completa **69/69** (59 unitarios + 10 de integración; consumidores + ACK/NACK **8/8**).
- [x] Productos: suite completa **69/69** (58 unitarios + 11 de integración; consumidores + ACK/NACK **8/8**).
- [x] Frontend Angular: **9/9** tests Vitest y `npm run build` correcto.
- [x] `docker compose config --quiet` sin errores.
- [x] Suites completas de integración RabbitMQ contra el clúster real.
- [x] DLQ end-to-end con broker real y cabecera `x-death`.
- [x] Login OIDC local contra Keycloak validado (PKCE, claims, 401/200, CORS).
- [ ] Login OIDC productivo contra Entra ID (sin tenant/Client ID).

### Infraestructura
- [x] `docker-compose.yml` declara 2 nodos RabbitMQ, PostgreSQL `55432`, ElasticMQ `9324` y Keycloak `8180`.
- [x] `infra/rabbitmq/join-cluster.sh` idempotente.
- [x] Scripts `rabbitmq-up.ps1` / `rabbitmq-status.ps1` actualizados.
- [x] Clúster validado en vivo (`cluster_status` con ambos nodos Running).
- [x] No se ejecutó `docker compose down -v` ni se borraron colas/volúmenes.

### Documentación
- [x] `docs/ep3/06-evidencias-rubrica.md` actualizado con la matriz ponderada.
- [x] `docs/ep3/08-checklist-entrega.md` (este documento) actualizado.
- [x] `docs/ep3/07-guion-presentacion.md` y `09-informe-final.md` alineados con el estado real.
- [x] Límites declarados sin ocultar pendientes.

### Seguridad
- [x] Sin credenciales reales: solo placeholders de desarrollo/test.
- [x] `.gitignore` cubre `.env`, claves, `target/`, `node_modules/`, `dist/`.
- [x] Perfil `aws` exige `${DB_PASSWORD:}` sin default.

### Git
- [x] Rama `main`, commit base `4175b1c`.
- [ ] Commit de esta actualización (a decisión del usuario).
- [ ] Push / merge (requiere autorización explícita).

---

## D. Comandos de verificación rápida

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.8'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"

# Git
git status
git log --oneline -8

# Clúster y validación en vivo
.\scripts\rabbitmq-up.ps1
.\scripts\rabbitmq-status.ps1
docker exec pedidos360-rabbitmq rabbitmqctl -q cluster_status
docker exec pedidos360-rabbitmq rabbitmqctl -q list_queues name messages consumers

# BFF (22) y Admin (28)
cd backend\bff-service; .\mvnw.cmd -B test
cd ..\rabbitmq-admin-service; .\mvnw.cmd -B test

# Suites completas con broker (Pedidos 69, Productos 69)
cd ..\pedidos-service; .\mvnw.cmd -B test
cd ..\productos-service; .\mvnw.cmd -B test

# Consumidores + ACK/NACK (subconjunto 8 + 8)
cd ..\pedidos-service
.\mvnw.cmd -B "-Dtest=PedidosColaConsumerTests,PedidoEventConsumerTests,ManualAckRabbitHandlerTests" test
cd ..\productos-service
.\mvnw.cmd -B "-Dtest=ProductosColaConsumerTests,ProductoEventConsumerTests,ManualAckRabbitHandlerTests" test

# Frontend
cd ..\..\frontend\pedidos360-web; npm test; npm run build

# Compose y Keycloak
cd ..\..; docker compose config --quiet
docker compose up -d keycloak
```

---

## E. Bloqueadores / pendientes declarados

| # | Pendiente | Impacto | Acción sugerida |
|---|---|---|---|
| 1 | OIDC productivo con Entra ID no probado | Medio | Configurar tenant/Client ID y probar login; Keycloak local ya validado |
| 2 | `environment.prod.ts` con placeholders | Medio | Reemplazar `REPLACE_WITH_*` antes del despliegue |
| 3 | Commit/push de esta actualización | Alto (entrega) | Requiere autorización explícita del usuario |

**No quedan pendientes de mensajería: clúster, integración RabbitMQ, ACK/NACK y DLQ E2E están verificados. El login local con Keycloak también quedó verificado; el pendiente restante es el login productivo con Entra ID y la entrega (commit/push).**
