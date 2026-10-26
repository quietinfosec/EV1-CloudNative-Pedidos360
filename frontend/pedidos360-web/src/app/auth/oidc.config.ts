import { UserManagerSettings, WebStorageStateStore } from 'oidc-client-ts';
import { environment } from '../../environments/environment';

export function oidcSettings(): UserManagerSettings {
  return {
    authority: environment.oidc.authority,
    client_id: environment.oidc.clientId,
    redirect_uri: environment.oidc.redirectUri,
    post_logout_redirect_uri: environment.oidc.postLogoutRedirectUri,
    response_type: environment.oidc.responseType,
    scope: environment.oidc.scope,
    loadUserInfo: true,
    automaticSilentRenew: false,
    monitorSession: false,
    userStore: new WebStorageStateStore({ store: window.localStorage })
  };
}
