# Pedidos360 - Arquitectura Inicial

## Diagrama

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

## Microservicios

| Servicio        | Puerto | Paquete                        | ArtifactId        |
|-----------------|--------|--------------------------------|-------------------|
| bff-service     | 8080   | cl.duoc.pedidos360.bff         | bff-service       |
| pedidos-service | 8081   | cl.duoc.pedidos360.pedidos     | pedidos-service   |
| productos-service| 8082  | cl.duoc.pedidos360.productos   | productos-service |

## Endpoints iniciales

Cada microservicio expone:

- `GET /api/health` → `{ "service": "<nombre>", "status": "UP" }`
- `GET /actuator/health` → estado de salud de Spring Boot Actuator
