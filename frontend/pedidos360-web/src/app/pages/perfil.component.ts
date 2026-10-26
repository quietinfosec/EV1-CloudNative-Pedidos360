import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { User } from 'oidc-client-ts';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container">
      <h2>Perfil de Usuario & Claims JWT</h2>
      <p class="subtitle">Identidad federada provista por el IDaaS (Keycloak local / Entra ID en producción)</p>

      <ng-container *ngIf="user$ | async as user; else noSession">
        <div class="profile-card">
          <div class="user-header">
            <div class="avatar">ID</div>
            <div>
              <h3>{{ user.profile['name'] || 'Usuario' }}</h3>
              <p class="email">{{ user.profile['preferred_username'] || user.profile['email'] }}</p>
            </div>
          </div>

          <div class="section">
            <h4>Detalles de la Cuenta</h4>
            <table class="claims-table">
              <tr><th>Issuer</th><td>{{ user.profile['iss'] }}</td></tr>
              <tr><th>Subject</th><td>{{ user.profile['sub'] }}</td></tr>
              <tr><th>Audiencia</th><td>{{ user.profile['aud'] }}</td></tr>
              <tr><th>Expira</th><td>{{ user.expires_at | date:'medium' }}</td></tr>
            </table>
          </div>

          <div class="section" *ngIf="user.profile">
            <h4>Claims del Token</h4>
            <table class="claims-table">
              <tr *ngFor="let item of getClaimsList(user)">
                <th>{{ item.key }}</th>
                <td>{{ item.val | json }}</td>
              </tr>
            </table>
          </div>

          <div class="section">
            <h4>Roles y Permisos Asignados</h4>
            <div *ngIf="getRoles(user).length > 0; else noRoles" class="roles-tags">
              <span class="tag" *ngFor="let role of getRoles(user)">{{ role }}</span>
            </div>
            <ng-template #noRoles>
              <p class="text-muted">No hay roles específicos asignados en el token actual.</p>
            </ng-template>
          </div>
        </div>
      </ng-container>

      <ng-template #noSession>
        <div class="no-session-card">
          <p>No has iniciado sesión con el IDaaS.</p>
          <button class="btn btn-primary" (click)="login()">Iniciar Sesión Ahora</button>
        </div>
      </ng-template>
    </div>
  `,
  styles: [`
    .container { max-width: 900px; margin: 2rem auto; padding: 0 1rem; }
    h2 { color: #0f172a; margin-bottom: 0.25rem; }
    .subtitle { color: #64748b; margin-bottom: 2rem; }
    .profile-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 2rem; box-shadow: 0 2px 4px rgba(0,0,0,0.03); }
    .user-header { display: flex; align-items: center; gap: 1.25rem; padding-bottom: 1.5rem; border-bottom: 1px solid #f1f5f9; margin-bottom: 1.5rem; }
    .avatar { display: flex; align-items: center; justify-content: center; width: 64px; height: 64px; border-radius: 50%; background: #e0f2fe; color: #0369a1; font-weight: 800; }
    .user-header h3 { margin: 0; color: #0f172a; font-size: 1.4rem; }
    .email { margin: 0.25rem 0 0; color: #64748b; }
    .section { margin-bottom: 2rem; }
    .section h4 { color: #0369a1; font-size: 1rem; margin-bottom: 0.75rem; }
    .claims-table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
    .claims-table th, .claims-table td { padding: 0.5rem 0.75rem; border-bottom: 1px solid #f1f5f9; text-align: left; }
    .claims-table th { width: 35%; color: #475569; font-weight: 600; background: #f8fafc; }
    .claims-table td { color: #0f172a; word-break: break-all; }
    .roles-tags { display: flex; gap: 0.5rem; flex-wrap: wrap; }
    .tag { background: #e0f2fe; color: #0369a1; padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.85rem; font-weight: 600; }
    .text-muted { color: #94a3b8; font-style: italic; }
    .no-session-card { text-align: center; background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 3rem; }
    .btn { padding: 0.5rem 1.25rem; border-radius: 6px; font-weight: 600; cursor: pointer; border: none; }
    .btn-primary { background: #0284c7; color: #fff; }
    .btn-primary:hover { background: #0369a1; }
  `]
})
export class PerfilComponent {
  private readonly authService = inject(AuthService);
  readonly user$ = this.authService.user$;

  getClaimsList(user: User): Array<{ key: string; val: unknown }> {
    return Object.entries(user.profile).map(([key, val]) => ({ key, val }));
  }

  getRoles(user: User): string[] {
    const roles = user.profile['roles'];
    return Array.isArray(roles) ? roles.map(String) : [];
  }

  login(): void {
    void this.authService.login();
  }
}
