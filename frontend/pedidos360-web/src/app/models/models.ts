export interface Producto {
  id?: number;
  nombre: string;
  descripcion?: string;
  precio: number;
  stock: number;
  activo?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export type EstadoPedido = 'PENDIENTE' | 'CONFIRMADO' | 'EN_PROCESO' | 'COMPLETADO' | 'CANCELADO';

export interface Pedido {
  id?: number;
  usuario: string;
  fecha?: string;
  estado: EstadoPedido;
  total: number;
  createdAt?: string;
  updatedAt?: string;
}
