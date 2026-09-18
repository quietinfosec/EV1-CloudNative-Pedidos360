# IAM - Pedidos360

## Estado verificado en AWS

Se ejecutaron `aws sts get-caller-identity` y `aws configure get region`.
La identidad es valida y usa un rol asumido; la region configurada es `us-east-1`.
La salida STS se proceso en memoria sin guardar ni mostrar Account ID, ARN del
principal o identificador de sesion. IAM es global; `us-east-1` es el contexto
operativo y la region prevista para el futuro backend, no una region del rol.

**La creacion del rol solicitado esta bloqueada por `AccessDenied` para
`iam:CreateRole`. No se completo la configuracion IAM objetivo.**

| Recurso o requisito | Resultado de las consultas de lectura |
|--------------------|--------------------------------------|
| `Pedidos360Ec2Role` | No existe: `GetRole` devuelve `NoSuchEntity` |
| Trust exclusivo de EC2 para ese rol | Preparado en `trust-policy.json`, no aplicado porque el rol no existe |
| `AmazonSSMManagedInstanceCore` | Ya adjunta a `LabRole`; no asociada al rol objetivo inexistente |
| `CloudWatchAgentServerPolicy` | Existe como politica administrada AWS; no esta adjunta a `LabRole` |
| `Pedidos360Ec2InstanceProfile` | Ya existe y contiene `LabRole`; conservado sin cambios |
| `LabInstanceProfile` | Alternativa provista por el laboratorio; contiene `LabRole` |

Se comprobo la existencia de los recursos antes de intentar crearlos. En esta
etapa no se creo ni modifico ningun recurso AWS: solo se realizo un intento de
`CreateRole`, que fue denegado. No se duplico el Instance Profile existente.

## Bloqueo de permisos

Comando AWS ejecutado desde la raiz del repositorio:

```powershell
aws iam create-role --role-name Pedidos360Ec2Role --assume-role-policy-document file://infra/aws/iam/trust-policy.json --region us-east-1 --query "Role.{RoleName:RoleName,TrustPolicy:AssumeRolePolicyDocument}" --output json --no-cli-pager
```

Error AWS recibido, con el ARN del principal y el Account ID sanitizados:

```text
aws: [ERROR]: An error occurred (AccessDenied) when calling the CreateRole operation: User: <CALLER_ARN_REDACTED> is not authorized to perform: iam:CreateRole on resource: arn:aws:iam::<ACCOUNT_ID_REDACTED>:role/Pedidos360Ec2Role because no identity-based policy allows the iam:CreateRole action
```

El codigo, la operacion, la accion y el motivo se conservan literalmente. Los
marcadores sustituyen exclusivamente identificadores de la cuenta y del principal.

Tras la denegacion se detuvieron todas las operaciones de escritura. No se
intento `AttachRolePolicy`, cambiar la trust de `LabRole`, quitarlo del Instance
Profile, asumir otro rol ni usar otro mecanismo para eludir la restriccion.
No se atribuye un nuevo error de `AttachRolePolicy` a esta ejecucion: esa accion
no se ejecuto en esta etapa.

### Recurso necesario

Se necesita `Pedidos360Ec2Role`, con trust exclusivamente para EC2, las dos
politicas administradas indicadas abajo y su asociacion al Instance Profile
existente `Pedidos360Ec2InstanceProfile`. La provision debe realizarla un
administrador autorizado del laboratorio o quedar habilitada por sus responsables.
No se solicitan permisos administrativos generales como solucion.

### Alternativa disponible en el laboratorio

Se puede conservar `Pedidos360Ec2InstanceProfile` con `LabRole`, como ya esta
configurado, o evaluar el perfil existente `LabInstanceProfile`. No se creo ni
se cambio ninguna de estas asociaciones en esta etapa.

`LabRole` permite `sts:AssumeRole` a EC2, pero tambien a otros servicios y a un
principal AWS. Tiene siete politicas administradas adjuntas y ninguna inline:
`AmazonSSMManagedInstanceCore`, `AmazonEKSClusterPolicy`,
`AmazonEC2ContainerRegistryReadOnly`, `AmazonEKSWorkerNodePolicy` y tres politicas
propias del laboratorio. No se exportan los ARN de estas politicas de la cuenta.

Esta alternativa **no equivale** al rol dedicado de minimo privilegio solicitado:
su trust no es exclusivo de EC2 y no tiene `CloudWatchAgentServerPolicy` adjunta.
No se modifican ni se eliminan los permisos preexistentes de `LabRole`. Tampoco
se ha evaluado si sus otras politicas conceden permisos equivalentes de CloudWatch.
El uso futuro del perfil requiere las autorizaciones del laboratorio, incluyendo
`iam:PassRole` y permisos de lanzamiento; no se han verificado ni utilizado aqui.

## Proposito de Pedidos360Ec2Role

El rol dedicado permitira que una futura instancia EC2 que ejecute `bff-service`,
`pedidos-service` y `productos-service` acceda a los servicios AWS necesarios
para administracion y observabilidad, sin credenciales estaticas de un usuario.
No concede por si mismo acceso a Microsoft Entra ni implementa la autenticacion
de las aplicaciones o la conectividad a RDS.

## Trust relationship con EC2

[`trust-policy.json`](trust-policy.json) contiene una unica declaracion `Allow`:

- Principal: solamente el servicio `ec2.amazonaws.com`.
- Accion: solamente `sts:AssumeRole`.
- No incluye usuarios, roles, cuentas, otros servicios ni principales comodin.

Este documento ya estaba sanitizado y se utilizo en el intento de `CreateRole`.
No se aplico a `LabRole` ni representa su trust efectiva. La trust permite que
EC2 asuma el rol; el Instance Profile es el mecanismo para asociarlo a una instancia.

## Politicas administradas y copias JSON

| Politica AWS | Version consultada | Copia sanitizada | Uso previsto |
|-------------|--------------------|------------------|--------------|
| `AmazonSSMManagedInstanceCore` | `v2` | [`policy-ssm-managed-instance-core.json`](policy-ssm-managed-instance-core.json) | Systems Manager, Session Manager, inventario y administracion |
| `CloudWatchAgentServerPolicy` | `v3` | [`policy-cloudwatch-agent-server.json`](policy-cloudwatch-agent-server.json) | CloudWatch Agent, logs y metricas |

ARN de las politicas administradas, sin Account ID:

```text
arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy
```

Se consulto `GetPolicy` para obtener la version predeterminada y
`GetPolicyVersion` para obtener cada documento. Los archivos contienen los
documentos de permisos reales (`Version` y `Statement`), no fichas descriptivas.
No contienen Account ID, Access Keys, Secret Keys, tokens ni credenciales.

Son copias de referencia de las versiones consultadas, no politicas nuevas
creadas en la cuenta. AWS puede actualizar sus versiones predeterminadas.
Guardar una copia no adjunta la politica a un rol. Entre los roles revisados,
SSM esta adjunta a `LabRole`; el rol objetivo no existe y la asociacion de
CloudWatch sigue pendiente. No se exportan politicas custom ni la trust de
`LabRole`, que contienen identificadores de la cuenta.

## Principio de minimo privilegio

Para el rol dedicado se previeron solamente las dos politicas administradas
solicitadas para el laboratorio. Los comodines de recursos de sus documentos
pertenecen a las politicas oficiales AWS y se conservan fielmente; no se agregan
acciones ni se presentan como politicas custom mas restrictivas.

No se adjuntaron `AdministratorAccess`, `PowerUserAccess`, `IAMFullAccess` ni
`AmazonEC2FullAccess`, ni se ampliaron los permisos de ningun rol. Los permisos
adicionales que ya tiene `LabRole` pertenecen a la configuracion del laboratorio,
no a un diseno de minimo privilegio implementado por esta etapa.

No se modificaron `AWSServiceRoleForAPIGateway` ni `APIGatewayServiceRolePolicy`.
No se crearon usuarios IAM, Access Keys, EC2, RDS ni API Gateway. No se cambiaron
la VPC, los Security Groups u otros recursos de etapas anteriores.

## Sin Access Keys dentro de EC2

No se generan ni se instalan Access Keys estaticas, Secret Keys o archivos de
credenciales dentro de la futura EC2. No se guardan credenciales en el repositorio.
Los SDK y agentes deberan obtener credenciales temporales del rol mediante el
Instance Profile y el servicio de metadatos, con IMDSv2 exigido al desplegar.
Esto es un requisito futuro: no se creo ninguna instancia en esta etapa.

## SSM preferido frente a SSH

Se prefiere AWS Systems Manager Session Manager frente a exponer SSH
permanentemente. No necesita una regla inbound para el puerto `22` ni llaves
SSH `.pem`. Requiere permisos del rol, SSM Agent operativo, conectividad hacia
los endpoints de SSM y permisos del operador; adjuntar una politica no prueba
que las sesiones funcionen.

El registro de sesiones en CloudWatch requiere configuracion explicita de
Session Manager y permisos apropiados: no se activa automaticamente al adjuntar
`AmazonSSMManagedInstanceCore`. Cuando SSM este operativo y verificado, se podra
retirar la regla SSH temporal documentada en [`../network/`](../network/README.md).
No se modifico dicha regla aqui.

## CloudWatch para logs

Posteriormente se utilizara CloudWatch para los logs de las tres aplicaciones
y metricas del host. Sera necesario instalar y configurar CloudWatch Agent,
definir los archivos de log, destinos y retencion, y verificar los permisos y
la conectividad. No se crearon agentes, log groups ni alarmas en esta etapa.

`CloudWatchAgentServerPolicy` sigue pendiente de asociacion al rol dedicado.
No se declara completada la preparacion de CloudWatch ni se intenta ampliar
`LabRole` para compensar la restriccion.

## Verificacion final de solo lectura

Los siguientes comandos no modifican AWS. Sus consultas `--query` limitan las
respuestas correctas para no mostrar Account ID, ARN del principal o credenciales,
pero no sanitizan los errores de stderr. Si AWS devuelve un error con identificadores,
sanitizarlo antes de compartirlo y no guardar la salida sin sanitizar en el repositorio.
`GetRole` debe seguir devolviendo `NoSuchEntity` para el rol solicitado mientras
no lo aprovisione un administrador autorizado.

```powershell
aws sts get-caller-identity --region us-east-1 --query "{IdentityVerified:starts_with(Arn, 'arn:aws:'),AssumedRole:contains(Arn, ':assumed-role/')}" --output json --no-cli-pager

aws configure get region

aws iam get-role --role-name Pedidos360Ec2Role --region us-east-1 --query "Role.RoleName" --output json --no-cli-pager

aws iam get-role --role-name LabRole --region us-east-1 --query "Role.{RoleName:RoleName,Ec2Trusted:contains(AssumeRolePolicyDocument.Statement[].Principal.Service[], 'ec2.amazonaws.com'),TrustedServiceCount:length(AssumeRolePolicyDocument.Statement[].Principal.Service[]),AWSPrincipalCount:length(AssumeRolePolicyDocument.Statement[].Principal.AWS[])}" --output json --no-cli-pager

aws iam list-attached-role-policies --role-name LabRole --region us-east-1 --query "{AmazonSSMManagedInstanceCore:contains(AttachedPolicies[].PolicyArn, 'arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore'),CloudWatchAgentServerPolicy:contains(AttachedPolicies[].PolicyArn, 'arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy'),AttachedPolicyCount:length(AttachedPolicies)}" --output json --no-cli-pager

aws iam list-role-policies --role-name LabRole --region us-east-1 --query "PolicyNames" --output json --no-cli-pager

aws iam get-instance-profile --instance-profile-name Pedidos360Ec2InstanceProfile --region us-east-1 --query "InstanceProfile.{InstanceProfileName:InstanceProfileName,Roles:Roles[].RoleName}" --output json --no-cli-pager

aws iam list-instance-profiles-for-role --role-name LabRole --region us-east-1 --query "InstanceProfiles[].{InstanceProfileName:InstanceProfileName,Roles:Roles[].RoleName}" --output json --no-cli-pager
```

El estado esperado en esta etapa es: region `us-east-1`, rol dedicado ausente,
`LabRole` con trust no exclusivo de EC2, SSM adjunta, CloudWatch no adjunta y
ambos perfiles existentes asociados a `LabRole`. Esto documenta la restriccion
real; no debe sustituirse por una lista de exitos que implique que el rol
solicitado y todas sus asociaciones quedaron configurados.
