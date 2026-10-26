# EP3 — Informe Final de Preparación para Entrega

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Proyecto:** Pedidos360
- **Rama:** `main` · **Commit base:** `4175b1c`
- **Fecha de actualización:** 2026-10-25
- **Autor:** Responsable de calidad y documentación final

---

## 1. Propósito

Dejar el proyecto **listo para entrega y defensa técnica** de la EP3 sin violar las reglas de la evaluación (sin `push`, sin despliegue AWS, sin eliminar recursos, sin ocultar fallos, sin inventar resultados). Este informe resume qué se verificó, qué se generó, qué queda pendiente y los comandos de publicación —**que no se ejecutan**.

---

## 2. Acciones realizadas en esta preparación

| # | Acción | Resultado |
|---|---|---|
| 1 | Verificación de estado Git | Rama `main`, commit base `4175b1c`; cambios de esta actualización en árbol local |
| 2 | Revisión de documentación EP3 | `docs/ep3/01`–`09` presentes; `06`, `07`, `08` y `09` actualizados al estado real |
| 3 | Revisión/actualización de `README.md` | Angular 22 + oidc-client-ts, Keycloak local, RabbitMQ Admin, ElasticMQ, PostgreSQL 55432 |
| 4 | Revisión de secretos | Solo placeholders de desarrollo; `.env.example` limpio; usuario demo documentado |
| 5 | Revisión de `docker-compose.yml` | Clúster RabbitMQ 2 nodos + PostgreSQL 55432 + ElasticMQ 9324 + Keycloak 8180 |
| 6 | Prueba de login local | Authorization Code + PKCE contra Keycloak; tokens y claims verificados |
| 7 | Matriz definitiva de 8 criterios EP3 | En `docs/ep3/08-checklist-entrega.md` §B |
| 8 | Guion de presentación (10 diapositivas) | `docs/ep3/07-guion-presentacion.md` |
| 9 | Checklist de entrega | `docs/ep3/08-checklist-entrega.md` |
| 10 | Informe de limitaciones | §4–§5 de este documento |
| 11 | Comandos de commit/push (NO ejecutados) | §7 |

### Cambios de contenido principales
- README: proveedor de identidad local Keycloak + Entra ID en producción; diagrama, secciones 5/8/10/11/12 y conteos actualizados.
- `docs/authentication.md`: reescrito para OIDC con Authorization Code + PKCE, configuración local/producción y matriz HTTP.
- `docs/testing.md`: conteos 22/28/69/69/9, total 188 backend, smoke OIDC local y comando de Keycloak.
- Frontend migrado de MSAL a `oidc-client-ts@3.5.0` con `authGuard`, interceptor, callback `/auth/callback`, perfil y navbar.
- BFF/Admin: validación de issuer/audience, CORS y validación estricta de DTOs.

---

## 3. Estado verificado (evidencia real, 2026-10-25)

- **Clúster RabbitMQ:** nodos `pedidos360-rabbitmq` (5672/15672) y `pedidos360-rabbitmq2` (5673/15673), cookie compartida y `rabbitmq2-join` idempotente. **Validado en vivo:** ambos nodos Running en `cluster_status`, sin alarmas ni particiones (RabbitMQ 4.2.9 / Erlang 27.3.4.19).
- **Topología RabbitMQ:** exchanges `pedidos360.direct.exchange` y `pedidos360.topic.exchange` (durables), `pedidos360.dlx`; colas `pedidos360.{pedidos,productos,auditoria}.cola` + sus `.dlq`; bindings topic `pedidos.pedido.#`, `productos.producto.#`, `#`.
- **Pruebas ejecutadas:** BFF **22/22**, Admin **28/28**, Pedidos **69/69** (59 unitarios + 10 de integración), Productos **69/69** (58 unitarios + 11 de integración), Frontend **9/9** (Vitest), DLQ E2E con `x-death` y build Angular OK (`432.56 kB`). Backend total **188**; global **197**.
- **OIDC local:** Authorization Code + PKCE contra Keycloak emitió `id_token`, `access_token` y `refresh_token`; claims `iss=http://localhost:8180/realms/pedidos360`, `aud=pedidos360-api`, `azp=pedidos360-web`, `roles=ADMIN,USER`.
- **Seguridad HTTP:** BFF `GET /api/pedidos` sin token → **401** y con token → **200**; Admin `GET /admin/queues` sin token → **401** y con token → **200**; `POST /admin/queues` con `amq.test` → **400**; preflight CORS desde `:4200` → **200**.

---

## 4. Limitaciones y pendientes declarados (honestidad técnica)

| # | Limitación | Qué SÍ está probado | Riesgo / impacto | Mitigación sugerida |
|---|---|---|---|---|
| L1 | **OIDC productivo con Entra ID no probado end-to-end** | OIDC local completo con Keycloak (PKCE, tokens, claims, 401/200, CORS); validación de issuer/audience con `JwtDecoder` de test | Medio: el tenant real puede revelar diferencias de claims/audiencia | Configurar `AZURE_TENANT_ID`/`AZURE_CLIENT_ID` y ejecutar login real antes de la demo |
| L2 | **Frontend de producción con placeholders** (`REPLACE_WITH_*`) | `environment.ts` local apunta a Keycloak y funciona; build de producción correcto | Medio: el login productivo no funciona sin IDs reales | Reemplazar en `environment.prod.ts` |
| L3 | **Mensajes de pruebas previas en colas legacy** | Evidencia válida, no un error | Bajo | **NO limpiar**: borrar colas/mensajes está prohibido por las reglas |
| L4 | **Push / merge no ejecutados** | Cambios en árbol local sobre `main` | Alto (entrega) | Requiere autorización explícita (§7) |

> **Declaración explícita:** la EP3 **no** declara probado el login productivo contra Microsoft Entra ID. La autenticación **sí** se validó de extremo a extremo en local contra Keycloak.

---

## 5. Trazabilidad de commits (fases previas, sin push)

| Commit | Fase / contenido |
|---|---|
| `89d80ed` | Respaldo de las fases 4.1–4.6 |
| `2497abe` | Routing keys de topic exchange |
| `c9b5ae2` | Propagación de routing key en adaptador SQS |
| `a4b3fbc` | NACK → DLQ (rechazo sin requeue) |
| `388bcc9` | Pruebas JWT extremo a extremo |
| `94a54ab` | Persistencia PostgreSQL real |
| `9d0ff1e` | Documentación 04–06 + reconciliación de 01–03 |

Los cambios de OIDC local, CORS, migración a `oidc-client-ts` y actualización documental están **sin commit** en el árbol local.

---

## 6. Prueba manual de frontend y login local

**Arranque:**
```powershell
# Infraestructura
docker compose up -d keycloak postgres elasticmq
.\scripts\rabbitmq-up.ps1

# Backend (JAVA_HOME jdk-21.0.8), perfiles local
$env:SPRING_PROFILES_ACTIVE='local'; $env:DB_PORT='55432'
# 8080 BFF, 8081 pedidos, 8082 productos, 8083 admin

# Frontend
cd frontend\pedidos360-web
npm ci
npm start          # http://localhost:4200
```

**Rutas verificadas:** `''` → `home`; `home` (pública); `login` (redirección al emisor OIDC); `auth/callback` (completa PKCE); `productos` (pública); `pedidos` y `perfil` (`authGuard`); `**` → `home`.

**Flujo probado:**
1. La SPA carga el layout con navbar y home. ✅
2. `/productos` consulta `GET {apiBaseUrl}/productos` (`http://localhost:8080/api`). ✅
3. Login con Keycloak (`demo` / `Demo1234!`) completa PKCE y devuelve a la SPA. ✅
4. `/pedidos` con sesión activa envía `Authorization: Bearer <JWT>` y recibe **200**. ✅
5. Sin token, BFF/Admin responden **401**; con rol insuficiente, **403**. ✅

**Conclusión:** la SPA compila, rutea, autentica contra Keycloak local y consume la API del BFF. El login productivo contra Entra ID queda pendiente de credenciales reales.

---

## 7. Comandos de publicación (NO ejecutar sin autorización)

> Estos comandos están listos para cuando el usuario autorice. **No se han ejecutado.**

```powershell
# 0. Verificación previa
git status
git log --oneline -8
git branch -vv

# 1. Commit de la actualización (código + docs + infra)
git add backend docker-compose.yml infra frontend docs README.md
git commit -m "feat(auth): OIDC local con Keycloak, CORS y actualizacion de evidencias EP3"

# 2. Publicar sobre main (requiere autorización)
git push origin main

# 3. (A decisión del usuario) Abrir PR si se trabaja en una rama feature.
```

**Remote de referencia:** `origin` = `https://github.com/quietinfosec/EV1-CloudNative-Pedidos360.git`

---

## 8. Archivos generados / modificados en esta preparación

| Archivo | Tipo | Descripción |
|---|---|---|
| `infra/keycloak/realm-pedidos360.json` | Nuevo | Realm, clientes, usuario demo, roles y mappers |
| `docs/authentication.md` | Reescrito | OIDC local/producción, PKCE, matriz HTTP y evidencia |
| `docs/testing.md` | Actualizado | Conteos, smoke OIDC y comando Keycloak |
| `docs/ep3/06-evidencias-rubrica.md` | Actualizado | Matriz de 8 criterios, conteos y límites |
| `docs/ep3/07-guion-presentacion.md` | Actualizado | Guion alineado con OIDC local y conteos actuales |
| `docs/ep3/08-checklist-entrega.md` | Actualizado | Checklist y matriz de criterios |
| `docs/ep3/09-informe-final.md` | Actualizado | Este informe |
| `backend/bff-service` | Modificado | CORS, issuer/audience y tests |
| `backend/rabbitmq-admin-service` | Modificado | Seguridad JWT, validación de DTOs y tests |
| `backend/pedidos-service`, `backend/productos-service` | Modificado | ACK/NACK manual, consumidores y tests |
| `frontend/pedidos360-web` | Modificado | OIDC con `oidc-client-ts`, guard, interceptor, callback y tests |
| `docker-compose.yml`, `infra/rabbitmq/`, `scripts/` | Modificado | Clúster RabbitMQ, Keycloak y utilidades locales |

---

## 9. Recomendación final

El proyecto está **apto para entrega y defensa**. Las 8 áreas de la rúbrica EP3 están implementadas; admin, consumidores, ACK/NACK y DLQ tienen pruebas unitarias, de integración y E2E en verde contra el clúster RabbitMQ real. La autenticación OIDC está validada localmente con Keycloak. Queda una sola verificación de entorno: **login productivo con Entra ID** (sin tenant/Client ID). Se recomienda configurar las credenciales reales antes de una presentación en producción; el guion de `07-guion-presentacion.md` explicita ese límite.
