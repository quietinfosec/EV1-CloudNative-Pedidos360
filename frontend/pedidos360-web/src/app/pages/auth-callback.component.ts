import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-auth-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="callback-container">
      <div class="callback-box" *ngIf="!errorMessage; else errorBlock">
        <div class="spinner"></div>
        <h2>Completando inicio de sesión</h2>
        <p>Validando el código de autorización con el IDaaS...</p>
      </div>

      <ng-template #errorBlock>
        <div class="callback-box">
          <h2>No se pudo completar el inicio de sesión</h2>
          <p class="error">{{ errorMessage }}</p>
          <button class="btn" type="button" (click)="retry()">Volver a intentar</button>
        </div>
      </ng-template>
    </div>
  `,
  styles: [`
    .callback-container {
      min-height: 70vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
    }
    .callback-box {
      background: #fff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 2.5rem;
      max-width: 460px;
      width: 100%;
      text-align: center;
      box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
    }
    h2 { color: #0f172a; margin-bottom: 0.5rem; }
    p { color: #64748b; }
    .error { color: #b91c1c; word-break: break-word; }
    .spinner {
      width: 42px;
      height: 42px;
      margin: 0 auto 1.25rem;
      border: 4px solid #e0f2fe;
      border-top-color: #0284c7;
      border-radius: 50%;
      animation: spin 0.9s linear infinite;
    }
    .btn {
      margin-top: 1rem;
      padding: 0.65rem 1.2rem;
      border: none;
      border-radius: 8px;
      background: #0284c7;
      color: #fff;
      font-weight: 600;
      cursor: pointer;
    }
    .btn:hover { background: #0369a1; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class AuthCallbackComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  errorMessage = '';

  async ngOnInit(): Promise<void> {
    try {
      await this.authService.completeLogin();
      await this.router.navigateByUrl('/home');
    } catch (error) {
      this.errorMessage = error instanceof Error ? error.message : 'Error desconocido al validar el token.';
    }
  }

  retry(): void {
    void this.authService.login();
  }
}
