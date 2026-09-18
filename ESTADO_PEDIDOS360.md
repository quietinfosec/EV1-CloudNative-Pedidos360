# Pedidos360 — Estado actual del proyecto

Hola cabros, les dejo un resumen del estado actual de la **Parcial 1 de Desarrollo Cloud Native I** para que todos tengamos claro qué está hecho, qué falta y cuál es el siguiente paso.

---

## ✅ Lo que ya está hecho

### 1. Infraestructura de red en AWS

Ya está preparada la base de red en **AWS `us-east-1`**:

- VPC por defecto localizada y utilizada.
- Security Group `pedidos360-backend-sg` creado/configurado.
- Puerto **22 (SSH)** restringido únicamente a la IP administrativa.
- Puerto **8080 (BFF)** restringido temporalmente a la misma IP administrativa.
- Puertos **8081 y 8082** no están publicados hacia Internet.
- No existen reglas abiertas a `0.0.0.0/0` para los servicios del backend.
- La salida de red se mantiene habilitada para que la instancia pueda descargar paquetes, consultar servicios AWS y posteriormente comunicarse con Entra ID/JWKS y RDS.

La arquitectura objetivo sigue siendo:

```text
Internet
   ↓
AWS API Gateway
   ↓
BFF
   ↓
Pedidos Service / Productos Service
```

---

### 2. IAM y administración de EC2

La cuenta académica no permite crear nuevos IAM Roles (`iam:CreateRole` devuelve `AccessDenied`), así que se reutilizó la configuración disponible en el laboratorio.

Actualmente tenemos:

- `Pedidos360Ec2InstanceProfile`
- asociado a `LabRole`
- soporte de **AWS Systems Manager (SSM)** disponible
- acceso a la instancia sin depender necesariamente de SSH o archivos `.pem`

También dejamos preparada y documentada la configuración IAM necesaria para el proyecto.

---

### 3. Instancia EC2

Ya está creada la instancia backend de Pedidos360.

Configuración principal:

- Amazon Linux 2023
- Java Amazon Corretto 21
- Spring Boot 3.3.4
- instancia `t3.medium`
- almacenamiento gp3 cifrado
- SSM funcionando y estado **Online**
- health checks de AWS correctos

Dentro de la instancia quedaron preparados los directorios:

```text
/opt/pedidos360/bff
/opt/pedidos360/pedidos
/opt/pedidos360/productos
/etc/pedidos360
/var/log/pedidos360/
```

También existe un usuario del sistema llamado:

```text
pedidos360
```

para evitar ejecutar las aplicaciones directamente como `root`.

---

## ✅ Backend preparado

Actualmente tenemos tres aplicaciones Spring Boot independientes:

```text
BFF Service        → puerto 8080
Pedidos Service    → puerto 8081
Productos Service  → puerto 8082
```

En ambiente AWS:

```text
BFF        → 0.0.0.0:8080
Pedidos    → 127.0.0.1:8081
Productos  → 127.0.0.1:8082
```

Esto significa que **Pedidos y Productos no quedan expuestos directamente a Internet**.  
El BFF será el punto de entrada hacia los microservicios.

El BFF utiliza variables de entorno para encontrar los servicios:

```text
PEDIDOS_SERVICE_URL=http://localhost:8081
PRODUCTOS_SERVICE_URL=http://localhost:8082
```

---

### 4. Perfiles Spring

Ya están preparados los perfiles:

```text
local
aws
```

La selección se realiza mediante:

```text
SPRING_PROFILES_ACTIVE
```

Por ahora no hay contraseñas, tokens ni credenciales escritas directamente en el código.

---

### 5. Health checks

Los tres servicios tienen:

```text
GET /api/health
GET /actuator/health
```

Estos endpoints nos permitirán comprobar fácilmente el despliegue en AWS.

---

### 6. Tests y compilación

Actualmente tenemos:

```text
26 tests
0 fallos
```

Los tres microservicios compilan correctamente con:

```bash
mvn clean package
```

Los `.jar` ya quedan preparados en:

```text
dist/backend/
```

con nombres:

```text
bff-service.jar
pedidos-service.jar
productos-service.jar
```

También se generan hashes SHA256 para comprobar posteriormente que los archivos desplegados en AWS sean exactamente los mismos que se compilaron localmente.

---

### 7. Servicios systemd

Ya están creadas las unidades:

```text
pedidos360-bff.service
pedidos360-pedidos.service
pedidos360-productos.service
```

La idea es que los microservicios se ejecuten como servicios Linux reales y no mediante comandos manuales como:

```bash
java -jar app.jar &
```

Con esto podremos usar:

```bash
systemctl start pedidos360-bff
systemctl restart pedidos360-bff
systemctl status pedidos360-bff
```

y además hacer que los servicios vuelvan a levantarse automáticamente si la instancia se reinicia.

---

### 8. Scripts de automatización

Ya existen scripts PowerShell y Bash para:

- compilar los microservicios
- ejecutar los tests
- generar los `.jar`
- preparar la carpeta `dist/`
- copiar los archivos systemd
- calcular hashes SHA256

Todo esto permite repetir el build sin tener que hacerlo manualmente archivo por archivo.

---

### 9. Seguridad del repositorio

Hasta ahora el repositorio se mantiene sin:

- Access Keys de AWS
- Secret Keys
- passwords
- tokens JWT
- client secrets
- archivos `.pem`
- claves privadas
- credenciales de base de datos

También están configurados `.gitignore`, archivos `.env.example` y documentación sanitizada.

---

# 🟡 Siguiente paso inmediato

Lo próximo es **desplegar realmente los tres `.jar` dentro de EC2**.

Actualmente están preparados localmente:

```text
dist/backend/
├── bff-service.jar
├── pedidos-service.jar
└── productos-service.jar
```

Debemos llevarlos a:

```text
EC2

/opt/pedidos360/bff/bff-service.jar
/opt/pedidos360/pedidos/pedidos-service.jar
/opt/pedidos360/productos/productos-service.jar
```

La transferencia se hará utilizando:

```text
S3 privado
   ↓
AWS Systems Manager
   ↓
EC2
```

sin publicar los archivos ni utilizar credenciales dentro del repositorio.

---

## Después del despliegue

Tenemos que instalar y habilitar los servicios systemd:

```bash
systemctl enable pedidos360-pedidos
systemctl enable pedidos360-productos
systemctl enable pedidos360-bff
```

y comprobar:

```text
pedidos360-bff        → active
pedidos360-pedidos    → active
pedidos360-productos  → active
```

Después, desde la propia EC2, probar:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8081/api/health
curl http://localhost:8082/api/health
```

El resultado esperado es:

```text
HTTP 200 OK
```

en los tres servicios.

---

# ❌ Lo que todavía falta

## Base de datos

- Crear RDS.
- Crear Security Group para la base de datos.
- Permitir acceso a RDS únicamente desde el backend.
- Configurar Spring Data JPA.
- Crear entidad `Producto`.
- Crear entidad `Pedido`.
- Crear repositories.
- Crear servicios y controladores.
- Implementar CRUD real.
- Comprobar persistencia de datos.

---

## AWS API Gateway

Después de tener el backend funcionando:

- crear API Gateway
- crear rutas para Pedidos360
- conectar API Gateway con el BFF
- configurar CORS
- probar las rutas mediante Postman
- comprobar respuestas HTTP

La idea final será:

```text
Angular
   ↓
API Gateway
   ↓
BFF
   ↓
Pedidos / Productos
   ↓
RDS
```

---

## Microsoft Entra ID + MSAL

Esta es una de las partes más importantes de la parcial.

Falta:

- registrar la aplicación en Microsoft Entra ID
- configurar el frontend Angular
- instalar/configurar MSAL
- implementar Login
- implementar Logout
- proteger rutas con `MsalGuard`
- utilizar `MsalInterceptor`
- obtener el Access Token
- adjuntar automáticamente el Bearer Token al consumir la API
- leer roles y scopes desde los claims

---

## Seguridad JWT en el BFF

También falta implementar Spring Security para que el BFF valide correctamente el JWT.

Debe validar como mínimo:

```text
firma
issuer
audience
expiración
roles/scopes
```

Y debemos demostrar respuestas:

```text
200 OK
401 Unauthorized
403 Forbidden
```

---

## Frontend funcional

Todavía falta completar las vistas reales de Pedidos360, por ejemplo:

```text
/login
/home
/productos
/pedidos
/perfil
```

y conectarlas con la API real.

---

## Hosting del frontend

Al final podemos desplegar Angular mediante:

```text
Amazon S3
   ↓
CloudFront
```

para tener el frontend publicado en AWS.

---

## Observabilidad

También queda pendiente:

- CloudWatch Logs
- revisión de logs de los servicios
- documentación de errores
- evidencia de health checks

---

## Entrega final

Antes de entregar todavía debemos preparar:

- repositorios GitHub definitivos
- README final
- arquitectura final
- evidencias/capturas
- pruebas `200 / 401 / 403`
- links de repositorios
- presentación
- demo completa

---

# 📌 Orden que vamos a seguir

```text
1. Transferir JARs a EC2
        ↓
2. Activar systemd
        ↓
3. Conseguir 200 OK en los 3 servicios
        ↓
4. Crear RDS
        ↓
5. Implementar JPA + CRUD
        ↓
6. Configurar API Gateway
        ↓
7. Configurar Microsoft Entra ID
        ↓
8. Implementar Angular + MSAL
        ↓
9. Implementar Spring Security + JWT
        ↓
10. Roles y scopes
        ↓
11. Probar 200 / 401 / 403
        ↓
12. Integración completa
        ↓
13. S3 + CloudFront
        ↓
14. GitHub + documentación
        ↓
15. Presentación + demo
```

---

## Arquitectura final esperada

```text
                Microsoft Entra ID
                       │
                  OAuth2 / OIDC
                       │
                       ▼
                  Angular + MSAL
                       │
                 Bearer JWT
                       │
                       ▼
                AWS API Gateway
                       │
                       ▼
               BFF Spring Boot
                       │
             Spring Security JWT
                       │
          ┌────────────┴────────────┐
          ▼                         ▼
  Pedidos Service            Productos Service
          │                         │
          └────────────┬────────────┘
                       ▼
                     RDS

Frontend:
Angular → S3 → CloudFront
```

---

## Resumen

En este momento ya tenemos **la infraestructura AWS base, EC2, configuración de seguridad, estructura backend, tests, builds, systemd y automatización bastante avanzados**.

Ahora tenemos que pasar de la preparación al funcionamiento real:

> **desplegar los microservicios dentro de EC2, comprobar los tres `200 OK` y después comenzar RDS + API Gateway + MSAL/JWT.**

Así todos podemos ir viendo qué parte está lista y en qué podemos repartir el trabajo que queda.
