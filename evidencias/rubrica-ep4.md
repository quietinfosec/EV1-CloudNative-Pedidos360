# Trazabilidad EP4 → Pedidos360

| Exigencia | Evidencia | Estado |
|---|---|---|
| API Manager / endpoints backend | `backend/bff-service`, `docs/architecture.md` | Implementado/documentado |
| JWT válido e inválido | `docs/ep3/06-evidencias-rubrica.md`, `JwtSecurityTests` | Validado localmente |
| Tenant y usuarios IDaaS | `infra/keycloak/realm-pedidos360.json` | Keycloak local validado |
| OAuth2/OIDC frontend | `frontend/pedidos360-web/src/app/auth`, `oidc.config.ts` | Validado con PKCE local |
| Backend/frontend integrados | `docker-compose.yml`, configuración Angular/BFF | Implementado |
| Clúster RabbitMQ 2 nodos | `scripts/rabbitmq-status.ps1`, `infra/rabbitmq/join-cluster.sh` | Validado según evidencia registrada |
| 3 colas + DLQ | `RabbitMQConfig.java` en pedidos/productos | Implementado y probado |
| Direct + Topic exchanges | `RabbitMQConfig.java` | Implementado |
| Docker Compose | `docker-compose.yml`, scripts RabbitMQ | Implementado |

## Rúbrica de mensajería
Los ocho indicadores tienen evidencia en `docs/ep3/06-evidencias-rubrica.md`: nombres centralizados, beans, separación, consumidores, ACK/NACK/DLQ, API REST, `RabbitAdminService` y validación DTO.
