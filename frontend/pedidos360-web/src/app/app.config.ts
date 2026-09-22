import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors, withInterceptorsFromDi } from '@angular/common/http';
import { routes } from './app.routes';
import { provideAuth, authInterceptor } from 'angular-auth-oidc-client';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // Aquí agregamos el interceptor para que adjunte el JWT a tus peticiones
    provideHttpClient(
      withInterceptors([authInterceptor()]), 
      withInterceptorsFromDi()
    ),
    provideAuth({
      config: {
        authority: 'https://cognito-idp.us-east-1.amazonaws.com/us-east-1_s3OGAUNw7',
        redirectUrl: 'http://localhost:4200',
        postLogoutRedirectUri: 'http://localhost:4200',
        clientId: 'tb744dmtqpdhvtiifn7amcn6f',
        scope: 'openid email',
        responseType: 'code',
        silentRenew: true,
        useRefreshToken: true,
        // ¡El candado! Solo las peticiones a esta URL llevarán el token JWT adjunto:
        secureRoutes: ['https://hw7t4i73t4.execute-api.us-east-1.amazonaws.com/api']
      }
    })
  ]
};