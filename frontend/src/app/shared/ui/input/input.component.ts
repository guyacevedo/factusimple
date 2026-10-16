import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'fs-input',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <input
      [value]="value()"
      [type]="type()"
      [placeholder]="placeholder()"
      [disabled]="disabled()"
      (input)="onInput($event)"
      [class]="classes()"
    />
  `,
})
export class InputComponent {
  readonly value = input('');
  readonly type = input<'text' | 'email' | 'password' | 'search' | 'tel' | 'url'>('text');
  readonly placeholder = input('');
  readonly disabled = input(false);
  readonly className = input('');

  readonly valueChange = output<string>();

  onInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.valueChange.emit(target.value);
  }

  protected readonly classes = computed(() =>
    [
      'h-10 w-full rounded-xl border border-border bg-surface px-4 text-sm',
      'text-foreground placeholder:text-muted-foreground',
      'transition focus:border-primary-500 focus:ring-2 focus:ring-primary-100',
      'disabled:cursor-not-allowed disabled:opacity-60',
      this.className(),
    ].join(' '),
  );
}
