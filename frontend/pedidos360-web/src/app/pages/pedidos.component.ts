import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MsalService } from '@azure/msal-angular';
import { PedidoService } from '../services/pedido.service';
import { Pedido, EstadoPedido } from '../models/models';

@Component({
  selector: 'app-pedidos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container">
      <div class="header-actions">
        <div>
          <h2>Gestión de Pedidos</h2>
          <p class="subtitle">Órdenes registradas en tiempo real en la base de datos</p>
        </div>
        <button class="btn btn-primary" (click)="abrirModalNuevo()">+ Crear Pedido</button>
      </div>

      <div *ngIf="cargando" class="loading">Cargando pedidos...</div>
      <div *ngIf="errorMsg" class="alert alert-danger">{{ errorMsg }}</div>
      <div *ngIf="successMsg" class="alert alert-success">{{ successMsg }}</div>

      <div *ngIf="!cargando && pedidos.length === 0" class="empty-state">
        <p>No existen pedidos registrados para tu cuenta.</p>
      </div>

      <table class="table" *ngIf="!cargando && pedidos.length > 0">
        <thead>
          <tr>
            <th>ID</th>
            <th>Usuario</th>
            <th>Fecha</th>
            <th>Estado</th>
            <th>Total</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let p of pedidos">
            <td>#{{ p.id }}</td>
            <td>{{ p.usuario }}</td>
            <td>{{ p.fecha | date:'short' }}</td>
            <td>
              <span class="badge" [ngClass]="'badge-' + p.estado.toLowerCase()">
                {{ p.estado }}
              </span>
            </td>
            <td class="font-bold">\${{ p.total | number:'1.2-2' }}</td>
            <td>
              <select [(ngModel)]="p.estado" (change)="cambiarEstado(p)" class="select-sm">
                <option value="PENDIENTE">PENDIENTE</option>
                <option value="CONFIRMADO">CONFIRMADO</option>
                <option value="EN_PROCESO">EN_PROCESO</option>
                <option value="COMPLETADO">COMPLETADO</option>
                <option value="CANCELADO">CANCELADO</option>
              </select>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Modal Crear Pedido -->
      <div class="modal" *ngIf="modalVisible">
        <div class="modal-content">
          <h3>Crear Nuevo Pedido</h3>
          <form (ngSubmit)="guardar()">
            <div class="form-group">
              <label>Usuario / Cliente *</label>
              <input type="text" [(ngModel)]="nuevoPedido.usuario" name="usuario" required />
            </div>
            <div class="form-group">
              <label>Monto Total (\$) *</label>
              <input type="number" step="0.01" min="0" [(ngModel)]="nuevoPedido.total" name="total" required />
            </div>
            <div class="form-group">
              <label>Estado Inicial</label>
              <select [(ngModel)]="nuevoPedido.estado" name="estado" class="select">
                <option value="PENDIENTE">PENDIENTE</option>
                <option value="CONFIRMADO">CONFIRMADO</option>
              </select>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn btn-outline" (click)="cerrarModal()">Cancelar</button>
              <button type="submit" class="btn btn-primary">Registrar Pedido</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .container { max-width: 1100px; margin: 2rem auto; padding: 0 1rem; }
    .header-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
    h2 { margin: 0; color: #0f172a; }
    .subtitle { color: #64748b; margin-top: 0.25rem; }
    .btn { padding: 0.5rem 1rem; border-radius: 6px; font-weight: 600; cursor: pointer; border: none; }
    .btn-primary { background: #0284c7; color: #fff; }
    .btn-primary:hover { background: #0369a1; }
    .btn-outline { background: transparent; border: 1px solid #cbd5e1; color: #475569; }
    .table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
    .table th, .table td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #e2e8f0; }
    .table th { background: #f8fafc; font-weight: 600; color: #475569; font-size: 0.85rem; }
    .font-bold { font-weight: 700; color: #0f172a; }
    .badge { font-size: 0.75rem; padding: 3px 8px; border-radius: 4px; font-weight: 700; }
    .badge-pendiente { background: #fef3c7; color: #b45309; }
    .badge-confirmado { background: #e0f2fe; color: #0369a1; }
    .badge-en_proceso { background: #f3e8ff; color: #7e22ce; }
    .badge-completado { background: #dcfce7; color: #15803d; }
    .badge-cancelado { background: #fee2e2; color: #b91c1c; }
    .select-sm { padding: 0.25rem 0.5rem; border-radius: 4px; border: 1px solid #cbd5e1; font-size: 0.85rem; }
    .modal { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; padding: 1rem; }
    .modal-content { background: #fff; padding: 2rem; border-radius: 12px; max-width: 440px; width: 100%; }
    .form-group { margin-bottom: 1rem; }
    .form-group label { display: block; font-size: 0.85rem; font-weight: 600; margin-bottom: 0.35rem; color: #334155; }
    .form-group input, .select { width: 100%; padding: 0.5rem; border: 1px solid #cbd5e1; border-radius: 6px; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem; }
    .alert { padding: 0.75rem; border-radius: 6px; margin-bottom: 1.5rem; }
    .alert-danger { background: #fee2e2; color: #991b1b; }
    .alert-success { background: #dcfce7; color: #166534; }
    .loading, .empty-state { text-align: center; color: #64748b; padding: 3rem 0; }
  `]
})
export class PedidosComponent implements OnInit {
  private readonly pedidoService = inject(PedidoService);
  private readonly msalService = inject(MsalService);

  pedidos: Pedido[] = [];
  cargando = true;
  errorMsg = '';
  successMsg = '';

  modalVisible = false;
  nuevoPedido: Pedido = { usuario: '', total: 0, estado: 'PENDIENTE' };

  ngOnInit(): void {
    this.cargarPedidos();
  }

  getCurrentUser(): string {
    const accounts = this.msalService.instance.getAllAccounts();
    return accounts.length > 0 ? (accounts[0].username || accounts[0].name || 'usuario_demo') : 'usuario_demo';
  }

  cargarPedidos(): void {
    this.cargando = true;
    this.errorMsg = '';
    this.pedidoService.getPedidos().subscribe({
      next: (data) => {
        this.pedidos = data;
        this.cargando = false;
      },
      error: (err) => {
        this.errorMsg = 'Error al consultar pedidos. Requiere autenticación JWT en BFF.';
        this.cargando = false;
      }
    });
  }

  abrirModalNuevo(): void {
    this.nuevoPedido = { usuario: this.getCurrentUser(), total: 0, estado: 'PENDIENTE' };
    this.modalVisible = true;
  }

  cerrarModal(): void {
    this.modalVisible = false;
  }

  guardar(): void {
    this.pedidoService.crearPedido(this.nuevoPedido).subscribe({
      next: () => {
        this.successMsg = 'Pedido registrado exitosamente.';
        this.cerrarModal();
        this.cargarPedidos();
      },
      error: () => {
        this.errorMsg = 'Error al registrar pedido. Verifica tu sesión.';
      }
    });
  }

  cambiarEstado(pedido: Pedido): void {
    if (!pedido.id) return;
    this.pedidoService.actualizarPedido(pedido.id, pedido).subscribe({
      next: () => {
        this.successMsg = `Estado del pedido #${pedido.id} actualizado.`;
      },
      error: () => {
        this.errorMsg = 'No fue posible actualizar el estado del pedido.';
      }
    });
  }
}
