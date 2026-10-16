import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'fs-form-field',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <label class="block space-y-2" [for]="for()">
      <span class="text-xs font-bold text-gray-500 uppercase tracking-wider">{{ label() }}</span>
      <ng-content />
      @if (hint()) {
        <p class="text-xs text-muted-foreground">{{ hint() }}</p>
      }
      <ng-content select="[error]" />
    </label>
  `,
})
export class FormFieldComponent {
  readonly label = input.required<string>();
  readonly for = input.required<string>();
  readonly hint = input<string | null>(null);
}
