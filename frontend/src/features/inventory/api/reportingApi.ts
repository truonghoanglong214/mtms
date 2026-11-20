import { apiClient } from '@/shared/api/apiClient'
import type {
  InventoryDashboardDto,
  StockMovementPageResponse,
  StockMovementQueryParams,
  CogsReportDto,
} from '../types/reporting.types'

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

export const reportingApi = {
  /**
   * Get branch inventory dashboard summary KPI & Top valued items (P0-INV-06)
   * GET /api/v1/branches/{branchId}/inventory/dashboard
   */
  async getInventoryDashboard(
    branchId: string,
    startDate?: string,
    endDate?: string
  ): Promise<InventoryDashboardDto> {
    const res = await apiClient.get(`/branches/${branchId}/inventory/dashboard`, {
      params: {
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      },
    })
    return extractData<InventoryDashboardDto>(res)
  },

  /**
   * Query branch stock movements ledger with filters and pagination (P0-INV-06 / FE-P0-28)
   * GET /api/v1/branches/{branchId}/inventory/movements
   */
  async getStockMovements(
    branchId: string,
    params?: StockMovementQueryParams
  ): Promise<StockMovementPageResponse> {
    const res = await apiClient.get(`/branches/${branchId}/inventory/movements`, {
      params,
    })
    return extractData<StockMovementPageResponse>(res)
  },

  /**
   * Get period inventory-to-COGS reconciliation report (P0-INV-07 / TC-020)
   * GET /api/v1/branches/{branchId}/inventory/cogs-report
   */
  async getCogsReport(
    branchId: string,
    startDate?: string,
    endDate?: string
  ): Promise<CogsReportDto> {
    const res = await apiClient.get(`/branches/${branchId}/inventory/cogs-report`, {
      params: {
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      },
    })
    return extractData<CogsReportDto>(res)
  },
}
