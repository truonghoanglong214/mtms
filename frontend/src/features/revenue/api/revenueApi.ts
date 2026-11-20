import { apiClient } from '@/shared/api/apiClient'
import type {
  BusinessDayDto,
  SetBusinessDayCommand,
  DailyRevenueReportDto,
  CreateDailyRevenueCommand,
  ConfirmRevenueCommand,
  MissingDateDto,
  DailyRevenueFilterParams,
  BusinessDayFilterParams,
  DailyRevenueListResponse,
} from '../types/revenue.types'
import type { CashLedgerEntryDto } from '@/features/finance'

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

export const revenueApi = {
  /**
   * Get business calendar days for branch (P0-REV-01 / BR-REV-02 / TC-005)
   * GET /api/v1/branches/{branchId}/business-days
   */
  async getBusinessDays(
    branchId: string,
    params?: BusinessDayFilterParams
  ): Promise<BusinessDayDto[]> {
    const res = await apiClient.get(`/branches/${branchId}/business-days`, {
      params,
    })
    return extractData<BusinessDayDto[]>(res)
  },

  /**
   * Update operation status of a business day (OPEN or CLOSED with reason)
   * PUT /api/v1/branches/{branchId}/business-days/{date}
   */
  async setBusinessDay(
    branchId: string,
    businessDate: string,
    command: SetBusinessDayCommand
  ): Promise<BusinessDayDto> {
    const res = await apiClient.put(
      `/branches/${branchId}/business-days/${businessDate}`,
      command
    )
    return extractData<BusinessDayDto>(res)
  },

  /**
   * List daily revenue reports with filters & pagination (P0-REV-03 / BR-REV-01)
   * GET /api/v1/branches/{branchId}/daily-revenue
   */
  async getDailyRevenueList(
    branchId: string,
    params?: DailyRevenueFilterParams
  ): Promise<DailyRevenueListResponse | DailyRevenueReportDto[]> {
    const res = await apiClient.get(`/branches/${branchId}/daily-revenue`, {
      params,
    })
    return extractData<DailyRevenueListResponse | DailyRevenueReportDto[]>(res)
  },

  /**
   * Get single daily revenue report by ID
   * GET /api/v1/branches/{branchId}/daily-revenue/{id}
   */
  async getDailyRevenueById(
    branchId: string,
    id: string
  ): Promise<DailyRevenueReportDto> {
    const res = await apiClient.get(`/branches/${branchId}/daily-revenue/${id}`)
    return extractData<DailyRevenueReportDto>(res)
  },

  /**
   * Capture daily aggregate revenue report (P0-REV-03 / BR-REV-01 / TC-006)
   * POST /api/v1/branches/{branchId}/daily-revenue
   */
  async createDailyRevenue(
    branchId: string,
    command: CreateDailyRevenueCommand
  ): Promise<DailyRevenueReportDto> {
    const res = await apiClient.post(
      `/branches/${branchId}/daily-revenue`,
      command
    )
    return extractData<DailyRevenueReportDto>(res)
  },

  /**
   * Confirm daily revenue report (P0-REV-03 / BR-REV-03 / TC-006)
   * POST /api/v1/branches/{branchId}/daily-revenue/{id}/confirm
   */
  async confirmDailyRevenue(
    branchId: string,
    id: string,
    command?: ConfirmRevenueCommand
  ): Promise<DailyRevenueReportDto> {
    const res = await apiClient.post(
      `/branches/${branchId}/daily-revenue/${id}/confirm`,
      command || {}
    )
    return extractData<DailyRevenueReportDto>(res)
  },

  /**
   * Get list of open business dates missing revenue reports (P0-REV-01 / TC-004)
   * GET /api/v1/branches/{branchId}/daily-revenue/missing-dates
   */
  async getMissingDates(
    branchId: string,
    params?: { startDate?: string; endDate?: string }
  ): Promise<MissingDateDto[]> {
    const res = await apiClient.get(
      `/branches/${branchId}/daily-revenue/missing-dates`,
      { params }
    )
    return extractData<MissingDateDto[]>(res)
  },

  /**
   * Get linked cash & bank ledger entries for confirmed revenue report (P0-REV-05 / BR-REV-04 / TC-007)
   * GET /api/v1/branches/{branchId}/daily-revenue/{reportId}/ledger-entries
   */
  async getRevenueLedgerEntries(
    branchId: string,
    reportId: string
  ): Promise<CashLedgerEntryDto[]> {
    const res = await apiClient.get(
      `/branches/${branchId}/daily-revenue/${reportId}/ledger-entries`
    )
    return extractData<CashLedgerEntryDto[]>(res)
  },
}

/**
 * Format numeric value to Vietnamese Dong (VNĐ)
 */
export function formatVnd(value: number | string | undefined | null): string {
  if (value === undefined || value === null || value === '') return '0 ₫'
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(num)
}

/**
 * Parse money input string into clean integer/number
 */
export function parseMoneyInput(raw: string): number {
  const cleaned = raw.replace(/[^\d.-]/g, '')
  const num = parseFloat(cleaned)
  return isNaN(num) ? 0 : num
}
