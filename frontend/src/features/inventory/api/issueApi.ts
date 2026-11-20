import { apiClient } from '@/shared/api/apiClient'
import type {
  StockDocumentDto,
  CreateStockDocumentCommand,
  IssueListResponse,
  IssueFilterParams,
} from '../types/issue.types'

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

export const issueApi = {
  /**
   * List stock issue documents for a branch with filters & pagination
   * GET /api/v1/branches/{branchId}/inventory/issues
   */
  async getIssueDocuments(
    branchId: string,
    params?: IssueFilterParams
  ): Promise<IssueListResponse> {
    const res = await apiClient.get(`/branches/${branchId}/inventory/issues`, {
      params,
    })
    return extractData<IssueListResponse>(res)
  },

  /**
   * Get single stock issue document details with lines and audit info
   * GET /api/v1/branches/{branchId}/inventory/issues/{documentId}
   */
  async getIssueDocumentById(
    branchId: string,
    documentId: string
  ): Promise<StockDocumentDto> {
    const res = await apiClient.get(
      `/branches/${branchId}/inventory/issues/${documentId}`
    )
    return extractData<StockDocumentDto>(res)
  },

  /**
   * Create new stock issue document (Direct post or Draft)
   * POST /api/v1/branches/{branchId}/inventory/issues
   * Automatically attaches Idempotency-Key
   */
  async createIssueDocument(
    branchId: string,
    data: CreateStockDocumentCommand
  ): Promise<StockDocumentDto> {
    const res = await apiClient.post(
      `/branches/${branchId}/inventory/issues`,
      data
    )
    return extractData<StockDocumentDto>(res)
  },

  /**
   * Post existing draft issue document (Deducts stock with Moving WAC valuation)
   * POST /api/v1/branches/{branchId}/inventory/issues/{documentId}/post
   * Automatically attaches Idempotency-Key
   */
  async postIssueDocument(
    branchId: string,
    documentId: string
  ): Promise<StockDocumentDto> {
    const res = await apiClient.post(
      `/branches/${branchId}/inventory/issues/${documentId}/post`
    )
    return extractData<StockDocumentDto>(res)
  },
}
