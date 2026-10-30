import { Observable } from 'rxjs';
import { FiltroPedidos, NuevaPartida, Pedido } from '../models/pedido.model';
import { Pagina, SolicitudPagina } from '../models/pagina.model';

/** Puerto del repositorio de pedidos. Implementación: PedidoHttpRepository. */
export abstract class PedidoRepository {
  /** GET /api/pedidos?estado=&folio=&pagina=&tamano= */
  abstract listar(filtro: FiltroPedidos, solicitud: SolicitudPagina): Observable<Pagina<Pedido>>;
  /** GET /api/pedidos/{id} */
  abstract obtenerPorId(id: number): Observable<Pedido>;
  /** POST /api/pedidos */
  abstract registrar(partidas: readonly NuevaPartida[]): Observable<Pedido>;
  /** POST /api/pedidos/{id}/confirmar */
  abstract confirmar(id: number): Observable<Pedido>;
  /** POST /api/pedidos/{id}/cancelar */
  abstract cancelar(id: number): Observable<Pedido>;
  /** POST /api/pedidos/{id}/completar */
  abstract completar(id: number): Observable<Pedido>;
}
