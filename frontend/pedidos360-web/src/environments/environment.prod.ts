export const environment = {
  production: true,
  apiBaseUrl: '/api',
  oidc: {
    authority: 'https://login.microsoftonline.com/REPLACE_WITH_TENANT_ID/v2.0',
    clientId: 'REPLACE_WITH_CLIENT_ID',
    redirectUri: `${window.location.origin}/auth/callback`,
    postLogoutRedirectUri: `${window.location.origin}/home`,
    scope: 'openid profile email api://REPLACE_WITH_API_CLIENT_ID/access_as_user',
    responseType: 'code'
  }
};
