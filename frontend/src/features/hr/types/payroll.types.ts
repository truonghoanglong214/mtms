/**
 * P0-HR-05 & P0-HR-06: Payroll Calculation Engine, Approval & Invariant Snapshots
 * References: BR-HR-04, BR-HR-05, BR-HR-10, TC-013, TC-014, TC-015, TC-017, FE-P0-20, FE-P0-21
 * API Contract: docs/api/contracts/P0-HR-05-06.md
 */

export type PayrollPeriodStatus = 'DRAFT' | 'APPROVED' | 'SUPERSEDED' | 'VOIDED'

export type AdjustmentCode =
  | 'ALLOWANCE'
  | 'BONUS'
  | 'PENALTY'
  | 'ADVANCE_DEDUCTION'
  | 'OTHER'

export type SalaryRateType = 'HOURLY' | 'MONTHLY'

/**
 * Midmonth wage slice breakdown for hourly staff (TC-013)
 */
export interface RateSlice {
  fromDate: string
  toDate: string
  rate: number
  hours: number
  amount: number
}

/**
 * Snapshot of salary policies at the time of payroll calculation
 */
export interface PolicySnapshot {
  rateType: SalaryRateType
  currentRate?: number
  standardWorkdays?: number
  agreedMonthlyRate?: number
  effectiveFrom?: string
  [key: string]: unknown
}

/**
 * Calculation snapshot detailing rate slices or monthly wage proration (TC-013 & TC-014)
 */
export interface CalculationSnapshot {
  slices?: RateSlice[]
  standardWorkdays?: number
  payableWorkdays?: number
  agreedMonthlyRate?: number
  calculatedBaseSalary?: number
  formula?: string
  totalApprovedMinutes?: number
  totalApprovedHours?: number
  [key: string]: unknown
}

/**
 * Adjustment item on a payroll line (manual allowances, bonuses, penalties, deductions)
 */
export interface PayrollAdjustment {
  id: string
  payrollLineId?: string
  adjustmentCode: AdjustmentCode
  signedAmount: number
  explanation: string
  approvedBy?: string | null
  createdAt?: string
}

/**
 * Individual employee payroll line in a period
 */
export interface PayrollLine {
  id: string
  payrollPeriodId: string
  employeeId: string
  staffCode: string
  employeeName: string
  positionName: string
  expenseBranchId: string
  rateType: SalaryRateType
  approvedMinutes: number
  payableWorkdays: number
  baseSalary: number
  adjustmentsTotal: number
  grossSalary: number
  paidAmount: number
  policySnapshot: PolicySnapshot
  calculationSnapshot: CalculationSnapshot
  adjustments: PayrollAdjustment[]
  createdAt?: string
  updatedAt?: string
}

/**
 * Payroll period summary for a branch in a month
 */
export interface PayrollPeriod {
  id: string
  branchId: string
  monthStart: string // YYYY-MM-01
  revision: number
  isCurrent: boolean
  status: PayrollPeriodStatus
  totalGrossSalary: number
  totalBaseSalary: number
  totalAdjustments: number
  employeeCount: number
  note?: string | null
  approvedBy?: string | null
  approverName?: string | null
  approvedAt?: string | null
  lines?: PayrollLine[]
  createdAt?: string
  updatedAt?: string
}

/**
 * Payload to create manual adjustment on a payroll line
 */
export interface CreateAdjustmentPayload {
  adjustmentCode: AdjustmentCode
  signedAmount: number
  explanation: string
}

/**
 * Payload to generate or recalculate payroll period
 */
export interface GeneratePayrollPayload {
  monthStart: string // YYYY-MM-01
}

/**
 * Configuration & Vietnamese presentation labels for Payroll Period Status
 */
export const PAYROLL_STATUS_CONFIG: Record<
  PayrollPeriodStatus,
  {
    label: string
    badgeClass: string
    dotClass: string
    bgLight: string
    textColor: string
    description: string
  }
> = {
  DRAFT: {
    label: 'Bản nháp (DRAFT)',
    badgeClass: 'bg-amber-50 text-amber-800 border-amber-200',
    dotClass: 'bg-amber-500',
    bgLight: 'bg-amber-50/70',
    textColor: 'text-amber-800',
    description: 'Bảng lương đang soạn thảo, có thể thêm điều chỉnh hoặc tính toán lại.',
  },
  APPROVED: {
    label: 'Đã chốt duyệt (APPROVED)',
    badgeClass: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    dotClass: 'bg-emerald-500',
    bgLight: 'bg-emerald-50/70',
    textColor: 'text-emerald-800',
    description: 'Bảng lương đã khóa vĩnh viễn (TC-017 / Invariant snapshot).',
  },
  SUPERSEDED: {
    label: 'Đã thay thế (SUPERSEDED)',
    badgeClass: 'bg-stone-100 text-stone-700 border-stone-200',
    dotClass: 'bg-stone-400',
    bgLight: 'bg-stone-50',
    textColor: 'text-stone-600',
    description: 'Bản ghi cũ đã bị thay thế bởi đợt tính toán điều chỉnh mới.',
  },
  VOIDED: {
    label: 'Đã hủy bỏ (VOIDED)',
    badgeClass: 'bg-rose-50 text-rose-800 border-rose-200',
    dotClass: 'bg-rose-500',
    bgLight: 'bg-rose-50/70',
    textColor: 'text-rose-800',
    description: 'Kỳ bảng lương đã bị hủy.',
  },
}

/**
 * Configuration & Vietnamese presentation labels for Adjustment Codes
 */
export const ADJUSTMENT_CODE_CONFIG: Record<
  AdjustmentCode,
  {
    label: string
    shortLabel: string
    defaultSign: 'POSITIVE' | 'NEGATIVE' | 'FLEXIBLE'
    badgeClass: string
    textColor: string
    description: string
  }
> = {
  ALLOWANCE: {
    label: 'Phụ cấp trách nhiệm / ăn ca',
    shortLabel: 'Phụ cấp',
    defaultSign: 'POSITIVE',
    badgeClass: 'bg-blue-50 text-blue-800 border-blue-200',
    textColor: 'text-blue-700',
    description: 'Khoản cộng thêm: phụ cấp trách nhiệm cửa hàng, tiền ăn trưa, xăng xe.',
  },
  BONUS: {
    label: 'Thưởng doanh số / chuyên cần',
    shortLabel: 'Thưởng',
    defaultSign: 'POSITIVE',
    badgeClass: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    textColor: 'text-emerald-700',
    description: 'Khoản cộng thêm: thưởng doanh số vượt chỉ tiêu, thưởng chuyên cần không nghỉ.',
  },
  PENALTY: {
    label: 'Phạt vi phạm quy chế / đi muộn',
    shortLabel: 'Phạt',
    defaultSign: 'NEGATIVE',
    badgeClass: 'bg-rose-50 text-rose-800 border-rose-200',
    textColor: 'text-rose-700',
    description: 'Khoản trừ: phạt đi muộn, vi phạm đồng phục vệ sinh, sai sót quy trình.',
  },
  ADVANCE_DEDUCTION: {
    label: 'Khấu trừ tạm ứng lương',
    shortLabel: 'Tạm ứng',
    defaultSign: 'NEGATIVE',
    badgeClass: 'bg-purple-50 text-purple-800 border-purple-200',
    textColor: 'text-purple-700',
    description: 'Khoản trừ: hoàn ứng lương nhân viên đã nhận trước trong tháng.',
  },
  OTHER: {
    label: 'Điều chỉnh khác',
    shortLabel: 'Khác',
    defaultSign: 'FLEXIBLE',
    badgeClass: 'bg-stone-100 text-stone-700 border-stone-200',
    textColor: 'text-stone-700',
    description: 'Khoản cộng hoặc trừ điều chỉnh đặc biệt có phê duyệt giải trình.',
  },
}
