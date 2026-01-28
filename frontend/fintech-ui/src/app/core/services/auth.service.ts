import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, AuthUser } from '../models/auth.models';
import { ToastService } from './toast.service';

const TOKEN_KEY = 'fintech_access_token';
const REFRESH_TOKEN_KEY = 'fintech_refresh_token';
const USER_KEY = 'fintech_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  private readonly _user = signal<AuthUser | null>(this.loadUserFromStorage());
  
  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => !!this._user()?.isAuthenticated);
  readonly userEmail = computed(() => this._user()?.email ?? '');
  readonly userName = computed(() => this._user()?.fullName ?? '');

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/login', request).pipe(
      tap(response => this.handleAuthSuccess(response)),
      catchError(err => {
        this.toast.error(err.error?.detail || 'Login failed');
        throw err;
      })
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/register', request).pipe(
      tap(response => this.handleAuthSuccess(response)),
      catchError(err => {
        this.toast.error(err.error?.detail || 'Registration failed');
        throw err;
      })
    );
  }

  refreshToken(): Observable<AuthResponse | null> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return of(null);
    }

    return this.http.post<AuthResponse>('/api/v1/auth/refresh', { refreshToken }).pipe(
      tap(response => this.handleAuthSuccess(response)),
      catchError(() => {
        this.logout();
        return of(null);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this._user.set(null);
    this.router.navigate(['/login']);
    this.toast.info('You have been logged out');
  }

  getAccessToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  private handleAuthSuccess(response: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, response.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    
    const user: AuthUser = {
      email: response.email,
      fullName: response.fullName,
      isAuthenticated: true
    };
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this._user.set(user);
  }

  private loadUserFromStorage(): AuthUser | null {
    const userJson = localStorage.getItem(USER_KEY);
    if (userJson) {
      try {
        return JSON.parse(userJson);
      } catch {
        return null;
      }
    }
    return null;
  }
}
