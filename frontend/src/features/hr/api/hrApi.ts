import { apiClient } from '@/shared/api/apiClient'
import type {
  EmployeeSummary,
  EmployeeDetail,
  EmployeeAssignment,
  SalaryPolicy,
  CreateEmployeePayload,
  UpdateEmployeePayload,
  AssignBranchPayload,
  CreateSalaryPolicyPayload,
  EmployeePageResponse,
  EmployeeFilterParams,
} from '../types/hr.types'

/**
 * Extracts payload whether backend wraps response in ApiResponse<T> or returns raw DTO.
 */
function extractData<T>(res: { data: unknown }): T {
  if (
    res.data &&
    typeof res.data === 'object' &&
    'data' in res.data &&
    (res.data as { data: unknown }).data !== undefined
  ) {
    return (res.data as { data: T }).data
  }
  return res.data as T
}

/**
 * Formats numeric or string amount to Vietnamese Dong display.
 * E.g.: 25000 -> "25.000 ₫"
 */
export function formatVnd(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') return '0 ₫'
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(num)
}

/**
 * Parses user input currency string (with dots, commas, symbols) into pure number.
 */
export function parseMoneyInput(value: string): number {
  if (!value) return 0
  const clean = value.replace(/[^\d.-]/g, '')
  const parsed = parseFloat(clean)
  return isNaN(parsed) ? 0 : parsed
}

export const hrApi = {
  /**
   * 3.1 GET /api/v1/branches/{branchId}/hr/employees
   */
  async getEmployees(
    branchId: string,
    params?: EmployeeFilterParams
  ): Promise<EmployeePageResponse> {
    const res = await apiClient.get<EmployeePageResponse>(
      `/branches/${branchId}/hr/employees`,
      {
        params: {
          search: params?.search || undefined,
          status: params?.status === 'ALL' ? undefined : params?.status || undefined,
          page: params?.page ?? 0,
          size: params?.size ?? 20,
        },
      }
    )
    return extractData<EmployeePageResponse>(res)
  },

  /**
   * 3.3 GET /api/v1/branches/{branchId}/hr/employees/{id}
   */
  async getEmployeeDetail(
    branchId: string,
    employeeId: string
  ): Promise<EmployeeDetail> {
    const res = await apiClient.get<EmployeeDetail>(
      `/branches/${branchId}/hr/employees/${employeeId}`
    )
    return extractData<EmployeeDetail>(res)
  },

  /**
   * 3.2 POST /api/v1/branches/{branchId}/hr/employees
   */
  async createEmployee(
    branchId: string,
    payload: CreateEmployeePayload
  ): Promise<EmployeeDetail | EmployeeSummary> {
    const res = await apiClient.post<EmployeeDetail>(
      `/branches/${branchId}/hr/employees`,
      payload
    )
    return extractData<EmployeeDetail | EmployeeSummary>(res)
  },

  /**
   * 3.4 PUT /api/v1/branches/{branchId}/hr/employees/{id}
   */
  async updateEmployee(
    branchId: string,
    employeeId: string,
    payload: UpdateEmployeePayload
  ): Promise<EmployeeDetail | EmployeeSummary> {
    const res = await apiClient.put<EmployeeDetail>(
      `/branches/${branchId}/hr/employees/${employeeId}`,
      payload
    )
    return extractData<EmployeeDetail | EmployeeSummary>(res)
  },

  /**
   * 3.5 POST /api/v1/branches/{branchId}/hr/employees/{id}/assignments (TC-015)
   */
  async assignBranch(
    branchId: string,
    employeeId: string,
    payload: AssignBranchPayload
  ): Promise<EmployeeAssignment> {
    const res = await apiClient.post<EmployeeAssignment>(
      `/branches/${branchId}/hr/employees/${employeeId}/assignments`,
      payload
    )
    return extractData<EmployeeAssignment>(res)
  },

  /**
   * GET /api/v1/branches/{branchId}/hr/employees/{id}/salary-policies
   */
  async getSalaryPolicies(
    branchId: string,
    employeeId: string
  ): Promise<SalaryPolicy[]> {
    try {
      const res = await apiClient.get<SalaryPolicy[]>(
        `/branches/${branchId}/hr/employees/${employeeId}/salary-policies`
      )
      return extractData<SalaryPolicy[]>(res)
    } catch {
      // Fallback: extract from employee detail
      const detail = await hrApi.getEmployeeDetail(branchId, employeeId)
      return detail.salaryPolicies || []
    }
  },

  /**
   * 3.6 POST /api/v1/branches/{branchId}/hr/employees/{id}/salary-policies (TC-013)
   */
  async createSalaryPolicy(
    branchId: string,
    employeeId: string,
    payload: CreateSalaryPolicyPayload
  ): Promise<SalaryPolicy> {
    const res = await apiClient.post<SalaryPolicy>(
      `/branches/${branchId}/hr/employees/${employeeId}/salary-policies`,
      payload
    )
    return extractData<SalaryPolicy>(res)
  },

  /**
   * 3.7 GET /api/v1/branches/{branchId}/hr/employees/{id}/effective-salary-policy (TC-013)
   */
  async getEffectiveSalaryPolicy(
    branchId: string,
    employeeId: string,
    date: string
  ): Promise<SalaryPolicy> {
    const res = await apiClient.get<SalaryPolicy>(
      `/branches/${branchId}/hr/employees/${employeeId}/effective-salary-policy`,
      {
        params: { date },
      }
    )
    return extractData<SalaryPolicy>(res)
  },
}
