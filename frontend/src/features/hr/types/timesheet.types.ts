export type TimesheetStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'

export type TimesheetSourceType = 'MANUAL' | 'IMPORT' | 'AUTO'

/**
 * Single timesheet record representation (P0-HR-03 & P0-HR-04 / FE-P0-19)
 */
export interface TimesheetEntry {
  id: string
  employeeId: string
  staffCode: string
  employeeName: string
  positionName: string
  workDate: string
  rawMinutes: number | null
  approvedMinutes: number
  payableWorkdays: number
  sourceType: TimesheetSourceType
  status: TimesheetStatus
  note: string | null
  approvedBy: string | null
  approverName: string | null
  approvedAt: string | null
  approvalNote: string | null
  createdAt?: string
  updatedAt?: string
}

/**
 * Monthly aggregate summary for an employee (P0-HR-04 / FE-P0-19 / TC-013)
 */
export interface MonthlyTimesheetSummary {
  employeeId: string
  staffCode: string
  employeeName: string
  positionName: string
  totalApprovedMinutes: number
  totalApprovedHours: number
  totalPayableWorkdays: number
  approvedDaysCount: number
  pendingDaysCount: number
  rejectedDaysCount: number
}

/**
 * Payload to create or update a single daily timesheet entry
 */
export interface CreateTimesheetPayload {
  employeeId: string
  workDate: string
  approvedMinutes: number
  payableWorkdays: number
  status?: TimesheetStatus
  note?: string | null
  rawMinutes?: number | null
}

/**
 * Item in bulk timesheet submission
 */
export interface BulkTimesheetItem {
  employeeId: string
  approvedMinutes: number
  payableWorkdays: number
  status?: TimesheetStatus
  note?: string | null
}

/**
 * Payload for bulk daily timesheets (shift entry)
 */
export interface BulkTimesheetPayload {
  workDate: string
  entries: BulkTimesheetItem[]
}

/**
 * Payload for single timesheet approval (TC-017)
 */
export interface ApproveTimesheetPayload {
  approvalNote?: string | null
}

/**
 * Payload for bulk timesheet approval (TC-017)
 */
export interface BulkApprovePayload {
  entryIds: string[]
  approvalNote?: string | null
}

/**
 * Bulk approval response
 */
export interface BulkApproveResponse {
  approvedCount: number
  failedCount: number
}

/**
 * Payload for timesheet rejection
 */
export interface RejectTimesheetPayload {
  rejectionReason: string
}

/**
 * Filter parameters for timesheet query
 */
export interface TimesheetFilterParams {
  startDate?: string
  endDate?: string
  employeeId?: string
  status?: TimesheetStatus | 'ALL'
  page?: number
  size?: number
}

/**
 * Paginated response of timesheets
 */
export interface TimesheetPageResponse {
  content: TimesheetEntry[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * Vietnamese labels and styling configs for timesheet statuses
 */
export const TIMESHEET_STATUS_CONFIG: Record<
  TimesheetStatus,
  {
    label: string
    badgeClass: string
    dotClass: string
    bgLight: string
    textColor: string
  }
> = {
  DRAFT: {
    label: 'Nháp',
    badgeClass: 'bg-stone-100 text-stone-700 border-stone-200',
    dotClass: 'bg-stone-400',
    bgLight: 'bg-stone-50',
    textColor: 'text-stone-600',
  },
  SUBMITTED: {
    label: 'Chờ duyệt',
    badgeClass: 'bg-amber-50 text-amber-800 border-amber-200',
    dotClass: 'bg-amber-500',
    bgLight: 'bg-amber-50/60',
    textColor: 'text-amber-700',
  },
  APPROVED: {
    label: 'Đã duyệt',
    badgeClass: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    dotClass: 'bg-emerald-500',
    bgLight: 'bg-emerald-50/60',
    textColor: 'text-emerald-700',
  },
  REJECTED: {
    label: 'Từ chối',
    badgeClass: 'bg-rose-50 text-rose-800 border-rose-200',
    dotClass: 'bg-rose-500',
    bgLight: 'bg-rose-50/60',
    textColor: 'text-rose-700',
  },
}

export const TIMESHEET_SOURCE_LABELS: Record<TimesheetSourceType, string> = {
  MANUAL: 'Nhập tay',
  IMPORT: 'Nhập file',
  AUTO: 'Tự động',
}
