import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';

@Component({
  selector: 'fs-textarea',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <textarea
      [value]="value()"
      [placeholder]="placeholder()"
      (input)="onInput($event)"
      rows="4"
      class="min-h-[120px] w-full resize-none rounded-2xl border border-border bg-surface px-4 py-3 text-sm outline-none transition focus:border-primary-500 focus:ring-4 focus:ring-primary-100"
    ></textarea>
  `,
})
export class TextareaComponent {
  readonly value = input('');
  readonly placeholder = input('');

  readonly valueChange = output<string>();

  onInput(event: Event): void {
    const target = event.target as HTMLTextAreaElement;
    this.valueChange.emit(target.value);
  }
}
