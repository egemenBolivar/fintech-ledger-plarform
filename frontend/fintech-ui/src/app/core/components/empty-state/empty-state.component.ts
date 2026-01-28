import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="empty-state">
      <div class="empty-icon">
        <ng-content select="[icon]"></ng-content>
        @if (!hasIcon) {
          <span>📭</span>
        }
      </div>
      <h3 class="empty-title">{{ title() }}</h3>
      @if (message()) {
        <p class="empty-message">{{ message() }}</p>
      }
      <div class="empty-action">
        <ng-content></ng-content>
      </div>
    </div>
  `,
  styles: [`
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 3rem 2rem;
      text-align: center;
    }

    .empty-icon {
      font-size: 4rem;
      margin-bottom: 1.5rem;
      opacity: 0.8;
    }

    .empty-title {
      margin: 0 0 0.5rem;
      color: #1a1a2e;
      font-size: 1.25rem;
      font-weight: 600;
    }

    .empty-message {
      margin: 0 0 1.5rem;
      color: #6b7280;
      max-width: 300px;
      line-height: 1.5;
    }

    .empty-action {
      margin-top: 0.5rem;
    }
  `]
})
export class EmptyStateComponent {
  title = input<string>('No data');
  message = input<string>('');
  hasIcon = false; // TODO: detect ng-content
}
