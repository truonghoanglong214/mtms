import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthResponse, UserDto } from './types'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: UserDto | null
  isAuthenticated: boolean
  activeBranchId: string | null
  activeBranchName: string | null

  setAuth: (data: AuthResponse) => void
  setAccessToken: (token: string) => void
  setActiveBranch: (branchId: string | null, branchName: string | null) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      activeBranchId: null,
      activeBranchName: 'Tất cả chi nhánh',

      setAuth: (data: AuthResponse) =>
        set({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          user: data.user,
          isAuthenticated: true,
        }),

      setAccessToken: (token: string) =>
        set({
          accessToken: token,
          isAuthenticated: true,
        }),

      setActiveBranch: (branchId: string | null, branchName: string | null) =>
        set({
          activeBranchId: branchId,
          activeBranchName: branchName || 'Tất cả chi nhánh',
        }),

      logout: () =>
        set({
          accessToken: null,
          refreshToken: null,
          user: null,
          isAuthenticated: false,
          activeBranchId: null,
          activeBranchName: 'Tất cả chi nhánh',
        }),
    }),
    {
      name: 'mtms_auth_session',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
        activeBranchId: state.activeBranchId,
        activeBranchName: state.activeBranchName,
      }),
    }
  )
)
