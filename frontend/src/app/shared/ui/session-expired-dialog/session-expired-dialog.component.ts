import {
  ChangeDetectionStrategy,
  Component,
  output,
} from '@angular/core';

import { LucideDynamicIcon, LucideLock } from '@lucide/angular';

import { ButtonComponent } from '../button/button.component';
import { ModalFooterComponent } from '../../overlay/modal-footer/modal-footer.component';
import { ModalShellComponent } from '../../overlay/modal-shell/modal-shell.component';

@Component({
  selector: 'fs-session-expired-dialog',
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
          class="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-red-100"
        >
          <svg
            [lucideIcon]="lockIcon"
            class="size-8 text-red-600"
          ></svg>
        </div>

        <h2 class="text-2xl font-semibold text-red-600">
          Sesión cerrada
        </h2>

        <p class="mt-3 text-sm leading-6 text-muted-foreground">
          Por seguridad, hemos cerrado tu sesión. Tu trabajo no guardado
          puede haberse perdido.
        </p>

        <details class="mt-4 text-left">
          <summary class="cursor-pointer text-sm text-muted-foreground hover:text-foreground">
            ¿Por qué pasó esto?
          </summary>
          <p class="mt-2 text-xs text-muted-foreground">
            El token expira tras 1 hora de inactividad. Intentamos
            refrescarlo automáticamente, pero no fue posible.
          </p>
        </details>
      </div>

      <fs-modal-footer>
        <fs-button
          variant="primary"
          (click)="loginClick.emit()"
        >
          ➜ ir al login
        </fs-button>
      </fs-modal-footer>
    </fs-modal-shell>
  `,
})
export class SessionExpiredDialogComponent {
  readonly loginClick = output<void>();
  protected readonly lockIcon = LucideLock;
}
