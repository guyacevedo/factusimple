import { TemplateRef } from '@angular/core';

export interface TableCellContext<T> {
  $implicit: T;
  value: string | number | null | undefined;
}

export interface TableColumn<T> {
  key: string;
  header: string;
  accessor: (row: T) => string | number | null | undefined;
  headerClass?: string;
  cellClass?: string;
  align?: 'left' | 'center' | 'right';
  cellTemplate?: TemplateRef<TableCellContext<T>>;
}

export interface TableContext<T> {
  $implicit: T;
}
