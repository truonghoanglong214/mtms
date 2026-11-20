import { apiClient } from '@/shared/api/apiClient'
import type {
  PurchaseReceiptDto,
  CreateReceiptCommand,
  ReceiptListResponse,
  PayablesResponse,
  ReceiptFilterParams,
  PayableFilterParams,
} from '../types/receipt.types'

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

export const receiptApi = {
  /**
   * List purchase receipts for a branch with filters & pagination
   * GET /api/v1/branches/{branchId}/inventory/receipts
   */
  async getReceipts(
    branchId: string,
    params?: ReceiptFilterParams
  ): Promise<ReceiptListResponse> {
    const res = await apiClient.get(`/branches/${branchId}/inventory/receipts`, {
      params,
    })
    return extractData<ReceiptListResponse>(res)
  },

  /**
   * Get single receipt details with lines and audit info
   * GET /api/v1/branches/{branchId}/inventory/receipts/{receiptId}
   */
  async getReceiptById(
    branchId: string,
    receiptId: string
  ): Promise<PurchaseReceiptDto> {
    const res = await apiClient.get(
      `/branches/${branchId}/inventory/receipts/${receiptId}`
    )
    return extractData<PurchaseReceiptDto>(res)
  },

  /**
   * Create new purchase receipt (Direct receive or Draft)
   * POST /api/v1/branches/{branchId}/inventory/receipts
   * Auto-protected with Idempotency-Key
   */
  async createReceipt(
    branchId: string,
    data: CreateReceiptCommand
  ): Promise<PurchaseReceiptDto> {
    const res = await apiClient.post(
      `/branches/${branchId}/inventory/receipts`,
      data
    )
    return extractData<PurchaseReceiptDto>(res)
  },

  /**
   * Post existing draft receipt (Increases stock, updates Moving WAC, creates Payable if unpaid)
   * POST /api/v1/branches/{branchId}/inventory/receipts/{receiptId}/post
   * Auto-protected with Idempotency-Key
   */
  async postReceipt(
    branchId: string,
    receiptId: string
  ): Promise<PurchaseReceiptDto> {
    const res = await apiClient.post(
      `/branches/${branchId}/inventory/receipts/${receiptId}/post`
    )
    return extractData<PurchaseReceiptDto>(res)
  },

  /**
   * Query branch payables ledger (Supplier debt / TC-008 & TC-009)
   * GET /api/v1/branches/{branchId}/inventory/payables
   */
  async getBranchPayables(
    branchId: string,
    params?: PayableFilterParams
  ): Promise<PayablesResponse> {
    const res = await apiClient.get(`/branches/${branchId}/inventory/payables`, {
      params,
    })
    return extractData<PayablesResponse>(res)
  },
}
