# Security Groups - Pedidos360

## Estado verificado

| Propiedad | Valor |
|-----------|-------|
| Region | `us-east-1` |
| Nombre | `pedidos360-backend-sg` |
| Security Group ID | `sg-000542deedef64065` |
| Descripcion | `Security Group backend Pedidos360` |
| VPC ID | `vpc-091400dfac7e47849` |
| CIDR de la VPC | `172.31.0.0/16` |
| VPC por defecto | `true` |
| Accion inicial | Creado, tras comprobar que no existia ese nombre dentro de la VPC |
| Ultima revision | `2026-09-08` |
| Accion actual | Reutilizado, sin cambios en AWS |
| Interfaces de red asociadas al verificar | `0` |

La VPC se reutilizo sin modificarla. En la configuracion inicial se creo el
Security Group y se autorizaron las dos reglas inbound indicadas abajo.
En esta revision se encontro el mismo grupo por nombre y VPC y se verifico que
ya cumple las restricciones: no se crearon recursos ni se modificaron reglas.
Todas las operaciones AWS de esta revision fueron de lectura. No se crearon
EC2, RDS, API Gateway, subredes, rutas ni otros recursos.

## Reglas inbound

`IP_ADMIN/32` representa la IPv4 publica validada del equipo del administrador.
En AWS ambas reglas contienen la misma IP real, no este marcador. La IP no se
incluye en el repositorio ni en el resumen para evitar divulgarla.

| Protocolo | Puerto | Origen efectivo | Uso | Estado |
|-----------|--------|-----------------|-----|--------|
| TCP | `22` | `IP_ADMIN/32` | Administracion temporal por SSH | Autorizado, restringido a una IPv4 |
| TCP | `8080` | `IP_ADMIN/32` | BFF, acceso temporal de laboratorio | Autorizado, restringido a la misma IPv4 |
| TCP | `8081` | Ninguno | `pedidos-service`, solo comunicacion interna | Sin regla inbound |
| TCP | `8082` | Ninguno | `productos-service`, solo comunicacion interna | Sin regla inbound |

Solo existen las reglas TCP `22` y `8080`. No hay reglas inbound IPv6, rangos de
puertos amplios, todos los protocolos, referencias a otros Security Groups ni
listas de prefijos. No se autoriza entrada desde `0.0.0.0/0` ni `::/0`.

### Deteccion de IP y SSH

La IP de salida del equipo donde se ejecuta AWS CLI se consulto mediante HTTPS
en `https://checkip.amazonaws.com` y `https://api.ipify.org`. Las dos respuestas
coincidieron, se valido que fuera una IPv4 publica y no se detecto un proxy
explicito. En la configuracion inicial se repitio la comprobacion antes de
autorizar las reglas; en la revision actual se valido nuevamente la IP y se
comparo en memoria con ambos origenes existentes, sin mostrarla ni guardarla.

Si no es posible determinar el origen de forma confiable en una ejecucion futura,
no se debe crear la regla SSH: queda pendiente de configuracion manual con una
IP confirmada `/32`. Tampoco se debe abrir `8080` sin un origen aprobado.

Si cambia la IP, la VPN o la ruta de salida, validar el origen real de SSH y del
BFF antes de sustituir las reglas temporales. No acumular IPs antiguas ni ampliar
el CIDR para resolver un problema de acceso. Una IP de salida compartida por NAT
no identifica a una persona ni sustituye la autenticacion.

Se mantiene la preferencia por SSM Session Manager descrita en
[`../iam/README.md`](../iam/README.md). SSH es una excepcion temporal; retirar su
regla cuando SSM este operativo y se haya validado como acceso administrativo.

### BFF y microservicios

La disposicion prevista para el futuro host es:

- `bff-service`: puerto `8080`, unico servicio de aplicacion que escuchara externamente.
- `pedidos-service`: `localhost:8081`, solo accesible desde el propio host.
- `productos-service`: `localhost:8082`, solo accesible desde el propio host.

Estas direcciones son un requisito de despliegue, no una configuracion de runtime
aplicada en esta etapa. El Security Group filtra trafico de red; no configura el
bind de las aplicaciones ni controla el trafico loopback. Antes del despliegue
se debe comprobar que ambos microservicios escuchen solo en loopback y que no
se publiquen sus puertos mediante contenedores u otros Security Groups.

**La exposicion definitiva de `8080` queda pendiente de la etapa API Gateway.**
No se agrega automaticamente `0.0.0.0/0` ni `::/0` para el BFF. La regla `/32` es
solo para pruebas del laboratorio y debera retirarse o sustituirse al definir la
integracion de API Gateway y su origen autorizado. No se presupone que API Gateway
disponga de un Security Group referenciable ni que JWT este implementado.

## Reglas outbound

| Protocolo | Puertos | Destino | Estado |
|-----------|---------|---------|--------|
| Todos (`-1`) | Todos | `0.0.0.0/0` (IPv4) | Unica regla de salida predeterminada, conservada sin cambios |

No hay reglas outbound IPv6 ni se agregaron reglas de salida adicionales.
Esta salida estandar se conserva expresamente para el laboratorio: permite, a
nivel de Security Group, descargar paquetes, consultar Microsoft Entra/JWKS,
acceder a servicios AWS y conectarse a RDS posteriormente. La conectividad real
tambien requerira rutas, DNS y permisos del servicio destino; no se crea RDS ni
se configura su acceso ahora.

El destino `0.0.0.0/0` aqui corresponde exclusivamente a **salida**, no a entrada.
Los Security Groups son stateful: el trafico de respuesta de conexiones permitidas
no requiere abrir puertos inbound adicionales. La salida podra restringirse cuando
se conozcan los destinos y la arquitectura final.

## Arquitectura objetivo

```text
Internet -> API Gateway -> BFF -> microservicios
```

API Gateway sera la entrada publica prevista. El acceso directo temporal al BFF
no es la arquitectura final. Esta configuracion prepara un grupo sin asociarlo a
ningun recurso; no despliega servicios ni publica endpoints.

## Verificacion de solo lectura

Ejecutar en PowerShell con las credenciales autorizadas del laboratorio y la
region explicita. No compartir credenciales, tokens ni salidas de identidad.
Los comandos siguientes solo consultan AWS; no modifican recursos.
Las consultas `--query` limitan las respuestas correctas, pero no sanitizan
errores de stderr. Sanitizar cualquier identificador o dato sensible de los
errores antes de compartirlos o guardarlos.

Primero localizar el grupo por nombre dentro de la VPC detectada. Los IDs
siguientes corresponden a la revision registrada; si la VPC o la busqueda por
nombre no coinciden, revisar los IDs antes de continuar.

```powershell
aws ec2 describe-vpcs --region us-east-1 `
  --filters "Name=is-default,Values=true" `
  --query "Vpcs[].{VpcId:VpcId,CIDR:CidrBlock,IsDefault:IsDefault}" `
  --output json --no-cli-pager

aws ec2 describe-security-groups --region us-east-1 `
  --filters "Name=vpc-id,Values=vpc-091400dfac7e47849" "Name=group-name,Values=pedidos360-backend-sg" `
  --query "SecurityGroups[].{Name:GroupName,GroupId:GroupId,VpcId:VpcId,Description:Description}" `
  --output json --no-cli-pager

aws ec2 describe-security-groups --region us-east-1 `
  --group-ids sg-000542deedef64065 `
  --query "SecurityGroups[].{Name:GroupName,GroupId:GroupId,VpcId:VpcId,Inbound:IpPermissions[].{Protocol:IpProtocol,FromPort:FromPort,ToPort:ToPort,IPv4:IpRanges[].{Source:'REDACTED',Host32:ends_with(CidrIp, '/32')},IPv6Count:length(Ipv6Ranges),SourceGroups:UserIdGroupPairs[].GroupId,PrefixLists:PrefixListIds[].PrefixListId},Outbound:IpPermissionsEgress[].{Protocol:IpProtocol,FromPort:FromPort,ToPort:ToPort,IPv4:IpRanges[].CidrIp,IPv6:Ipv6Ranges[].CidrIpv6,DestinationGroups:UserIdGroupPairs[].GroupId,PrefixLists:PrefixListIds[].PrefixListId}}" `
  --output json --no-cli-pager

aws ec2 describe-network-interfaces --region us-east-1 `
  --filters "Name=group-id,Values=sg-000542deedef64065" `
  --query "{AttachedNetworkInterfaces:length(NetworkInterfaces)}" `
  --output json --no-cli-pager
```

La consulta oculta las IPs inbound y muestra `Host32` para cada origen IPv4.
El resultado esperado son exactamente dos reglas TCP (`22` y `8080`), cada una
con un origen IPv4 y `Host32: true`, sin otros tipos de origen. No debe existir
ninguna regla que permita `8081` o `8082`, ni siquiera mediante un rango o todos
los protocolos. La salida debe coincidir con la tabla outbound y las interfaces
asociadas deben ser `0` en esta etapa.

La revision del `2026-09-08` tambien comprobo que las dos reglas comparten el
mismo origen `/32` y que coincide con la IPv4 publica administrativa validada
mediante dos consultas HTTPS, sin mostrar su valor. No fue necesario autorizar,
revocar ni modificar ninguna regla. Si las reglas cambian posteriormente,
validar localmente que la IP autorizada sigue siendo la correcta; `Host32: true`
por si solo no demuestra que ambos origenes coincidan ni que pertenezcan al
administrador.
