import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'fs-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section [class]="classes()">
      <ng-content />
    </section>
  `,
})
export class CardComponent {
  readonly padded = input(true);

  protected readonly classes = computed(() =>
    ['rounded-2xl border border-border bg-surface shadow-[var(--shadow-soft)]', this.padded() ? 'p-4 sm:p-6' : ''].join(' '),
  );

}
