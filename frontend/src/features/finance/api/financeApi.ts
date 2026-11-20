import { apiClient } from '@/shared/api/apiClient'
import { generateIdempotencyKey, IDEMPOTENCY_HEADER } from '@/shared/utils/idempotency'
import type {
  FinancialCategoryDto,
  CreateCategoryRequest,
  FinancialAccountDto,
  CreateFinancialAccountRequest,
  CashLedgerEntryDto,
  CashLedgerPageResponse,
  CashLedgerFilterParams,
  InternalTransferRequest,
  InternalTransferResponse,
  CreateManualEntryRequest,
  CategoryGroup,
  FlowDirection,
  PayableDto,
  PayableDetailDto,
  CreatePayableRequest,
  SettlePayableRequest,
  PayablePaymentDto,
  BranchPayablesPageResponse,
  ReceivableDto,
  ReceivableDetailDto,
  CreateReceivableRequest,
  ReceivableReceiptRequest,
  ReceivableReceiptDto,
  BranchReceivablesPageResponse,
  ExpenseDto,
  ExpenseDetailDto,
  CreateExpenseRequest,
  VoidExpenseRequest,
  BranchExpenseConfigDto,
  UpdateExpenseConfigRequest,
  BranchExpensesPageResponse,
  CashReconciliationDto,
  CreateReconciliationPayload,
  ExplainDiscrepancyPayload,
  AdjustReconciliationPayload,
  CashReconciliationPageResponse,
  TaxObligationDto,
  TaxObligationDetailDto,
  CreateTaxObligationPayload,
  PayTaxObligationPayload,
  TaxObligationPageResponse,
  OpeningBalanceBatchDto,
  OpeningBalanceStatusResponse,
  CreateOpeningBatchPayload,
} from '../types/finance.types'

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
 * Formats numeric or string amount to Vietnamese Dong display.
 * E.g.: 15000000 -> "15.000.000 ₫"
 */
export function formatVnd(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') return '0 ₫'
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(num)
}

/**
 * Formats signed amount with leading + or - sign.
 */
export function formatSignedVnd(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') return '0 ₫'
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0 ₫'
  const formatted = formatVnd(Math.abs(num))
  if (num > 0) return `+${formatted}`
  if (num < 0) return `-${formatted}`
  return formatted
}

/**
 * Parses user input currency string (with dots, commas, symbols) into pure float/integer number.
 */
export function parseMoneyInput(value: string): number {
  if (!value) return 0
  const clean = value.replace(/[^\d.-]/g, '')
  const parsed = parseFloat(clean)
  return isNaN(parsed) ? 0 : parsed
}

/**
 * Safely extracts items list from CashLedgerPageResponse regardless of backend format.
 */
export function getLedgerItems(res: CashLedgerPageResponse | undefined | null): CashLedgerEntryDto[] {
  if (!res) return []
  if (Array.isArray(res)) return res as CashLedgerEntryDto[]
  if (Array.isArray(res.items)) return res.items
  if (Array.isArray(res.content)) return res.content
  return []
}

/**
 * Safely extracts items list from BranchPayablesPageResponse.
 */
export function getPayableItems(res: BranchPayablesPageResponse | undefined | null): PayableDto[] {
  if (!res) return []
  if (Array.isArray(res)) return res as PayableDto[]
  if (Array.isArray(res.items)) return res.items
  if (Array.isArray(res.content)) return res.content
  return []
}

/**
 * Safely extracts items list from BranchReceivablesPageResponse.
 */
export function getReceivableItems(res: BranchReceivablesPageResponse | undefined | null): ReceivableDto[] {
  if (!res) return []
  if (Array.isArray(res)) return res as ReceivableDto[]
  if (Array.isArray(res.items)) return res.items
  if (Array.isArray(res.content)) return res.content
  return []
}

/**
 * Safely extracts total elements count from CashLedgerPageResponse.
 */
export function getLedgerTotalElements(res: CashLedgerPageResponse | undefined | null): number {
  if (!res) return 0
  if (typeof res.totalElements === 'number') return res.totalElements
  if (typeof res.totalItems === 'number') return res.totalItems
  const items = getLedgerItems(res)
  return items.length
}

/**
 * Safely extracts items list from BranchExpensesPageResponse.
 */
export function getExpenseItems(res: BranchExpensesPageResponse | undefined | null): ExpenseDto[] {
  if (!res) return []
  if (Array.isArray(res)) return res as ExpenseDto[]
  if (Array.isArray(res.items)) return res.items
  if (Array.isArray(res.content)) return res.content
  return []
}

/**
 * Safely extracts total elements count from BranchExpensesPageResponse.
 */
export function getExpenseTotalElements(res: BranchExpensesPageResponse | undefined | null): number {
  if (!res) return 0
  if (typeof res.totalElements === 'number') return res.totalElements
  if (typeof res.totalItems === 'number') return res.totalItems
  const items = getExpenseItems(res)
  return items.length
}

/**
 * Safely extracts items list from CashReconciliationPageResponse.
 */
export function getReconciliationItems(res: CashReconciliationPageResponse | undefined | null): CashReconciliationDto[] {
  if (!res) return []
  if (Array.isArray(res)) return res as CashReconciliationDto[]
  if (Array.isArray(res.items)) return res.items
  if (Array.isArray(res.content)) return res.content
  return []
}

/**
 * Safely extracts total elements count from CashReconciliationPageResponse.
 */
export function getReconciliationTotalElements(res: CashReconciliationPageResponse | undefined | null): number {
  if (!res) return 0
  if (typeof res.totalElements === 'number') return res.totalElements
  const items = getReconciliationItems(res)
  return items.length
}

/**
 * Safely extracts items list from TaxObligationPageResponse.
 */
export function getTaxObligationItems(res: TaxObligationPageResponse | undefined | null): TaxObligationDto[] {
  if (!res) return []
  if (Array.isArray(res)) return res as TaxObligationDto[]
  if (Array.isArray(res.items)) return res.items
  if (Array.isArray(res.content)) return res.content
  return []
}

/**
 * Safely extracts total elements count from TaxObligationPageResponse.
 */
export function getTaxObligationTotalElements(res: TaxObligationPageResponse | undefined | null): number {
  if (!res) return 0
  if (typeof res.totalElements === 'number') return res.totalElements
  const items = getTaxObligationItems(res)
  return items.length
}

export const financeApi = {
  /**
   * Get list of financial categories
   * GET /api/v1/finance/categories
   */
  async getCategories(params?: {
    group?: CategoryGroup
    flowDirection?: FlowDirection
    activeOnly?: boolean
  }): Promise<FinancialCategoryDto[]> {
    const res = await apiClient.get('/finance/categories', { params })
    return extractData<FinancialCategoryDto[]>(res)
  },

  /**
   * Create custom financial category (enforced OPERATING_EXPENSE / OUTFLOW)
   * POST /api/v1/finance/categories
   */
  async createCategory(body: CreateCategoryRequest): Promise<FinancialCategoryDto> {
    const res = await apiClient.post('/finance/categories', body)
    return extractData<FinancialCategoryDto>(res)
  },

  /**
   * Get financial accounts (cash, bank) for branch
   * GET /api/v1/branches/{branchId}/finance/accounts
   */
  async getAccounts(branchId: string): Promise<FinancialAccountDto[]> {
    const res = await apiClient.get(`/branches/${branchId}/finance/accounts`)
    return extractData<FinancialAccountDto[]>(res)
  },

  /**
   * Create a new financial account (e.g. connecting bank account)
   * POST /api/v1/branches/{branchId}/finance/accounts
   */
  async createAccount(
    branchId: string,
    body: CreateFinancialAccountRequest
  ): Promise<FinancialAccountDto> {
    const res = await apiClient.post(`/branches/${branchId}/finance/accounts`, body)
    return extractData<FinancialAccountDto>(res)
  },

  /**
   * Get paginated cash ledger entries for a branch
   * GET /api/v1/branches/{branchId}/finance/ledger
   */
  async getLedgerEntries(
    branchId: string,
    params?: CashLedgerFilterParams
  ): Promise<CashLedgerPageResponse> {
    const res = await apiClient.get(`/branches/${branchId}/finance/ledger`, { params })
    return extractData<CashLedgerPageResponse>(res)
  },

  /**
   * Record internal transfer between Cash & Bank accounts (TC-010)
   * POST /api/v1/branches/{branchId}/finance/transfers
   */
  async recordTransfer(
    branchId: string,
    body: InternalTransferRequest,
    customIdempotencyKey?: string
  ): Promise<InternalTransferResponse> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }

    const res = await apiClient.post(`/branches/${branchId}/finance/transfers`, body, {
      headers,
    })
    return extractData<InternalTransferResponse>(res)
  },

  /**
   * Record direct manual cash ledger entry (Capital injection, direct expense)
   * POST /api/v1/branches/{branchId}/finance/entries
   */
  async recordEntry(
    branchId: string,
    body: CreateManualEntryRequest,
    customIdempotencyKey?: string
  ): Promise<CashLedgerEntryDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }

    const res = await apiClient.post(`/branches/${branchId}/finance/entries`, body, {
      headers,
    })
    return extractData<CashLedgerEntryDto>(res)
  },

  /**
   * Get payables ledger for branch (TC-009 / FE-P0-13)
   * GET /api/v1/branches/{branchId}/payables
   */
  async getPayables(
    branchId: string,
    params?: { status?: string; search?: string; page?: number; size?: number }
  ): Promise<BranchPayablesPageResponse> {
    const res = await apiClient.get(`/branches/${branchId}/payables`, { params })
    return extractData<BranchPayablesPageResponse>(res)
  },

  /**
   * Get detail of a payable with its payment history
   * GET /api/v1/branches/{branchId}/payables/{payableId}
   */
  async getPayableDetail(branchId: string, payableId: string): Promise<PayableDetailDto> {
    const res = await apiClient.get(`/branches/${branchId}/payables/${payableId}`)
    return extractData<PayableDetailDto>(res)
  },

  /**
   * Create manual payable
   * POST /api/v1/branches/{branchId}/payables
   */
  async createPayable(
    branchId: string,
    body: CreatePayableRequest,
    customIdempotencyKey?: string
  ): Promise<PayableDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }
    const res = await apiClient.post(`/branches/${branchId}/payables`, body, { headers })
    return extractData<PayableDto>(res)
  },

  /**
   * Settle payable (partial or full debt settlement) - TC-009
   * POST /api/v1/branches/{branchId}/payables/{payableId}/payments
   */
  async settlePayable(
    branchId: string,
    payableId: string,
    body: SettlePayableRequest,
    customIdempotencyKey?: string
  ): Promise<PayablePaymentDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }
    const res = await apiClient.post(
      `/branches/${branchId}/payables/${payableId}/payments`,
      body,
      { headers }
    )
    return extractData<PayablePaymentDto>(res)
  },

  /**
   * Get receivables ledger for branch
   * GET /api/v1/branches/{branchId}/receivables
   */
  async getReceivables(
    branchId: string,
    params?: { status?: string; search?: string; page?: number; size?: number }
  ): Promise<BranchReceivablesPageResponse> {
    const res = await apiClient.get(`/branches/${branchId}/receivables`, { params })
    return extractData<BranchReceivablesPageResponse>(res)
  },

  /**
   * Get detail of a receivable with collection history
   * GET /api/v1/branches/{branchId}/receivables/{receivableId}
   */
  async getReceivableDetail(branchId: string, receivableId: string): Promise<ReceivableDetailDto> {
    const res = await apiClient.get(`/branches/${branchId}/receivables/${receivableId}`)
    return extractData<ReceivableDetailDto>(res)
  },

  /**
   * Create manual receivable
   * POST /api/v1/branches/{branchId}/receivables
   */
  async createReceivable(
    branchId: string,
    body: CreateReceivableRequest,
    customIdempotencyKey?: string
  ): Promise<ReceivableDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }
    const res = await apiClient.post(`/branches/${branchId}/receivables`, body, { headers })
    return extractData<ReceivableDto>(res)
  },

  /**
   * Record receipt against receivable
   * POST /api/v1/branches/{branchId}/receivables/{receivableId}/receipts
   */
  async recordReceivableReceipt(
    branchId: string,
    receivableId: string,
    body: ReceivableReceiptRequest,
    customIdempotencyKey?: string
  ): Promise<ReceivableReceiptDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }
    const res = await apiClient.post(
      `/branches/${branchId}/receivables/${receivableId}/receipts`,
      body,
      { headers }
    )
    return extractData<ReceivableReceiptDto>(res)
  },

  // ==========================================================================
  // P0-FIN-03 & P0-FIN-04: Operating Expenses API (FE-P0-12 / BR-FIN-07)
  // ==========================================================================

  /**
   * Get paginated operating expenses for branch
   * GET /api/v1/branches/{branchId}/expenses
   */
  async getExpenses(
    branchId: string,
    params?: {
      status?: string
      categoryId?: string
      startDate?: string
      endDate?: string
      reviewFlagOnly?: boolean
      search?: string
      page?: number
      size?: number
    }
  ): Promise<BranchExpensesPageResponse> {
    const res = await apiClient.get(`/branches/${branchId}/expenses`, { params })
    return extractData<BranchExpensesPageResponse>(res)
  },

  /**
   * Get detail of an operating expense
   * GET /api/v1/branches/{branchId}/expenses/{expenseId}
   */
  async getExpenseDetail(branchId: string, expenseId: string): Promise<ExpenseDetailDto> {
    const res = await apiClient.get(`/branches/${branchId}/expenses/${expenseId}`)
    return extractData<ExpenseDetailDto>(res)
  },

  /**
   * Create operating expense (with optional immediate confirmation)
   * POST /api/v1/branches/{branchId}/expenses
   */
  async createExpense(
    branchId: string,
    body: CreateExpenseRequest,
    customIdempotencyKey?: string
  ): Promise<ExpenseDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }
    const res = await apiClient.post(`/branches/${branchId}/expenses`, body, { headers })
    return extractData<ExpenseDto>(res)
  },

  /**
   * Confirm draft expense (posts to cash ledger or creates payable)
   * POST /api/v1/branches/{branchId}/expenses/{expenseId}/confirm
   */
  async confirmExpense(
    branchId: string,
    expenseId: string,
    customIdempotencyKey?: string
  ): Promise<ExpenseDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }
    const res = await apiClient.post(
      `/branches/${branchId}/expenses/${expenseId}/confirm`,
      {},
      { headers }
    )
    return extractData<ExpenseDto>(res)
  },

  /**
   * Void operating expense (reverses cash ledger entry or cancels payable)
   * POST /api/v1/branches/{branchId}/expenses/{expenseId}/void
   */
  async voidExpense(
    branchId: string,
    expenseId: string,
    body: VoidExpenseRequest,
    customIdempotencyKey?: string
  ): Promise<ExpenseDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }
    const res = await apiClient.post(
      `/branches/${branchId}/expenses/${expenseId}/void`,
      body,
      { headers }
    )
    return extractData<ExpenseDto>(res)
  },

  /**
   * Get branch expense threshold configurations
   * GET /api/v1/branches/{branchId}/expenses/config
   */
  async getExpenseConfig(branchId: string): Promise<BranchExpenseConfigDto> {
    const res = await apiClient.get(`/branches/${branchId}/expenses/config`)
    return extractData<BranchExpenseConfigDto>(res)
  },

  /**
   * Update branch expense threshold configurations
   * PUT /api/v1/branches/{branchId}/expenses/config
   */
  async updateExpenseConfig(
    branchId: string,
    body: Partial<BranchExpenseConfigDto> | UpdateExpenseConfigRequest
  ): Promise<BranchExpenseConfigDto> {
    const res = await apiClient.put(`/branches/${branchId}/expenses/config`, body)
    return extractData<BranchExpenseConfigDto>(res)
  },

  // ==========================================================================
  // P0-FIN-06 & FE-P0-14: Cash & Bank Account Reconciliation (BR-FIN-06 / TC-012)
  // ==========================================================================

  /**
   * Get paginated cash reconciliations for branch
   * GET /api/v1/branches/{branchId}/finance/reconciliations
   */
  async getReconciliations(
    branchId: string,
    params?: {
      accountId?: string
      startDate?: string
      endDate?: string
      disposition?: string
      page?: number
      size?: number
    }
  ): Promise<CashReconciliationPageResponse> {
    const res = await apiClient.get(`/branches/${branchId}/finance/reconciliations`, { params })
    return extractData<CashReconciliationPageResponse>(res)
  },

  /**
   * Get details of a single reconciliation
   * GET /api/v1/branches/{branchId}/finance/reconciliations/{id}
   */
  async getReconciliationDetail(branchId: string, id: string): Promise<CashReconciliationDto> {
    const res = await apiClient.get(`/branches/${branchId}/finance/reconciliations/${id}`)
    return extractData<CashReconciliationDto>(res)
  },

  /**
   * Create a new cash/bank reconciliation snapshot
   * POST /api/v1/branches/{branchId}/finance/reconciliations
   */
  async createReconciliation(
    branchId: string,
    payload: CreateReconciliationPayload,
    customIdempotencyKey?: string
  ): Promise<CashReconciliationDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }
    const res = await apiClient.post(
      `/branches/${branchId}/finance/reconciliations`,
      payload,
      { headers }
    )
    return extractData<CashReconciliationDto>(res)
  },

  /**
   * Explain an open discrepancy (disposition -> EXPLAINED)
   * POST /api/v1/branches/{branchId}/finance/reconciliations/{id}/explain
   */
  async explainDiscrepancy(
    branchId: string,
    id: string,
    payload: ExplainDiscrepancyPayload
  ): Promise<CashReconciliationDto> {
    const res = await apiClient.post(
      `/branches/${branchId}/finance/reconciliations/${id}/explain`,
      payload
    )
    return extractData<CashReconciliationDto>(res)
  },

  /**
   * Authorized adjustment of reconciliation variance (TC-012)
   * Posts adjusting entry to cash ledger and updates account balance atomically
   * POST /api/v1/branches/{branchId}/finance/reconciliations/{id}/adjust
   */
  async adjustReconciliation(
    branchId: string,
    id: string,
    payload: AdjustReconciliationPayload,
    customIdempotencyKey?: string
  ): Promise<CashReconciliationDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }
    const res = await apiClient.post(
      `/branches/${branchId}/finance/reconciliations/${id}/adjust`,
      payload,
      { headers }
    )
    return extractData<CashReconciliationDto>(res)
  },

  /**
   * Close verified reconciliation (disposition -> CLOSED)
   * POST /api/v1/branches/{branchId}/finance/reconciliations/{id}/close
   */
  async closeReconciliation(branchId: string, id: string): Promise<CashReconciliationDto> {
    const res = await apiClient.post(
      `/branches/${branchId}/finance/reconciliations/${id}/close`,
      {}
    )
    return extractData<CashReconciliationDto>(res)
  },

  // ============================================================================
  // P0-FIN-07: Tax Manual Obligations (BR-FIN-04, TC-012, FE-P0-15)
  // ============================================================================

  /**
   * Get paginated tax obligations for branch/entity
   * GET /api/v1/branches/{branchId}/finance/tax-obligations
   */
  async getTaxObligations(
    branchId: string,
    params?: {
      obligationType?: string
      status?: string
      startDate?: string
      endDate?: string
      page?: number
      size?: number
    }
  ): Promise<TaxObligationPageResponse> {
    const res = await apiClient.get(`/branches/${branchId}/finance/tax-obligations`, {
      params,
    })
    return extractData<TaxObligationPageResponse>(res)
  },

  /**
   * Get single tax obligation detail with payment history
   * GET /api/v1/branches/{branchId}/finance/tax-obligations/{id}
   */
  async getTaxObligationDetail(
    branchId: string,
    id: string
  ): Promise<TaxObligationDetailDto> {
    const res = await apiClient.get(
      `/branches/${branchId}/finance/tax-obligations/${id}`
    )
    return extractData<TaxObligationDetailDto>(res)
  },

  /**
   * Create tax manual obligation (BR-FIN-04 / TC-012)
   * POST /api/v1/branches/{branchId}/finance/tax-obligations
   */
  async createTaxObligation(
    branchId: string,
    payload: CreateTaxObligationPayload,
    customIdempotencyKey?: string
  ): Promise<TaxObligationDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }
    const res = await apiClient.post(
      `/branches/${branchId}/finance/tax-obligations`,
      payload,
      { headers }
    )
    return extractData<TaxObligationDto>(res)
  },

  /**
   * Record payment for tax obligation (posts OUTFLOW to cash/bank ledger)
   * POST /api/v1/branches/{branchId}/finance/tax-obligations/{id}/pay
   */
  async payTaxObligation(
    branchId: string,
    id: string,
    payload: PayTaxObligationPayload,
    customIdempotencyKey?: string
  ): Promise<TaxObligationDetailDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }
    const res = await apiClient.post(
      `/branches/${branchId}/finance/tax-obligations/${id}/pay`,
      payload,
      { headers }
    )
    return extractData<TaxObligationDetailDto>(res)
  },

  /**
   * Cancel tax obligation (only allowed when amount_paid == 0)
   * POST /api/v1/branches/{branchId}/finance/tax-obligations/{id}/cancel
   */
  async cancelTaxObligation(branchId: string, id: string): Promise<void> {
    await apiClient.post(`/branches/${branchId}/finance/tax-obligations/${id}/cancel`, {})
  },

  // ============================================================================
  // P0-FIN-08 & FE-P0-16: Opening Balances Import & Cutover Batch (BR-FIN-08 / GAP-01)
  // ============================================================================

  /**
   * Get opening balance status and active batch for branch
   * GET /api/v1/branches/{branchId}/finance/opening-balances/status
   */
  async getOpeningBalanceStatus(branchId: string): Promise<OpeningBalanceStatusResponse> {
    const res = await apiClient.get(`/branches/${branchId}/finance/opening-balances/status`)
    return extractData<OpeningBalanceStatusResponse>(res)
  },

  /**
   * Get single opening balance batch detail
   * GET /api/v1/branches/{branchId}/finance/opening-balances/{id}
   */
  async getOpeningBatchDetail(branchId: string, id: string): Promise<OpeningBalanceBatchDto> {
    const res = await apiClient.get(`/branches/${branchId}/finance/opening-balances/${id}`)
    return extractData<OpeningBalanceBatchDto>(res)
  },

  /**
   * Create or update draft opening balance batch
   * POST /api/v1/branches/{branchId}/finance/opening-balances
   */
  async saveOpeningBatch(
    branchId: string,
    payload: CreateOpeningBatchPayload,
    customIdempotencyKey?: string
  ): Promise<OpeningBalanceBatchDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }
    const res = await apiClient.post(
      `/branches/${branchId}/finance/opening-balances`,
      payload,
      { headers }
    )
    return extractData<OpeningBalanceBatchDto>(res)
  },

  /**
   * Confirm and post opening balance batch (locks batch, writes to cash ledger)
   * POST /api/v1/branches/{branchId}/finance/opening-balances/{id}/post
   */
  async postOpeningBatch(
    branchId: string,
    id: string,
    customIdempotencyKey?: string
  ): Promise<OpeningBalanceBatchDto> {
    const headers: Record<string, string> = {}
    if (customIdempotencyKey) {
      headers[IDEMPOTENCY_HEADER] = customIdempotencyKey
    } else {
      headers[IDEMPOTENCY_HEADER] = generateIdempotencyKey()
    }
    const res = await apiClient.post(
      `/branches/${branchId}/finance/opening-balances/${id}/post`,
      {},
      { headers }
    )
    return extractData<OpeningBalanceBatchDto>(res)
  },
}
