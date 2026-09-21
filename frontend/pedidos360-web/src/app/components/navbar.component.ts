import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar">
      <div class="nav-container">
        <a routerLink="/home" class="brand">
          <span class="brand-icon">📦</span>
          <span class="brand-text">Pedidos<strong>360</strong></span>
        </a>

        <div class="nav-links">
          <a routerLink="/home" routerLinkActive="active">Inicio</a>
          <a routerLink="/productos" routerLinkActive="active">Productos</a>
          <a routerLink="/pedidos" routerLinkActive="active">Pedidos</a>
          <a routerLink="/perfil" routerLinkActive="active">Mi Perfil</a>
        </div>

        <div class="nav-auth">
          <ng-container *ngIf="isLoggedIn(); else loginBtn">
            <span class="user-badge">{{ getUsername() }}</span>
            <button class="btn btn-outline" (click)="logout()">Cerrar Sesion</button>
          </ng-container>
          <ng-template #loginBtn>
            <button class="btn btn-primary" (click)="login()">Iniciar Sesion</button>
          </ng-template>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      background: #0f172a;
      color: #fff;
      padding: 0.75rem 1.5rem;
      border-bottom: 2px solid #0284c7;
    }
    .nav-container {
      max-width: 1200px;
      margin: 0 auto;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      color: #fff;
      text-decoration: none;
      font-size: 1.25rem;
      font-weight: 700;
    }
    .brand-icon { font-size: 1.5rem; }
    .brand strong { color: #38bdf8; }
    .nav-links {
      display: flex;
      gap: 1rem;
    }
    .nav-links a {
      color: #94a3b8;
      text-decoration: none;
      font-weight: 500;
      padding: 0.4rem 0.8rem;
      border-radius: 6px;
      transition: all 0.2s;
    }
    .nav-links a:hover, .nav-links a.active {
      color: #fff;
      background: #1e293b;
    }
    .nav-auth {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .user-badge {
      font-size: 0.85rem;
      color: #38bdf8;
      background: #1e293b;
      padding: 0.35rem 0.75rem;
      border-radius: 9999px;
      border: 1px solid #334155;
    }
    .btn {
      padding: 0.45rem 1rem;
      border-radius: 6px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s;
    }
    .btn-primary {
      background: #0284c7;
      color: #fff;
    }
    .btn-primary:hover { background: #0369a1; }
    .btn-outline {
      background: transparent;
      color: #cbd5e1;
      border: 1px solid #475569;
    }
    .btn-outline:hover {
      background: #334155;
      color: #fff;
    }
  `]
})
export class NavbarComponent {
  private readonly msalService = inject(MsalService);

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

  logout(): void {
    this.msalService.logoutRedirect();
  }
}
