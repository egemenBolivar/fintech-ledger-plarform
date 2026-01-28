import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unexpected error occurred';

      if (error.error instanceof ErrorEvent) {
        // Client-side error
        errorMessage = error.error.message;
      } else {
        // Server-side error - RFC 7807 ProblemDetail format
        if (error.error?.detail) {
          errorMessage = error.error.detail;
        } else if (error.error?.title) {
          errorMessage = error.error.title;
        } else {
          switch (error.status) {
            case 0:
              errorMessage = 'Unable to connect to server. Please check your connection.';
              break;
            case 400:
              errorMessage = 'Invalid request. Please check your input.';
              break;
            case 401:
              errorMessage = 'Unauthorized. Please log in again.';
              break;
            case 403:
              errorMessage = 'Access denied.';
              break;
            case 404:
              errorMessage = 'Resource not found.';
              break;
            case 409:
              errorMessage = 'Conflict. The operation could not be completed.';
              break;
            case 422:
              errorMessage = 'Validation error. Please check your input.';
              break;
            case 500:
              errorMessage = 'Server error. Please try again later.';
              break;
            default:
              errorMessage = `Error ${error.status}: ${error.statusText}`;
          }
        }
      }

      // Don't show toast for FX preview errors (user is still typing)
      const isFxPreview = req.url.includes('/fx/rate');
      if (!isFxPreview) {
        toast.error(errorMessage);
      }

      return throwError(() => ({
        ...error,
        userMessage: errorMessage
      }));
    })
  );
};
