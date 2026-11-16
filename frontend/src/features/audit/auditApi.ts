import { apiClient } from '@/shared/api/apiClient'
import type { ApiResponse } from '@/shared/types'
import type { AuditPageResponse } from './types'

export const auditApi = {
  /**
   * Truy vấn nhật ký kiểm toán toàn hệ thống (Dành cho System Admin)
   * GET /api/v1/audit-events
   */
  async getGlobalAuditEvents(page: number = 0, size: number = 20): Promise<AuditPageResponse> {
    const res = await apiClient.get<ApiResponse<AuditPageResponse> | AuditPageResponse>('/audit-events', {
      params: { page, size },
    })
    const payload = res.data as ApiResponse<AuditPageResponse>
    if (payload && typeof payload === 'object' && 'data' in payload && payload.data) {
      return payload.data
    }
    return res.data as AuditPageResponse
  },

  /**
   * Truy vấn nhật ký kiểm toán theo phạm vi chi nhánh (Branch-scoped audit events)
   * GET /api/v1/branches/{branchId}/audit-events
   */
  async getBranchAuditEvents(
    branchId: string,
    page: number = 0,
    size: number = 20
  ): Promise<AuditPageResponse> {
    const res = await apiClient.get<ApiResponse<AuditPageResponse> | AuditPageResponse>(
      `/branches/${branchId}/audit-events`,
      {
        params: { page, size },
      }
    )
    const payload = res.data as ApiResponse<AuditPageResponse>
    if (payload && typeof payload === 'object' && 'data' in payload && payload.data) {
      return payload.data
    }
    return res.data as AuditPageResponse
  },
}
