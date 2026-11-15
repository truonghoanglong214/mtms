import { apiClient } from '@/shared/api/apiClient'
import type { ApiResponse } from '@/shared/types'
import type {
  BusinessEntityDto,
  BranchDto,
  CreateBusinessEntityRequest,
  CreateBranchRequest,
  CreateUserRequest,
  UpdateUserStatusRequest,
  GrantRoleRequest,
  ScopedRoleGrantDto,
  UserDto,
} from './types'

export const organizationApi = {
  // Business Entities
  async getEntities(): Promise<BusinessEntityDto[]> {
    const res = await apiClient.get<ApiResponse<BusinessEntityDto[]>>('/organizations/entities')
    return res.data.data
  },

  async createEntity(data: CreateBusinessEntityRequest): Promise<BusinessEntityDto> {
    const res = await apiClient.post<ApiResponse<BusinessEntityDto>>('/organizations/entities', data)
    return res.data.data
  },

  // Branches
  async getBranches(entityId: string): Promise<BranchDto[]> {
    const res = await apiClient.get<ApiResponse<BranchDto[]>>(`/organizations/entities/${entityId}/branches`)
    return res.data.data
  },

  async createBranch(entityId: string, data: CreateBranchRequest): Promise<BranchDto> {
    const res = await apiClient.post<ApiResponse<BranchDto>>(
      `/organizations/entities/${entityId}/branches`,
      data
    )
    return res.data.data
  },

  // Users
  async getUsers(): Promise<UserDto[]> {
    const res = await apiClient.get<ApiResponse<UserDto[]>>('/users')
    return res.data.data
  },

  async createUser(data: CreateUserRequest): Promise<UserDto> {
    const res = await apiClient.post<ApiResponse<UserDto>>('/users', data)
    return res.data.data
  },

  async updateUserStatus(userId: string, data: UpdateUserStatusRequest): Promise<UserDto> {
    const res = await apiClient.patch<ApiResponse<UserDto>>(`/users/${userId}/status`, data)
    return res.data.data
  },

  // Scoped Role Grants
  async grantRole(data: GrantRoleRequest): Promise<ScopedRoleGrantDto> {
    const res = await apiClient.post<ApiResponse<ScopedRoleGrantDto>>('/auth/grants', data)
    return res.data.data
  },

  async getUserGrants(userId: string): Promise<ScopedRoleGrantDto[]> {
    const res = await apiClient.get<ApiResponse<ScopedRoleGrantDto[]>>(`/auth/grants/user/${userId}`)
    return res.data.data
  },

  async revokeGrant(grantId: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`/auth/grants/${grantId}`)
  },
}
