import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast" [class]="toast.type">
          <div class="toast-icon">
            @switch (toast.type) {
              @case ('success') { <span>✓</span> }
              @case ('error') { <span>✕</span> }
              @case ('warning') { <span>⚠</span> }
              @case ('info') { <span>ℹ</span> }
            }
          </div>
          <div class="toast-content">
            <p class="toast-message">{{ toast.message }}</p>
          </div>
          <button class="toast-close" (click)="toastService.dismiss(toast.id)">
            <span>×</span>
          </button>
          <div class="toast-progress" [style.animation-duration.ms]="toast.duration"></div>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 12px;
      max-width: 400px;
      pointer-events: none;
    }

    .toast {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 16px;
      border-radius: 12px;
      background: #1a1a2e;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4), 
                  0 0 0 1px rgba(255, 255, 255, 0.05);
      backdrop-filter: blur(10px);
      animation: slideIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      pointer-events: auto;
      position: relative;
      overflow: hidden;
    }

    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateX(100%);
      }
      to {
        opacity: 1;
        transform: translateX(0);
      }
    }

    .toast-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 600;
      flex-shrink: 0;
    }

    .toast.success .toast-icon {
      background: linear-gradient(135deg, #10b981, #059669);
      color: white;
    }

    .toast.error .toast-icon {
      background: linear-gradient(135deg, #ef4444, #dc2626);
      color: white;
    }

    .toast.warning .toast-icon {
      background: linear-gradient(135deg, #f59e0b, #d97706);
      color: white;
    }

    .toast.info .toast-icon {
      background: linear-gradient(135deg, #3b82f6, #2563eb);
      color: white;
    }

    .toast-content {
      flex: 1;
      min-width: 0;
    }

    .toast-message {
      margin: 0;
      color: #e5e5e5;
      font-size: 14px;
      line-height: 1.5;
      word-wrap: break-word;
    }

    .toast-close {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 24px;
      height: 24px;
      border: none;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 6px;
      color: #9ca3af;
      cursor: pointer;
      transition: all 0.2s ease;
      flex-shrink: 0;
    }

    .toast-close:hover {
      background: rgba(255, 255, 255, 0.2);
      color: white;
    }

    .toast-close span {
      font-size: 18px;
      line-height: 1;
    }

    .toast-progress {
      position: absolute;
      bottom: 0;
      left: 0;
      height: 3px;
      background: rgba(255, 255, 255, 0.3);
      animation: progress linear forwards;
      border-radius: 0 0 0 12px;
    }

    @keyframes progress {
      from { width: 100%; }
      to { width: 0%; }
    }

    .toast.success .toast-progress { background: #10b981; }
    .toast.error .toast-progress { background: #ef4444; }
    .toast.warning .toast-progress { background: #f59e0b; }
    .toast.info .toast-progress { background: #3b82f6; }

    /* Hover efekti - progress durur */
    .toast:hover .toast-progress {
      animation-play-state: paused;
    }

    /* Border glow efekti */
    .toast.success { border-left: 3px solid #10b981; }
    .toast.error { border-left: 3px solid #ef4444; }
    .toast.warning { border-left: 3px solid #f59e0b; }
    .toast.info { border-left: 3px solid #3b82f6; }
  `]
})
export class ToastComponent {
  readonly toastService = inject(ToastService);
}
