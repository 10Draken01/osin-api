import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Pedido } from '@core/domain/models/pedido.model';
import { IconoComponent } from '@shared/ui/atoms';

type EstadoPaso = 'hecho' | 'actual' | 'pendiente' | 'omitido';

interface Paso {
  readonly titulo: string;
  readonly descripcion: string;
  readonly estado: EstadoPaso;
  readonly fecha: Date | null;
}

/**
 * Organismo: ciclo de vida del pedido como recorrido Borrador → Confirmado → Completado.
 * Es el elemento visual principal del detalle: dice en qué punto está el pedido,
 * qué pasó con el inventario y qué sigue. Un pedido cancelado corta el recorrido.
 *
 * La API no guarda historial de estados; solo la fecha de creación, la de la última
 * actualización y la de cancelación. Por eso de un pedido cancelado no se afirma
 * si había sido confirmado antes.
 */
@Component({
  selector: 'app-linea-vida-pedido',
  imports: [DatePipe, IconoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block rounded-lg border border-linea bg-superficie p-5' },
  template: `
    <h2 class="sr-only">Ciclo de vida</h2>
    <ol class="grid gap-4 sm:grid-cols-3 sm:gap-0">
      @for (paso of pasos(); track paso.titulo; let ultimo = $last) {
        <li class="relative flex gap-3 sm:flex-col sm:gap-3 sm:pr-6" [attr.aria-current]="paso.estado === 'actual' ? 'step' : null">
          <div class="flex items-center sm:w-full">
            <span
              class="relative z-10 grid size-8 shrink-0 place-items-center rounded-full border-2 text-sm font-semibold"
              [class]="claseMarcador(paso.estado)"
            >
              @switch (paso.estado) {
                @case ('hecho') { <ui-icono nombre="check" [tamano]="16" /> }
                @case ('omitido') { <ui-icono nombre="cerrar" [tamano]="16" /> }
                @default { <span class="size-2 rounded-full bg-current"></span> }
              }
            </span>
            @if (!ultimo) {
              <span
                class="ml-2 hidden h-0.5 flex-1 sm:block"
                [class]="paso.estado === 'hecho' ? 'bg-marca' : 'bg-linea'"
              ></span>
            }
          </div>
          <div class="min-w-0 space-y-0.5">
            <p class="font-semibold" [class.text-tinta-suave]="paso.estado === 'pendiente'">
              {{ paso.titulo }}
            </p>
            <p class="text-sm text-tinta-suave">{{ paso.descripcion }}</p>
            @if (paso.fecha) {
              <p class="cifra text-xs text-tinta-suave">{{ paso.fecha | date: "d MMM y, HH:mm" }}</p>
            }
          </div>
        </li>
      }
    </ol>

    @if (pedido().estado === 'CANCELADO') {
      <div class="mt-5 flex items-start gap-3 rounded-md border border-cancelado/30 bg-cancelado-suave p-3 text-sm">
        <ui-icono nombre="cerrar" class="mt-0.5 text-cancelado" />
        <div>
          <p class="font-semibold text-cancelado">Cancelado</p>
          <p>
            Si estaba confirmado, su inventario ya se repuso.
            @if (pedido().fechaCancelacion; as f) {
              <span class="cifra text-tinta-suave">{{ f | date: "d MMM y, HH:mm" }}</span>
            }
          </p>
        </div>
      </div>
    }
  `,
})
export class LineaVidaPedidoComponent {
  readonly pedido = input.required<Pedido>();

  protected readonly pasos = computed<readonly Paso[]>(() => {
    const p = this.pedido();
    const cancelado = p.estado === 'CANCELADO';
    const orden = { BORRADOR: 0, CONFIRMADO: 1, COMPLETADO: 2, CANCELADO: -1 } as const;
    const actual = orden[p.estado];

    const estadoDe = (indice: number): EstadoPaso => {
      if (cancelado) return indice === 0 ? 'hecho' : 'omitido';
      if (indice < actual) return 'hecho';
      if (indice === actual) return p.estado === 'COMPLETADO' ? 'hecho' : 'actual';
      return 'pendiente';
    };

    return [
      {
        titulo: 'Borrador',
        descripcion: 'Pedido capturado. El inventario no se ha tocado.',
        estado: estadoDe(0),
        fecha: p.fechaCreacion,
      },
      {
        titulo: 'Confirmado',
        descripcion: 'Existencias validadas y descontadas.',
        estado: estadoDe(1),
        fecha: p.estado === 'CONFIRMADO' ? p.fechaActualizacion : null,
      },
      {
        titulo: 'Completado',
        descripcion: 'Compra finalizada. Ya no se puede cancelar.',
        estado: estadoDe(2),
        fecha: p.estado === 'COMPLETADO' ? p.fechaActualizacion : null,
      },
    ];
  });

  protected claseMarcador(estado: EstadoPaso): string {
    switch (estado) {
      case 'hecho':
        return 'border-marca bg-marca text-white';
      case 'actual':
        return 'border-marca bg-marca-suave text-marca';
      case 'omitido':
        return 'border-linea bg-papel text-tinta-suave';
      default:
        return 'border-linea bg-superficie text-linea-fuerte';
    }
  }
}
