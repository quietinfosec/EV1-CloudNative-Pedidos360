# Pedidos360

Proyecto para la asignatura **Desarrollo Cloud Native I (DSY1107)** - Duoc UC.

Evaluación Parcial 1: estructura inicial del proyecto.

## Arquitectura inicial

```
Angular (frontend/pedidos360-web)
        |
        v
AWS API Gateway  (etapa posterior)
        |
        v
BFF Spring Boot (backend/bff-service, puerto 8080)
        |
        +--> Pedidos Service (backend/pedidos-service, puerto 8081)
        |
        +--> Productos Service (backend/productos-service, puerto 8082)
                |
                v
        Base de datos cloud (etapa posterior)
```

## Estructura de carpetas

```
Pedidos360/
├── frontend/
│   └── pedidos360-web/        # Angular standalone components
├── backend/
│   ├── bff-service/           # Spring Boot (puerto 8080)
│   ├── pedidos-service/       # Spring Boot (puerto 8081)
│   └── productos-service/     # Spring Boot (puerto 8082)
└── docs/                      # Documentación del proyecto
```

## Tecnologías

**Frontend:**
- Angular (standalone components)
- TypeScript
- Angular Router
- HttpClient

**Backend:**
- Java 21
- Spring Boot 3.3.x
- Maven
- Spring Web
- Spring Boot Actuator
- Spring Validation

## Microservicios

| Servicio          | Puerto | Paquete                        | ArtifactId        |
|-------------------|--------|--------------------------------|-------------------|
| bff-service       | 8080   | cl.duoc.pedidos360.bff         | bff-service       |
| pedidos-service   | 8081   | cl.duoc.pedidos360.pedidos     | pedidos-service   |
| productos-service | 8082   | cl.duoc.pedidos360.productos   | productos-service |

groupId: `cl.duoc.pedidos360`

## Endpoints iniciales

Cada microservicio expone:

- `GET /api/health` → `{ "service": "<nombre>", "status": "UP" }`
- `GET /actuator/health` → estado de Spring Boot Actuator

## Cómo ejecutar

### Frontend

```bash
cd frontend/pedidos360-web
npm install
npm run build      # compilar
npm start          # servir en desarrollo (http://localhost:4200)
```

### Backend

Cada microservicio se compila y ejecuta de forma independiente:

```bash
cd backend/bff-service
./mvnw clean test          # compilar y probar
./mvnw spring-boot:run     # ejecutar
```

Repetir para `pedidos-service` (puerto 8081) y `productos-service` (puerto 8082).

## Próximas etapas

Posteriormente se implementará:

- Microsoft Entra ID (Azure AD) para autenticación
- MSAL Angular en el frontend
- JWT (emisión y validación)
- AWS API Gateway
- AWS EC2 para despliegue de microservicios
- Base de datos cloud

> Esta etapa **no** incluye autenticación, JWT, Azure, AWS, base de datos ni lógica de negocio avanzada.
