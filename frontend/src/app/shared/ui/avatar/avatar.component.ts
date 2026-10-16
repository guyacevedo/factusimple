import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'fs-avatar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div [class]="classes()">
      @if (src()) {
        <img [src]="src()" [alt]="alt()" class="h-full w-full object-cover" />
      } @else {
        <span>{{ initials() }}</span>
      }
    </div>
  `,
})
export class AvatarComponent {
  readonly name = input('');
  readonly src = input<string | null>(null);
  readonly alt = input('Avatar');

  protected initials(): string {
    const parts = this.name().trim().split(/\s+/).filter(Boolean);
    return parts
      .slice(0, 2)
      .map((p) => p[0]?.toUpperCase() ?? '')
      .join('');
  }

  protected readonly classes = computed(() =>
    [
      'flex h-10 w-10 items-center justify-center overflow-hidden rounded-full bg-muted dark:bg-primary-200 text-sm font-semibold text-muted-foreground dark:text-primary-900',
    ].join(' '),
  );
}
