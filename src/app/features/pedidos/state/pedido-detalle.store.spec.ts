import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PedidoRepository } from '@core/domain/ports/pedido.repository';
import { AppError } from '@core/domain/errors/app-error';
import { unPedido } from '../../../../testing/datos-prueba';
import { PedidoDetalleStore } from './pedido-detalle.store';

describe('PedidoDetalleStore', () => {
  let repo: jasmine.SpyObj<PedidoRepository>;
  let store: PedidoDetalleStore;

  beforeEach(() => {
    repo = jasmine.createSpyObj<PedidoRepository>('PedidoRepository', [
      'obtenerPorId',
      'confirmar',
      'cancelar',
      'completar',
    ]);
    TestBed.configureTestingModule({
      providers: [PedidoDetalleStore, { provide: PedidoRepository, useValue: repo }],
    });
    store = TestBed.inject(PedidoDetalleStore);
  });

  it('completar actualiza el pedido en pantalla', async () => {
    repo.obtenerPorId.and.returnValue(of(unPedido({ estado: 'CONFIRMADO' })));
    repo.completar.and.returnValue(of(unPedido({ estado: 'COMPLETADO' })));
    store.cargar(1);

    await store.ejecutar('completar');

    expect(store.pedido()?.estado).toBe('COMPLETADO');
    expect(store.procesando()).toBeNull();
  });

  it('ante un 409 guarda el error y recarga el pedido (pudo cambiarlo otro usuario)', async () => {
    repo.obtenerPorId.and.returnValue(of(unPedido()));
    repo.confirmar.and.returnValue(
      throwError(() => new AppError('CONFLICTO', 'Stock insuficiente', 409)),
    );
    store.cargar(1);

    await expectAsync(store.ejecutar('confirmar')).toBeRejected();

    expect(store.errorAccion()?.message).toBe('Stock insuficiente');
    expect(repo.obtenerPorId).toHaveBeenCalledTimes(2);
  });
});
