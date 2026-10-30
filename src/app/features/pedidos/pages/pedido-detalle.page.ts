import { ChangeDetectionStrategy, Component, effect, inject, input, untracked } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  AccionPedido,
  ETIQUETA_ESTADO,
  efectoEnInventario,
  estadoDestino,
} from '@core/domain/rules/estado-pedido.rules';
import { BotonComponent, SpinnerComponent } from '@shared/ui/atoms';
import { AlertaComponent, EncabezadoPaginaComponent, EstadoVacioComponent } from '@shared/ui/molecules';
import { PlantillaDetalleComponent } from '@shared/ui/templates';
import { DialogoService, NotificacionService } from '@shared/ui/services';
import { EstadoPedidoInsigniaComponent } from '../ui/molecules/estado-pedido-insignia.component';
import { LineaVidaPedidoComponent } from '../ui/organisms/linea-vida-pedido.component';
import { TablaPartidasComponent } from '../ui/organisms/tabla-partidas.component';
import { AccionesPedidoComponent, DEFINICION_ACCION } from '../ui/organisms/acciones-pedido.component';
import { PedidoDetalleStore } from '../state/pedido-detalle.store';

/** Texto en participio para el aviso de éxito ("Pedido PED-000004 confirmado."). */
const PARTICIPIO: Record<AccionPedido, string> = {
  confirmar: 'confirmado',
  cancelar: 'cancelado',
  completar: 'completado',
};

/**
 * Página: detalle de pedido (CU-06) y cambios de estado (CU-07, CU-08, CU-09).
 * GET /api/pedidos/{id} y POST /api/pedidos/{id}/confirmar|cancelar|completar.
 */
@Component({
  selector: 'app-pedido-detalle-page',
  imports: [
    DatePipe,
    RouterLink,
    BotonComponent,
    SpinnerComponent,
    AlertaComponent,
    EncabezadoPaginaComponent,
    EstadoVacioComponent,
    PlantillaDetalleComponent,
    EstadoPedidoInsigniaComponent,
    LineaVidaPedidoComponent,
    TablaPartidasComponent,
    AccionesPedidoComponent,
  ],
  providers: [PedidoDetalleStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (store.pedido(); as pedido) {
      <ui-plantilla-detalle>
        <ui-encabezado-pagina encabezado regresarA="/pedidos" textoRegreso="Pedidos">
          <span titulo class="cifra inline-flex flex-wrap items-center gap-3">
            {{ pedido.folio }}
            <ui-estado-pedido [estado]="pedido.estado" />
          </span>
        </ui-encabezado-pagina>

        <div principal class="flex flex-col gap-6">
          @if (store.errorAccion(); as error) {
            <ui-alerta tono="error" titulo="No se aplicó el cambio" [detalles]="error.detalles">
              {{ error.message }}
              @if (error.tipo === 'CONFLICTO') {
                <p class="mt-1 text-tinta-suave">Se recargó el pedido con su información más reciente.</p>
              }
            </ui-alerta>
          }
          <app-linea-vida-pedido [pedido]="pedido" />
          <app-tabla-partidas [partidas]="pedido.partidas" [total]="pedido.total" />
        </div>

        <div lateral class="flex flex-col gap-6">
          <app-acciones-pedido
            [pedido]="pedido"
            [procesando]="store.procesando()"
            (ejecutar)="ejecutar($event)"
          />
          <dl class="cifra grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 rounded-lg border border-linea bg-superficie p-5 text-sm">
            <dt class="text-tinta-suave">Creado</dt>
            <dd>{{ pedido.fechaCreacion | date: 'd MMM y, HH:mm:ss' }}</dd>
            <dt class="text-tinta-suave">Última actualización</dt>
            <dd>{{ pedido.fechaActualizacion | date: 'd MMM y, HH:mm:ss' }}</dd>
            @if (pedido.fechaCancelacion; as f) {
              <dt class="text-tinta-suave">Cancelado</dt>
              <dd>{{ f | date: 'd MMM y, HH:mm:ss' }}</dd>
            }
          </dl>
        </div>
      </ui-plantilla-detalle>
    } @else if (store.error(); as error) {
      @if (error.tipo === 'NO_ENCONTRADO') {
        <ui-estado-vacio icono="pedido" titulo="Este pedido no existe" [descripcion]="error.message">
          <a uiBoton routerLink="/pedidos">Ver pedidos</a>
        </ui-estado-vacio>
      } @else {
        <ui-alerta tono="error" titulo="No se pudo cargar el pedido">
          {{ error.message }}
          <button uiBoton type="button" tamano="sm" class="mt-2" (click)="recargar()">Reintentar</button>
        </ui-alerta>
      }
    } @else {
      <div class="flex items-center justify-center gap-2 py-16 text-tinta-suave">
        <ui-spinner /> Cargando pedido
      </div>
    }
  `,
})
export class PedidoDetallePage {
  protected readonly store = inject(PedidoDetalleStore);
  private readonly dialogo = inject(DialogoService);
  private readonly notificaciones = inject(NotificacionService);

  /** Parámetro de ruta :id. */
  readonly id = input.required<string>();

  constructor() {
    effect(() => {
      const id = Number(this.id());
      untracked(() => this.store.cargar(id));
    });
  }

  protected recargar(): void {
    this.store.cargar(Number(this.id()));
  }

  /** Toda acción pide confirmación y explica su efecto en el inventario antes de enviarse. */
  protected async ejecutar(accion: AccionPedido): Promise<void> {
    const pedido = this.store.pedido();
    if (!pedido) return;
    const aceptado = await this.dialogo.confirmar({
      titulo: `${DEFINICION_ACCION[accion].texto} ${pedido.folio}`,
      mensaje: `El pedido pasará a ${ETIQUETA_ESTADO[estadoDestino(accion)].toLowerCase()}.`,
      consecuencia: efectoEnInventario(pedido.estado, accion),
      textoConfirmar: DEFINICION_ACCION[accion].texto,
      textoCancelar: 'Volver',
      peligro: accion === 'cancelar',
    });
    if (!aceptado) return;

    try {
      const actualizado = await this.store.ejecutar(accion);
      this.notificaciones.exito(`Pedido ${actualizado.folio} ${PARTICIPIO[accion]}.`);
    } catch {
      // El error queda en store.errorAccion() y se muestra junto al pedido; no se duplica en un aviso.
    }
  }
}
