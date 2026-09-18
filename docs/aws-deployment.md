# Pedidos360 - Despliegue en AWS

> Esta documento describe las etapas FUTURAS de despliegue.
> En esta etapa NO se crean recursos reales en AWS.

## Arquitectura objetivo

```
Microsoft Entra ID (JWT)
        |
Angular -> S3 -> CloudFront
        |
AWS API Gateway
        |
BFF (EC2, :8080) -- valida JWT
        |
        +---> Pedidos Service (EC2, :8081)
        |
        +---> Productos Service (EC2, :8082)
                  |
                  v
              AWS RDS
```

## Etapas de despliegue

### 1. IAM

- Crear usuario/rol IAM para despliegue.
- Politicas de minimos privilegios para EC2, RDS, S3, CloudFront, API Gateway.
- Access keys solo para CI/CD, no para desarrollo local.

### 2. Security Groups

- SG para BFF: permite 443 desde API Gateway.
- SG para pedidos-service: permite 8081 desde BFF.
- SG para productos-service: permite 8082 desde BFF.
- SG para RDS: permite 5432 desde los microservicios.

### 3. EC2

- Lanzar 3 instancias EC2 (o 1 con los 3 servicios detras de un ALB).
- Amazon Linux 2023, JDK 21.
- UserData script que instala el JAR y configura `SPRING_PROFILES_ACTIVE=aws`.
- Variables de entorno inyectadas via Systems Manager Parameter Store.

### 4. RDS

- Instancia RDS PostgreSQL (db.t3.micro para desarrollo).
- Subnet group en subnets privadas.
- Backup automatizado habilitado.

### 5. API Gateway

- API HTTP (REST) que enruta al BFF en EC2.
- Integracion con el private IP/DNS del BFF.
- Throttling y quotas configuradas.

### 6. CORS

- Configurar CORS en API Gateway para permitir el origen de CloudFront.
- El BFF tambien debe permitir CORS para desarrollo local.

### 7. S3

- Bucket S3 para hosting del frontend (build de Angular).
- Bloqueo de acceso publico (CloudFront accede via Origin Access Identity).

### 8. CloudFront

- Distribucion CloudFront con origen S3.
- HTTPS via Certificate Manager (certificado para el dominio).
- Redireccion HTTP -> HTTPS.
- Cache invalidation al desplegar nueva version del frontend.

### 9. Logs CloudWatch

- Logs de aplicacion de cada microservicio a CloudWatch.
- Metricas y alarms para errores, latencia y disponibilidad.

### 10. Pruebas finales

- Verificar flujo de login con Microsoft Entra ID.
- Verificar que el JWT llega al BFF y es validado.
- Verificar respuestas de pedidos-service y productos-service.
- Verificar persistencia en RDS.
- Verificar que el frontend se sirve correctamente via CloudFront.

## Notas importantes

- **Autenticacion**: Microsoft Entra ID (NO Amazon Cognito). Es requisito de evaluacion.
- **JWT**: El BFF valida el JWT emitido por Entra ID (issuer, audience, expiracion).
- **Secretos**: Ningun secreto se almacena en el repositorio. Usar AWS Systems Manager
  Parameter Store o Secrets Manager para credenciales de RDS y configuracion de Entra ID.
- **Costos**: Usar instancias y recursos de nivel gratuito donde sea posible.
