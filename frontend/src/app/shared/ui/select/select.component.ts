import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
} from '@angular/core';

@Component({
  selector: 'fs-select',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <select
      (change)="onChange($event)"
      [disabled]="disabled()"
      [class]="classes()"
    >
      @for (option of options(); track option.value) {
        <option [value]="option.value" [selected]="option.value === value()">
          {{ option.label }}
        </option>
      }
    </select>
  `,
})
export class SelectComponent {
  readonly value = input<string>('');

  readonly options = input<
    {
      label: string;
      value: string;
    }[]
  >([]);

  readonly disabled = input(false);

  readonly valueChange = output<string>();

  protected readonly classes = computed(() =>
    [
      'h-12 w-full rounded-2xl border border-border bg-surface px-4 text-sm outline-none transition',
      'focus:border-primary-500 focus:ring-4 focus:ring-primary-100',
      'disabled:cursor-not-allowed disabled:opacity-60 disabled:bg-muted',
    ].join(' '),
  );

  onChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.valueChange.emit(target.value);
  }
}
