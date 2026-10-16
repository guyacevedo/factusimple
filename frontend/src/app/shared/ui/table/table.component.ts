import {
  ChangeDetectionStrategy,
  Component,
  TemplateRef,
  input,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { TableColumn, TableContext } from './table.types';

@Component({
  selector: 'fs-table',
  standalone: true,
  imports: [NgTemplateOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="overflow-hidden rounded-2xl border border-border bg-surface shadow-(--shadow-soft)">
      <div class="overflow-x-auto">
        <table class="min-w-full border-separate border-spacing-0">
          <thead class="bg-surface-elevated">
            <tr class="text-left text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              @for (column of columns(); track column.key) {
                <th
                  class="border-b border-border px-6 py-4"
                  [class.text-center]="column.align === 'center'"
                  [class.text-right]="column.align === 'right'"
                  [class]="column.headerClass ?? ''"
                >
                  {{ column.header }}
                </th>
              }

              @if (actionsTemplate()) {
                <th class="border-b border-border px-6 py-4 text-right">Acciones</th>
              }
            </tr>
          </thead>

          <tbody>
            @if (loading()) {
              @for (placeholder of skeletonRows(); track placeholder) {
                <tr class="animate-pulse">
                  @for (column of columns(); track column.key) {
                    <td class="border-b border-border px-6 py-5">
                      <div class="h-4 w-3/4 rounded bg-muted"></div>
                    </td>
                  }
                  @if (actionsTemplate()) {
                    <td class="border-b border-border px-6 py-5">
                      <div class="ml-auto h-4 w-16 rounded bg-muted"></div>
                    </td>
                  }
                </tr>
              }
            } @else if (rows().length === 0) {
              <tr>
                <td
                  class="px-6 py-14 text-center text-sm text-muted-foreground"
                  [attr.colspan]="columns().length + (actionsTemplate() ? 1 : 0)"
                >
                  <ng-content select="[empty]" />
                </td>
              </tr>
            } @else {
              @for (row of rows(); track $index) {
                <tr class="transition hover:bg-muted/50">
                  @for (column of columns(); track column.key) {
                    <td
                      class="border-b border-border px-6 py-5 text-sm text-foreground"
                      [class.text-center]="column.align === 'center'"
                      [class.text-right]="column.align === 'right'"
                      [class]="column.cellClass ?? ''"
                    >
                      @if (column.cellTemplate) {
                        <ng-container
                          [ngTemplateOutlet]="column.cellTemplate"
                          [ngTemplateOutletContext]="{ $implicit: row, value: column.accessor(row) }"
                        />
                      } @else {
                        {{ column.accessor(row) }}
                      }
                    </td>
                  }

                  @if (actionsTemplate()) {
                    <td class="border-b border-border px-6 py-5 text-right">
                      <ng-container
                        [ngTemplateOutlet]="actionsTemplate()!"
                        [ngTemplateOutletContext]="{ $implicit: row }"
                      />
                    </td>
                  }
                </tr>
              }
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class TableComponent<T> {
  readonly rows = input<readonly T[]>([]);
  readonly columns = input.required<readonly TableColumn<T>[]>();
  readonly loading = input(false);
  readonly actionsTemplate = input<TemplateRef<TableContext<T>> | null>(null);

  protected skeletonRows(): number[] {
    return Array.from({ length: 5 }, (_, index) => index);
  }
}
