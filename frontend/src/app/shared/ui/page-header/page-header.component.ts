import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'fs-page-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="flex flex-col gap-4 border-b border-border pb-6 lg:flex-row lg:items-end lg:justify-between">
      <div class="space-y-1">
        <h1 class="text-2xl font-semibold tracking-tight">{{ title() }}</h1>
        @if (subtitle()) {
          <p class="text-sm text-muted-foreground">{{ subtitle() }}</p>
        }
      </div>

      <div class="flex items-center gap-3">
        <ng-content select="[actions]" />
      </div>
    </header>
  `,
})
export class PageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string | null>(null);
}
