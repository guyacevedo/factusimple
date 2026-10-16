import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type BadgeTone = 'success' | 'neutral' | 'primary' | 'danger';

@Component({
  selector: 'fs-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span [class]="classes()"><ng-content /></span>`,
})
export class BadgeComponent {
  readonly tone = input<BadgeTone>('neutral');

  protected readonly classes = computed(() => {
    const tones: Record<BadgeTone, string> = {
      success: 'bg-primary-50 text-primary-700 border-primary-100',
      neutral: 'bg-muted text-muted-foreground border-border',
      primary: 'bg-primary-50 text-primary-700 border-primary-100',
      danger: 'bg-red-50 text-red-700 border-red-100',
    };

    return ['inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-medium', tones[this.tone()]].join(' ');
  });
}
