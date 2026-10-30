import { Pedido } from '@core/domain/models/pedido.model';
import { Producto } from '@core/domain/models/producto.model';

/** Datos de prueba basados en database/oracle/02_data.sql. */
export function unProducto(cambios: Partial<Producto> = {}): Producto {
  return {
    id: 5,
    nombre: 'Leche 1 L',
    descripcion: 'Leche entera',
    precioUnitario: 27,
    existencia: 120,
    activo: true,
    ...cambios,
  };
}

export function unPedido(cambios: Partial<Pedido> = {}): Pedido {
  return {
    id: 1,
    folio: 'PED-000001',
    estado: 'BORRADOR',
    total: 54,
    partidas: [{ idProducto: 5, nombre: 'Leche 1 L', cantidad: 2, precioUnitario: 27, subtotal: 54 }],
    fechaCreacion: new Date(2026, 9, 30, 7, 41),
    fechaActualizacion: new Date(2026, 9, 30, 7, 41),
    fechaCancelacion: null,
    ...cambios,
  };
}
