import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="home-container">
      <header class="hero">
        <h1>Bienvenido a <span>Pedidos360</span></h1>
        <p class="subtitle">Plataforma Cloud Native de Gestión de Pedidos y Catálogo de Productos</p>
      </header>

      <div class="cards-grid">
        <div class="card">
          <div class="card-icon">🛍️</div>
          <h3>Catálogo de Productos</h3>
          <p>Consulta el stock en tiempo real, precios y descripciones sincronizadas con Amazon RDS.</p>
          <a routerLink="/productos" class="card-link">Ver Productos &rarr;</a>
        </div>

        <div class="card">
          <div class="card-icon">📋</div>
          <h3>Gestión de Pedidos</h3>
          <p>Administra los pedidos de clientes, monitorea estados y totales de forma segura.</p>
          <a routerLink="/pedidos" class="card-link">Ir a Pedidos &rarr;</a>
        </div>

        <div class="card">
          <div class="card-icon">🔐</div>
          <h3>Seguridad & Entra ID</h3>
          <p>Autenticación federada mediante OAuth2/OIDC con Microsoft Entra ID y tokens JWT en BFF.</p>
          <a routerLink="/perfil" class="card-link">Ver Mi Perfil &rarr;</a>
        </div>
      </div>

      <div class="arch-banner">
        <h4>☁️ Arquitectura Cloud Native en Producción</h4>
        <p>Frontend en Angular &rarr; AWS API Gateway &rarr; VPC Link &rarr; Internal ALB &rarr; BFF Spring Boot &rarr; Microservicios &rarr; Amazon RDS PostgreSQL</p>
      </div>
    </div>
  `,
  styles: [`
    .home-container {
      max-width: 1100px;
      margin: 2rem auto;
      padding: 0 1rem;
    }
    .hero {
      text-align: center;
      margin-bottom: 3rem;
    }
    .hero h1 {
      font-size: 2.5rem;
      font-weight: 800;
      color: #0f172a;
      margin-bottom: 0.5rem;
    }
    .hero h1 span {
      color: #0284c7;
    }
    .subtitle {
      font-size: 1.15rem;
      color: #64748b;
    }
    .cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 1.5rem;
      margin-bottom: 3rem;
    }
    .card {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 1.75rem;
      box-shadow: 0 2px 4px rgba(0,0,0,0.03);
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      transition: transform 0.2s, box-shadow 0.2s;
    }
    .card:hover {
      transform: translateY(-4px);
      box-shadow: 0 10px 15px -3px rgba(0,0,0,0.08);
    }
    .card-icon { font-size: 2.2rem; margin-bottom: 0.75rem; }
    .card h3 {
      font-size: 1.3rem;
      color: #0f172a;
      margin-bottom: 0.5rem;
    }
    .card p {
      color: #64748b;
      line-height: 1.5;
      margin-bottom: 1.25rem;
    }
    .card-link {
      color: #0284c7;
      font-weight: 600;
      text-decoration: none;
    }
    .card-link:hover { text-decoration: underline; }
    .arch-banner {
      background: #f0f9ff;
      border: 1px solid #bae6fd;
      border-radius: 10px;
      padding: 1.25rem;
      text-align: center;
    }
    .arch-banner h4 {
      color: #0369a1;
      margin-bottom: 0.4rem;
    }
    .arch-banner p {
      color: #0c4a6e;
      font-size: 0.95rem;
      margin: 0;
    }
  `]
})
export class HomeComponent {}
