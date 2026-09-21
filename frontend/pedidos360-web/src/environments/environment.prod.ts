export const environment = {
  production: true,
  apiBaseUrl: '/api', // En produccion a traves de CloudFront o API Gateway
  msal: {
    clientId: '00000000-0000-0000-0000-000000000000',
    tenantId: 'common',
    redirectUri: window.location.origin,
    scopes: ['api://pedidos360-api/access_as_user', 'User.Read']
  }
};
