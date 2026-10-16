import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';

@Component({
  selector: 'fs-modal-shell',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      <div
        tabindex="0"
        class="absolute inset-0 bg-black/40 backdrop-blur-[2px]"
        (click)="backdropClick.emit()"
        (keydown.escape)="backdropClick.emit()"
      ></div>

      <section
        class="relative z-10 flex max-h-[calc(100vh-2rem)] w-full flex-col overflow-hidden rounded-3xl border border-border bg-surface shadow-(--shadow-card)"
        [class.max-w-md]="width() === 'sm'"
        [class.max-w-2xl]="width() === 'md'"
        [class.max-w-4xl]="width() === 'lg'"
        [class.max-w-5xl]="width() === 'xl'"
      >
        <ng-content select="fs-modal-header" />
        <div class="flex-1 overflow-y-auto">
          <ng-content />
        </div>
        <ng-content select="fs-modal-footer" />
      </section>
    </div>
  `,
})
export class ModalShellComponent {
  readonly width = input<'sm' | 'md' | 'lg' | 'xl'>('md');

  readonly backdropClick = output<void>();
}
