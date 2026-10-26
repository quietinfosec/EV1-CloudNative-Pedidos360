# Pedidos360 - Arquitectura

## Diagrama general

```text
Proveedor OIDC
(Keycloak local / Microsoft Entra ID en AWS)
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
| Frontend              | Angular + oidc-client-ts            | UI, login PKCE, obtencion de JWT         |
| Identidad local       | Keycloak 26.2 (:8180)               | Emite JWT en desarrollo                  |
| Identidad produccion  | Microsoft Entra ID (Azure AD)       | Emite JWT, valida identidad              |
| API Gateway           | AWS API Gateway v2 (HTTP API)       | Punto de entrada publico, CORS, routing  |
| VPC Link              | AWS API Gateway VPC Link            | Tunel privado hacia la VPC               |
| Load Balancer         | AWS Internal ALB                    | Enrutamiento privado hacia el BFF        |
| BFF                   | Spring Boot (bff-service, :8080)    | Valida JWT, orquesta microservicios      |
| Pedidos Service       | Spring Boot (pedidos-service, :8081)| Logica de pedidos, Spring Data JPA       |
| Productos Service     | Spring Boot (productos-service, :8082) | Logica de productos, Spring Data JPA  |
| RabbitMQ Admin        | Spring Boot (rabbitmq-admin-service, :8083) | Observabilidad y CRUD del broker   |
| Base de datos         | Amazon RDS PostgreSQL (:5432)       | Persistencia de pedidos y productos      |
| Hosting frontend      | Amazon S3 + CloudFront              | Sirve el build de Angular con HTTPS      |

## Flujo de autenticacion

```
1. Usuario abre Angular en el navegador
2. Angular (oidc-client-ts) redirige al proveedor OIDC:
   - Local: http://localhost:8180/realms/pedidos360 (Keycloak)
   - Produccion: Microsoft Entra ID
3. Usuario inicia sesion (Authorization Code + PKCE)
4. El proveedor emite ID Token y Access Token JWT
5. oidc-client-ts completa el callback /auth/callback y guarda la sesion
6. El interceptor adjunta el JWT al API Gateway (Authorization: Bearer)
7. API Gateway reenvia la peticion al BFF
8. BFF valida el JWT:
   - firma contra el JWKS del issuer
   - issuer (Keycloak local / Entra ID)
   - audience (pedidos360-api / api://client-id)
   - expiracion
   - claim roles
9. Si el JWT es valido, el BFF invoca a pedidos-service y/o productos-service
10. Los microservicios consultan AWS RDS y devuelven la respuesta
11. El BFF consolida la respuesta y la retorna al frontend
```

## Flujo de una peticion

```
Usuario
  -> Angular
  -> Proveedor OIDC (Keycloak local / Entra ID) (obtiene JWT)
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
| bff-service             | 8080   |
| pedidos-service         | 8081   |
| productos-service       | 8082   |
| rabbitmq-admin-service  | 8083   |
| keycloak                | 8180   |
| postgres (compose)      | 55432  |
| elasticmq               | 9324   |

## Endpoints

Cada microservicio expone:

- `GET /api/health` -> `{ "service": "<nombre>", "status": "UP" }`
- `GET /actuator/health` -> estado de Spring Boot Actuator

## Variables de entorno

Ver `.env.example` en la raiz del proyecto para la lista completa.
Ninguna credencial real se almacena en el repositorio.
