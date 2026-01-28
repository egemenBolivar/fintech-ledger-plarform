import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { ToastComponent } from './core/components/toast/toast.component';
import { LoadingSpinnerComponent } from './core/components/loading-spinner/loading-spinner.component';
import { LoadingService } from './core/services/loading.service';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastComponent, LoadingSpinnerComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected title = 'Fintech Ledger';
  protected readonly loading = inject(LoadingService);
  protected readonly auth = inject(AuthService);
}
