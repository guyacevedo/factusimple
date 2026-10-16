import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
} from '@angular/core';

import {
  LucideTriangleAlert,
  LucideCircleAlert,
  LucideDynamicIcon,
} from '@lucide/angular';
import type { LucideIconInput } from '@lucide/angular';

import { ButtonComponent } from '../button/button.component';
import { ModalFooterComponent } from '../../overlay/modal-footer/modal-footer.component';
import { ModalShellComponent } from '../../overlay/modal-shell/modal-shell.component';

export type ConfirmTone = 'danger' | 'success' | 'neutral';

@Component({
  selector: 'fs-confirm-dialog',
  standalone: true,
  imports: [
    ModalShellComponent,
    ModalFooterComponent,
    ButtonComponent,
    LucideDynamicIcon,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <fs-modal-shell width="sm">
      <div class="px-8 py-8 text-center">
        <div
          class="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full"
          [class.bg-red-100]="tone() === 'danger'"
          [class.bg-primary-100]="tone() === 'success'"
          [class.bg-neutral-100]="tone() === 'neutral'"
        >
          <svg
            [lucideIcon]="resolvedIcon()"
            class="size-8"
            [class.text-red-600]="tone() === 'danger'"
            [class.text-primary-700]="tone() === 'success'"
            [class.text-neutral-600]="tone() === 'neutral'"
          ></svg>
        </div>

        <h2 class="text-2xl font-semibold">
          {{ title() }}
        </h2>

        <p class="mt-3 text-sm leading-6 text-muted-foreground">
          {{ description() }}
        </p>
      </div>

      <fs-modal-footer>
        <fs-button
          variant="secondary"
          (click)="cancelled.emit()"
        >
          Cancelar
        </fs-button>

        <fs-button
          [variant]="tone() === 'danger' ? 'danger' : 'primary'"
          (click)="confirmed.emit()"
        >
          {{ confirmText() }}
        </fs-button>
      </fs-modal-footer>
    </fs-modal-shell>
  `,
})
export class ConfirmDialogComponent {
  readonly tone = input<ConfirmTone>('success');
  readonly customIcon = input<LucideIconInput | null>(null);

  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly confirmText = input('Confirmar');

  readonly cancelled = output<void>();
  readonly confirmed = output<void>();

  protected readonly resolvedIcon = computed<LucideIconInput>(() => {
    if (this.customIcon()) return this.customIcon()!;
    return this.tone() === 'danger' ? LucideTriangleAlert : LucideCircleAlert;
  });
}
