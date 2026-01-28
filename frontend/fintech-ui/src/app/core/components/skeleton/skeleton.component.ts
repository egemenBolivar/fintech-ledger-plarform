import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="skeleton" 
         [class.circle]="type() === 'circle'"
         [class.text]="type() === 'text'"
         [class.card]="type() === 'card'"
         [style.width]="width()"
         [style.height]="height()">
    </div>
  `,
  styles: [`
    .skeleton {
      background: linear-gradient(
        90deg,
        #e5e7eb 0%,
        #f3f4f6 50%,
        #e5e7eb 100%
      );
      background-size: 200% 100%;
      animation: shimmer 1.5s infinite;
      border-radius: 6px;
    }

    .skeleton.circle {
      border-radius: 50%;
    }

    .skeleton.text {
      height: 1rem;
      border-radius: 4px;
    }

    .skeleton.card {
      height: 180px;
      border-radius: 12px;
    }

    @keyframes shimmer {
      0% {
        background-position: 200% 0;
      }
      100% {
        background-position: -200% 0;
      }
    }
  `]
})
export class SkeletonComponent {
  type = input<'text' | 'circle' | 'card' | 'custom'>('custom');
  width = input<string>('100%');
  height = input<string>('1rem');
}
