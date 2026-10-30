import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { PedidoRepository } from '../../../domain/ports/pedido.repository';
import { AppError } from '../../../domain/errors/app-error';
import { unPedido } from '../../../../../testing/datos-prueba';
import { ConfirmarPedidoUseCase } from './confirmar-pedido.use-case';
import { CancelarPedidoUseCase } from './cancelar-pedido.use-case';
import { CompletarPedidoUseCase } from './completar-pedido.use-case';

describe('Casos de uso de cambio de estado', () => {
  let repo: jasmine.SpyObj<PedidoRepository>;

  beforeEach(() => {
    repo = jasmine.createSpyObj<PedidoRepository>('PedidoRepository', [
      'confirmar',
      'cancelar',
      'completar',
    ]);
    repo.confirmar.and.returnValue(of(unPedido({ estado: 'CONFIRMADO' })));
    repo.cancelar.and.returnValue(of(unPedido({ estado: 'CANCELADO' })));
    repo.completar.and.returnValue(of(unPedido({ estado: 'COMPLETADO' })));
    TestBed.configureTestingModule({ providers: [{ provide: PedidoRepository, useValue: repo }] });
  });

  it('confirma un BORRADOR', () => {
    TestBed.inject(ConfirmarPedidoUseCase).execute(unPedido()).subscribe();
    expect(repo.confirmar).toHaveBeenCalledWith(1);
  });

  it('cancela un CONFIRMADO', () => {
    TestBed.inject(CancelarPedidoUseCase).execute(unPedido({ estado: 'CONFIRMADO' })).subscribe();
    expect(repo.cancelar).toHaveBeenCalledWith(1);
  });

  it('completa un CONFIRMADO', () => {
    let resultado = '';
    TestBed.inject(CompletarPedidoUseCase)
      .execute(unPedido({ estado: 'CONFIRMADO' }))
      .subscribe((p) => (resultado = p.estado));
    expect(repo.completar).toHaveBeenCalledWith(1);
    expect(resultado).toBe('COMPLETADO');
  });

  it('no completa un BORRADOR: error 409 local sin llamar a la API', () => {
    let error: AppError | undefined;
    TestBed.inject(CompletarPedidoUseCase)
      .execute(unPedido({ estado: 'BORRADOR' }))
      .subscribe({ error: (e) => (error = e) });
    expect(repo.completar).not.toHaveBeenCalled();
    expect(error?.tipo).toBe('CONFLICTO');
    expect(error?.message).toBe(
      'El pedido no puede cambiar a Completado porque esta en estado Borrador',
    );
  });

  it('escenario E: no confirma un pedido CANCELADO', () => {
    let error: AppError | undefined;
    TestBed.inject(ConfirmarPedidoUseCase)
      .execute(unPedido({ estado: 'CANCELADO' }))
      .subscribe({ error: (e) => (error = e) });
    expect(repo.confirmar).not.toHaveBeenCalled();
    expect(error?.status).toBe(409);
  });

  it('escenario D: no cancela dos veces', () => {
    let error: AppError | undefined;
    TestBed.inject(CancelarPedidoUseCase)
      .execute(unPedido({ estado: 'CANCELADO' }))
      .subscribe({ error: (e) => (error = e) });
    expect(repo.cancelar).not.toHaveBeenCalled();
    expect(error?.tipo).toBe('CONFLICTO');
  });
});
