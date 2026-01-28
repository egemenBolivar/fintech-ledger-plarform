import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="spinner-container" [class.overlay]="overlay()" [class.inline]="!overlay()">
      <div class="spinner" [class.small]="size() === 'small'" [class.large]="size() === 'large'">
        <div class="spinner-ring"></div>
        <div class="spinner-ring"></div>
        <div class="spinner-ring"></div>
      </div>
      @if (message()) {
        <p class="spinner-message">{{ message() }}</p>
      }
    </div>
  `,
  styles: [`
    .spinner-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 1rem;
    }

    .spinner-container.overlay {
      position: fixed;
      inset: 0;
      background: rgba(26, 26, 46, 0.8);
      backdrop-filter: blur(4px);
      z-index: 9998;
    }

    .spinner-container.inline {
      padding: 2rem;
    }

    .spinner {
      position: relative;
      width: 48px;
      height: 48px;
    }

    .spinner.small {
      width: 24px;
      height: 24px;
    }

    .spinner.large {
      width: 72px;
      height: 72px;
    }

    .spinner-ring {
      position: absolute;
      inset: 0;
      border-radius: 50%;
      border: 3px solid transparent;
      border-top-color: #3b82f6;
      animation: spin 1.2s cubic-bezier(0.5, 0, 0.5, 1) infinite;
    }

    .spinner.small .spinner-ring {
      border-width: 2px;
    }

    .spinner.large .spinner-ring {
      border-width: 4px;
    }

    .spinner-ring:nth-child(1) {
      animation-delay: -0.45s;
    }

    .spinner-ring:nth-child(2) {
      animation-delay: -0.3s;
      border-top-color: #8b5cf6;
    }

    .spinner-ring:nth-child(3) {
      animation-delay: -0.15s;
      border-top-color: #10b981;
    }

    @keyframes spin {
      0% {
        transform: rotate(0deg);
      }
      100% {
        transform: rotate(360deg);
      }
    }

    .spinner-message {
      color: white;
      font-size: 0.875rem;
      margin: 0;
      opacity: 0.9;
    }

    .inline .spinner-message {
      color: #6b7280;
    }
  `]
})
export class LoadingSpinnerComponent {
  overlay = input(false);
  size = input<'small' | 'medium' | 'large'>('medium');
  message = input<string>('');
}
