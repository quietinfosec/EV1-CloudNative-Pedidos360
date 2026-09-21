import {
  IPublicClientApplication,
  PublicClientApplication,
  InteractionType,
  BrowserCacheLocation,
  LogLevel
} from '@azure/msal-browser';
import { MsalGuardConfiguration, MsalInterceptorConfiguration } from '@azure/msal-angular';
import { environment } from '../../environments/environment';

export function MSALInstanceFactory(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: environment.msal.clientId,
      authority: `https://login.microsoftonline.com/${environment.msal.tenantId}`,
      redirectUri: environment.msal.redirectUri,
      postLogoutRedirectUri: environment.msal.redirectUri
    },
    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage
    },
    system: {
      loggerOptions: {
        loggerCallback: () => {},
        logLevel: LogLevel.Warning,
        piiLoggingEnabled: false
      }
    }
  });
}

export function MSALGuardConfigFactory(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: environment.msal.scopes
    }
  };
}

export function MSALInterceptorConfigFactory(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string>>();
  protectedResourceMap.set(`${environment.apiBaseUrl}/pedidos`, environment.msal.scopes);
  protectedResourceMap.set(`${environment.apiBaseUrl}/productos`, environment.msal.scopes);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap
  };
}
