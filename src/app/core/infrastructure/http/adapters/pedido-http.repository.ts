import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { PedidoRepository } from '../../../domain/ports/pedido.repository';
import { FiltroPedidos, NuevaPartida, Pedido } from '../../../domain/models/pedido.model';
import { Pagina, SolicitudPagina } from '../../../domain/models/pagina.model';
import { AccionPedido } from '../../../domain/rules/estado-pedido.rules';
import { API_BASE_URL } from '../api-config';
import { PedidoResponseDto } from '../dto/pedido.dto';
import { PaginaDto } from '../dto/pagina.dto';
import { PedidoMapper } from '../mappers/pedido.mapper';
import { aPagina } from '../mappers/pagina.mapper';

/** Adaptador HTTP de PedidoRepository para /api/pedidos. */
@Injectable()
export class PedidoHttpRepository extends PedidoRepository {
  private readonly http = inject(HttpClient);
  private readonly url = `${inject(API_BASE_URL)}/pedidos`;

  listar(filtro: FiltroPedidos, s: SolicitudPagina): Observable<Pagina<Pedido>> {
    let params = new HttpParams().set('pagina', s.pagina).set('tamano', s.tamano);
    if (filtro.estado) params = params.set('estado', filtro.estado);
    if (filtro.folio) params = params.set('folio', filtro.folio);
    return this.http
      .get<PaginaDto<PedidoResponseDto>>(this.url, { params })
      .pipe(map((dto) => aPagina(dto, PedidoMapper.aDominio)));
  }

  obtenerPorId(id: number): Observable<Pedido> {
    return this.http.get<PedidoResponseDto>(`${this.url}/${id}`).pipe(map(PedidoMapper.aDominio));
  }

  registrar(partidas: readonly NuevaPartida[]): Observable<Pedido> {
    return this.http
      .post<PedidoResponseDto>(this.url, PedidoMapper.aRequest(partidas))
      .pipe(map(PedidoMapper.aDominio));
  }

  confirmar(id: number): Observable<Pedido> {
    return this.ejecutar(id, 'confirmar');
  }

  cancelar(id: number): Observable<Pedido> {
    return this.ejecutar(id, 'cancelar');
  }

  completar(id: number): Observable<Pedido> {
    return this.ejecutar(id, 'completar');
  }

  /** Las tres acciones son POST sin cuerpo a /{id}/<accion>. */
  private ejecutar(id: number, accion: AccionPedido): Observable<Pedido> {
    return this.http
      .post<PedidoResponseDto>(`${this.url}/${id}/${accion}`, null)
      .pipe(map(PedidoMapper.aDominio));
  }
}
