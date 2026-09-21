export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api',
  msal: {
    clientId: '00000000-0000-0000-0000-000000000000', // Configurar con Client ID real de Microsoft Entra ID
    tenantId: 'common', // O Tenant ID de Duoc UC / Organizacion
    redirectUri: 'http://localhost:4200',
    scopes: ['api://pedidos360-api/access_as_user', 'User.Read']
  }
};
