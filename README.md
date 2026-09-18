# Pedidos360

Proyecto para la asignatura **Desarrollo Cloud Native I (DSY1107)** - Duoc UC.

Estado actual: backend desplegado y verificado en AWS EC2 (`us-east-1`), con
los tres microservicios en ejecución como servicios systemd. Detalle y
evidencia en [docs/backend-deployment-aws.md](docs/backend-deployment-aws.md).

## Arquitectura

Despliegue actual verificado:

```text
BFF Spring Boot (backend/bff-service, 0.0.0.0:8080)
        |
        +--> Pedidos Service (backend/pedidos-service, 127.0.0.1:8081)
        |
        +--> Productos Service (backend/productos-service, 127.0.0.1:8082)
```

Pedidos y Productos escuchan solo en loopback; el BFF es la única entrada
entre los tres servicios. La transferencia se realizó por S3 privado y la
instalación por AWS Systems Manager, sin SCP ni Key Pairs.

Arquitectura objetivo (etapas posteriores):

```text
Angular (frontend/pedidos360-web)
        |
        v
AWS API Gateway (etapa posterior)
        |
        v
BFF Spring Boot (:8080, con validación JWT en etapa posterior)
        |
        +--> Pedidos Service (:8081)
        |
        +--> Productos Service (:8082)
                |
                v
        Base de datos cloud (etapa posterior)
```

## Estructura de carpetas

```text
Pedidos360/
├── frontend/
│   └── pedidos360-web/        # Angular con componentes standalone
├── backend/
│   ├── bff-service/           # Spring Boot (puerto 8080)
│   ├── pedidos-service/       # Spring Boot (puerto 8081)
│   └── productos-service/     # Spring Boot (puerto 8082)
├── infra/aws/
│   ├── ec2/                   # Unidades systemd, entorno y user-data
│   ├── iam/                   # Documentación y políticas de referencia
│   └── network/               # Documentación de red y Security Groups
├── scripts/                   # Compilación, ejecución local y despliegue
├── docs/                      # Documentación del proyecto
├── dist/                      # Artefactos locales generados (no versionado)
└── .env.example               # Plantilla de variables, sin valores reales
```

## Tecnologías

**Frontend:**

- Angular (componentes standalone)
- TypeScript
- Angular Router
- HttpClient

**Backend:**

- Java 21 (Amazon Corretto 21 en EC2)
- Spring Boot 3.3.x
- Maven (wrappers `mvnw` por servicio)
- Spring Web
- Spring Boot Actuator
- Spring Validation

## Microservicios

| Servicio          | Puerto | Paquete                      | ArtifactId        |
|-------------------|--------|------------------------------|-------------------|
| bff-service       | 8080   | cl.duoc.pedidos360.bff       | bff-service       |
| pedidos-service   | 8081   | cl.duoc.pedidos360.pedidos   | pedidos-service   |
| productos-service | 8082   | cl.duoc.pedidos360.productos | productos-service |

groupId: `cl.duoc.pedidos360`

Cada microservicio expone:

- `GET /api/health` → `{ "service": "<nombre>", "status": "UP" }`
- `GET /actuator/health` → estado de Spring Boot Actuator

Perfiles Spring:

| Perfil | Uso                     | Configuración              |
|--------|-------------------------|----------------------------|
| local  | Desarrollo local        | `application-local.properties` |
| aws    | Despliegue en AWS (EC2) | `application-aws.properties`   |

Perfil activo por defecto:

```text
spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}
```

## Cómo ejecutar en local

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

Repetir para `pedidos-service` (puerto 8081) y `productos-service`
(puerto 8082).

También hay scripts de apoyo en `scripts/`: `build-all` compila frontend y
backend, y `run-local` ayuda con la ejecución local. Ver cada script para su
uso en Windows (`.ps1`) o Bash (`.sh`).

## Despliegue del backend en AWS

Resumen verificado (sin datos sensibles):

- Canal de transferencia: bucket S3 privado, con Block Public Access y
  cifrado SSE-S3; solo JAR y unidades systemd, sin `.env` reales.
- Instalación: AWS Systems Manager Run Command, con validación SHA256 antes
  de instalar.
- Ubicaciones en EC2: JAR en `/opt/pedidos360/<servicio>/`, entorno en
  `/etc/pedidos360/<servicio>.env` y unidades en `/etc/systemd/system/`.
- Servicios `active` y `enabled`: `pedidos360-pedidos`,
  `pedidos360-productos` y `pedidos360-bff`, en ese orden de arranque.
- Salud: seis respuestas HTTP `200` con estado `UP`.
- Red: `0.0.0.0:8080`, `127.0.0.1:8081` y `127.0.0.1:8082`, con Java forzado a
  IPv4 (`-Djava.net.preferIPv4Stack=true`).

Documento completo: [docs/backend-deployment-aws.md](docs/backend-deployment-aws.md).

## Documentación

- [docs/backend-deployment-aws.md](docs/backend-deployment-aws.md) - Despliegue verificado: S3 privado, SSM, systemd, health checks y diagnóstico.
- [docs/architecture.md](docs/architecture.md) - Arquitectura objetivo y flujos.
- [docs/arquitectura.md](docs/arquitectura.md) - Arquitectura inicial.
- [docs/aws-deployment.md](docs/aws-deployment.md) - Etapas futuras de despliegue.
- [ESTADO_PEDIDOS360.md](ESTADO_PEDIDOS360.md) - Estado operativo y reparto de trabajo.

## Seguridad

- Ningún secreto se almacena en el repositorio: sin Access Keys, tokens,
  contraseñas, `.pem` ni credenciales de base de datos.
- Usar [.env.example](.env.example) como plantilla y completar los valores
  fuera del repositorio.
- Los archivos de entorno de EC2 (`/etc/pedidos360/*.env`) se crean
  directamente en la instancia y contienen solo configuración no sensible.

## Próximas etapas

- Base de datos cloud (RDS) y persistencia con JPA.
- AWS API Gateway, rutas y CORS.
- Microsoft Entra ID, MSAL Angular y protección de rutas.
- Validación JWT en el BFF (firma, issuer, audience, expiración y roles).
- Vistas funcionales del frontend y conexión con la API real.
- Hosting del frontend (S3 + CloudFront) y observabilidad con CloudWatch.
