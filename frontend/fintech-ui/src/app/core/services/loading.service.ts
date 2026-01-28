import { Injectable, signal, computed } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoadingService {
  private loadingCount = signal(0);
  private loadingMessage = signal<string>('');

  readonly isLoading = computed(() => this.loadingCount() > 0);
  readonly message = computed(() => this.loadingMessage());

  show(message: string = ''): void {
    this.loadingCount.update(c => c + 1);
    if (message) {
      this.loadingMessage.set(message);
    }
  }

  hide(): void {
    this.loadingCount.update(c => Math.max(0, c - 1));
    if (this.loadingCount() === 0) {
      this.loadingMessage.set('');
    }
  }

  reset(): void {
    this.loadingCount.set(0);
    this.loadingMessage.set('');
  }
}
