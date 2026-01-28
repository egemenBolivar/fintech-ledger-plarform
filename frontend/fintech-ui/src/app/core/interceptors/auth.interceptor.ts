import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  
  // Skip auth header for public endpoints
  const isAuthEndpoint = req.url.includes('/api/v1/auth/');
  
  if (!isAuthEndpoint) {
    const token = authService.getAccessToken();
    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Handle 401 Unauthorized - try refresh token
      if (error.status === 401 && !isAuthEndpoint) {
        return authService.refreshToken().pipe(
          switchMap(response => {
            if (response) {
              // Retry the original request with new token
              const newReq = req.clone({
                setHeaders: {
                  Authorization: `Bearer ${response.accessToken}`
                }
              });
              return next(newReq);
            }
            return throwError(() => error);
          }),
          catchError(() => {
            authService.logout();
            return throwError(() => error);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
