import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PedidoRepository } from '../../../domain/ports/pedido.repository';
import { Pedido } from '../../../domain/models/pedido.model';
import { validarTransicion } from './validar-transicion';

/** CU-09. Completar: solo desde CONFIRMADO. No mueve inventario; después ya no se puede cancelar. */
@Injectable({ providedIn: 'root' })
export class CompletarPedidoUseCase {
  private readonly repositorio = inject(PedidoRepository);

  execute(pedido: Pedido): Observable<Pedido> {
    return validarTransicion(pedido, 'completar') ?? this.repositorio.completar(pedido.id);
  }
}
