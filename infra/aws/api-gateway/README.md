# AWS API Gateway (HTTP API) — Pedidos360

Este directorio documenta la arquitectura, enrutamiento, seguridad e integración de **Amazon API Gateway (HTTP API v2)** como punto único de entrada público para **Pedidos360** en `us-east-1`.

---

## 1. Arquitectura de Red y Flujo de Tráfico

La integración sigue el patrón de diseño privado y seguro:

```text
               Internet (Angular / Postman)
                         │
                         ▼
        Amazon API Gateway (pedidos360-api)
                         │
                         │  VPC Link (pedidos360-vpc-link)
                         ▼
        Internal ALB (pedidos360-internal-alb)
                         │
                         ▼  TCP 8080
        EC2 BFF Service (pedidos360-bff)
                         │
           ┌─────────────┴─────────────┐
           ▼                           ▼
  Pedidos Service (:8081)     Productos Service (:8082)
           │                           │
           └─────────────┬─────────────┘
                         │  TCP 5432
                         ▼
               Amazon RDS PostgreSQL
```

- **Aislamiento**: Los microservicios `pedidos-service` (`127.0.0.1:8081`) y `productos-service` (`127.0.0.1:8082`) escuchan exclusivamente en loopback dentro de EC2.
- **BFF (`0.0.0.0:8080`)**: Recibe peticiones exclusivamente desde el Internal ALB mediante `pedidos360-backend-sg`.
- **RDS (`:5432`)**: Privado dentro de la VPC, accesible únicamente desde `pedidos360-backend-sg`.

---

## 2. Componentes de Integración

| Recurso | Nombre Lógico | Configuración |
|---|---|---|
| **Security Group VPC Link** | `pedidos360-vpclink-sg` | Egress hacia la VPC, sin inbound público |
| **Security Group ALB** | `pedidos360-alb-sg` | Inbound TCP 80 desde `pedidos360-vpclink-sg` |
| **Target Group** | `pedidos360-bff-tg` | Protocolo HTTP :8080, Health Check `/api/health` (HTTP 200) |
| **Internal ALB** | `pedidos360-internal-alb` | Scheme `internal`, subredes multi-AZ en `vpc-091400dfac7e47849` |
| **Listener ALB** | Puerto 80 HTTP | Forward a `pedidos360-bff-tg` |
| **VPC Link** | `pedidos360-vpc-link` | Tipo `VPC_LINK` para HTTP API v2 |
| **HTTP API** | `pedidos360-api` | Protocolo HTTP, Stage `$default`, Auto-Deploy habilitado |

---

## 3. Enrutamiento y CORS

### 3.1 Rutas
- `GET /api/health` -> Health check del BFF
- `ANY /api/{proxy+}` -> Enrutamiento transparente a `/api/{proxy}` en el BFF (Productos y Pedidos)

### 3.2 Política CORS
- **Allowed Origins**: `http://localhost:4200`
- **Allowed Methods**: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`
- **Allowed Headers**: `authorization`, `content-type`
- **Preflight**: Gestionado automáticamente por API Gateway (retorna `204 No Content`)

---

## 4. Comandos de Diagnóstico y Verificación

```bash
# 1. Comprobar salud vía API Gateway
curl -i https://<API_ID>.execute-api.us-east-1.amazonaws.com/api/health

# 2. Listar productos vía API Gateway
curl -i https://<API_ID>.execute-api.us-east-1.amazonaws.com/api/productos

# 3. Listar pedidos vía API Gateway
curl -i https://<API_ID>.execute-api.us-east-1.amazonaws.com/api/pedidos

# 4. Probar CORS preflight (OPTIONS)
curl -i -X OPTIONS https://<API_ID>.execute-api.us-east-1.amazonaws.com/api/productos \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: authorization,content-type"
```
