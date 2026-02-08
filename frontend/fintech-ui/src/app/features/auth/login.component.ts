import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="auth-container">
      <div class="auth-card">
        <div class="auth-header">
          <img src="imagelogo.png" alt="Fintech Ledger Logo" class="auth-logo" />
          <h1 class="brand-title">WALLED</h1>
          <h2 class="brand-subtitle">WALLET LEDGER</h2>
          <p>Sign in to your account</p>
        </div>

        @if (isLogin()) {
          <form (ngSubmit)="login()">
            <div class="form-group">
              <label for="email">Email</label>
              <input 
                type="email" 
                id="email" 
                [(ngModel)]="email" 
                name="email" 
                placeholder="Enter your email"
                required 
              />
            </div>
            <div class="form-group">
              <label for="password">Password</label>
              <input 
                type="password" 
                id="password" 
                [(ngModel)]="password" 
                name="password" 
                placeholder="Enter your password"
                required 
              />
            </div>
            <button type="submit" class="btn btn-primary btn-full" [disabled]="loading()">
              {{ loading() ? 'Signing in...' : 'Sign In' }}
            </button>
          </form>
          <div class="auth-footer">
            <p>Don't have an account? <a (click)="isLogin.set(false)">Sign up</a></p>
          </div>
        } @else {
          <form (ngSubmit)="register()">
            <div class="form-group">
              <label for="fullName">Full Name</label>
              <input 
                type="text" 
                id="fullName" 
                [(ngModel)]="fullName" 
                name="fullName" 
                placeholder="Enter your full name"
                required 
              />
            </div>
            <div class="form-group">
              <label for="email">Email</label>
              <input 
                type="email" 
                id="email" 
                [(ngModel)]="email" 
                name="email" 
                placeholder="Enter your email"
                required 
              />
            </div>
            <div class="form-group">
              <label for="password">Password</label>
              <input 
                type="password" 
                id="password" 
                [(ngModel)]="password" 
                name="password" 
                placeholder="Min 6 characters"
                minlength="6"
                required 
              />
            </div>
            <button type="submit" class="btn btn-primary btn-full" [disabled]="loading()">
              {{ loading() ? 'Creating account...' : 'Create Account' }}
            </button>
          </form>
          <div class="auth-footer">
            <p>Already have an account? <a (click)="isLogin.set(true)">Sign in</a></p>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .auth-container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #1e2832 0%, #2d3a4a 50%, #3d4f61 100%);
      padding: 2rem;
    }

    .auth-card {
      background: white;
      border-radius: 16px;
      padding: 2.5rem;
      width: 100%;
      max-width: 400px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
    }

    .auth-header {
      text-align: center;
      margin-bottom: 2rem;
    }

    .auth-logo {
      width: 120px;
      height: auto;
      margin-bottom: 1rem;
    }

    .brand-title {
      margin: 0;
      color: #1e2a3a;
      font-size: 2.5rem;
      font-weight: 800;
      letter-spacing: 2px;
    }

    .brand-subtitle {
      margin: 0.25rem 0 1rem;
      color: #4a5568;
      font-size: 1rem;
      font-weight: 400;
      letter-spacing: 3px;
    }

    .auth-header h1 {
      margin: 0 0 0.5rem;
      color: #1a1a2e;
      font-size: 1.5rem;
    }

    .auth-header p {
      margin: 0;
      color: #6b7280;
    }

    .form-group {
      margin-bottom: 1.25rem;
    }

    .form-group label {
      display: block;
      margin-bottom: 0.5rem;
      font-weight: 500;
      color: #374151;
    }

    .form-group input {
      width: 100%;
      padding: 0.875rem 1rem;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      font-size: 1rem;
      transition: border-color 0.2s, box-shadow 0.2s;
      box-sizing: border-box;
    }

    .form-group input:focus {
      outline: none;
      border-color: #d4a84b;
      box-shadow: 0 0 0 3px rgba(212, 168, 75, 0.15);
    }

    .btn {
      padding: 0.875rem 1.5rem;
      border-radius: 8px;
      border: none;
      font-size: 1rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-primary {
      background: linear-gradient(135deg, #d4a84b 0%, #b87333 100%);
      color: #1e2832;
      font-weight: 600;
    }

    .btn-primary:hover {
      background: linear-gradient(135deg, #e0b85c 0%, #c98040 100%);
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(212, 168, 75, 0.3);
    }

    .btn-primary:disabled {
      background: linear-gradient(135deg, #c9c9c9 0%, #a0a0a0 100%);
      cursor: not-allowed;
      transform: none;
      box-shadow: none;
    }

    .btn-full {
      width: 100%;
    }

    .auth-footer {
      margin-top: 1.5rem;
      text-align: center;
      padding-top: 1.5rem;
      border-top: 1px solid #e5e7eb;
    }

    .auth-footer p {
      margin: 0;
      color: #6b7280;
    }

    .auth-footer a {
      color: #d4a84b;
      cursor: pointer;
      font-weight: 500;
    }

    .auth-footer a:hover {
      text-decoration: underline;
    }
  `]
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);

  isLogin = signal(true);
  loading = signal(false);
  
  email = '';
  password = '';
  fullName = '';

  login() {
    if (!this.email || !this.password) {
      this.toast.warning('Please fill in all fields');
      return;
    }

    this.loading.set(true);
    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.toast.success('Welcome back!');
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/wallets';
        this.router.navigateByUrl(returnUrl);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  register() {
    if (!this.email || !this.password || !this.fullName) {
      this.toast.warning('Please fill in all fields');
      return;
    }

    if (this.password.length < 6) {
      this.toast.warning('Password must be at least 6 characters');
      return;
    }

    this.loading.set(true);
    this.authService.register({ 
      email: this.email, 
      password: this.password, 
      fullName: this.fullName 
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.toast.success('Account created successfully!');
        this.router.navigate(['/wallets']);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }
}
