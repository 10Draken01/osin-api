import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_BASE_URL } from '../api-config';
import { PedidoHttpRepository } from './pedido-http.repository';
import { PedidoResponseDto } from '../dto/pedido.dto';
import { problemaInterceptor } from '../../interceptors/problema.interceptor';
import { AppError } from '../../../domain/errors/app-error';
import { Pedido } from '../../../domain/models/pedido.model';

const DTO: PedidoResponseDto = {
  id: 2,
  folio: 'PED-000002',
  estado: 'CONFIRMADO',
  total: 158,
  fechaCreacion: '2026-10-30T07:41:12.123456',
  fechaActualizacion: '2026-10-30T07:45:00',
  fechaCancelacion: null,
  productos: [
    { idProducto: 3, nombre: 'Aceite 1 L', cantidad: 2, precioUnitario: 45, subtotal: 90 },
    { idProducto: 6, nombre: 'Cafe 250 g', cantidad: 1, precioUnitario: 68, subtotal: 68 },
  ],
};

describe('PedidoHttpRepository', () => {
  let repo: PedidoHttpRepository;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PedidoHttpRepository,
        provideHttpClient(withInterceptors([problemaInterceptor])),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: '/api' },
      ],
    });
    repo = TestBed.inject(PedidoHttpRepository);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista con filtros y paginación y mapea el formato de Spring (content + page)', () => {
    let resultado: Pedido[] = [];
    repo
      .listar({ estado: 'CONFIRMADO', folio: 'PED-0000' }, { pagina: 1, tamano: 10 })
      .subscribe((p) => (resultado = [...p.elementos]));

    const req = http.expectOne(
      (r) =>
        r.url === '/api/pedidos' &&
        r.params.get('estado') === 'CONFIRMADO' &&
        r.params.get('folio') === 'PED-0000' &&
        r.params.get('pagina') === '1' &&
        r.params.get('tamano') === '10',
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [DTO], page: { size: 10, number: 1, totalElements: 11, totalPages: 2 } });

    expect(resultado[0].partidas.length).toBe(2);
    expect(resultado[0].fechaCreacion.getMilliseconds()).toBe(123);
  });

  it('no envía filtros vacíos', () => {
    repo.listar({ estado: null, folio: null }, { pagina: 0, tamano: 10 }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/pedidos');
    expect(req.request.params.has('estado')).toBeFalse();
    expect(req.request.params.has('folio')).toBeFalse();
    req.flush({ content: [], page: { size: 10, number: 0, totalElements: 0, totalPages: 0 } });
  });

  it('crea el pedido con el cuerpo { productos: [{ id, cantidad }] }', () => {
    repo.registrar([{ idProducto: 5, cantidad: 2 }]).subscribe();
    const req = http.expectOne('/api/pedidos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ productos: [{ id: 5, cantidad: 2 }] });
    req.flush({ ...DTO, estado: 'BORRADOR' });
  });

  for (const accion of ['confirmar', 'cancelar', 'completar'] as const) {
    it(`${accion}: POST /api/pedidos/{id}/${accion} sin cuerpo`, () => {
      repo[accion](2).subscribe();
      const req = http.expectOne(`/api/pedidos/2/${accion}`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toBeNull();
      req.flush(DTO);
    });
  }

  it('convierte el 409 de stock insuficiente en AppError CONFLICTO con el detail del servidor', () => {
    let error: AppError | undefined;
    repo.confirmar(4).subscribe({ error: (e) => (error = e) });
    http.expectOne('/api/pedidos/4/confirmar').flush(
      {
        status: 409,
        detail: "Stock insuficiente para el producto 'Cafe 250 g': disponible 40, solicitado 41",
      },
      { status: 409, statusText: 'Conflict' },
    );
    expect(error).toEqual(jasmine.any(AppError));
    expect(error?.tipo).toBe('CONFLICTO');
    expect(error?.message).toContain('disponible 40, solicitado 41');
  });
});
