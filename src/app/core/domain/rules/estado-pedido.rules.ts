import { EstadoPedido } from '../models/pedido.model';

/** Acciones que cambian el estado de un pedido (un endpoint POST /api/pedidos/{id}/<accion> cada una). */
export type AccionPedido = 'confirmar' | 'cancelar' | 'completar';

/**
 * Tabla de transiciones. Es una copia de EstadoPedido.puedeCambiar() del backend y solo sirve
 * para que la interfaz muestre los botones correctos. La autoridad sigue siendo el servidor (409).
 */
const ACCIONES_POR_ESTADO: Readonly<Record<EstadoPedido, readonly AccionPedido[]>> = {
  BORRADOR: ['confirmar', 'cancelar'],
  CONFIRMADO: ['completar', 'cancelar'],
  COMPLETADO: [],
  CANCELADO: [],
};

const ESTADO_DESTINO: Readonly<Record<AccionPedido, EstadoPedido>> = {
  confirmar: 'CONFIRMADO',
  cancelar: 'CANCELADO',
  completar: 'COMPLETADO',
};

export const ETIQUETA_ESTADO: Readonly<Record<EstadoPedido, string>> = {
  BORRADOR: 'Borrador',
  CONFIRMADO: 'Confirmado',
  COMPLETADO: 'Completado',
  CANCELADO: 'Cancelado',
};

export function accionesPermitidas(estado: EstadoPedido): readonly AccionPedido[] {
  return ACCIONES_POR_ESTADO[estado];
}

export function puedeEjecutar(estado: EstadoPedido, accion: AccionPedido): boolean {
  return ACCIONES_POR_ESTADO[estado].includes(accion);
}

export function estadoDestino(accion: AccionPedido): EstadoPedido {
  return ESTADO_DESTINO[accion];
}

export function esEstadoFinal(estado: EstadoPedido): boolean {
  return ACCIONES_POR_ESTADO[estado].length === 0;
}

/** Qué pasa con el inventario al ejecutar la acción. Se muestra antes de confirmar la operación. */
export function efectoEnInventario(estado: EstadoPedido, accion: AccionPedido): string {
  switch (accion) {
    case 'confirmar':
      return 'Se descontarán del inventario las cantidades de todas las partidas. Si algún producto no alcanza, no se descuenta nada.';
    case 'cancelar':
      return estado === 'CONFIRMADO'
        ? 'Se repondrán al inventario las cantidades de todas las partidas.'
        : 'El inventario no cambia porque el pedido todavía no se había confirmado.';
    case 'completar':
      return 'El inventario no cambia. Un pedido completado ya no se puede cancelar.';
  }
}
