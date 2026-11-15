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
  BranchBankAccountDto,
  CreateBankAccountCommand,
  BranchReadinessDto,
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

  // Branch Bank Accounts & Readiness (BR-ORG-03, TC-003)
  async getBranchBankAccounts(branchId: string): Promise<BranchBankAccountDto[]> {
    const res = await apiClient.get<ApiResponse<BranchBankAccountDto[]>>(
      `/branches/${branchId}/bank-accounts`
    )
    return res.data.data
  },

  async createBranchBankAccount(
    branchId: string,
    cmd: CreateBankAccountCommand
  ): Promise<BranchBankAccountDto> {
    const res = await apiClient.post<ApiResponse<BranchBankAccountDto>>(
      `/branches/${branchId}/bank-accounts`,
      cmd
    )
    return res.data.data
  },

  async setPrimaryBankAccount(branchId: string, accountId: string): Promise<void> {
    await apiClient.patch<ApiResponse<void>>(
      `/branches/${branchId}/bank-accounts/${accountId}/primary`
    )
  },

  async getBranchReadiness(branchId: string): Promise<BranchReadinessDto> {
    const res = await apiClient.get<ApiResponse<BranchReadinessDto>>(
      `/branches/${branchId}/readiness`
    )
    return res.data.data
  },

  async activateBranch(branchId: string): Promise<BranchDto> {
    const res = await apiClient.patch<ApiResponse<BranchDto>>(
      `/branches/${branchId}/activate`
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
