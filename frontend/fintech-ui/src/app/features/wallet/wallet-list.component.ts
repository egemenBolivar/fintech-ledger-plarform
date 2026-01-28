import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { WalletApiService } from './services/wallet-api.service';
import { ToastService } from '../../core/services/toast.service';
import { SkeletonComponent } from '../../core/components/skeleton/skeleton.component';
import { ConfirmDialogComponent } from '../../core/components/confirm-dialog/confirm-dialog.component';
import { Wallet, Balance } from '../../shared/models/api.models';

interface WalletWithBalance extends Wallet {
  balance?: Balance;
}

@Component({
  selector: 'app-wallet-list',
  standalone: true,
  imports: [CommonModule, RouterModule, SkeletonComponent, ConfirmDialogComponent],
  template: `
    <div class="wallet-list">
      <header class="page-header">
        <h1>Wallets</h1>
        <button class="btn btn-primary" (click)="showCreateModal.set(true)">
          + New Wallet
        </button>
      </header>

      @if (loading()) {
        <!-- Skeleton Loading State -->
        <div class="wallet-grid">
          @for (i of [1, 2, 3]; track i) {
            <div class="wallet-card skeleton-card">
              <div class="wallet-header">
                <app-skeleton width="60px" height="28px" />
                <app-skeleton width="70px" height="22px" />
              </div>
              <div class="wallet-balance">
                <app-skeleton width="120px" height="2.5rem" />
              </div>
              <app-skeleton width="100px" height="0.875rem" />
              <div class="wallet-actions" style="margin-top: 1rem;">
                <app-skeleton width="70px" height="32px" />
                <app-skeleton width="70px" height="32px" />
              </div>
            </div>
          }
        </div>
      } @else if (error()) {
        <div class="error-state">
          <div class="error-icon">⚠️</div>
          <h3>Failed to load wallets</h3>
          <p>{{ error() }}</p>
          <button class="btn btn-primary" (click)="loadWallets()">Try Again</button>
        </div>
      } @else {
        <div class="wallet-grid">
          @for (wallet of walletsWithBalance(); track wallet.id) {
            <div class="wallet-card" [class.suspended]="wallet.status === 'SUSPENDED'" [class.closed]="wallet.status === 'CLOSED'">
              <div class="wallet-header">
                <span class="currency-badge">{{ wallet.baseCurrency }}</span>
                <span class="status-badge" [class]="wallet.status.toLowerCase()">{{ wallet.status }}</span>
              </div>
              <div class="wallet-balance">
                @if (wallet.balance) {
                  <span class="amount">{{ wallet.balance.balance.amount | number:'1.2-2' }}</span>
                  <span class="currency">{{ wallet.balance.balance.currency }}</span>
                } @else {
                  <app-skeleton type="text" width="100px" height="2rem" />
                }
              </div>
              <div class="wallet-id">{{ wallet.id | slice:0:8 }}...</div>
              <div class="wallet-actions">
                <a [routerLink]="['/wallets', wallet.id]" class="btn btn-sm">Details</a>
                @if (wallet.status === 'ACTIVE') {
                  <button class="btn btn-sm btn-warning" (click)="confirmSuspend(wallet)">Suspend</button>
                } @else if (wallet.status === 'SUSPENDED') {
                  <button class="btn btn-sm btn-success" (click)="confirmActivate(wallet)">Activate</button>
                }
              </div>
            </div>
          } @empty {
            <div class="empty-state">
              <div class="empty-icon">💼</div>
              <h3>No wallets yet</h3>
              <p>Create your first wallet to get started!</p>
              <button class="btn btn-primary" (click)="showCreateModal.set(true)">
                + Create Wallet
              </button>
            </div>
          }
        </div>
      }

      @if (showCreateModal()) {
        <div class="modal-overlay" (click)="showCreateModal.set(false)">
          <div class="modal" (click)="$event.stopPropagation()">
            <h2>Create New Wallet</h2>
            <form (submit)="createWallet($event)">
              <div class="form-group">
                <label>Currency</label>
                <select #currencySelect required>
                  <option value="USD">USD - US Dollar</option>
                  <option value="EUR">EUR - Euro</option>
                  <option value="GBP">GBP - British Pound</option>
                  <option value="TRY">TRY - Turkish Lira</option>
                </select>
              </div>
              <div class="modal-actions">
                <button type="button" class="btn" (click)="showCreateModal.set(false)">Cancel</button>
                <button type="submit" class="btn btn-primary" [disabled]="createLoading()">
                  {{ createLoading() ? 'Creating...' : 'Create Wallet' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Confirm Dialog -->
      @if (confirmDialog()) {
        <app-confirm-dialog
          [title]="confirmDialog()!.title"
          [message]="confirmDialog()!.message"
          [type]="confirmDialog()!.type"
          [confirmText]="confirmDialog()!.confirmText"
          [loading]="confirmLoading()"
          (confirmed)="confirmDialog()!.onConfirm()"
          (cancelled)="confirmDialog.set(null)"
        />
      }
    </div>
  `,
  styles: [`
    .wallet-list { padding: 2rem; max-width: 1200px; margin: 0 auto; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
    .page-header h1 { margin: 0; color: #1a1a2e; }
    
    .wallet-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1.5rem; }
    
    .wallet-card {
      background: white;
      border-radius: 12px;
      padding: 1.5rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.08);
      transition: transform 0.2s, box-shadow 0.2s;
    }
    .wallet-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.12); }
    .wallet-card.suspended { opacity: 0.7; border-left: 4px solid #f59e0b; }
    .wallet-card.closed { opacity: 0.5; border-left: 4px solid #ef4444; }
    
    .wallet-header { display: flex; justify-content: space-between; margin-bottom: 1rem; }
    .currency-badge { 
      background: #3b82f6; color: white; padding: 0.25rem 0.75rem; 
      border-radius: 20px; font-weight: 600; font-size: 0.875rem; 
    }
    .status-badge { 
      padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.75rem; font-weight: 500;
    }
    .status-badge.active { background: #d1fae5; color: #059669; }
    .status-badge.suspended { background: #fef3c7; color: #d97706; }
    .status-badge.closed { background: #fee2e2; color: #dc2626; }
    
    .wallet-balance { margin-bottom: 0.5rem; }
    .wallet-balance .amount { font-size: 2rem; font-weight: 700; color: #1a1a2e; }
    .wallet-balance .currency { font-size: 1rem; color: #6b7280; margin-left: 0.5rem; }
    .loading-balance { color: #9ca3af; }
    
    .wallet-id { font-size: 0.75rem; color: #9ca3af; font-family: monospace; margin-bottom: 1rem; }
    
    .wallet-actions { display: flex; gap: 0.5rem; }
    
    .btn {
      padding: 0.5rem 1rem; border-radius: 6px; border: 1px solid #e5e7eb;
      background: white; cursor: pointer; font-size: 0.875rem; text-decoration: none; color: #374151;
    }
    .btn:hover { background: #f9fafb; }
    .btn-primary { background: #3b82f6; color: white; border-color: #3b82f6; }
    .btn-primary:hover { background: #2563eb; }
    .btn-warning { background: #f59e0b; color: white; border-color: #f59e0b; }
    .btn-success { background: #10b981; color: white; border-color: #10b981; }
    .btn-sm { padding: 0.375rem 0.75rem; font-size: 0.75rem; }
    
    .loading, .error, .empty-state { text-align: center; padding: 3rem; color: #6b7280; }
    .error { color: #dc2626; }
    
    .error-state {
      text-align: center;
      padding: 4rem 2rem;
      background: white;
      border-radius: 16px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    }
    .error-state .error-icon { font-size: 3rem; margin-bottom: 1rem; }
    .error-state h3 { margin: 0 0 0.5rem; color: #dc2626; }
    .error-state p { margin: 0 0 1.5rem; color: #6b7280; }
    
    .empty-state {
      grid-column: 1 / -1;
      background: white;
      border-radius: 16px;
      padding: 4rem 2rem;
      box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    }
    .empty-state .empty-icon { font-size: 4rem; margin-bottom: 1rem; }
    .empty-state h3 { margin: 0 0 0.5rem; color: #1a1a2e; }
    .empty-state p { margin: 0 0 1.5rem; color: #6b7280; }
    
    .skeleton-card { pointer-events: none; }
    
    .modal-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,0.5);
      display: flex; align-items: center; justify-content: center; z-index: 1000;
    }
    .modal {
      background: white; padding: 2rem; border-radius: 12px;
      width: 100%; max-width: 400px;
    }
    .modal h2 { margin: 0 0 1.5rem; }
    .form-group { margin-bottom: 1rem; }
    .form-group label { display: block; margin-bottom: 0.5rem; font-weight: 500; }
    .form-group input, .form-group select {
      width: 100%; padding: 0.75rem; border: 1px solid #e5e7eb;
      border-radius: 6px; font-size: 1rem;
    }
    .modal-actions { display: flex; gap: 1rem; justify-content: flex-end; margin-top: 1.5rem; }
  `]
})
export class WalletListComponent implements OnInit {
  private readonly api = inject(WalletApiService);
  private readonly toast = inject(ToastService);
  
  wallets = signal<Wallet[]>([]);
  balances = signal<Map<string, Balance>>(new Map());
  loading = signal(true);
  error = signal<string | null>(null);
  showCreateModal = signal(false);
  createLoading = signal(false);
  confirmLoading = signal(false);
  
  // Confirm dialog state
  confirmDialog = signal<{
    title: string;
    message: string;
    type: 'danger' | 'warning' | 'info';
    confirmText: string;
    onConfirm: () => void;
  } | null>(null);

  walletsWithBalance = computed(() => {
    const balanceMap = this.balances();
    return this.wallets().map(w => ({
      ...w,
      balance: balanceMap.get(w.id)
    }));
  });

  ngOnInit() {
    this.loadWallets();
  }

  loadWallets() {
    this.loading.set(true);
    this.error.set(null);
    this.api.getWallets().subscribe({
      next: (wallets) => {
        this.wallets.set(wallets);
        this.loading.set(false);
        wallets.forEach(w => this.loadBalance(w.id));
      },
      error: (err) => {
        this.error.set(err.userMessage || err.message || 'Failed to load wallets');
        this.loading.set(false);
      }
    });
  }

  loadBalance(walletId: string) {
    this.api.getBalance(walletId).subscribe({
      next: (balance) => {
        this.balances.update(m => new Map(m).set(walletId, balance));
      }
    });
  }

  createWallet(event: Event) {
    event.preventDefault();
    const form = event.target as HTMLFormElement;
    const currency = (form.querySelector('select') as HTMLSelectElement).value as any;
    
    this.createLoading.set(true);
    this.api.createWallet({ baseCurrency: currency }).subscribe({
      next: (wallet) => {
        this.showCreateModal.set(false);
        this.createLoading.set(false);
        this.toast.success(`Wallet created successfully (${wallet.baseCurrency})`);
        this.loadWallets();
      },
      error: () => {
        this.createLoading.set(false);
      }
    });
  }

  confirmSuspend(wallet: WalletWithBalance) {
    this.confirmDialog.set({
      title: 'Suspend Wallet',
      message: `Are you sure you want to suspend the ${wallet.baseCurrency} wallet? No operations will be allowed until activated.`,
      type: 'warning',
      confirmText: 'Suspend',
      onConfirm: () => this.suspendWallet(wallet.id)
    });
  }

  confirmActivate(wallet: WalletWithBalance) {
    this.confirmDialog.set({
      title: 'Activate Wallet',
      message: `Are you sure you want to activate the ${wallet.baseCurrency} wallet?`,
      type: 'info',
      confirmText: 'Activate',
      onConfirm: () => this.activateWallet(wallet.id)
    });
  }

  suspendWallet(id: string) {
    this.confirmLoading.set(true);
    this.api.suspendWallet(id).subscribe({
      next: () => {
        this.confirmLoading.set(false);
        this.confirmDialog.set(null);
        this.toast.success('Wallet suspended');
        this.loadWallets();
      },
      error: () => {
        this.confirmLoading.set(false);
      }
    });
  }

  activateWallet(id: string) {
    this.confirmLoading.set(true);
    this.api.activateWallet(id).subscribe({
      next: () => {
        this.confirmLoading.set(false);
        this.confirmDialog.set(null);
        this.toast.success('Wallet activated');
        this.loadWallets();
      },
      error: () => {
        this.confirmLoading.set(false);
      }
    });
  }
}
