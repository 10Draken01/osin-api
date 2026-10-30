import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { Pedido } from '@core/domain/models/pedido.model';
import { AccionPedido, accionesPermitidas } from '@core/domain/rules/estado-pedido.rules';
import { BotonComponent, VarianteBoton } from '@shared/ui/atoms';

interface DefinicionAccion {
  readonly texto: string;
  readonly ayuda: string;
  readonly variante: VarianteBoton;
}

/** Texto y estilo de cada acción. El orden de los botones sale de la tabla de transiciones. */
export const DEFINICION_ACCION: Readonly<Record<AccionPedido, DefinicionAccion>> = {
  confirmar: {
    texto: 'Confirmar pedido',
    ayuda: 'Valida que haya existencia de todas las partidas y la descuenta.',
    variante: 'primario',
  },
  completar: {
    texto: 'Completar pedido',
    ayuda: 'Marca la compra como finalizada. No mueve inventario.',
    variante: 'primario',
  },
  cancelar: {
    texto: 'Cancelar pedido',
    ayuda: 'Si ya estaba confirmado, repone el inventario.',
    variante: 'peligro-borde',
  },
};

/**
 * Organismo: acciones disponibles según el estado (Strategy por tabla: accionesPermitidas).
 * Solo se dibujan los botones válidos; así el operador no ve opciones que darían 409.
 */
@Component({
  selector: 'app-acciones-pedido',
  imports: [BotonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block rounded-lg border border-linea bg-superficie p-5' },
  template: `
    <h2 class="mb-3 font-semibold">Siguiente paso</h2>
    @if (acciones().length === 0) {
      <p class="text-sm text-tinta-suave">
        {{ pedido().estado === 'CANCELADO' ? 'El pedido está cancelado.' : 'El pedido está completado.' }}
        Es un estado final y no admite más cambios.
      </p>
    } @else {
      <ul class="space-y-4">
        @for (a of acciones(); track a) {
          <li class="space-y-1.5">
            <button
              uiBoton
              type="button"
              class="w-full"
              [variante]="definicion[a].variante"
              [cargando]="procesando() === a"
              [deshabilitado]="procesando() !== null"
              (click)="ejecutar.emit(a)"
            >
              {{ definicion[a].texto }}
            </button>
            <p class="text-xs text-tinta-suave">{{ definicion[a].ayuda }}</p>
          </li>
        }
      </ul>
    }
  `,
})
export class AccionesPedidoComponent {
  readonly pedido = input.required<Pedido>();
  /** Acción en curso: deshabilita todas para evitar dobles envíos. */
  readonly procesando = input<AccionPedido | null>(null);
  readonly ejecutar = output<AccionPedido>();

  protected readonly definicion = DEFINICION_ACCION;
  protected readonly acciones = computed(() => accionesPermitidas(this.pedido().estado));
}
