import { Routes } from '@angular/router';
import { authGuard, publicGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'wallets',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent),
    canActivate: [publicGuard]
  },
  {
    path: 'wallets',
    loadChildren: () => import('./features/wallet/wallet.routes').then(m => m.walletRoutes),
    canActivate: [authGuard]
  }
];
