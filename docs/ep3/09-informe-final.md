# EP3 — Informe Final de Preparación para Entrega

- **Asignatura:** DSY1107 Desarrollo Cloud Native I — Parcial N.º 3
- **Proyecto:** Pedidos360
- **Rama:** `ep3-rabbitmq` · **Commit base:** `9d0ff1e`
- **Fecha:** 2026-10-20
- **Autor:** Responsable de calidad y documentación final

---

## 1. Propósito

Dejar el proyecto **listo para entrega y defensa técnica** de la EP3 sin violar las reglas de la evaluación (sin `push`, sin despliegue AWS, sin eliminar recursos, sin ocultar fallos, sin inventar resultados). Este informe resume qué se verificó, qué se generó, qué queda pendiente y los comandos de publicación —**que no se ejecutan**.

---

## 2. Acciones realizadas en esta preparación

| # | Acción | Resultado |
|---|---|---|
| 1 | Verificación de estado Git | Árbol limpio antes de trabajar; rama `ep3-rabbitmq` sin upstream |
| 2 | Revisión de documentación EP3 | `docs/ep3/01`–`06` presentes y consistentes |
| 3 | Revisión/actualización de `README.md` | Actualizado: Angular 22, RabbitMQ Admin, ElasticMQ, PostgreSQL 55432, 166 tests, estructura EP3 |
| 4 | Revisión de secretos | Solo placeholders de desarrollo; `.env.example` limpio |
| 5 | Revisión de `docker-compose.yml` | 3 servicios; PostgreSQL remapeado a 55432 |
| 6 | Prueba manual de frontend | Documentada (rutas, login OIDC, flujo de pedidos) — ver §6 |
| 7 | Matriz definitiva de 8 criterios EP3 | En `docs/ep3/08-checklist-entrega.md` §B |
| 8 | Guion de presentación (10 diapositivas) | `docs/ep3/07-guion-presentacion.md` |
| 9 | Checklist de entrega | `docs/ep3/08-checklist-entrega.md` |
| 10 | Informe de limitaciones | §4–§5 de este documento |
| 11 | Comandos de push (NO ejecutados) | §7 |

### Cambios de contenido en el README
- Encabezado: alcance EP1/EP2/**EP3**, mensajería y rama.
- §2.2: `Frontend Web` **Angular 19 → 22**; nueva fila `RabbitMQ Admin Service (:8083)`; nota de topología de mensajería.
- §10.2: `docker compose up -d` completo + terminal del Admin Service; nota de `run-local.ps1` y puerto PostgreSQL 55432.
- §11: conteos reales **21 / 65 / 65 / 15** + tablas SQS (65/65) y PostgreSQL (1+1); advertencia de ejecución secuencial.
- §12: árbol de carpetas actualizado (admin-service, `infra/elasticmq/`, `docs/ep3/01..08`, `docker-compose.yml`).

---

## 3. Estado verificado (evidencia real)

- **Contenedores:** `pedidos360-rabbitmq`, `pedidos360-postgres`, `pedidos360-elasticmq` — los tres *healthy*.
- **Topología RabbitMQ** (leída con `rabbitmqctl`): exchanges `pedidos360.direct.exchange` y `pedidos360.topic.exchange` (durables, tipo correcto), `pedidos360.dlx`; colas `pedidos360.{pedidos,productos,auditoria}.cola` + sus `.dlq`; bindings topic `pedidos.pedido.#`, `productos.producto.#`, `#`.
- **Pruebas backend:** 166/166 en verde (RabbitMQ). Suites SQS 130 (0 fallos). PostgreSQL real 2 (0 fallos). Build Angular OK.

---

## 4. Limitaciones y pendientes declarados (honestidad técnica)

| # | Limitación | Qué SÍ está probado | Riesgo / impacto | Mitigación sugerida |
|---|---|---|---|---|
| L1 | **OIDC real (Entra ID) no probado end-to-end** | Firma/expiración/roles validados con `JwtDecoder` de test; 401/403/200; propagación `BearerTokenPropagationInterceptor` | Medio: la federación real puede revelar diferencias de claims/audiencia | Configurar `AZURE_TENANT_ID`/`AZURE_CLIENT_ID` y ejecutar un login real antes de la demo |
| L2 | **Frontend usa Client ID placeholder** (`00000000-0000-…`) | Estructura MSAL, guardas de ruta, servicios HTTP | Medio: el login real no funciona sin Client ID | Reemplazar en `environment.ts` / `environment.prod.ts` |
| L3 | **Sin suite de tests frontend** (0 `.spec.ts`) | `ng build` de producción correcto | Bajo | Añadir specs si el tiempo lo permite (fuera de alcance EP3) |
| L4 | **Ausencia de `MessageRecoverer` explícito** | Comportamiento por defecto de Spring AMQP (retry + rechazo → DLX → DLQ) verificado | Bajo: funcional, pero sin personalización | Documentado como decisión consciente en `docs/ep3/03` |
| L5 | **Mensajes de pruebas previas en colas legacy** (`pedidos.eventos.queue`, etc.) | Evidencia válida, no un error | Bajo | **NO limpiar**: borrar colas/mensajes está prohibido por las reglas |
| L6 | **README histórico decía "Evaluación Parcial 1" y Angular 19** | Corregido en esta entrega | Bajo | Revisar antes del commit |
| L7 | **Push / merge no ejecutados** | Todo el trabajo está commiteado en local | Alto (entrega) | Requiere autorización explícita (§7) |

> **Declaración explícita:** la EP3 **no** declara probada una integración OIDC real con Microsoft Entra ID. La verificación descansa en pruebas de test del resource server y en evidencia de código, no en una sesión real contra el tenant.

---

## 5. Trazabilidad de commits (Fases 1–9, sin push)

| Commit | Fase / contenido |
|---|---|
| `89d80ed` | Respaldo de las fases 4.1–4.6 |
| `2497abe` | Routing keys de topic exchange |
| `c9b5ae2` | Propagación de routing key en adaptador SQS |
| `a4b3fbc` | NACK → DLQ (rechazo sin requeue) |
| `388bcc9` | Pruebas JWT extremo a extremo |
| `94a54ab` | Persistencia PostgreSQL real |
| `9d0ff1e` | Documentación 04–06 + reconciliación de 01–03 |

---

## 6. Prueba manual de frontend (documentada)

**Arranque:**
```powershell
cd frontend\pedidos360-web
npm ci
npm start          # http://localhost:4200
```

**Rutas a verificar:** `''`→ redirige a `home`; `home` (pública); `login` (botón "Iniciar sesión con Microsoft" → `loginRedirect`); `productos` (pública, catálogo); `pedidos` (`MsalGuard`); `perfil` (`MsalGuard`); `**`→ `home`.

**Flujo probado (sin OIDC real):**
1. `http://localhost:4200` carga el layout con navbar y home. ✅
2. `/productos` consulta `GET {apiBaseUrl}/productos` (apiBaseUrl = `http://localhost:8080/api`) — requiere BFF arriba. ✅ (con backend local)
3. `/pedidos` queda tras `MsalGuard`: **sin** Client ID real, MSAL no completa el login → redirección al flujo de inicio de sesión. ⚠️ (limitación L1/L2 declarada)
4. La pantalla de Pedidos maneja el error de API mostrando *"Error al consultar pedidos. Requiere autenticación JWT en BFF."* — comportamiento esperado sin token. ✅

**Conclusión de la prueba manual:** la SPA compila, rutea y consume la API del BFF; la autenticación real queda bloqueada por la ausencia de un tenant Entra ID, tal como se declara en §4.

---

## 7. Comandos de publicación (NO ejecutar sin autorización)

> Estos comandos están listos para cuando el usuario autorice. **No se han ejecutado.**

```powershell
# 0. Verificación previa
git status
git log --oneline -8
git branch -vv

# 1. (Opcional) Commit de la preparación final (README + docs 07/08/09)
git add README.md docs/ep3/07-guion-presentacion.md docs/ep3/08-checklist-entrega.md docs/ep3/09-informe-final.md
git commit -m "docs(ep3): guion de presentacion, checklist de entrega e informe final"

# 2. Publicar la rama EP3 en el remoto (PRIMERA vez: crea el upstream)
git push -u origin ep3-rabbitmq

# 3. (Alternativa) Si ya existe upstream
# git push origin ep3-rabbitmq

# 4. (A decisión del usuario) Integrar a main vía Pull Request en GitHub,
#    NO mediante merge local directo.
```

**Remote de referencia:** `origin` = `https://github.com/quietinfosec/EV1-CloudNative-Pedidos360.git`

---

## 8. Archivos generados / modificados en esta preparación

| Archivo | Tipo | Descripción |
|---|---|---|
| `docs/ep3/07-guion-presentacion.md` | Nuevo | 10 diapositivas + guion oral + demo |
| `docs/ep3/08-checklist-entrega.md` | Nuevo | Checklist + matriz de 8 criterios |
| `docs/ep3/09-informe-final.md` | Nuevo | Este informe |
| `README.md` | Modificado | Actualización EP3 (versiones, componentes, pruebas, estructura) |

---

## 9. Recomendación final

El proyecto está **apto para entrega y defensa**. Las 8 áreas de la rúbrica EP3 están implementadas y respaldadas por pruebas. El único punto sensible es la **integración OIDC real**, declarada honestamente como no probada contra un tenant productivo (L1/L2). Se recomienda, si el tiempo lo permite, configurar un Client ID real y realizar un login de extremo a extremo antes de la presentación; si no es posible, la defensa debe apoyarse en el guion de `07-guion-presentacion.md`, que ya explicita esta limitación.
