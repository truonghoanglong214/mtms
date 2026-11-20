/**
 * MTMS Revenue Module Types — P0-REV-01 through P0-REV-04
 * Traceability: BR-REV-01, BR-REV-02, BR-REV-03, TC-004, TC-005, TC-006, TC-007
 */

export type OperationStatus = 'OPEN' | 'CLOSED'

export interface BusinessDayDto {
  id: string
  branchId: string
  businessEntityId?: string
  businessDate: string // YYYY-MM-DD
  operationStatus: OperationStatus
  closeReason: string | null
  recordedByName?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface SetBusinessDayCommand {
  operationStatus: OperationStatus
  closeReason?: string | null
}

export type RevenueReportStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'CONFIRMED'
  | 'SUPERSEDED'
  | 'CANCELLED'

export interface DailyRevenueReportDto {
  id: string
  branchId: string
  businessEntityId?: string
  businessDate: string // YYYY-MM-DD
  revision: number
  isCurrent: boolean
  cashAmount: number
  bankTransferAmount: number
  deliveryAppNetAmount: number
  otherAdjustmentAmount: number
  totalRevenue: number
  status: RevenueReportStatus
  submittedBy?: string
  submittedByName?: string | null
  submittedAt?: string | null
  confirmedBy?: string | null
  confirmedByName?: string | null
  confirmedAt?: string | null
  adjustmentReason?: string | null
  evidenceFileId?: string | null
  note?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface CreateDailyRevenueCommand {
  businessDate: string // YYYY-MM-DD
  cashAmount: number
  bankTransferAmount: number
  deliveryAppNetAmount: number
  otherAdjustmentAmount: number
  evidenceFileId?: string | null
  note?: string | null
  submitImmediately?: boolean
}

export interface ConfirmRevenueCommand {
  adjustmentReason?: string | null
}

export interface MissingDateDto {
  businessDate: string // YYYY-MM-DD
  operationStatus?: OperationStatus
  reason?: string
}

export interface DailyRevenueFilterParams {
  startDate?: string
  endDate?: string
  status?: RevenueReportStatus | 'ALL'
  page?: number
  size?: number
}

export interface BusinessDayFilterParams {
  startDate?: string
  endDate?: string
  month?: string // YYYY-MM
}

export interface DailyRevenueListResponse {
  content: DailyRevenueReportDto[]
  totalElements?: number
  totalPages?: number
  size?: number
  number?: number
}
