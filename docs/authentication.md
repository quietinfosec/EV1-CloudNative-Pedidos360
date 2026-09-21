# Autenticación y Autorización — Microsoft Entra ID & Spring Security JWT

Este documento detalla la arquitectura de seguridad, autenticación federada (OAuth2/OIDC) y validación de tokens JWT en **Pedidos360**.

---

## 1. Flujo de Identidad y Tokens

```text
1. Usuario abre Angular (Single Page Application).
2. Angular utiliza @azure/msal-angular para iniciar sesión contra Microsoft Entra ID.
3. Entra ID valida credenciales y emite:
   - ID Token (información del usuario)
   - Access Token JWT (destinado a la API).
4. Angular adjunta automáticamente el Access Token en cabecera:
   Authorization: Bearer <JWT>
5. La petición llega al BFF (Resource Server).
6. Spring Security valida criptográficamente el JWT:
   - Firma contra el JWKS de Microsoft Entra ID
   - Issuer (https://login.microsoftonline.com/{tenant-id}/v2.0)
   - Audience (api://{client-id})
   - Expiración (exp)
7. Si el token es válido, se procesa la solicitud hacia los microservicios.
```

---

## 2. Matriz de Respuestas HTTP de Seguridad

| Escenario | Código HTTP | Mensaje / Causa |
|---|---|---|
| Petición pública permitida (`GET /api/productos`, `/api/health`) | `200 OK` | Acceso libre sin requerir token |
| Petición protegida sin token | `401 Unauthorized` | Cabecera `Authorization` ausente |
| Petición con token expirado o firma inválida | `401 Unauthorized` | Fallo de validación contra JWKS de Entra ID |
| Petición con token válido pero sin rol/scope necesario | `403 Forbidden` | Acceso denegado por políticas de autorización |
| Petición protegida con token válido | `200 OK` / `201 Created` | Operación autorizada y procesada con éxito |

---

## 3. Configuración en Angular (`src/environments/environment.ts`)

```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api',
  msal: {
    clientId: 'TU_CLIENT_ID_ENTRA_ID',
    tenantId: 'TU_TENANT_ID',
    redirectUri: 'http://localhost:4200',
    scopes: ['api://pedidos360-api/access_as_user', 'User.Read']
  }
};
```
