import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { WalletApiService } from './services/wallet-api.service';
import { ToastService } from '../../core/services/toast.service';
import { SkeletonComponent } from '../../core/components/skeleton/skeleton.component';
import { Wallet, Balance, Transaction, Page, Currency } from '../../shared/models/api.models';

@Component({
  selector: 'app-wallet-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, SkeletonComponent],
  template: `
    <div class="wallet-detail">
      <a routerLink="/wallets" class="back-link">← Back to Wallets</a>
      
      @if (loading()) {
        <!-- Skeleton Loading -->
        <div class="wallet-header-card skeleton">
          <div class="wallet-main">
            <app-skeleton width="80px" height="40px" />
            <div class="balance-display">
              <app-skeleton width="180px" height="3rem" />
            </div>
            <app-skeleton width="80px" height="36px" />
          </div>
          <div class="wallet-meta">
            <app-skeleton width="300px" height="1rem" />
            <app-skeleton width="280px" height="1rem" />
            <app-skeleton width="200px" height="1rem" />
          </div>
        </div>
      } @else if (error()) {
        <div class="error-state">
          <div class="error-icon">⚠️</div>
          <h3>Failed to load wallet</h3>
          <p>{{ error() }}</p>
          <a routerLink="/wallets" class="btn btn-primary">Back to Wallets</a>
        </div>
      } @else if (wallet()) {
        <div class="wallet-info">
          <div class="wallet-header-card">
            <div class="wallet-main">
              <span class="currency-badge large">{{ wallet()!.baseCurrency }}</span>
              <div class="balance-display">
                @if (balance()) {
                  <span class="amount">{{ balance()!.balance.amount | number:'1.2-4' }}</span>
                  <span class="currency">{{ balance()!.balance.currency }}</span>
                }
              </div>
              <span class="status-badge" [class]="wallet()!.status.toLowerCase()">{{ wallet()!.status }}</span>
            </div>
            <div class="wallet-meta">
              <div><strong>Wallet ID:</strong> {{ wallet()!.id }}</div>
              <div><strong>Owner ID:</strong> {{ wallet()!.ownerId }}</div>
              <div><strong>Created:</strong> {{ wallet()!.createdAt | date:'medium' }}</div>
            </div>
          </div>

          <div class="actions-panel">
            <h3>Operations</h3>
            <div class="action-buttons">
              <button class="btn btn-success" (click)="showModal.set('deposit')" [disabled]="wallet()!.status !== 'ACTIVE'">
                💰 Deposit
              </button>
              <button class="btn btn-warning" (click)="showModal.set('withdraw')" [disabled]="wallet()!.status !== 'ACTIVE'">
                💸 Withdraw
              </button>
              <button class="btn btn-primary" (click)="showModal.set('transfer')" [disabled]="wallet()!.status !== 'ACTIVE'">
                ↔️ Transfer
              </button>
              <button class="btn btn-fx" (click)="showModal.set('fx')" [disabled]="wallet()!.status !== 'ACTIVE'">
                💱 FX Convert
              </button>
            </div>
          </div>

          <div class="transactions-panel">
            <h3>Transaction History</h3>
            <div class="transaction-list">
              @for (tx of transactions()?.content; track tx.id) {
                <div class="transaction-item" [class.credit]="tx.direction === 'CREDIT'" [class.debit]="tx.direction === 'DEBIT'">
                  <div class="tx-icon">
                    {{ tx.direction === 'CREDIT' ? '↓' : '↑' }}
                  </div>
                  <div class="tx-details">
                    <div class="tx-type">{{ tx.referenceType }}</div>
                    <div class="tx-desc">{{ tx.description || '-' }}</div>
                    <div class="tx-time">{{ tx.occurredAt | date:'short' }}</div>
                  </div>
                  <div class="tx-amount" [class.positive]="tx.direction === 'CREDIT'" [class.negative]="tx.direction === 'DEBIT'">
                    {{ tx.direction === 'CREDIT' ? '+' : '-' }}{{ tx.amount.amount | number:'1.2-4' }} {{ tx.amount.currency }}
                  </div>
                </div>
              } @empty {
                <div class="empty">No transactions yet</div>
              }
            </div>
            @if (transactions() && transactions()!.totalPages > 1) {
              <div class="pagination">
                <button [disabled]="transactions()!.first" (click)="loadTransactions(transactions()!.number - 1)">Previous</button>
                <span>Page {{ transactions()!.number + 1 }} of {{ transactions()!.totalPages }}</span>
                <button [disabled]="transactions()!.last" (click)="loadTransactions(transactions()!.number + 1)">Next</button>
              </div>
            }
          </div>
        </div>
      }

      @if (showModal()) {
        <div class="modal-overlay" (click)="showModal.set(null)">
          <div class="modal" (click)="$event.stopPropagation()">
            @switch (showModal()) {
              @case ('deposit') {
                <h2>Deposit Funds</h2>
                <form (submit)="submitDeposit($event)">
                  <div class="form-group">
                    <label>Amount</label>
                    <input type="number" step="0.01" min="0.01" [(ngModel)]="operationAmount" name="amount" required />
                  </div>
                  <div class="modal-actions">
                    <button type="button" class="btn" (click)="showModal.set(null)">Cancel</button>
                    <button type="submit" class="btn btn-success" [disabled]="operationLoading()">
                      {{ operationLoading() ? 'Processing...' : 'Deposit' }}
                    </button>
                  </div>
                </form>
              }
              @case ('withdraw') {
                <h2>Withdraw Funds</h2>
                <form (submit)="submitWithdraw($event)">
                  <div class="form-group">
                    <label>Amount</label>
                    <input type="number" step="0.01" min="0.01" [(ngModel)]="operationAmount" name="amount" required />
                  </div>
                  <div class="modal-actions">
                    <button type="button" class="btn" (click)="showModal.set(null)">Cancel</button>
                    <button type="submit" class="btn btn-warning" [disabled]="operationLoading()">
                      {{ operationLoading() ? 'Processing...' : 'Withdraw' }}
                    </button>
                  </div>
                </form>
              }
              @case ('transfer') {
                <h2>Transfer Funds</h2>
                <form (submit)="submitTransfer($event)">
                  <div class="form-group">
                    <label>Target Wallet</label>
                    @if (otherWallets().length === 0) {
                      <p class="no-wallets">No other wallets available</p>
                    } @else {
                      <select [(ngModel)]="targetWalletId" name="targetWallet" required>
                        <option value="">-- Select wallet --</option>
                        @for (w of otherWallets(); track w.wallet.id) {
                          <option [value]="w.wallet.id">
                            {{ w.wallet.baseCurrency }} - {{ w.balance !== null ? (w.balance | number:'1.2-2') : '...' }} ({{ w.wallet.id | slice:0:8 }}...)
                          </option>
                        }
                      </select>
                    }
                  </div>
                  <div class="form-group">
                    <label>Amount ({{ wallet()!.baseCurrency }})</label>
                    <input type="number" step="0.01" min="0.01" [(ngModel)]="operationAmount" name="amount" required />
                  </div>
                  <div class="form-group">
                    <label>Description (optional)</label>
                    <input type="text" [(ngModel)]="transferDescription" name="description" />
                  </div>
                  <div class="modal-actions">
                    <button type="button" class="btn" (click)="showModal.set(null)">Cancel</button>
                    <button type="submit" class="btn btn-primary" [disabled]="operationLoading() || !targetWalletId">
                      {{ operationLoading() ? 'Processing...' : 'Transfer' }}
                    </button>
                  </div>
                </form>
              }
              @case ('fx') {
                <h2>FX Convert</h2>
                <form (submit)="submitFxConvert($event)">
                  <div class="form-group">
                    <label>Amount ({{ wallet()!.baseCurrency }})</label>
                    <input type="number" step="0.01" min="0.01" [(ngModel)]="operationAmount" name="amount" required 
                           (input)="updateFxPreview()" />
                  </div>
                  <div class="form-group">
                    <label>Target Wallet (different currency)</label>
                    @if (fxTargetWallets().length === 0) {
                      <p class="no-wallets">No wallets with different currency available</p>
                    } @else {
                      <select [(ngModel)]="fxTargetWalletId" name="targetWalletId" required (change)="updateFxPreview()">
                        <option value="">-- Select wallet --</option>
                        @for (w of fxTargetWallets(); track w.wallet.id) {
                          <option [value]="w.wallet.id">
                            {{ w.wallet.baseCurrency }} - {{ w.balance !== null ? (w.balance | number:'1.2-2') : '...' }} ({{ w.wallet.id | slice:0:8 }}...)
                          </option>
                        }
                      </select>
                    }
                  </div>
                  @if (fxPreviewLoading()) {
                    <div class="fx-preview loading">
                      <div class="fx-loading-spinner"></div>
                      <span>Calculating exchange rate...</span>
                    </div>
                  } @else if (fxPreview()) {
                    <div class="fx-preview success">
                      <div class="fx-preview-header">
                        <span class="fx-icon">💱</span>
                        <span class="fx-preview-label">Exchange Preview</span>
                      </div>
                      <div class="fx-conversion">
                        <div class="fx-from">
                          <span class="fx-amount">{{ operationAmount | number:'1.2-2' }}</span>
                          <span class="fx-currency">{{ wallet()!.baseCurrency }}</span>
                        </div>
                        <div class="fx-arrow">→</div>
                        <div class="fx-to">
                          <span class="fx-amount highlight">{{ fxPreview()!.targetAmount | number:'1.2-4' }}</span>
                          <span class="fx-currency">{{ fxPreview()!.targetCurrency }}</span>
                        </div>
                      </div>
                      <div class="fx-rate-info">
                        <span class="fx-rate-label">Rate:</span>
                        <span class="fx-rate-value">1 {{ wallet()!.baseCurrency }} = {{ fxPreview()!.rate | number:'1.4-6' }} {{ fxPreview()!.targetCurrency }}</span>
                      </div>
                    </div>
                  } @else if (fxTargetWalletId && operationAmount > 0) {
                    <div class="fx-preview hint">
                      <span>💡 Exchange rate will be calculated automatically</span>
                    </div>
                  }
                  <div class="modal-actions">
                    <button type="button" class="btn" (click)="showModal.set(null)">Cancel</button>
                    <button type="submit" class="btn btn-fx" [disabled]="operationLoading() || !fxTargetWalletId">
                      {{ operationLoading() ? 'Processing...' : 'Convert' }}
                    </button>
                  </div>
                </form>
              }
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .wallet-detail { padding: 2rem; max-width: 900px; margin: 0 auto; }
    .back-link { color: #d4a84b; text-decoration: none; display: inline-block; margin-bottom: 1rem; }
    .back-link:hover { text-decoration: underline; color: #b87333; }
    
    .wallet-header-card {
      background: linear-gradient(135deg, #1e2832 0%, #2d3a4a 100%);
      color: white; border-radius: 16px; padding: 2rem; margin-bottom: 1.5rem;
    }
    .wallet-main { display: flex; align-items: center; gap: 1.5rem; margin-bottom: 1.5rem; flex-wrap: wrap; }
    .currency-badge.large { font-size: 1.25rem; padding: 0.5rem 1rem; background: rgba(255,255,255,0.2); border-radius: 8px; }
    .balance-display { flex: 1; }
    .balance-display .amount { font-size: 2.5rem; font-weight: 700; }
    .balance-display .currency { font-size: 1.25rem; opacity: 0.8; margin-left: 0.5rem; }
    .status-badge { padding: 0.5rem 1rem; border-radius: 20px; font-size: 0.875rem; font-weight: 600; }
    .status-badge.active { background: #10b981; }
    .status-badge.suspended { background: #f59e0b; }
    .status-badge.closed { background: #ef4444; }
    .wallet-meta { font-size: 0.875rem; opacity: 0.8; display: flex; flex-direction: column; gap: 0.25rem; }
    
    .actions-panel { background: white; border-radius: 12px; padding: 1.5rem; margin-bottom: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
    .actions-panel h3 { margin: 0 0 1rem; color: #2d3a4a; }
    .action-buttons { display: flex; gap: 1rem; flex-wrap: wrap; }
    
    .transactions-panel { background: white; border-radius: 12px; padding: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
    .transactions-panel h3 { margin: 0 0 1rem; color: #2d3a4a; }
    
    .transaction-item {
      display: flex; align-items: center; gap: 1rem; padding: 1rem;
      border-bottom: 1px solid #f3f4f6;
    }
    .transaction-item:last-child { border-bottom: none; }
    .tx-icon {
      width: 40px; height: 40px; border-radius: 50%; display: flex;
      align-items: center; justify-content: center; font-size: 1.25rem;
    }
    .transaction-item.credit .tx-icon { background: #d1fae5; color: #059669; }
    .transaction-item.debit .tx-icon { background: #fee2e2; color: #dc2626; }
    .tx-details { flex: 1; }
    .tx-type { font-weight: 600; color: #2d3a4a; }
    .tx-desc { font-size: 0.875rem; color: #6b7280; }
    .tx-time { font-size: 0.75rem; color: #9ca3af; }
    .tx-amount { font-weight: 600; font-family: monospace; }
    .tx-amount.positive { color: #059669; }
    .tx-amount.negative { color: #dc2626; }
    
    .pagination { display: flex; justify-content: center; align-items: center; gap: 1rem; margin-top: 1rem; padding-top: 1rem; border-top: 1px solid #f3f4f6; }
    .pagination button { padding: 0.5rem 1rem; border: 1px solid #e5e7eb; border-radius: 6px; background: white; cursor: pointer; }
    .pagination button:disabled { opacity: 0.5; cursor: not-allowed; }
    
    .empty { text-align: center; padding: 2rem; color: #9ca3af; }
    .loading, .error { text-align: center; padding: 3rem; }
    .error { color: #dc2626; }
    
    .btn {
      padding: 0.75rem 1.5rem; border-radius: 8px; border: none;
      cursor: pointer; font-size: 1rem; font-weight: 500;
    }
    .btn:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-success { background: #10b981; color: white; }
    .btn-warning { background: #f59e0b; color: white; }
    .btn-primary { background: linear-gradient(135deg, #d4a84b 0%, #b87333 100%); color: #1e2832; font-weight: 600; }
    .btn-fx { background: #8b5cf6; color: white; }
    
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
      border-radius: 6px; font-size: 1rem; box-sizing: border-box;
    }
    .modal-actions { display: flex; gap: 1rem; justify-content: flex-end; margin-top: 1.5rem; }
    .no-wallets { color: #9ca3af; font-style: italic; margin: 0; padding: 0.75rem; background: #f9fafb; border-radius: 6px; }
    
    /* FX Preview - Enhanced */
    .fx-preview {
      border-radius: 12px;
      padding: 1rem;
      margin: 1rem 0;
    }
    
    .fx-preview.loading {
      background: #f9fafb;
      border: 1px dashed #d1d5db;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.75rem;
      color: #6b7280;
      font-size: 0.875rem;
    }
    
    .fx-loading-spinner {
      width: 18px;
      height: 18px;
      border: 2px solid #e5e7eb;
      border-top-color: #d4a84b;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    
    .fx-preview.success {
      background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
      border: 1px solid #6ee7b7;
    }
    
    .fx-preview.hint {
      background: #fffbeb;
      border: 1px dashed #fcd34d;
      text-align: center;
      color: #92400e;
      font-size: 0.875rem;
    }
    
    .fx-preview-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.75rem;
    }
    
    .fx-icon {
      font-size: 1.25rem;
    }
    
    .fx-preview-label {
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #059669;
      font-weight: 600;
    }
    
    .fx-conversion {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      margin: 0.75rem 0;
    }
    
    .fx-from, .fx-to {
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    
    .fx-amount {
      font-size: 1.25rem;
      font-weight: 700;
      color: #2d3a4a;
    }
    
    .fx-amount.highlight {
      color: #059669;
      font-size: 1.5rem;
    }
    
    .fx-currency {
      font-size: 0.75rem;
      color: #6b7280;
      font-weight: 500;
    }
    
    .fx-arrow {
      font-size: 1.5rem;
      color: #10b981;
      font-weight: bold;
    }
    
    .fx-rate-info {
      display: flex;
      justify-content: center;
      gap: 0.5rem;
      font-size: 0.75rem;
      color: #6b7280;
      padding-top: 0.5rem;
      border-top: 1px dashed #a7f3d0;
      margin-top: 0.5rem;
    }
    
    .fx-rate-label {
      font-weight: 500;
    }
    
    .fx-rate-value {
      font-family: monospace;
    }
    
    /* Error State */
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
    
    /* Skeleton in header */
    .wallet-header-card.skeleton {
      background: linear-gradient(135deg, #e5e7eb 0%, #f3f4f6 100%);
    }
    .wallet-header-card.skeleton .wallet-meta {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }
  `]
})
export class WalletDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(WalletApiService);
  private readonly toast = inject(ToastService);

  wallet = signal<Wallet | null>(null);
  balance = signal<Balance | null>(null);
  transactions = signal<Page<Transaction> | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  showModal = signal<'deposit' | 'withdraw' | 'transfer' | 'fx' | null>(null);
  operationLoading = signal(false);

  operationAmount = 0;
  targetWalletId = '';
  transferDescription = '';
  fxTargetWalletId = '';

  // FX Preview
  fxPreview = signal<{ rate: number; targetAmount: number; targetCurrency: string } | null>(null);
  fxPreviewLoading = signal(false);
  private fxPreviewDebounceTimer: ReturnType<typeof setTimeout> | null = null;

  // Other wallets for transfer/FX selection
  allWallets = signal<{ wallet: Wallet; balance: number | null }[]>([]);
  
  // Computed: all wallets except current one (for transfer)
  otherWallets = computed(() => 
    this.allWallets().filter(w => w.wallet.id !== this.wallet()?.id && w.wallet.status === 'ACTIVE')
  );
  
  // Computed: wallets with different currency (for FX)
  fxTargetWallets = computed(() =>
    this.allWallets().filter(w => 
      w.wallet.id !== this.wallet()?.id && 
      w.wallet.baseCurrency !== this.wallet()?.baseCurrency &&
      w.wallet.status === 'ACTIVE'
    )
  );

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.loadWallet(id);
    this.loadAllWallets();
  }

  loadWallet(id: string) {
    this.loading.set(true);
    this.api.getWallet(id).subscribe({
      next: (wallet) => {
        this.wallet.set(wallet);
        this.loading.set(false);
        this.loadBalance(id);
        this.loadTransactions(0);
      },
      error: (err) => {
        this.error.set(err.userMessage || err.error?.detail || 'Failed to load wallet');
        this.loading.set(false);
      }
    });
  }

  loadAllWallets() {
    this.api.getWallets().subscribe({
      next: (wallets) => {
        const walletsWithBalance = wallets.map(w => ({ wallet: w, balance: null as number | null }));
        this.allWallets.set(walletsWithBalance);
        // Load balances for each wallet
        wallets.forEach((w, index) => {
          this.api.getBalance(w.id).subscribe({
            next: (bal) => {
              const current = this.allWallets();
              current[index] = { ...current[index], balance: bal.balance.amount };
              this.allWallets.set([...current]);
            }
          });
        });
      }
    });
  }

  loadBalance(walletId: string) {
    this.api.getBalance(walletId).subscribe({
      next: (balance) => this.balance.set(balance)
    });
  }

  loadTransactions(page: number) {
    const walletId = this.wallet()?.id;
    if (!walletId) return;
    this.api.getTransactions(walletId, { page, size: 10, sort: 'occurredAt,desc' }).subscribe({
      next: (txs) => this.transactions.set(txs)
    });
  }

  submitDeposit(event: Event) {
    event.preventDefault();
    const wallet = this.wallet();
    if (!wallet) return;
    
    this.operationLoading.set(true);
    this.api.deposit({
      walletId: wallet.id,
      amount: this.operationAmount,
      currency: wallet.baseCurrency
    }).subscribe({
      next: () => {
        this.showModal.set(null);
        this.operationLoading.set(false);
        this.operationAmount = 0;
        this.toast.success('Deposit successful');
        this.loadBalance(wallet.id);
        this.loadTransactions(0);
      },
      error: () => {
        this.operationLoading.set(false);
      }
    });
  }

  submitWithdraw(event: Event) {
    event.preventDefault();
    const wallet = this.wallet();
    if (!wallet) return;
    
    this.operationLoading.set(true);
    this.api.withdraw({
      walletId: wallet.id,
      amount: this.operationAmount,
      currency: wallet.baseCurrency
    }).subscribe({
      next: () => {
        this.showModal.set(null);
        this.operationLoading.set(false);
        this.operationAmount = 0;
        this.toast.success('Withdrawal successful');
        this.loadBalance(wallet.id);
        this.loadTransactions(0);
      },
      error: () => {
        this.operationLoading.set(false);
      }
    });
  }

  submitTransfer(event: Event) {
    event.preventDefault();
    const wallet = this.wallet();
    if (!wallet) return;
    
    this.operationLoading.set(true);
    this.api.transfer({
      sourceWalletId: wallet.id,
      targetWalletId: this.targetWalletId,
      amount: this.operationAmount,
      currency: wallet.baseCurrency,
      description: this.transferDescription || undefined
    }).subscribe({
      next: () => {
        this.showModal.set(null);
        this.operationLoading.set(false);
        this.operationAmount = 0;
        this.targetWalletId = '';
        this.transferDescription = '';
        this.toast.success('Transfer successful');
        this.loadBalance(wallet.id);
        this.loadTransactions(0);
        this.loadAllWallets();
      },
      error: () => {
        this.operationLoading.set(false);
      }
    });
  }

  submitFxConvert(event: Event) {
    event.preventDefault();
    const wallet = this.wallet();
    if (!wallet) return;
    
    this.operationLoading.set(true);
    this.api.fxConvert({
      sourceWalletId: wallet.id,
      targetWalletId: this.fxTargetWalletId,
      sourceCurrency: wallet.baseCurrency,
      amount: this.operationAmount
    }).subscribe({
      next: (result) => {
        this.showModal.set(null);
        this.operationLoading.set(false);
        this.operationAmount = 0;
        this.fxTargetWalletId = '';
        this.fxPreview.set(null);
        this.toast.success(`Converted ${result.sourceAmount.amount} ${result.sourceAmount.currency} → ${result.targetAmount.amount} ${result.targetAmount.currency} (Rate: ${result.exchangeRate})`);
        this.loadBalance(wallet.id);
        this.loadTransactions(0);
        this.loadAllWallets();
      },
      error: () => {
        this.operationLoading.set(false);
      }
    });
  }

  // FX Preview - called when amount or target wallet changes (debounced)
  updateFxPreview() {
    // Clear previous timer
    if (this.fxPreviewDebounceTimer) {
      clearTimeout(this.fxPreviewDebounceTimer);
    }

    const wallet = this.wallet();
    if (!wallet || !this.fxTargetWalletId || this.operationAmount <= 0) {
      this.fxPreview.set(null);
      this.fxPreviewLoading.set(false);
      return;
    }

    const targetWallet = this.fxTargetWallets().find(w => w.wallet.id === this.fxTargetWalletId);
    if (!targetWallet) {
      this.fxPreview.set(null);
      this.fxPreviewLoading.set(false);
      return;
    }

    // Show loading immediately
    this.fxPreviewLoading.set(true);

    // Debounce: wait 500ms before making API call
    this.fxPreviewDebounceTimer = setTimeout(() => {
      this.api.getFxRate(wallet.baseCurrency, targetWallet.wallet.baseCurrency, this.operationAmount).subscribe({
        next: (rate) => {
          this.fxPreview.set({
            rate: rate.rate,
            targetAmount: rate.targetAmount,
            targetCurrency: rate.toCurrency
          });
          this.fxPreviewLoading.set(false);
        },
        error: () => {
          this.fxPreview.set(null);
          this.fxPreviewLoading.set(false);
        }
      });
    }, 500);
  }
}
