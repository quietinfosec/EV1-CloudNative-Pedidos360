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
