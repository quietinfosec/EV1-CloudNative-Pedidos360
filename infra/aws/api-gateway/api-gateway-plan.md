# Plan de Integración AWS API Gateway — Pedidos360

Este documento describe la planificación e inspección técnica para la exposición del backend de **Pedidos360** mediante **Amazon API Gateway (HTTP API v2)** en la región `us-east-1`.

> **Estado**: Fase de Inspección y Planificación.  
> **Regla estricta**: En esta etapa **NO** se crean recursos en AWS ni se modifican los Security Groups existentes.

---

## 1. Contexto de Arquitectura

El backend de Pedidos360 opera actualmente en una instancia Amazon EC2 (`us-east-1`):

```text
               Internet
                  │
                  ▼
          AWS API Gateway (pedidos360-api)
                  │
                  ▼
         BFF Spring Boot (:8080)
        ┌─────────┴─────────┐
        ▼                   ▼
  Pedidos Service     Productos Service
  (127.0.0.1:8081)    (127.0.0.1:8082)
        │                   │
        └─────────┬─────────┘
                  ▼
        Amazon RDS PostgreSQL (:5432)
```

- **BFF (Backend For Frontend)** en puerto `8080` es el único punto de entrada de aplicación hacia los microservicios internos (`pedidos-service:8081` y `productos-service:8082`).
- **AWS API Gateway** será la fachada pública gestionada que recibirá las peticiones de los clientes (Angular / Postman), manejará CORS y las redirigirá al BFF.

---

## 2. Comparación de Opciones de Integración

### Opción A: API Gateway HTTP API + VPC Link + Internal ALB/NLB (Diseño Recomendado Empresarial)

```text
Internet ──> HTTP API Gateway ──> VPC Link ──> Internal ALB/NLB ──> EC2 BFF (:8080)
```

- **Mecanismo**: Integración privada (`$default` / `VPC_LINK`).
- **Recursos necesarios**:
  1. Internal Application Load Balancer o Network Load Balancer en subredes privadas/públicas de `vpc-091400dfac7e47849`.
  2. Target Group apuntando a la instancia EC2 en el puerto `8080`.
  3. API Gateway v2 VPC Link (`apigatewayv2 create-vpc-link`) asociado a subredes y Security Groups.
  4. Security Group para el ALB permitiendo tráfico inbound desde el VPC Link.
  5. Regla en `pedidos360-backend-sg` permitiendo `8080` exclusivamente desde el Security Group del ALB.
- **Ventajas**:
  - Máximo aislamiento: La instancia EC2 no necesita exponer el puerto `8080` a Internet.
  - El BFF permanece estrictamente privado dentro de la VPC.
- **Limitaciones / Desafíos en Entorno Académico**:
  - Requiere permisos de creación de Load Balancer (`elasticloadbalancing:CreateLoadBalancer`) y VPC Link (`apigatewayv2:CreateVpcLink`).
  - Mayor consumo de recursos de laboratorio (ALB añade costo por hora y consumo de ENIs).

---

### Opción B: API Gateway HTTP API + HTTP Proxy Integration (Fallback de Laboratorio)

```text
Internet ──> HTTP API Gateway ──> HTTP Proxy Integration (URI pública) ──> EC2 BFF (:8080)
```

- **Mecanismo**: Integración de tipo `HTTP_PROXY` configurada directamente hacia la URL del BFF (`http://<EC2_PUBLIC_DNS_OR_IP>:8080`).
- **Recursos necesarios**:
  1. API Gateway HTTP API (`apigatewayv2 create-api`).
  2. Integración `HTTP_PROXY` con `integration-uri = http://<EC2_HOST>:8080/api/{proxy}`.
  3. Ruta `ANY /api/{proxy+}` y `$default stage` con auto-deploy.
- **Ventajas**:
  - Cero costos adicionales: No requiere balanceadores de carga ni VPC Links.
  - Compatible al 100% con cuentas académicas con permisos restringidos.
  - Despliegue simple y verificación rápida.
- **Limitaciones / Consideraciones de Seguridad**:
  - Requiere que el puerto `8080` de EC2 sea accesible por API Gateway.
  - **REGLA DE SEGURIDAD**: **NO** abrir `8080` a `0.0.0.0/0` de forma anticipada. Mantener la restricción actual `/32` hasta la autorización expresa en la etapa de despliegue de API Gateway.

---

## 3. Planificación de Rutas y Endpoints

### 3.1 Nombre de la API
- **Nombre**: `pedidos360-api`
- **Protocolo**: `HTTP` (API Gateway v2)

### 3.2 Tabla de Rutas
| Método | Ruta API Gateway | Destino BFF | Propósito |
|---|---|---|---|
| `GET` | `/api/health` | `http://<BFF_HOST>:8080/api/health` | Health check de conectividad básica |
| `ANY` | `/api/{proxy+}` | `http://<BFF_HOST>:8080/api/{proxy}` | Enrutamiento transparente de operaciones CRUD (Productos, Pedidos) |

---

## 4. Política de CORS (Cross-Origin Resource Sharing)

Para permitir que el frontend Angular (local en desarrollo y alojado en S3/CloudFront en producción) consuma la API sin bloqueos del navegador:

- **Orígenes permitidos (AllowedOrigins)**:
  - `http://localhost:4200` (desarrollo local de Angular)
  - Dominio de distribución CloudFront (cuando se aprovisione)
  - **NO** utilizar comodín abierto `*` en producción.
- **Métodos permitidos (AllowedMethods)**:
  - `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS`
- **Cabeceras permitidas (AllowedHeaders)**:
  - `authorization` (para el token Bearer JWT de Microsoft Entra ID)
  - `content-type`
  - `x-requested-with`
- **ExposeHeaders**:
  - `authorization`
- **MaxAge**: `300` segundos

---

## 5. Matriz de Decisión para la Cuenta Académica

| Criterio | Opción A (VPC Link + ALB) | Opción B (HTTP Proxy) |
|---|---|---|
| **Seguridad de red** | Excelente (100% privado) | Aceptable para laboratorio (filtrado por SG) |
| **Permisos requeridos** | Altos (ELB, VPC Link, Service Roles) | Mínimos (`apigatewayv2:*`) |
| **Complejidad de despliegue** | Alta (múltiples componentes) | Baja (directo y liviano) |
| **Costo en Lab** | Mayor (cargo por hora de ALB) | Gratuito dentro del Free Tier de API Gateway |
| **Viabilidad en LabRole** | Sujeto a cuotas y permisos | 100% viable |

### Decisión Recomendada
1. **Evaluar primero la viabilidad de la Opción A** consultando los permisos de `apigatewayv2:CreateVpcLink` y `elasticloadbalancing:CreateLoadBalancer`.
2. **Si la cuenta de laboratorio no permite crear ALBs o VPC Links**, aplicar de inmediato la **Opción B (HTTP Proxy)** como fallback documentado y seguro para el entorno de evaluación.

---

## 6. Procedimiento de Verificación de Solo Lectura (AWS CLI)

Comandos para evaluar el estado antes de la creación en la siguiente etapa:

```bash
# 1. Consultar APIs existentes en API Gateway v2
aws apigatewayv2 get-apis --region us-east-1

# 2. Consultar VPC Links existentes
aws apigatewayv2 get-vpc-links --region us-east-1

# 3. Consultar Load Balancers existentes
aws elbv2 describe-load-balancers --region us-east-1

# 4. Verificar subnets de la VPC
aws ec2 describe-subnets --region us-east-1 \
  --filters "Name=vpc-id,Values=vpc-091400dfac7e47849"
```
