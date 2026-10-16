import { Component } from '@angular/core';
import { ButtonVariant, CardComponent, ButtonComponent } from '../../shared/ui';

@Component({
  imports: [CardComponent, ButtonComponent],
  selector: 'fs-kit',
  styles: ``,
  template: `
     <div class="p-8">
      <h1 class="text-2xl font-bold text-primary-800">Kit de interfaz</h1>
      <p class="mt-1 text-sm text-primary-400">
        Cada componente con sus variantes y estados. Solo en desarrollo.
      </p>
    <section class="mt-6">
        <h2 class="text-lg font-semibold text-marino-800">Botones</h2>

        <fs-card class="mt-3 block">
          <p class="text-xs font-semibold uppercase tracking-wide text-marino-400">Variantes</p>
          <div class="mt-2 flex flex-wrap items-center gap-3">
            @for (v of variantesBotones; track v) {
              <fs-button [variant]="v">{{ v }}</fs-button>
            }
          </div>

           <p class="mt-6 text-xs font-semibold uppercase tracking-wide text-marino-400">Tamanos</p>
          <div class="mt-2 flex flex-wrap items-center gap-3">
            <fs-button [size]="'sm'">Pequeno</fs-button>
            <fs-button [size]="'md'">Mediano</fs-button>
            <fs-button [size]="'lg'">Grande</fs-button>
          </div>

          <p class="mt-6 text-xs font-semibold uppercase tracking-wide text-marino-400">Estados</p>
          <div class="mt-2 flex flex-wrap items-center gap-3">
            <fs-button>Normal</fs-button>
            <fs-button [disabled]="true">Disabled</fs-button>
          </div>
        </fs-card>

    </section>
     </div>
  `,
})
export class Kit {

  protected readonly variantesBotones: ButtonVariant[] = [
    'primary',
    'secondary',
    'ghost',
    'danger'
  ];
}
