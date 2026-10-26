# Autenticación y Autorización — OIDC (Keycloak local / Entra ID)

Este documento describe la autenticación federada (OAuth2/OIDC), la emisión y el consumo de tokens JWT en **Pedidos360**.

---

## 1. Proveedores de identidad

| Entorno | Proveedor | Issuer | Cliente SPA | Audiencia de la API |
|---|---|---|---|---|
| Local | Keycloak 26.2 | `http://localhost:8180/realms/pedidos360` | `pedidos360-web` (público, PKCE S256) | `pedidos360-api` |
| Producción | Microsoft Entra ID | `https://login.microsoftonline.com/{tenant-id}/v2.0` | Client ID de la SPA | `api://{api-client-id}` |

En local, `docker compose up -d keycloak` importa `infra/keycloak/realm-pedidos360.json`. Usuario de prueba: `demo` / `Demo1234!`, con roles `ADMIN` y `USER`.

---

## 2. Flujo de identidad y tokens

```text
1. Usuario abre la SPA Angular.
2. La SPA usa oidc-client-ts con Authorization Code + PKCE y redirige al emisor OIDC.
3. El proveedor OIDC valida credenciales y emite:
   - ID Token (perfil del usuario)
   - Access Token JWT (destinado a pedidos360-api)
4. La SPA recibe el callback en /auth/callback y completa la sesión.
5. El interceptor funcional adjunta el token:
   Authorization: Bearer <JWT>
6. Spring Security valida el JWT en el BFF (y en el Admin Service):
   - Firma contra el JWKS del issuer configurado
   - Issuer exacto
   - Audience obligatoria
   - Expiración (exp)
   - Roles del claim `roles`
7. Si el token es válido, la petición se procesa; si no, se responde 401/403.
```

El frontend no usa MSAL ni almacena client secrets: el cliente `pedidos360-web` es público y PKCE evita el intercambio de secretos.

---

## 3. Configuración

### 3.1 Frontend local (`src/environments/environment.ts`)

```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api',
  oidc: {
    authority: 'http://localhost:8180/realms/pedidos360',
    clientId: 'pedidos360-web',
    redirectUri: 'http://localhost:4200/auth/callback',
    postLogoutRedirectUri: 'http://localhost:4200/home',
    scope: 'openid profile email',
    responseType: 'code'
  }
};
```

### 3.2 Frontend producción (`src/environments/environment.prod.ts`)

Mantiene placeholders hasta contar con el tenant real:

```typescript
authority: 'https://login.microsoftonline.com/REPLACE_WITH_TENANT_ID/v2.0'
clientId: 'REPLACE_WITH_CLIENT_ID'
scope: 'openid profile email api://REPLACE_WITH_API_CLIENT_ID/access_as_user'
```

### 3.3 BFF y Admin local

```properties
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${AZURE_JWKS_URI:http://localhost:8180/realms/pedidos360/protocol/openid-connect/certs}
pedidos360.security.issuer-uri=${AZURE_ISSUER_URI:http://localhost:8180/realms/pedidos360}
pedidos360.security.audience=${AZURE_API_AUDIENCE:pedidos360-api}
```

Si se usa Entra ID en local, deben configurarse las tres variables de forma coherente: `AZURE_JWKS_URI`, `AZURE_ISSUER_URI` y `AZURE_API_AUDIENCE`.

### 3.4 CORS del BFF

El BFF permite `http://localhost:4200` por defecto (`pedidos360.cors.allowed-origins` / `CORS_ALLOWED_ORIGINS`) para el preflight de Angular. En AWS, el origen público se gestiona en API Gateway.

---

## 4. Matriz de respuestas HTTP de seguridad

| Escenario | Código HTTP | Causa |
|---|---|---|
| Petición pública permitida (`GET /api/productos`, `/api/health`) | `200 OK` | Acceso sin token |
| Petición protegida sin token | `401 Unauthorized` | Cabecera `Authorization` ausente |
| Token expirado, firma inválida, issuer o audience incorrectos | `401 Unauthorized` | Fallo de validación JWT |
| Token válido sin rol/scope necesario | `403 Forbidden` | Autorización insuficiente |
| Petición protegida con token válido | `200 OK` / `201 Created` | Operación autorizada |
| Nombre de cola reservado o DTO inválido en Admin | `400 Bad Request` | Validación de entrada |

---

## 5. Evidencia local ejecutada (2026-10-25)

- Authorization Code + PKCE contra Keycloak: **token emitido** con `id_token`, `access_token` y `refresh_token`.
- Claims verificados: `iss=http://localhost:8180/realms/pedidos360`, `aud=pedidos360-api`, `azp=pedidos360-web`, `roles=ADMIN,USER`.
- BFF: `GET /api/pedidos` sin token → **401**; con token → **200**.
- Admin: `GET /admin/queues` sin token → **401**; con token → **200**.
- Admin: `POST /admin/queues` con nombre reservado `amq.test` → **400**.
- Preflight CORS desde `http://localhost:4200` → **200** con `Access-Control-Allow-Origin` correcto.
- Frontend: `npm test` → **9/9**; `npm run build` → **BUILD SUCCESS**.

**Pendiente:** login productivo contra Entra ID (requiere tenant y Client IDs reales). No se declara probado.
