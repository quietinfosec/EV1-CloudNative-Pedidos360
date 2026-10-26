import { oidcSettings } from './oidc.config';
import { environment } from '../../environments/environment';

describe('OIDC configuration', () => {
  it('uses Authorization Code with the local IDaaS authority', () => {
    const settings = oidcSettings();

    expect(settings.authority).toBe(environment.oidc.authority);
    expect(settings.client_id).toBe(environment.oidc.clientId);
    expect(settings.redirect_uri).toBe(environment.oidc.redirectUri);
    expect(settings.response_type).toBe('code');
    expect(settings.scope).toContain('openid');
  });

  it('redirects logout back to the application', () => {
    const settings = oidcSettings();

    expect(settings.post_logout_redirect_uri).toBe(environment.oidc.postLogoutRedirectUri);
    expect(settings.automaticSilentRenew).toBe(false);
    expect(settings.monitorSession).toBe(false);
  });
});
