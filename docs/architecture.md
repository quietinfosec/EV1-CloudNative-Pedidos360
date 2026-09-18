# Pedidos360 - Arquitectura

## Diagrama general

```text
Microsoft Entra ID (Azure AD)
        │
        │  JWT (Access Token)
        ▼
Angular (frontend/pedidos360-web)
        │
        ▼
AWS API Gateway (pedidos360-api)
        │
        │  VPC Link (pedidos360-vpc-link)
        ▼
Internal ALB (pedidos360-internal-alb)
        │
        ▼  TCP 8080
BFF Spring Boot (backend/bff-service, EC2)
        │  validacion JWT (issuer, audience, expiracion)
        │
        ├─────────────────────────┐
        ▼                         ▼
  Pedidos Service           Productos Service
  (127.0.0.1:8081)          (127.0.0.1:8082)
        │                         │
        └────────────┬────────────┘
                     │  TCP 5432
                     ▼
           Amazon RDS PostgreSQL
```

## Frontend en produccion

```
Angular (build estatico)
        |
        v
Amazon S3 (bucket de hosting)
        |
        v
CloudFront (CDN + HTTPS)
```

## Componentes

| Componente            | Tecnologia                          | Responsabilidad                          |
|-----------------------|-------------------------------------|------------------------------------------|
| Frontend              | Angular + MSAL Angular              | UI, login, obtencion de JWT              |
| Identidad (IdaaS)     | Microsoft Entra ID (Azure AD)       | Emite JWT, valida identidad              |
| API Gateway           | AWS API Gateway v2 (HTTP API)       | Punto de entrada publico, CORS, routing  |
| VPC Link              | AWS API Gateway VPC Link            | Tunel privado hacia la VPC               |
| Load Balancer         | AWS Internal ALB                    | Enrutamiento privado hacia el BFF        |
| BFF                   | Spring Boot (bff-service, :8080)    | Valida JWT, orquesta microservicios      |
| Pedidos Service       | Spring Boot (pedidos-service, :8081)| Logica de pedidos, Spring Data JPA       |
| Productos Service     | Spring Boot (productos-service, :8082) | Logica de productos, Spring Data JPA  |
| Base de datos         | Amazon RDS PostgreSQL (:5432)       | Persistencia de pedidos y productos      |
| Hosting frontend      | Amazon S3 + CloudFront              | Sirve el build de Angular con HTTPS      |

## Flujo de autenticacion

```
1. Usuario abre Angular en el navegador
2. Angular (MSAL) redirige a Microsoft Entra ID
3. Usuario inicia sesion en Microsoft Entra ID
4. Entra ID emite un Access Token JWT
5. MSAL almacena el JWT en el navegador
6. Angular llama al AWS API Gateway adjuntando el JWT (Authorization: Bearer)
7. API Gateway reenvia la peticion al BFF
8. BFF valida el JWT:
   - issuer (Entra ID)
   - audience (api://client-id)
   - expiracion
   - claims/roles
9. Si el JWT es valido, el BFF invoca a pedidos-service y/o productos-service
10. Los microservicios consultan AWS RDS y devuelven la respuesta
11. El BFF consolida la respuesta y la retorna al frontend
```

## Flujo de una peticion

```
Usuario
  -> Angular
  -> Microsoft Entra ID (obtiene JWT)
  -> Access Token JWT
  -> AWS API Gateway
  -> BFF (valida JWT)
  -> pedidos-service / productos-service
  -> AWS RDS
  -> respuesta de vuelta al Usuario
```

## Perfiles Spring

| Perfil  | Uso                        | Configuracion                         |
|---------|----------------------------|---------------------------------------|
| local   | Desarrollo local           | application-local.properties           |
| aws     | Despliegue en AWS (EC2)    | application-aws.properties             |

Perfil activo por defecto:

```
spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}
```

## Puertos locales

| Servicio           | Puerto |
|--------------------|--------|
| bff-service        | 8080   |
| pedidos-service    | 8081   |
| productos-service  | 8082   |

## Endpoints

Cada microservicio expone:

- `GET /api/health` -> `{ "service": "<nombre>", "status": "UP" }`
- `GET /actuator/health` -> estado de Spring Boot Actuator

## Variables de entorno

Ver `.env.example` en la raiz del proyecto para la lista completa.
Ninguna credencial real se almacena en el repositorio.
