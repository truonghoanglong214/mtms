import { apiClient } from '@/shared/api/apiClient'
import type {
  PayrollPeriod,
  PayrollAdjustment,
  CreateAdjustmentPayload,
  PayrollPeriodStatus,
} from '../types/payroll.types'

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
 * Formats standard Vietnamese currency (VND) with thousands separators.
 * E.g.: 2350000 -> "2.350.000 ₫", -100000 -> "-100.000 ₫", 0 -> "0 ₫"
 */
export function formatVND(amount: number | null | undefined): string {
  if (amount === null || amount === undefined || isNaN(amount)) {
    return '0 ₫'
  }
  const rounded = Math.round(amount)
  return `${new Intl.NumberFormat('vi-VN').format(rounded)} ₫`
}

/**
 * Formats minutes into decimal hours string with one or two decimals.
 * E.g.: 6000 -> "100.0h", 450 -> "7.5h"
 */
export function formatHours(minutes: number | null | undefined): string {
  if (minutes === null || minutes === undefined || isNaN(minutes)) {
    return '0.0h'
  }
  const hours = minutes / 60
  return `${hours.toFixed(1)}h`
}

/**
 * Formats payable workdays with standard 2 decimal precision.
 * E.g.: 13.0 -> "13.00 công", 26.0 -> "26.00 công"
 */
export function formatWorkdays(workdays: number | null | undefined): string {
  if (workdays === null || workdays === undefined || isNaN(workdays)) {
    return '0.00 công'
  }
  return `${Number(workdays).toFixed(2)} công`
}

/**
 * Formats a ratio into percentage string.
 * E.g.: 0.5 -> "50.0%", 1.0 -> "100.0%"
 */
export function formatPercent(ratio: number | null | undefined): string {
  if (ratio === null || ratio === undefined || isNaN(ratio)) {
    return '0.0%'
  }
  return `${(ratio * 100).toFixed(1)}%`
}

export const payrollApi = {
  /**
   * 3.1 GET /api/v1/branches/{branchId}/hr/payroll-periods
   * Lấy danh sách các kỳ bảng lương theo năm/tháng hoặc trạng thái
   */
  async getPayrollPeriods(
    branchId: string,
    year?: number,
    status?: PayrollPeriodStatus
  ): Promise<PayrollPeriod[]> {
    const res = await apiClient.get<PayrollPeriod[]>(
      `/branches/${branchId}/hr/payroll-periods`,
      {
        params: {
          year: year || undefined,
          status: status || undefined,
        },
      }
    )
    return extractData<PayrollPeriod[]>(res)
  },

  /**
   * 3.2 GET /api/v1/branches/{branchId}/hr/payroll-periods/current
   * Lấy thông tin bảng lương hiện hành của một tháng (month=YYYY-MM-01).
   * Nếu chưa có kỳ, backend có thể trả về bản nháp khởi tạo hoặc 404.
   */
  async getCurrentPayrollPeriod(
    branchId: string,
    month: string
  ): Promise<PayrollPeriod> {
    const res = await apiClient.get<PayrollPeriod>(
      `/branches/${branchId}/hr/payroll-periods/current`,
      {
        params: { month },
      }
    )
    return extractData<PayrollPeriod>(res)
  },

  /**
   * 3.3 POST /api/v1/branches/{branchId}/hr/payroll-periods/generate
   * Tính toán lại toàn bộ bảng lương tháng (recalculate) từ dữ liệu chấm công mới nhất.
   */
  async generateOrRecalculatePayroll(
    branchId: string,
    monthStart: string
  ): Promise<PayrollPeriod> {
    const res = await apiClient.post<PayrollPeriod>(
      `/branches/${branchId}/hr/payroll-periods/generate`,
      { monthStart }
    )
    return extractData<PayrollPeriod>(res)
  },

  /**
   * 3.4 POST /api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/adjustments
   * Thêm khoản điều chỉnh lương (phụ cấp, thưởng, phạt, tạm ứng).
   */
  async addAdjustment(
    branchId: string,
    lineId: string,
    payload: CreateAdjustmentPayload
  ): Promise<PayrollAdjustment> {
    const res = await apiClient.post<PayrollAdjustment>(
      `/branches/${branchId}/hr/payroll-lines/${lineId}/adjustments`,
      payload
    )
    return extractData<PayrollAdjustment>(res)
  },

  /**
   * 3.5 DELETE /api/v1/branches/{branchId}/hr/payroll-adjustments/{adjId}
   * Xóa khoản điều chỉnh lương (chỉ khả dụng khi bảng lương ở trạng thái DRAFT).
   */
  async deleteAdjustment(branchId: string, adjId: string): Promise<void> {
    await apiClient.delete(`/branches/${branchId}/hr/payroll-adjustments/${adjId}`)
  },

  /**
   * 3.6 POST /api/v1/branches/{branchId}/hr/payroll-periods/{id}/approve
   * Chủ quán (Owner) hoặc Admin phê duyệt chốt bảng lương kỳ tháng (TC-017).
   */
  async approvePayrollPeriod(
    branchId: string,
    periodId: string
  ): Promise<PayrollPeriod> {
    const res = await apiClient.post<PayrollPeriod>(
      `/branches/${branchId}/hr/payroll-periods/${periodId}/approve`
    )
    return extractData<PayrollPeriod>(res)
  },
}
