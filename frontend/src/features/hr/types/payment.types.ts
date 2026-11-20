/**
 * MTMS Payroll Payment & Employee Statement Types
 * Task references:
 * - P0-HR-07: Payroll Payment Record (BR-HR-06, TC-016, FE-P0-22)
 * - P0-HR-08: Payroll Statement UI & Privacy Guard (BR-HR-05, TC-017, FE-P0-23)
 * - P0-HR-09: Primary-branch allocation test & lifecycle integration (BR-HR-03, TC-015)
 * API Contract: docs/api/contracts/P0-HR-07-09.md
 */

import type {
  SalaryRateType,
  PolicySnapshot,
  CalculationSnapshot,
  PayrollAdjustment,
} from './payroll.types'

export type PaymentMethod = 'CASH' | 'BANK_TRANSFER'

export type PayrollPaymentStatus = 'POSTED' | 'REVERSED'

export type PayrollPaymentProgressStatus = 'PAID' | 'PARTIALLY_PAID' | 'UNPAID'

/**
 * Record of an individual payroll payment linked to cash/bank ledger (P0-HR-07)
 */
export interface PayrollPaymentDto {
  id: string
  payrollLineId: string
  accountId: string
  accountName?: string
  amount: number
  paidOn: string // YYYY-MM-DD
  paymentMethod: PaymentMethod
  status: PayrollPaymentStatus
  externalReference?: string | null
  evidenceFileId?: string | null
  ledgerEntryId?: string | null
  note?: string | null
  confirmedBy?: string | null
  confirmedByName?: string | null
  paidAmountAfter?: number
  remainingSalary?: number
  createdAt: string
}

/**
 * Payload to pay salary for an individual payroll line (P0-HR-07)
 */
export interface PaySalaryPayload {
  accountId: string
  amount: number
  paidOn: string // YYYY-MM-DD
  paymentMethod: PaymentMethod
  externalReference?: string
  evidenceFileId?: string | null
  note?: string
}

/**
 * Single item inside a bulk salary payment request
 */
export interface BulkPaymentItem {
  payrollLineId: string
  amount: number
  externalReference?: string
}

/**
 * Payload to bulk pay salaries for multiple payroll lines in a period (P0-HR-07)
 */
export interface BulkPaySalaryPayload {
  accountId: string
  paidOn: string // YYYY-MM-DD
  paymentMethod: PaymentMethod
  items: BulkPaymentItem[]
  note?: string
}

/**
 * Response from bulk salary payment operation
 */
export interface BulkPaySalaryResponse {
  totalPaidAmount: number
  paidCount: number
  paymentIds: string[]
}

/**
 * Detailed employee payslip statement (P0-HR-08 / FE-P0-23)
 */
export interface PayslipDto {
  lineId: string
  payrollPeriodId: string
  employeeId: string
  staffCode: string
  employeeName: string
  positionName: string
  expenseBranchId: string
  branchName?: string
  monthStart: string // YYYY-MM-01
  rateType: SalaryRateType
  approvedMinutes: number
  approvedHours?: number
  payableWorkdays: number
  baseSalary: number
  adjustmentsTotal: number
  grossSalary: number
  paidAmount: number
  remainingSalary: number
  status: PayrollPaymentProgressStatus
  policySnapshot: PolicySnapshot
  calculationSnapshot: CalculationSnapshot
  adjustments: PayrollAdjustment[]
  payments?: PayrollPaymentDto[]
  approvedAt?: string | null
  approverName?: string | null
}

/**
 * Vietnamese presentation labels for payment methods
 */
export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Tiền mặt tại quỹ',
  BANK_TRANSFER: 'Chuyển khoản ngân hàng',
}

/**
 * Vietnamese presentation labels & styling for payment progress
 */
export const PAYMENT_PROGRESS_CONFIG: Record<
  PayrollPaymentProgressStatus,
  {
    label: string
    badgeClass: string
    dotClass: string
    textColor: string
    bgClass: string
  }
> = {
  PAID: {
    label: 'Đã thanh toán',
    badgeClass: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    dotClass: 'bg-emerald-500',
    textColor: 'text-emerald-700',
    bgClass: 'bg-emerald-50/70',
  },
  PARTIALLY_PAID: {
    label: 'Thanh toán một phần',
    badgeClass: 'bg-amber-50 text-amber-800 border-amber-200',
    dotClass: 'bg-amber-500',
    textColor: 'text-amber-700',
    bgClass: 'bg-amber-50/70',
  },
  UNPAID: {
    label: 'Chưa thanh toán',
    badgeClass: 'bg-stone-100 text-stone-700 border-stone-200',
    dotClass: 'bg-stone-400',
    textColor: 'text-stone-700',
    bgClass: 'bg-stone-50',
  },
}

/**
 * Determines payment progress status from gross salary and paid amount
 */
export function getPaymentProgressStatus(
  grossSalary: number,
  paidAmount: number
): PayrollPaymentProgressStatus {
  const gross = Number(grossSalary) || 0
  const paid = Number(paidAmount) || 0
  if (paid >= gross && gross > 0) return 'PAID'
  if (paid > 0 && paid < gross) return 'PARTIALLY_PAID'
  return 'UNPAID'
}
