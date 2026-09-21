# AWS API Gateway & Networking — Pedidos360

Este documento describe la integración de **AWS API Gateway (HTTP API v2)** como fachada pública gestionada para la plataforma **Pedidos360** en `us-east-1`.

---

## 1. Topología de Integración

```text
               Internet (Navegador / App / Postman)
                                │
                                ▼
               AWS API Gateway (pedidos360-api)
                                │
                                │  VPC Link Privado
                                ▼
               Internal ALB (pedidos360-internal-alb)
                                │
                                ▼  TCP 8080
               BFF Spring Boot (:8080 en EC2)
```

- **Protocolo**: HTTP API v2 (ligero, baja latencia, soporte nativo de CORS y VPC Link).
- **Nombre**: `pedidos360-api`
- **Stage**: `$default` con Auto-Deploy habilitado.

---

## 2. Enrutamiento y Proxies

| Método | Ruta API Gateway | Destino BFF | Descripción |
|---|---|---|---|
| `GET` | `/api/health` | `/api/health` | Verificación de salud y estado del BFF |
| `ANY` | `/api/{proxy+}` | `/api/{proxy}` | Enrutamiento dinámico y transparente para `/api/productos` y `/api/pedidos` |

---

## 3. Política de CORS (Cross-Origin Resource Sharing)

- **Allowed Origins**: `http://localhost:4200` (desarrollo) y dominio CloudFront (producción).
- **Allowed Methods**: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`
- **Allowed Headers**: `authorization`, `content-type`, `x-requested-with`
- **Preflight (OPTIONS)**: Gestionado por API Gateway con respuesta `204 No Content`.
