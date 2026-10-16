import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';

import { LucideDynamicIcon, LucideX } from '@lucide/angular';

@Component({
  selector: 'fs-modal-header',
  standalone: true,
  imports: [LucideDynamicIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header
      class="flex items-start justify-between border-b border-border px-7 py-6"
    >
      <div class="space-y-1">
        <h2 class="text-2xl font-semibold tracking-tight">
          {{ title() }}
        </h2>

        @if (description()) {
          <p class="text-sm text-muted-foreground">
            {{ description() }}
          </p>
        }
      </div>

      @if (closable()) {
        <button
          type="button"
          class="inline-flex h-10 w-10 items-center justify-center rounded-xl text-muted-foreground transition hover:bg-muted"
          (click)="closed.emit()"
        >
          <svg [lucideIcon]="icons.X" class="size-5"></svg>
        </button>
      }
    </header>
  `,
})
export class ModalHeaderComponent {
  readonly title = input.required<string>();
  readonly description = input<string | null>(null);
  readonly closable = input(true);

  readonly closed = output<void>();

  protected readonly icons = {
    X: LucideX,
  };
}
