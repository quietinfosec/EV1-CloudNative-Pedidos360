import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { User, UserManager } from 'oidc-client-ts';
import { oidcSettings } from './oidc.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userManager = new UserManager(oidcSettings());
  private readonly userSubject = new BehaviorSubject<User | null>(null);

  readonly user$: Observable<User | null> = this.userSubject.asObservable();

  constructor() {
    this.userManager.events.addUserLoaded((user) => this.userSubject.next(user));
    this.userManager.events.addUserUnloaded(() => this.userSubject.next(null));
    this.userManager.events.addAccessTokenExpired(() => this.userSubject.next(null));
    this.userManager.events.addSilentRenewError(() => this.userSubject.next(null));
    void this.loadUser();
  }

  async initialize(): Promise<void> {
    await this.loadUser();
  }

  async loadUser(): Promise<User | null> {
    const user = await this.userManager.getUser();
    this.userSubject.next(user);
    return user;
  }

  async completeLogin(): Promise<User> {
    const user = await this.userManager.signinRedirectCallback();
    this.userSubject.next(user);
    return user;
  }

  login(): Promise<void> {
    return this.userManager.signinRedirect();
  }

  logout(): Promise<void> {
    return this.userManager.signoutRedirect();
  }

  getUser(): User | null {
    return this.userSubject.value;
  }

  isAuthenticated(): boolean {
    const user = this.userSubject.value;
    return user !== null && !user.expired;
  }

  getAccessToken(): string | null {
    const user = this.userSubject.value;
    return user !== null && !user.expired ? user.access_token : null;
  }

  getUsername(): string {
    const profile = this.userSubject.value?.profile;
    if (!profile) {
      return '';
    }
    return (profile['name'] as string)
      || (profile['preferred_username'] as string)
      || (profile['email'] as string)
      || '';
  }

  getRoles(): string[] {
    const roles = this.userSubject.value?.profile?.['roles'];
    return Array.isArray(roles) ? roles.map(String) : [];
  }

  getClaims(): Array<{ key: string; val: unknown }> {
    const profile = this.userSubject.value?.profile;
    if (!profile) {
      return [];
    }
    return Object.entries(profile).map(([key, val]) => ({ key, val }));
  }
}
