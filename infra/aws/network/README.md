# Red inicial - Pedidos360

## Alcance

Configuracion inicial del Security Group del backend en `us-east-1`.
Se localizo y reutilizo la VPC por defecto sin modificarla. En la configuracion
inicial se creo `pedidos360-backend-sg` porque no existia dentro de esa VPC.

En la revision del `2026-09-08` se encontro el grupo por nombre y VPC y se reutilizo
sin modificarlo ni duplicarlo. Sus dos reglas inbound ya coinciden con la IPv4
administrativa actual validada `/32`. No se realizaron cambios en AWS; unicamente
se actualizaron estos documentos con el resultado de las consultas de lectura.

No se crearon EC2, RDS ni API Gateway. Tampoco se modificaron subredes, rutas,
Network ACLs ni otros recursos. El grupo no tiene interfaces de red asociadas
al momento de la verificacion; por si solo no publica ningun servicio.

El estado efectivo, los identificadores y los comandos AWS CLI de verificacion
de solo lectura estan en [`security-groups.md`](security-groups.md).

## Principio de minimo privilegio

- Solo se autorizaron los puertos TCP `22` y `8080`, desde una misma IPv4 validada `/32`.
- SSH se permite unicamente como administracion temporal restringida por IP. Se prefiere SSM Session Manager cuando este operativo.
- Si no se puede confirmar la IP administrativa de forma confiable, SSH debe quedar pendiente de configuracion manual, sin crear la regla.
- `8080` corresponde exclusivamente al BFF y su acceso `/32` es temporal para el laboratorio.
- No se permite entrada desde `0.0.0.0/0` ni `::/0`; no hay reglas inbound de todos los puertos o protocolos.
- `8081` y `8082` no tienen reglas de entrada, ni siquiera desde el CIDR de la VPC.
- Se conserva unicamente la regla outbound IPv4 predeterminada, segun lo permitido para esta etapa, sin agregar reglas innecesarias.
- No se guardan credenciales, tokens ni la IP publica administrativa en estos documentos.

La salida estandar permite a nivel de Security Group descargar paquetes,
acceder a Microsoft Entra/JWKS y servicios AWS, y preparar conectividad futura
hacia RDS. No equivale a abrir entrada desde Internet ni garantiza conectividad
sin rutas, DNS y permisos del destino. Se revisara al definir la arquitectura final.

## Por que no exponer los microservicios

El BFF sera el unico punto de acceso a las operaciones del backend. Publicar
`pedidos-service` o `productos-service` permitiria saltarse los controles del BFF
y aumentaria la superficie de ataque, las interfaces que proteger y el riesgo
de acceso directo a operaciones internas.

En el futuro despliegue sobre un mismo host, `pedidos-service` debera escuchar
en `localhost:8081` y `productos-service` en `localhost:8082`. Solo `bff-service`
escuchara externamente en `8080`. El bind de las aplicaciones se configurara y
verificara en esa etapa; el Security Group no lo configura por si mismo.

Una regla de red no sustituye la autenticacion ni la autorizacion. Esta etapa
no implementa ni verifica JWT, TLS o la integracion con Microsoft Entra.

## Arquitectura objetivo

```text
Internet -> API Gateway -> BFF -> microservicios
```

**La exposicion correcta del BFF se decidira en la etapa API Gateway.**
No se abrira automaticamente `8080` a `0.0.0.0/0` ni a `::/0`. Se deberan definir
la integracion, la conectividad y los origenes permitidos antes de retirar o
sustituir el acceso temporal `/32`. No se crea API Gateway ahora.

## Operacion segura

Usar las credenciales autorizadas del laboratorio y especificar siempre
`--region us-east-1`. No asumir que el perfil activo apunta a la cuenta academica;
si se usa un perfil con nombre, seleccionar el perfil correcto en cada comando.
No compartir salidas que contengan credenciales, tokens o identidad de la cuenta.

Al continuar el despliegue, reutilizar `pedidos360-backend-sg` dentro de su VPC,
revisar sus reglas antes de asociarlo a recursos y comprobar tambien los demas
Security Groups asociados: sus permisos se suman. No crear un duplicado ni abrir
puertos adicionales para compensar problemas de configuracion.
