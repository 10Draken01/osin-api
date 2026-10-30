import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { InsigniaComponent, TonoInsignia } from '@shared/ui/atoms';
import { EstadoPedido } from '@core/domain/models/pedido.model';
import { ETIQUETA_ESTADO } from '@core/domain/rules/estado-pedido.rules';

const TONO_POR_ESTADO: Record<EstadoPedido, TonoInsignia> = {
  BORRADOR: 'neutro',
  CONFIRMADO: 'info',
  COMPLETADO: 'exito',
  CANCELADO: 'peligro',
};

/** Molécula: estado del pedido con color y texto (el color nunca va solo, por accesibilidad). */
@Component({
  selector: 'ui-estado-pedido',
  imports: [InsigniaComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ui-insignia [tono]="tono()">
      <span class="size-1.5 rounded-full bg-current"></span>
      {{ etiqueta() }}
    </ui-insignia>
  `,
})
export class EstadoPedidoInsigniaComponent {
  readonly estado = input.required<EstadoPedido>();
  protected readonly tono = computed(() => TONO_POR_ESTADO[this.estado()]);
  protected readonly etiqueta = computed(() => ETIQUETA_ESTADO[this.estado()]);
}
