export type RoleKey =
  | 'SYSTEM_ADMIN'
  | 'BRANCH_OWNER'
  | 'MANAGER'
  | 'FINANCE'
  | 'EMPLOYEE'
  | 'INVESTOR'

export interface ScopedRoleGrantDto {
  id: string
  businessEntityId: string
  branchId: string | null
  userId: string
  roleKey: RoleKey
  actionScope: string
  rowScope: string | null
  reportPermissionGroup: string | null
  validFrom: string
  validTo: string | null
  grantedBy: string
  createdAt: string
}

export interface UserDto {
  id: string
  loginIdentifier: string
  displayName: string
  status: 'ACTIVE' | 'SUSPENDED' | 'DISABLED'
  createdAt: string
  grants?: ScopedRoleGrantDto[]
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
