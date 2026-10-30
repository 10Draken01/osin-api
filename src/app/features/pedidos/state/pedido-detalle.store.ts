import { Injectable, inject, signal } from '@angular/core';
import { Observable, finalize } from 'rxjs';
import { Pedido } from '@core/domain/models/pedido.model';
import { AppError } from '@core/domain/errors/app-error';
import { AccionPedido } from '@core/domain/rules/estado-pedido.rules';
import { ObtenerPedidoUseCase } from '@core/application/use-cases/pedidos/obtener-pedido.use-case';
import { ConfirmarPedidoUseCase } from '@core/application/use-cases/pedidos/confirmar-pedido.use-case';
import { CancelarPedidoUseCase } from '@core/application/use-cases/pedidos/cancelar-pedido.use-case';
import { CompletarPedidoUseCase } from '@core/application/use-cases/pedidos/completar-pedido.use-case';

/** Estado del detalle de un pedido y de sus cambios de estado. */
@Injectable()
export class PedidoDetalleStore {
  private readonly obtener = inject(ObtenerPedidoUseCase);
  private readonly casos: Readonly<Record<AccionPedido, { execute(p: Pedido): Observable<Pedido> }>> = {
    confirmar: inject(ConfirmarPedidoUseCase),
    cancelar: inject(CancelarPedidoUseCase),
    completar: inject(CompletarPedidoUseCase),
  };

  readonly pedido = signal<Pedido | null>(null);
  readonly cargando = signal(false);
  readonly error = signal<AppError | null>(null);
  readonly procesando = signal<AccionPedido | null>(null);
  readonly errorAccion = signal<AppError | null>(null);

  cargar(id: number): void {
    this.cargando.set(true);
    this.error.set(null);
    this.obtener
      .execute(id)
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (p) => this.pedido.set(p),
        error: (e: unknown) => this.error.set(AppError.desde(e)),
      });
  }

  /**
   * Ejecuta confirmar/cancelar/completar. Si el servidor responde 409 (stock insuficiente,
   * transición no válida u otro usuario lo modificó) se recarga el pedido para mostrar
   * su estado real.
   */
  ejecutar(accion: AccionPedido): Promise<Pedido> {
    const actual = this.pedido();
    if (!actual) return Promise.reject(new AppError('DESCONOCIDO', 'No hay pedido cargado.'));
    this.procesando.set(accion);
    this.errorAccion.set(null);
    return new Promise<Pedido>((resolve, reject) => {
      this.casos[accion]
        .execute(actual)
        .pipe(finalize(() => this.procesando.set(null)))
        .subscribe({
          next: (p) => {
            this.pedido.set(p);
            resolve(p);
          },
          error: (e: unknown) => {
            const error = AppError.desde(e);
            this.errorAccion.set(error);
            if (error.tipo === 'CONFLICTO') this.cargar(actual.id);
            reject(error);
          },
        });
    });
  }
}
