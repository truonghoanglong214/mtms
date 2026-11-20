import { apiClient } from '@/shared/api/apiClient'
import { generateIdempotencyKey, IDEMPOTENCY_HEADER } from '@/shared/utils/idempotency'
import type {
  PayrollPaymentDto,
  PaySalaryPayload,
  BulkPaySalaryPayload,
  BulkPaySalaryResponse,
  PayslipDto,
} from '../types/payment.types'

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

export const paymentApi = {
  /**
   * Pay salary for an individual employee line (partial or full)
   * POST /api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/pay
   */
  async paySalary(
    branchId: string,
    lineId: string,
    payload: PaySalaryPayload,
    customIdempotencyKey?: string
  ): Promise<PayrollPaymentDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }

    const res = await apiClient.post(
      `/branches/${branchId}/hr/payroll-lines/${lineId}/pay`,
      payload,
      { headers }
    )
    return extractData<PayrollPaymentDto>(res)
  },

  /**
   * Bulk pay salaries for multiple/all employees in a payroll period
   * POST /api/v1/branches/{branchId}/hr/payroll-periods/{periodId}/bulk-pay
   */
  async bulkPaySalary(
    branchId: string,
    periodId: string,
    payload: BulkPaySalaryPayload,
    customIdempotencyKey?: string
  ): Promise<BulkPaySalaryResponse> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }

    const res = await apiClient.post(
      `/branches/${branchId}/hr/payroll-periods/${periodId}/bulk-pay`,
      payload,
      { headers }
    )
    return extractData<BulkPaySalaryResponse>(res)
  },

  /**
   * Get payment history for an individual employee payroll line
   * GET /api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/payments
   */
  async getLinePayments(branchId: string, lineId: string): Promise<PayrollPaymentDto[]> {
    const res = await apiClient.get(`/branches/${branchId}/hr/payroll-lines/${lineId}/payments`)
    return extractData<PayrollPaymentDto[]>(res)
  },

  /**
   * Get detailed payslip for an individual payroll line (Manager/Owner view or print)
   * GET /api/v1/branches/{branchId}/hr/payroll-lines/{lineId}/payslip
   */
  async getLinePayslip(branchId: string, lineId: string): Promise<PayslipDto> {
    const res = await apiClient.get(`/branches/${branchId}/hr/payroll-lines/${lineId}/payslip`)
    return extractData<PayslipDto>(res)
  },

  /**
   * Get current authenticated employee's own payslips (TC-017 / Privacy Guard)
   * GET /api/v1/branches/{branchId}/hr/my-payslips?month=YYYY-MM-01
   */
  async getMyPayslips(branchId: string, month?: string): Promise<PayslipDto[]> {
    const res = await apiClient.get(`/branches/${branchId}/hr/my-payslips`, {
      params: month ? { month } : undefined,
    })
    return extractData<PayslipDto[]>(res)
  },
}
