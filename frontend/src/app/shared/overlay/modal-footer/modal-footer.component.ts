import {
  ChangeDetectionStrategy,
  Component,
} from '@angular/core';

@Component({
  selector: 'fs-modal-footer',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <footer
      class="flex items-center justify-end gap-3 border-t border-border bg-surface-elevated px-7 py-5"
    >
      <ng-content />
    </footer>
  `,
})
export class ModalFooterComponent {}
