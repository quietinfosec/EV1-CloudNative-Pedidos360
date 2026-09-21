import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="login-container">
      <div class="login-box">
        <div class="logo">🔐</div>
        <h2>Acceso a Pedidos360</h2>
        <p class="desc">Inicia sesión con tu cuenta institucional de Microsoft Entra ID para acceder al sistema.</p>

        <div *ngIf="!isLoggedIn(); else alreadyLoggedIn">
          <button class="btn btn-microsoft" (click)="login()">
            <svg class="ms-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 23 23">
              <path fill="#f35325" d="M1 1h10v10H1z"/>
              <path fill="#81bc06" d="M12 1h10v10H12z"/>
              <path fill="#05a6f0" d="M1 12h10v10H1z"/>
              <path fill="#ffba08" d="M12 12h10v10H12z"/>
            </svg>
            Iniciar sesión con Microsoft
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
    .logo { font-size: 2.5rem; margin-bottom: 1rem; }
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
    .btn-microsoft {
      background: #2f2f2f;
      color: #fff;
    }
    .btn-microsoft:hover { background: #1a1a1a; }
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
  private readonly msalService = inject(MsalService);
  private readonly router = inject(Router);

  isLoggedIn(): boolean {
    return this.msalService.instance.getAllAccounts().length > 0;
  }

  getUsername(): string {
    const accounts = this.msalService.instance.getAllAccounts();
    return accounts.length > 0 ? (accounts[0].name || accounts[0].username) : '';
  }

  login(): void {
    this.msalService.loginRedirect();
  }

  goToHome(): void {
    this.router.navigate(['/home']);
  }
}
