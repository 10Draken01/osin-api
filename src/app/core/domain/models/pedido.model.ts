/** Estados posibles de un pedido (mismos valores que el enum EstadoPedido del backend). */
export type EstadoPedido = 'BORRADOR' | 'CONFIRMADO' | 'COMPLETADO' | 'CANCELADO';

export const ESTADOS_PEDIDO: readonly EstadoPedido[] = [
  'BORRADOR',
  'CONFIRMADO',
  'COMPLETADO',
  'CANCELADO',
];

/** Partida (línea) de un pedido. Precio y subtotal vienen calculados por el servidor. */
export interface PartidaPedido {
  readonly idProducto: number;
  readonly nombre: string;
  readonly cantidad: number;
  readonly precioUnitario: number;
  readonly subtotal: number;
}

export interface Pedido {
  readonly id: number;
  readonly folio: string;
  readonly partidas: readonly PartidaPedido[];
  readonly estado: EstadoPedido;
  readonly total: number;
  readonly fechaCreacion: Date;
  readonly fechaActualizacion: Date;
  readonly fechaCancelacion: Date | null;
}

/** Lo único que el cliente envía al crear un pedido: producto y cantidad. */
export interface NuevaPartida {
  readonly idProducto: number;
  readonly cantidad: number;
}

export interface FiltroPedidos {
  readonly estado: EstadoPedido | null;
  readonly folio: string | null;
}
