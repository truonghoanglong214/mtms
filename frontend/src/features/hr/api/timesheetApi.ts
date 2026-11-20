import { apiClient } from '@/shared/api/apiClient'
import type {
  TimesheetEntry,
  MonthlyTimesheetSummary,
  CreateTimesheetPayload,
  BulkTimesheetPayload,
  ApproveTimesheetPayload,
  BulkApprovePayload,
  BulkApproveResponse,
  RejectTimesheetPayload,
  TimesheetFilterParams,
  TimesheetPageResponse,
} from '../types/timesheet.types'

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
 * Formats minutes into human-readable Vietnamese time.
 * E.g.: 480 -> "8h 00m", 510 -> "8h 30m", 0 -> "0h 00m"
 */
export function formatMinutesToHours(minutes: number | null | undefined): string {
  if (minutes === null || minutes === undefined || isNaN(minutes)) {
    return '0h 00m'
  }
  const positiveMin = Math.max(0, Math.floor(minutes))
  const hrs = Math.floor(positiveMin / 60)
  const mins = positiveMin % 60
  return `${hrs}h ${mins.toString().padStart(2, '0')}m`
}

/**
 * Formats minutes into decimal hours string.
 * E.g.: 480 -> "8.00h", 510 -> "8.50h"
 */
export function formatHoursDecimal(minutes: number | null | undefined): string {
  if (minutes === null || minutes === undefined || isNaN(minutes)) {
    return '0.00h'
  }
  const hours = (minutes / 60).toFixed(2)
  return `${hours}h`
}

/**
 * Formats payable workdays to string with unit.
 * E.g.: 1.0 -> "1.00 công", 0.5 -> "0.50 công"
 */
export function formatWorkdays(workdays: number | null | undefined): string {
  if (workdays === null || workdays === undefined || isNaN(workdays)) {
    return '0.00 công'
  }
  return `${Number(workdays).toFixed(2)} công`
}

/**
 * Converts hours decimal to integer minutes.
 * E.g.: 8 -> 480, 4.5 -> 270
 */
export function convertHoursToMinutes(hours: number): number {
  if (isNaN(hours) || hours <= 0) return 0
  return Math.round(hours * 60)
}

/**
 * Converts integer minutes to decimal hours.
 * E.g.: 480 -> 8, 270 -> 4.5
 */
export function convertMinutesToHours(minutes: number): number {
  if (isNaN(minutes) || minutes <= 0) return 0
  return Number((minutes / 60).toFixed(2))
}

export const timesheetApi = {
  /**
   * 3.1 GET /api/v1/branches/{branchId}/hr/timesheets
   */
  async getTimesheets(
    branchId: string,
    params?: TimesheetFilterParams
  ): Promise<TimesheetPageResponse> {
    const res = await apiClient.get<TimesheetPageResponse>(
      `/branches/${branchId}/hr/timesheets`,
      {
        params: {
          startDate: params?.startDate || undefined,
          endDate: params?.endDate || undefined,
          employeeId: params?.employeeId || undefined,
          status: params?.status === 'ALL' ? undefined : params?.status || undefined,
          page: params?.page ?? 0,
          size: params?.size ?? 50,
        },
      }
    )
    return extractData<TimesheetPageResponse>(res)
  },

  /**
   * 3.2 GET /api/v1/branches/{branchId}/hr/timesheets/monthly-summary
   */
  async getMonthlySummary(
    branchId: string,
    year: number,
    month: number
  ): Promise<MonthlyTimesheetSummary[]> {
    const res = await apiClient.get<MonthlyTimesheetSummary[]>(
      `/branches/${branchId}/hr/timesheets/monthly-summary`,
      {
        params: { year, month },
      }
    )
    return extractData<MonthlyTimesheetSummary[]>(res)
  },

  /**
   * 3.3 POST /api/v1/branches/{branchId}/hr/timesheets
   */
  async createOrUpdateTimesheet(
    branchId: string,
    payload: CreateTimesheetPayload
  ): Promise<TimesheetEntry> {
    const res = await apiClient.post<TimesheetEntry>(
      `/branches/${branchId}/hr/timesheets`,
      payload
    )
    return extractData<TimesheetEntry>(res)
  },

  /**
   * 3.4 POST /api/v1/branches/{branchId}/hr/timesheets/bulk
   */
  async bulkCreateTimesheets(
    branchId: string,
    payload: BulkTimesheetPayload
  ): Promise<TimesheetEntry[]> {
    const res = await apiClient.post<TimesheetEntry[]>(
      `/branches/${branchId}/hr/timesheets/bulk`,
      payload
    )
    return extractData<TimesheetEntry[]>(res)
  },

  /**
   * 3.5 POST /api/v1/branches/{branchId}/hr/timesheets/{id}/approve
   */
  async approveTimesheet(
    branchId: string,
    timesheetId: string,
    payload?: ApproveTimesheetPayload
  ): Promise<TimesheetEntry> {
    const res = await apiClient.post<TimesheetEntry>(
      `/branches/${branchId}/hr/timesheets/${timesheetId}/approve`,
      payload || {}
    )
    return extractData<TimesheetEntry>(res)
  },

  /**
   * 3.6 POST /api/v1/branches/{branchId}/hr/timesheets/bulk-approve
   */
  async bulkApproveTimesheets(
    branchId: string,
    payload: BulkApprovePayload
  ): Promise<BulkApproveResponse> {
    const res = await apiClient.post<BulkApproveResponse>(
      `/branches/${branchId}/hr/timesheets/bulk-approve`,
      payload
    )
    return extractData<BulkApproveResponse>(res)
  },

  /**
   * 3.7 POST /api/v1/branches/{branchId}/hr/timesheets/{id}/reject
   */
  async rejectTimesheet(
    branchId: string,
    timesheetId: string,
    payload: RejectTimesheetPayload
  ): Promise<TimesheetEntry> {
    const res = await apiClient.post<TimesheetEntry>(
      `/branches/${branchId}/hr/timesheets/${timesheetId}/reject`,
      payload
    )
    return extractData<TimesheetEntry>(res)
  },
}
