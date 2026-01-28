import { Routes } from '@angular/router';

export const walletRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./wallet-list.component').then(m => m.WalletListComponent)
  },
  {
    path: ':id',
    loadComponent: () => import('./wallet-detail.component').then(m => m.WalletDetailComponent)
  }
];
