import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="login-container">
      <div class="login-box">
        <div class="logo">IDaaS</div>
        <h2>Acceso a Pedidos360</h2>
        <p class="desc">Inicia sesión con el IDaaS local (Keycloak) para acceder al sistema.</p>

        <div *ngIf="!isLoggedIn(); else alreadyLoggedIn">
          <button class="btn btn-idaas" (click)="login()">
            <span class="idaas-icon">OIDC</span>
            Iniciar sesión con IDaaS
          </button>
        </div>

        <ng-template #alreadyLoggedIn>
          <div class="logged-in-alert">
            <p>Ya tienes una sesión activa como: <strong>{{ getUsername() }}</strong></p>
            <button class="btn btn-primary" (click)="goToHome()">Ir al Inicio</button>
          </div>
        </ng-template>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      min-height: 70vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
    }
    .login-box {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 16px;
      padding: 2.5rem;
      max-width: 440px;
      width: 100%;
      text-align: center;
      box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
    }
    .logo {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 72px;
      height: 72px;
      margin-bottom: 1rem;
      border-radius: 50%;
      background: #e0f2fe;
      color: #0369a1;
      font-weight: 800;
      font-size: 0.9rem;
      letter-spacing: 0.04em;
    }
    h2 { color: #0f172a; margin-bottom: 0.5rem; }
    .desc { color: #64748b; font-size: 0.95rem; margin-bottom: 2rem; line-height: 1.5; }
    .btn {
      width: 100%;
      padding: 0.75rem 1.25rem;
      border-radius: 8px;
      font-weight: 600;
      font-size: 1rem;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.75rem;
      border: none;
      transition: all 0.2s;
    }
    .btn-idaas {
      background: #0f172a;
      color: #fff;
    }
    .btn-idaas:hover { background: #1e293b; }
    .idaas-icon {
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.04em;
      background: #0284c7;
      color: #fff;
      border-radius: 4px;
      padding: 0.2rem 0.4rem;
    }
    .btn-primary { background: #0284c7; color: #fff; margin-top: 1rem; }
    .btn-primary:hover { background: #0369a1; }
    .logged-in-alert {
      background: #f0fdf4;
      border: 1px solid #bbf7d0;
      padding: 1rem;
      border-radius: 8px;
      color: #166534;
    }
  `]
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  isLoggedIn(): boolean {
    return this.authService.isAuthenticated();
  }

  getUsername(): string {
    return this.authService.getUsername();
  }

  login(): void {
    void this.authService.login();
  }

  goToHome(): void {
    this.router.navigate(['/home']);
  }
}
