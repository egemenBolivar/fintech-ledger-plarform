import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dialog-overlay" (click)="onCancel()">
      <div class="dialog" (click)="$event.stopPropagation()">
        <div class="dialog-icon" [class]="type()">
          @switch (type()) {
            @case ('danger') { <span>⚠</span> }
            @case ('warning') { <span>⚠</span> }
            @case ('info') { <span>ℹ</span> }
            @default { <span>?</span> }
          }
        </div>
        <h2 class="dialog-title">{{ title() }}</h2>
        <p class="dialog-message">{{ message() }}</p>
        <div class="dialog-actions">
          <button class="btn btn-secondary" (click)="onCancel()">
            {{ cancelText() }}
          </button>
          <button class="btn" [class]="'btn-' + type()" (click)="onConfirm()" [disabled]="loading()">
            @if (loading()) {
              <span class="btn-spinner"></span>
            }
            {{ confirmText() }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dialog-overlay {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.6);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 10000;
      animation: fadeIn 0.2s ease;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .dialog {
      background: white;
      border-radius: 16px;
      padding: 2rem;
      width: 100%;
      max-width: 400px;
      text-align: center;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
      animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
    }

    @keyframes slideUp {
      from {
        opacity: 0;
        transform: translateY(20px) scale(0.95);
      }
      to {
        opacity: 1;
        transform: translateY(0) scale(1);
      }
    }

    .dialog-icon {
      width: 64px;
      height: 64px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 1.5rem;
      font-size: 1.75rem;
    }

    .dialog-icon.danger {
      background: linear-gradient(135deg, #fee2e2, #fecaca);
      color: #dc2626;
    }

    .dialog-icon.warning {
      background: linear-gradient(135deg, #fef3c7, #fde68a);
      color: #d97706;
    }

    .dialog-icon.info {
      background: linear-gradient(135deg, #fef3c7, #fde68a);
      color: #b87333;
    }

    .dialog-title {
      margin: 0 0 0.75rem;
      font-size: 1.25rem;
      color: #2d3a4a;
    }

    .dialog-message {
      margin: 0 0 1.5rem;
      color: #6b7280;
      line-height: 1.5;
    }

    .dialog-actions {
      display: flex;
      gap: 1rem;
      justify-content: center;
    }

    .btn {
      padding: 0.75rem 1.5rem;
      border-radius: 8px;
      border: none;
      font-size: 0.875rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .btn-secondary {
      background: #f3f4f6;
      color: #374151;
    }

    .btn-secondary:hover {
      background: #e5e7eb;
    }

    .btn-danger {
      background: #ef4444;
      color: white;
    }

    .btn-danger:hover {
      background: #dc2626;
    }

    .btn-warning {
      background: #f59e0b;
      color: white;
    }

    .btn-warning:hover {
      background: #d97706;
    }

    .btn-info {
      background: linear-gradient(135deg, #d4a84b 0%, #b87333 100%);
      color: #1e2832;
      font-weight: 600;
    }

    .btn-info:hover {
      background: linear-gradient(135deg, #e0b85c 0%, #c98040 100%);
    }

    .btn:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }

    .btn-spinner {
      width: 16px;
      height: 16px;
      border: 2px solid transparent;
      border-top-color: currentColor;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class ConfirmDialogComponent {
  title = input<string>('Confirm Action');
  message = input<string>('Are you sure you want to proceed?');
  type = input<'danger' | 'warning' | 'info'>('warning');
  confirmText = input<string>('Confirm');
  cancelText = input<string>('Cancel');
  loading = input<boolean>(false);

  confirmed = output<void>();
  cancelled = output<void>();

  onConfirm(): void {
    this.confirmed.emit();
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
