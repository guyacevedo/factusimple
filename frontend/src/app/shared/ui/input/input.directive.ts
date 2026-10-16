import { Directive, computed } from '@angular/core';

@Directive({
  selector: 'input[fsInput]',
  standalone: true,
  host: { '[class]': 'classes()' },
})
export class InputDirective {
  protected readonly classes = computed(() =>
    [
      'h-10 w-full rounded-lg border border-border bg-(--color-surface) px-3 text-sm',
      'text-foreground placeholder:text-muted-foreground',
      'transition focus:border-primary-500 focus:ring-2 focus:ring-primary-100',
      'disabled:cursor-not-allowed disabled:opacity-50',
    ].join(' '),
  );
}
