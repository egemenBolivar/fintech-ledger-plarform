import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { LoadingService } from '../services/loading.service';

// URLs that should NOT trigger global loading indicator
const SILENT_URLS = [
  '/fx/rate',       // FX preview (has its own loading)
  '/balance',       // Balance refresh (inline loading)
  '/transactions'   // Transaction list (inline loading)
];

export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loading = inject(LoadingService);

  // Check if this request should be silent
  const isSilent = SILENT_URLS.some(url => req.url.includes(url));

  if (!isSilent) {
    loading.show();
  }

  return next(req).pipe(
    finalize(() => {
      if (!isSilent) {
        loading.hide();
      }
    })
  );
};
