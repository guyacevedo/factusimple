import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Kit } from './features/kit/kit';

@Component({
  imports: [RouterOutlet, Kit],
  selector: 'fs-root',
  styles: [],
  template: `
   <fs-kit></fs-kit>

    <router-outlet />
  `,
})
export class App {
  protected readonly title = signal('factusimple');
}
