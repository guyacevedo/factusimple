import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
} from '@angular/core';

import {
  LucideCheckCircle2,
  LucideXCircle,
  LucideInfo,
  LucideDynamicIcon,
  LucideX,
} from '@lucide/angular';

import { ToastService } from './toast.service';
import { ToastTone } from './toast.model';

@Component({
  selector: 'fs-toast-container',
  standalone: true,
  imports: [LucideDynamicIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      @for (toast of toasts(); track toast.id) {
        <div [class]="toast.classes">
          <svg
            [lucideIcon]="getIcon(toast.tone)"
            class="size-5 flex-shrink-0"
          ></svg>

          <span class="flex-1">{{ toast.message }}</span>

          <button
            type="button"
            (click)="service.dismiss(toast.id)"
            class="flex-shrink-0 opacity-70 hover:opacity-100 transition-opacity"
            [attr.aria-label]="'Cerrar notificación: ' + toast.message"
          >
            <svg
              [lucideIcon]="LucideX"
              class="size-4"
            ></svg>
          </button>
        </div>
      }
    </div>
  `,
})
export class ToastContainerComponent {
  protected readonly service = inject(ToastService);
  protected readonly LucideX = LucideX;

  private readonly toneClasses: Record<ToastTone, string> = {
    success: 'bg-green-50 text-green-900 dark:bg-green-950 dark:text-green-50',
    error: 'bg-red-50 text-red-900 dark:bg-red-950 dark:text-red-50',
    info: 'bg-blue-50 text-blue-900 dark:bg-blue-950 dark:text-blue-50',
  };

  protected readonly toasts = computed(() =>
    this.service.toasts().map((toast) => ({
      ...toast,
      classes: ['flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium', this.toneClasses[toast.tone]].join(' '),
    })),
  );

  getIcon(tone: ToastTone) {
    switch (tone) {
      case 'success':
        return LucideCheckCircle2;
      case 'error':
        return LucideXCircle;
      case 'info':
        return LucideInfo;
    }
  }
}
