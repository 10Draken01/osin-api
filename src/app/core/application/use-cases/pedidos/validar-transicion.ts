import { Observable, throwError } from 'rxjs';
import { Pedido } from '../../../domain/models/pedido.model';
import {
  AccionPedido,
  ETIQUETA_ESTADO,
  estadoDestino,
  puedeEjecutar,
} from '../../../domain/rules/estado-pedido.rules';
import { AppError } from '../../../domain/errors/app-error';

/**
 * Si la transición no está permitida devuelve un Observable con error (mismo mensaje que el 409
 * del backend) y evita la petición. Si está permitida devuelve null y el caso de uso continúa.
 */
export function validarTransicion(pedido: Pedido, accion: AccionPedido): Observable<never> | null {
  if (puedeEjecutar(pedido.estado, accion)) return null;
  const destino = ETIQUETA_ESTADO[estadoDestino(accion)];
  const actual = ETIQUETA_ESTADO[pedido.estado];
  return throwError(
    () =>
      new AppError(
        'CONFLICTO',
        `El pedido no puede cambiar a ${destino} porque esta en estado ${actual}`,
        409,
      ),
  );
}
