# Pedidos360 — Despliegue del backend en AWS

Este documento describe el despliegue verificado del backend de Pedidos360 en
`us-east-1`: transferencia de artefactos mediante un bucket S3 privado e
instalación y operación mediante AWS Systems Manager, sin SCP, sin Key Pairs y
sin modificar los Security Groups.

Verificación final: **18 de septiembre de 2026, 04:11 UTC**, mediante
SSM Run Command, con estado `Success` y código de salida `0`.

Fuera del alcance de este despliegue: RDS, API Gateway, JWT, cambios de red y
cualquier credencial o secreto. Los archivos de entorno creados en EC2 contienen
solamente configuración no sensible.

## 1. Resultado verificado

| Servicio | Unidad systemd | Estado | Arranque automático | Escucha |
|---|---|---|---|---|
| BFF Service (`bff-service`) | `pedidos360-bff.service` | `active` | `enabled` | `0.0.0.0:8080` |
| Pedidos Service (`pedidos-service`) | `pedidos360-pedidos.service` | `active` | `enabled` | `127.0.0.1:8081` |
| Productos Service (`productos-service`) | `pedidos360-productos.service` | `active` | `enabled` | `127.0.0.1:8082` |

Las seis comprobaciones de salud respondieron HTTP `200` con estado `UP`.
Los tres JAR instalados coincidieron con los SHA256 calculados localmente.
Los procesos se ejecutan como `pedidos360`, con Java Amazon Corretto 21 y
perfil Spring `aws`.

## 2. Transferencia mediante S3 privado

Se listaron los buckets existentes y no se encontró ninguno apropiado para
reutilizar, por lo que se creó un único bucket privado como canal de
despliegue del backend:

```text
pedidos360-deploy-e3f9099439a44caa8168852db7a967e9
```

Configuración comprobada:

- Región `us-east-1`. S3 devuelve `LocationConstraint: null` para esta región;
  ese valor es el esperado.
- Block Public Access habilitado en sus cuatro opciones:
  `BlockPublicAcls`, `IgnorePublicAcls`, `BlockPublicPolicy` y
  `RestrictPublicBuckets`.
- Object Ownership `BucketOwnerEnforced`, con ACL deshabilitadas.
- ACL con control total únicamente para el propietario, sin concesiones públicas.
- Sin bucket policy y sin configuración de hosting web.
- Objetos cifrados con SSE-S3 (`AES256`).

Se transfirieron exclusivamente estos seis objetos:

```text
backend/
├── bff-service.jar
├── pedidos-service.jar
└── productos-service.jar
systemd/
├── pedidos360-bff.service
├── pedidos360-pedidos.service
└── pedidos360-productos.service
```

Los archivos locales proceden de `dist/backend/` y `dist/systemd/`, que son
directorios locales generados y no versionados. La subida se realizó con
`aws s3 cp`, cifrado `AES256`, checksum `SHA256` y un metadato auxiliar `sha256`
con el hash del archivo completo.

La integridad no se validó con el ETag de S3, sino calculando el SHA256 de los
archivos descargados en EC2 antes de instalarlos.

El bucket funciona como canal temporal de transferencia, no como dependencia de
ejecución: los servicios arrancan desde sus copias locales en EC2. Los objetos
permanecen en el bucket como evidencia y para permitir una reinstalación
trazable. Antes de otro despliegue se debe comprobar si este bucket sigue
siendo apropiado y reutilizarlo en ese caso.

No se subió ningún archivo `.env` real.

## 3. Administración mediante Systems Manager

La instancia existente se comprobó como `running` en EC2 y `Online` en SSM.
Toda la instalación y verificación se ejecutó mediante **SSM Run Command**,
con el documento `AWS-RunShellScript`. No se modificó la configuración IAM.

Antes de instalar, se comprobó desde la propia EC2 el acceso de lectura al
bucket:

```bash
aws s3 ls s3://pedidos360-deploy-e3f9099439a44caa8168852db7a967e9 --region us-east-1
```

Además se consultó cada objeto con `head-object` para comprobar su acceso,
tamaño, cifrado y metadatos. Como referencia de diagnóstico, el listado
requiere `s3:ListBucket` y la descarga requiere `s3:GetObject`. Si una
ejecución devolviera `AccessDenied`, el despliegue debería detenerse y
registrarse la acción y el recurso afectados.

Para un diagnóstico interactivo posterior, con la sesión local de AWS vigente y
el identificador de la instancia en `INSTANCE_ID`:

```bash
aws ssm start-session --target "$INSTANCE_ID" --region us-east-1
```

Ese comando requiere el plugin de Session Manager en el equipo del operador.
Run Command permite ejecutar los mismos diagnósticos sin abrir sesión
interactiva. Salvo indicación contraria, los comandos Linux de este documento
se ejecutan **dentro de EC2**.

## 4. Instalación y permisos

Antes de instalar se verificaron Java 21, el usuario y grupo `pedidos360`, los
directorios existentes, la ausencia de archivos de un despliegue anterior y la
disponibilidad de los puertos `8080`, `8081` y `8082`.

Los artefactos se descargaron a un directorio temporal de EC2. Sus seis SHA256
se validaron antes de copiar ningún archivo a su ubicación definitiva. El
directorio temporal se eliminó automáticamente al terminar.

| JAR instalado | Propietario | Permisos |
|---|---|---|
| `/opt/pedidos360/bff/bff-service.jar` | `pedidos360:pedidos360` | `644` |
| `/opt/pedidos360/pedidos/pedidos-service.jar` | `pedidos360:pedidos360` | `644` |
| `/opt/pedidos360/productos/productos-service.jar` | `pedidos360:pedidos360` | `644` |

Los directorios de aplicación ya existentes conservan propietario
`root:pedidos360` y permisos `750`.

Los archivos de entorno se crearon directamente en EC2 y no forman parte de S3
ni del repositorio:

| Archivo | Contenido |
|---|---|
| `/etc/pedidos360/bff.env` | `SPRING_PROFILES_ACTIVE=aws` y URLs internas de Pedidos y Productos |
| `/etc/pedidos360/pedidos.env` | `SPRING_PROFILES_ACTIVE=aws` |
| `/etc/pedidos360/productos.env` | `SPRING_PROFILES_ACTIVE=aws` |

Su propietario es `root:pedidos360` y sus permisos son `640`. El BFF localiza
los servicios internos en `http://localhost:8081` y `http://localhost:8082`.
No contienen contraseñas de base de datos, secretos de Entra ID, JWT ni
credenciales de AWS.

## 5. Unidades systemd y arranque

Las fuentes versionadas están en `infra/aws/ec2/systemd/`. Los artefactos
locales preparados están en `dist/systemd/`. En EC2, las unidades están
instaladas en:

```text
/etc/systemd/system/pedidos360-bff.service
/etc/systemd/system/pedidos360-pedidos.service
/etc/systemd/system/pedidos360-productos.service
```

Con propietario `root:root` y permisos `644`.

Cada unidad define `User=pedidos360`, `Group=pedidos360`, su directorio de
trabajo, su `EnvironmentFile` y el JAR correspondiente. Por ejemplo, el BFF:

```ini
ExecStart=/usr/bin/java -Djava.net.preferIPv4Stack=true -jar /opt/pedidos360/bff/bff-service.jar
Restart=always
RestartSec=5
```

Las otras dos unidades siguen el mismo patrón, con su JAR, directorio y
archivo de entorno respectivos.

### 5.1 Corrección del binding IPv4

La primera comprobación de red mostró sockets dual-stack de Java: `*:8080` y
direcciones IPv4 mapeadas en IPv6 para los servicios internos. Para obtener
exactamente los bindings previstos, se añadió a las tres unidades:

```text
-Djava.net.preferIPv4Stack=true
```

La corrección quedó aplicada en las fuentes versionadas, los artefactos
locales, los objetos de S3 y las unidades instaladas en EC2. Después se
reiniciaron los servicios en orden —Pedidos, Productos y finalmente BFF—,
esperando a que `/actuator/health` respondiera antes de continuar con el
siguiente.

La instalación inicial siguió este orden:

```bash
sudo systemctl daemon-reload
sudo systemctl enable pedidos360-pedidos
sudo systemctl enable pedidos360-productos
sudo systemctl enable pedidos360-bff

sudo systemctl start pedidos360-pedidos
sudo systemctl start pedidos360-productos
sudo systemctl start pedidos360-bff
```

### 5.2 Estrategia de reinicio

- `Restart=always` vuelve a levantar el proceso si termina de forma anormal o
  inesperada, tras una espera de cinco segundos (`RestartSec=5`).
- Una parada administrativa con `systemctl stop` no provoca un reinicio: el
  servicio permanece detenido hasta que se arranque de nuevo.
- `enabled` junto con `WantedBy=multi-user.target` hace que los servicios
  arranquen automáticamente al iniciar EC2.
- Las unidades declaran `After=network.target`. Ese orden respecto a la red no
  establece orden entre las tres aplicaciones; el orden Pedidos, Productos y
  BFF se controla en el procedimiento de despliegue.
- Para un reinicio completo controlado, se recomienda el mismo orden y
  comprobar la salud de cada servicio antes de continuar.
- Si cambia una unidad, ejecutar `daemon-reload` antes de reiniciarla. Si
  cambia un archivo de entorno, reiniciar su servicio para aplicar los valores.

Ejemplo de reinicio de un servicio:

```bash
sudo systemctl restart pedidos360-pedidos
curl --fail --retry 30 --retry-connrefused --retry-delay 2 --retry-max-time 90 \
  --connect-timeout 2 --max-time 5 http://localhost:8081/actuator/health
```

## 6. Comprobaciones de salud y red

Cada servicio expone dos endpoints, por lo que se validaron seis respuestas:

| Servicio | `/api/health` | `/actuator/health` |
|---|---|---|
| BFF, puerto 8080 | `200`, `bff-service`, `UP` | `200`, `UP` |
| Pedidos, puerto 8081 | `200`, `pedidos-service`, `UP` | `200`, `UP` |
| Productos, puerto 8082 | `200`, `productos-service`, `UP` | `200`, `UP` |

Comprobación desde EC2:

```bash
curl -i http://localhost:8080/api/health
curl -i http://localhost:8081/api/health
curl -i http://localhost:8082/api/health

curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8081/actuator/health
curl -i http://localhost:8082/actuator/health

ss -lntp
```

La validación comprobó tanto el código HTTP como el JSON devuelto:
`status` igual a `UP` en los seis casos y `service` con el nombre esperado en
`/api/health`.

Bindings finales comprobados:

```text
0.0.0.0:8080    BFF
127.0.0.1:8081  Pedidos
127.0.0.1:8082  Productos
```

Pedidos y Productos escuchan exclusivamente en loopback y no aceptan conexiones
directas desde fuera de la instancia. La consulta de las reglas existentes del
Security Group mostró entradas solamente para los puertos 22 y 8080, sin reglas
de entrada para 8081 ni 8082. No se modificó ningún Security Group.

## 7. Integridad SHA256

Estos hashes se calcularon localmente antes de la transferencia y coincidieron
con los JAR instalados en la comprobación final:

| Artefacto | SHA256 |
|---|---|
| `bff-service.jar` | `3c002e421ef874d84fe0c04010d14c22ccd006cf101acfd3e4572ff9d183b3fe` |
| `pedidos-service.jar` | `9171eb43108ab74d7a31f7f194b5d496e9ff41f9ad3053d1dff2ba2a093059d9` |
| `productos-service.jar` | `170572d0616066aea56d031206703f6b42d8e1de1d7bf2019f1810357032e66a` |

En PowerShell, desde la raíz local del proyecto:

```powershell
Get-FileHash -Algorithm SHA256 -LiteralPath @(
    "dist/backend/bff-service.jar",
    "dist/backend/pedidos-service.jar",
    "dist/backend/productos-service.jar"
)
```

En EC2:

```bash
sha256sum /opt/pedidos360/bff/bff-service.jar \
  /opt/pedidos360/pedidos/pedidos-service.jar \
  /opt/pedidos360/productos/productos-service.jar
```

Las tres unidades systemd también se validaron por SHA256 antes de instalarlas
en EC2.

## 8. Diagnóstico

```bash
systemctl is-active pedidos360-bff pedidos360-pedidos pedidos360-productos
systemctl is-enabled pedidos360-bff pedidos360-pedidos pedidos360-productos

sudo systemctl status pedidos360-bff --no-pager --full
sudo journalctl -u pedidos360-bff -n 100 --no-pager
sudo journalctl -u pedidos360-pedidos -n 100 --no-pager
sudo journalctl -u pedidos360-productos -n 100 --no-pager

systemctl show pedidos360-bff --property=MainPID,NRestarts,Restart,RestartUSec
sudo systemctl cat pedidos360-bff
sudo ss -lntp
/usr/bin/java -version

stat -c '%U:%G %a %n' /opt/pedidos360/bff/bff-service.jar /etc/pedidos360/bff.env
```

Si un servicio falla, se deben consultar sus últimas cien líneas de journal,
identificar la causa real y corregirla antes de dar el despliegue por válido.
Una conexión rechazada durante los primeros segundos del arranque es un estado
transitorio normal: hay que esperar y repetir la comprobación. Que un servicio
figure como `active` no basta por sí solo; la prueba válida es el HTTP `200`.

En la verificación final, los tres servicios tenían `NRestarts=0` y la
ejecución de diagnóstico terminó sin errores. `systemd-analyze verify` mostró
una advertencia preexistente del sistema sobre la ruta antigua de
`acpid.socket`; no afecta a las unidades de Pedidos360 ni impidió su
validación o arranque.
