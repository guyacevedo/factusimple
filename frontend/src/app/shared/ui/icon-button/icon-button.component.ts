import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'fs-icon-button',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      [attr.type]="type()"
      [attr.aria-label]="ariaLabel()"
      [disabled]="disabled()"
      [class]="classes()"
      (click)="clicked.emit()"
    >
      <ng-content />
    </button>
  `,
})
export class IconButtonComponent {
  readonly ariaLabel = input.required<string>();
  readonly disabled = input(false);
  readonly type = input<'button' | 'submit' | 'reset'>('button');
  readonly clicked = output<void>();

  protected readonly classes = computed(() =>
    [
      'inline-flex h-9 w-9 items-center justify-center rounded-xl border border-border bg-surface',
      'text-muted-foreground transition hover:bg-muted hover:text-foreground',
      'focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:ring-offset-2 disabled:opacity-50',
    ].join(' '),
  );
}
