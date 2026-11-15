import type { RoleKey, ScopedRoleGrantDto, UserDto } from '../auth/types'

export interface BusinessEntityDto {
  id: string
  entityCode: string
  registeredName: string
  taxCode: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
}

export interface BranchDto {
  id: string
  businessEntityId: string
  branchCode: string
  name: string
  timeZone: string
  status: 'ACTIVE' | 'INACTIVE' | 'DRAFT'
  createdAt: string
}

export interface CreateBusinessEntityRequest {
  entityCode: string
  registeredName: string
  taxCode: string
  status: 'ACTIVE' | 'INACTIVE'
}

export interface CreateBranchRequest {
  branchCode: string
  name: string
  timeZone: string
  status: 'ACTIVE' | 'INACTIVE' | 'DRAFT'
}

export interface CreateUserRequest {
  loginIdentifier: string
  password: string
  displayName: string
}

export interface UpdateUserStatusRequest {
  status: 'ACTIVE' | 'SUSPENDED'
}

export interface GrantRoleRequest {
  businessEntityId: string
  branchId?: string | null
  userId: string
  roleKey: RoleKey
  actionScope?: string
  rowScope?: string
  reportPermissionGroup?: string
  validFrom: string
  validTo?: string | null
}

export type { RoleKey, ScopedRoleGrantDto, UserDto }
