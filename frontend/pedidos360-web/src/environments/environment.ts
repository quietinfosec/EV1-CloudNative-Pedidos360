export const environment = {
  production: false,
  apiBaseUrl: 'http://127.0.0.1:8080/api',
  rabbitmqAdminUrl: 'http://127.0.0.1:8083/admin',
  oidc: {
    authority: 'http://localhost:8180/realms/pedidos360',
    clientId: 'pedidos360-web',
    redirectUri: 'http://localhost:4200/auth/callback',
    postLogoutRedirectUri: 'http://localhost:4200/home',
    scope: 'openid profile email',
    responseType: 'code'
  }
};
