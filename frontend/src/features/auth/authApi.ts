import { apiClient } from '@/shared/api/apiClient'
import type { ApiResponse } from '@/shared/types'
import type { AuthResponse, LoginCredentials, UserDto } from './types'

export const authApi = {
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>(
      '/auth/login',
      credentials
    )
    return response.data.data
  },

  async logout(): Promise<void> {
    try {
      await apiClient.post<ApiResponse<null>>('/auth/logout')
    } catch {
      // Ignore network failure on logout; local state will be purged regardless
    }
  },

  async getCurrentUser(): Promise<UserDto> {
    const response = await apiClient.get<ApiResponse<UserDto>>('/auth/me')
    return response.data.data
  },
}
