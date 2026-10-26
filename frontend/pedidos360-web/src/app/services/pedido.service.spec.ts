import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Pedido } from '../models/models';
import { PedidoService } from './pedido.service';
import { environment } from '../../environments/environment';

describe('PedidoService', () => {
  let service: PedidoService;
  let http: HttpTestingController;
  const apiUrl = `${environment.apiBaseUrl}/pedidos`;
  const pedido: Pedido = {
    usuario: 'ana',
    estado: 'PENDIENTE',
    total: 12990
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PedidoService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(PedidoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists pedidos without filter', () => {
    service.getPedidos().subscribe((pedidos) => expect(pedidos).toEqual([pedido]));

    const req = http.expectOne(apiUrl);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('usuario')).toBe(false);
    req.flush([pedido]);
  });

  it('lists pedidos filtered by usuario', () => {
    service.getPedidos('ana').subscribe();

    const req = http.expectOne((request) => request.url === apiUrl && request.params.get('usuario') === 'ana');
    expect(req.request.method).toBe('GET');
    req.flush([pedido]);
  });

  it('creates a pedido', () => {
    service.crearPedido(pedido).subscribe((created) => expect(created).toEqual({ ...pedido, id: 1 }));

    const req = http.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(pedido);
    req.flush({ ...pedido, id: 1 });
  });

  it('updates and deletes a pedido', () => {
    service.actualizarPedido(1, pedido).subscribe();
    const update = http.expectOne(`${apiUrl}/1`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(pedido);
    update.flush({ ...pedido, id: 1 });

    service.eliminarPedido(1).subscribe();
    const remove = http.expectOne(`${apiUrl}/1`);
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });
});
