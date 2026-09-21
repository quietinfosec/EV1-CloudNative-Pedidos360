import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoService } from '../services/producto.service';
import { Producto } from '../models/models';

@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container">
      <div class="header-actions">
        <div>
          <h2>Catálogo de Productos</h2>
          <p class="subtitle">Gestión sincronizada con Amazon RDS PostgreSQL</p>
        </div>
        <button class="btn btn-primary" (click)="abrirModalNuevo()">+ Nuevo Producto</button>
      </div>

      <div *ngIf="cargando" class="loading">Cargando productos desde el backend...</div>

      <div *ngIf="errorMsg" class="alert alert-danger">{{ errorMsg }}</div>
      <div *ngIf="successMsg" class="alert alert-success">{{ successMsg }}</div>

      <div *ngIf="!cargando && productos.length === 0" class="empty-state">
        <p>No hay productos registrados actualmente.</p>
      </div>

      <div class="grid" *ngIf="!cargando && productos.length > 0">
        <div class="product-card" *ngFor="let p of productos">
          <div class="card-header">
            <h3>{{ p.nombre }}</h3>
            <span class="badge" [class.badge-active]="p.activo" [class.badge-inactive]="!p.activo">
              {{ p.activo ? 'Activo' : 'Inactivo' }}
            </span>
          </div>
          <p class="desc">{{ p.descripcion || 'Sin descripción' }}</p>
          <div class="details">
            <span class="price">\${{ p.precio | number:'1.2-2' }}</span>
            <span class="stock">Stock: {{ p.stock }}</span>
          </div>
          <div class="card-actions">
            <button class="btn btn-sm btn-outline" (click)="editar(p)">Editar</button>
            <button class="btn btn-sm btn-danger" (click)="eliminar(p.id!)">Desactivar</button>
          </div>
        </div>
      </div>

      <!-- Modal de Formulario -->
      <div class="modal" *ngIf="modalVisible">
        <div class="modal-content">
          <h3>{{ productoEditando.id ? 'Editar Producto' : 'Nuevo Producto' }}</h3>
          <form (ngSubmit)="guardar()">
            <div class="form-group">
              <label>Nombre *</label>
              <input type="text" [(ngModel)]="productoEditando.nombre" name="nombre" required placeholder="Ej. Monitor 27 pulgadas" />
            </div>
            <div class="form-group">
              <label>Descripción</label>
              <textarea [(ngModel)]="productoEditando.descripcion" name="descripcion" rows="3" placeholder="Detalles del producto..."></textarea>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>Precio *</label>
                <input type="number" step="0.01" min="0" [(ngModel)]="productoEditando.precio" name="precio" required />
              </div>
              <div class="form-group">
                <label>Stock *</label>
                <input type="number" min="0" [(ngModel)]="productoEditando.stock" name="stock" required />
              </div>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn btn-outline" (click)="cerrarModal()">Cancelar</button>
              <button type="submit" class="btn btn-primary">Guardar</button>
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
    .btn-danger { background: #ef4444; color: #fff; }
    .btn-danger:hover { background: #dc2626; }
    .btn-sm { padding: 0.35rem 0.65rem; font-size: 0.85rem; }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1.5rem; }
    .product-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 1.25rem; display: flex; flex-direction: column; justify-content: space-between; }
    .card-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.5rem; }
    .card-header h3 { margin: 0; font-size: 1.15rem; color: #0f172a; }
    .badge { font-size: 0.75rem; padding: 2px 6px; border-radius: 4px; font-weight: 700; }
    .badge-active { background: #dcfce7; color: #166534; }
    .badge-inactive { background: #fee2e2; color: #991b1b; }
    .desc { color: #64748b; font-size: 0.9rem; line-height: 1.4; margin-bottom: 1rem; }
    .details { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; font-weight: 600; }
    .price { color: #0f172a; font-size: 1.2rem; }
    .stock { color: #64748b; font-size: 0.9rem; }
    .card-actions { display: flex; gap: 0.5rem; border-top: 1px solid #f1f5f9; padding-top: 0.75rem; }
    .modal { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; padding: 1rem; }
    .modal-content { background: #fff; padding: 2rem; border-radius: 12px; max-width: 480px; width: 100%; }
    .form-group { margin-bottom: 1rem; }
    .form-group label { display: block; font-size: 0.85rem; font-weight: 600; margin-bottom: 0.35rem; color: #334155; }
    .form-group input, .form-group textarea { width: 100%; padding: 0.5rem; border: 1px solid #cbd5e1; border-radius: 6px; }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    .modal-actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem; }
    .alert { padding: 0.75rem; border-radius: 6px; margin-bottom: 1.5rem; }
    .alert-danger { background: #fee2e2; color: #991b1b; }
    .alert-success { background: #dcfce7; color: #166534; }
    .loading, .empty-state { text-align: center; color: #64748b; padding: 3rem 0; }
  `]
})
export class ProductosComponent implements OnInit {
  private readonly productoService = inject(ProductoService);

  productos: Producto[] = [];
  cargando = true;
  errorMsg = '';
  successMsg = '';

  modalVisible = false;
  productoEditando: Producto = { nombre: '', precio: 0, stock: 0 };

  ngOnInit(): void {
    this.cargarProductos();
  }

  cargarProductos(): void {
    this.cargando = true;
    this.errorMsg = '';
    this.productoService.getProductos(true).subscribe({
      next: (data) => {
        this.productos = data;
        this.cargando = false;
      },
      error: (err) => {
        this.errorMsg = 'Error al cargar productos desde el servidor backend/RDS.';
        this.cargando = false;
      }
    });
  }

  abrirModalNuevo(): void {
    this.productoEditando = { nombre: '', descripcion: '', precio: 0, stock: 0 };
    this.modalVisible = true;
  }

  editar(p: Producto): void {
    this.productoEditando = { ...p };
    this.modalVisible = true;
  }

  cerrarModal(): void {
    this.modalVisible = false;
  }

  guardar(): void {
    if (this.productoEditando.id) {
      this.productoService.actualizarProducto(this.productoEditando.id, this.productoEditando).subscribe({
        next: () => {
          this.successMsg = 'Producto actualizado exitosamente.';
          this.cerrarModal();
          this.cargarProductos();
        },
        error: () => {
          this.errorMsg = 'Error al actualizar producto.';
        }
      });
    } else {
      this.productoService.crearProducto(this.productoEditando).subscribe({
        next: () => {
          this.successMsg = 'Producto creado exitosamente.';
          this.cerrarModal();
          this.cargarProductos();
        },
        error: () => {
          this.errorMsg = 'Error al crear producto. Verifica que estés autenticado.';
        }
      });
    }
  }

  eliminar(id: number): void {
    if (confirm('¿Deseas desactivar este producto?')) {
      this.productoService.eliminarProducto(id).subscribe({
        next: () => {
          this.successMsg = 'Producto desactivado.';
          this.cargarProductos();
        },
        error: () => {
          this.errorMsg = 'Error al desactivar el producto.';
        }
      });
    }
  }
}
