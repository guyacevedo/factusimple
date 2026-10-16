import { Injectable, signal } from '@angular/core';
import { Toast, ToastTone } from './toast.model';

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly _toasts = signal<Toast[]>([]);
  readonly toasts = this._toasts.asReadonly();

  show(message: string, tone: ToastTone, duration = 3000): void {
    const id = crypto.randomUUID();
    const toast: Toast = { id, message, tone };

    this._toasts.update((toasts) => [...toasts, toast]);

    setTimeout(() => {
      this.dismiss(id);
    }, duration);
  }

  dismiss(id: string): void {
    this._toasts.update((toasts) => toasts.filter((t) => t.id !== id));
  }
}
