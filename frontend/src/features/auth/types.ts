export interface UserDto {
  id: string
  loginIdentifier: string
  displayName: string
  status: 'ACTIVE' | 'SUSPENDED' | 'DISABLED'
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresInSeconds: number
  user: UserDto
}

export interface TokenRefreshResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
}

export interface LoginCredentials {
  loginIdentifier: string
  password: string
}

export interface ApiProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  timestamp?: string
  fieldErrors?: Record<string, string>
}
