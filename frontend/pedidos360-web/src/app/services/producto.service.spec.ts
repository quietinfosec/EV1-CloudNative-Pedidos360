import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Producto } from '../models/models';
import { ProductoService } from './producto.service';
import { environment } from '../../environments/environment';

describe('ProductoService', () => {
  let service: ProductoService;
  let http: HttpTestingController;
  const apiUrl = `${environment.apiBaseUrl}/productos`;
  const producto: Producto = {
    nombre: 'Teclado',
    precio: 24990,
    stock: 10,
    activo: true
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ProductoService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(ProductoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists active productos by default', () => {
    service.getProductos().subscribe((productos) => expect(productos).toEqual([producto]));

    const req = http.expectOne((request) => request.url === apiUrl && request.params.get('soloActivos') === 'true');
    expect(req.request.method).toBe('GET');
    req.flush([producto]);
  });

  it('allows listing inactive productos', () => {
    service.getProductos(false).subscribe();

    const req = http.expectOne((request) => request.url === apiUrl && request.params.get('soloActivos') === 'false');
    expect(req.request.method).toBe('GET');
    req.flush([producto]);
  });

  it('gets, creates, updates and deletes a producto', () => {
    service.getProductoById(1).subscribe();
    http.expectOne(`${apiUrl}/1`).flush({ ...producto, id: 1 });

    service.crearProducto(producto).subscribe();
    const create = http.expectOne(apiUrl);
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(producto);
    create.flush({ ...producto, id: 1 });

    service.actualizarProducto(1, producto).subscribe();
    const update = http.expectOne(`${apiUrl}/1`);
    expect(update.request.method).toBe('PUT');
    expect(update.request.body).toEqual(producto);
    update.flush({ ...producto, id: 1 });

    service.eliminarProducto(1).subscribe();
    const remove = http.expectOne(`${apiUrl}/1`);
    expect(remove.request.method).toBe('DELETE');
    remove.flush(null);
  });
});
