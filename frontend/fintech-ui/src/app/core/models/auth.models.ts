export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  email: string;
  fullName: string;
}

export interface AuthUser {
  email: string;
  fullName: string;
  isAuthenticated: boolean;
}
