/**
 * MTMS Finance Domain Types
 * Task references: P0-FIN-01 (BR-FIN-01, TC-010) & P0-FIN-02 (BR-FIN-09, TC-010, TC-011)
 * Frontend Backlog Reference: FE-P0-11 (Branch money ledger view)
 * Strictly mirrors docs/api/contracts/P0-FIN-01-02.md
 */

export type CategoryGroup =
  | 'OPERATING_EXPENSE'
  | 'CAPITAL_FLOW'
  | 'INTERNAL_TRANSFER'
  | 'DEBT_SETTLEMENT'
  | 'REVENUE'
  | 'PAYROLL'
  | 'OTHER'

export type FlowDirection = 'INFLOW' | 'OUTFLOW' | 'BOTH'

export type AccountKind = 'CASH' | 'BANK'

export type AccountStatus = 'ACTIVE' | 'INACTIVE'

export type EntryType = 'INFLOW' | 'OUTFLOW'

export type SourceType =
  | 'DAILY_REVENUE'
  | 'SUPPLIER_PAYMENT'
  | 'INTERNAL_TRANSFER'
  | 'MANUAL_EXPENSE'
  | 'CAPITAL_FLOW'
  | 'PAYROLL_PAYMENT'
  | 'MANUAL_ADJUSTMENT'
  | 'TAX_PAYMENT'

export type LedgerStatus = 'POSTED' | 'REVERSED'

export interface FinancialCategoryDto {
  id: string
  categoryCode: string
  categoryName: string
  categoryGroup: CategoryGroup
  flowDirection: FlowDirection
  isSystem: boolean
  isActive: boolean
  description?: string | null
  createdAt?: string
}

export interface CreateCategoryRequest {
  categoryCode: string
  categoryName: string
  description?: string
  categoryGroup?: CategoryGroup
  flowDirection?: FlowDirection
}

export interface FinancialAccountDto {
  id: string
  branchId: string
  bankAccountId?: string | null
  accountName: string
  accountKind: AccountKind
  currency: string
  currentBalance: number | string
  status: AccountStatus
  createdAt?: string
}

export interface CreateFinancialAccountRequest {
  accountName: string
  accountKind: AccountKind
  bankAccountId?: string | null
}

export interface CashLedgerEntryDto {
  id: string
  accountId: string
  accountName?: string
  accountKind?: AccountKind
  branchId: string
  businessDate: string
  entryType: EntryType
  amount: number | string
  signedAmount: number | string
  runningBalance: number | string
  categoryId: string
  categoryCode?: string
  categoryName?: string
  categoryGroup?: CategoryGroup
  sourceType: SourceType
  sourceId: string
  sourceLeg: string
  status: LedgerStatus
  reversalOfId?: string | null
  memo?: string | null
  evidenceFileId?: string | null
  evidenceToken?: string | null
  createdBy?: string
  createdByName?: string
  postedAt?: string
}

export interface CashLedgerPageResponse {
  items?: CashLedgerEntryDto[]
  content?: CashLedgerEntryDto[]
  totalElements?: number
  totalItems?: number
  totalPages?: number
  page?: number
  size?: number
  number?: number
}

export interface InternalTransferRequest {
  fromAccountId: string
  toAccountId: string
  amount: number | string
  businessDate: string
  note?: string
  evidenceFileId?: string | null
}

export interface InternalTransferResponse {
  id: string
  branchId: string
  fromAccountId: string
  toAccountId: string
  amount: number | string
  businessDate: string
  status: string
  note?: string | null
  evidenceFileId?: string | null
  createdAt: string
}

export interface CreateManualEntryRequest {
  accountId: string
  categoryId: string
  entryType: EntryType
  amount: number | string
  businessDate: string
  memo?: string
  evidenceFileId?: string | null
}

export interface CashLedgerFilterParams {
  accountId?: string
  categoryId?: string
  startDate?: string
  endDate?: string
  sourceType?: SourceType | string
  entryType?: EntryType | string
  search?: string
  page?: number
  size?: number
}

export const CATEGORY_GROUP_LABELS: Record<CategoryGroup, string> = {
  OPERATING_EXPENSE: 'Chi phí vận hành',
  CAPITAL_FLOW: 'Dòng vốn chủ sở hữu',
  INTERNAL_TRANSFER: 'Chuyển quỹ nội bộ',
  DEBT_SETTLEMENT: 'Thanh toán nợ NCC',
  REVENUE: 'Doanh thu bán hàng',
  PAYROLL: 'Chi trả lương nhân sự',
  OTHER: 'Khác',
}

export const FLOW_DIRECTION_LABELS: Record<FlowDirection, string> = {
  INFLOW: 'Dòng tiền vào (+ Thu)',
  OUTFLOW: 'Dòng tiền ra (- Chi)',
  BOTH: 'Cả hai hướng (Vào & Ra)',
}

export const SOURCE_TYPE_LABELS: Record<SourceType, string> = {
  DAILY_REVENUE: 'Doanh thu bán hàng',
  SUPPLIER_PAYMENT: 'Chi trả nợ NCC',
  INTERNAL_TRANSFER: 'Chuyển quỹ nội bộ',
  MANUAL_EXPENSE: 'Chi phí vận hành trực tiếp',
  CAPITAL_FLOW: 'Vốn góp / Rút vốn CSH',
  PAYROLL_PAYMENT: 'Chi trả lương nhân viên',
  MANUAL_ADJUSTMENT: 'Điều chỉnh sổ quỹ',
  TAX_PAYMENT: 'Chi nộp thuế / Ngân sách',
}

// ============================================================================
// P0-FIN-05 / FE-P0-13: Payables & Receivables Ledger & Debt Settlement
// ============================================================================

export type PayableStatus = 'OPEN' | 'PARTIALLY_PAID' | 'SETTLED' | 'CANCELLED'

export type ReceivableStatus = 'OPEN' | 'PARTIALLY_COLLECTED' | 'SETTLED' | 'CANCELLED'

export type PaymentMethod = 'CASH' | 'BANK_TRANSFER'

export interface PayablePaymentDto {
  id: string
  payableId: string
  accountId: string
  accountName?: string
  accountKind?: AccountKind
  amount: number | string
  paidOn: string
  paymentMethod: PaymentMethod
  bankReference?: string | null
  evidenceFileId?: string | null
  status: 'POSTED' | 'REVERSED'
  postedBy?: string
  postedByName?: string
  createdAt: string
}

export interface PayableDto {
  id: string
  branchId: string
  businessEntityId?: string
  counterpartyLabel: string
  sourceType: string
  sourceId?: string | null
  incurredDate: string
  dueDate?: string | null
  originalAmount: number | string
  paidAmount: number | string
  remainingAmount?: number | string
  remainingDebt?: number | string
  status: PayableStatus
  notes?: string | null
  createdAt?: string
}

export interface PayableDetailDto extends PayableDto {
  payments: PayablePaymentDto[]
}

export interface CreatePayableRequest {
  counterpartyLabel: string
  incurredDate: string
  dueDate?: string | null
  originalAmount: number | string
  notes?: string
}

export interface SettlePayableRequest {
  accountId: string
  amount: number | string
  paidOn: string
  paymentMethod: PaymentMethod
  bankReference?: string | null
  evidenceFileId?: string | null
}

export interface BranchPayablesPageResponse {
  items?: PayableDto[]
  content?: PayableDto[]
  totalElements?: number
  totalItems?: number
  totalPages?: number
  totalOutstandingDebt?: number | string
  totalOriginalDebt?: number | string
  totalPaidDebt?: number | string
  page?: number
  size?: number
  number?: number
}

export interface ReceivableReceiptDto {
  id: string
  receivableId: string
  accountId: string
  accountName?: string
  accountKind?: AccountKind
  amount: number | string
  receivedOn: string
  paymentMethod: PaymentMethod
  bankReference?: string | null
  evidenceFileId?: string | null
  status: 'POSTED' | 'REVERSED'
  postedBy?: string
  postedByName?: string
  createdAt: string
}

export interface ReceivableDto {
  id: string
  branchId: string
  businessEntityId?: string
  counterpartyLabel: string
  sourceType: string
  sourceId?: string | null
  incurredDate: string
  dueDate?: string | null
  originalAmount: number | string
  receivedAmount: number | string
  remainingAmount?: number | string
  status: ReceivableStatus
  notes?: string | null
  createdBy?: string
  createdByName?: string
  createdAt?: string
}

export interface ReceivableDetailDto extends ReceivableDto {
  receipts: ReceivableReceiptDto[]
}

export interface CreateReceivableRequest {
  counterpartyLabel: string
  incurredDate: string
  dueDate?: string | null
  originalAmount: number | string
  notes?: string
}

export interface ReceivableReceiptRequest {
  accountId: string
  amount: number | string
  receivedOn: string
  paymentMethod: PaymentMethod
  bankReference?: string | null
  evidenceFileId?: string | null
}

export interface BranchReceivablesPageResponse {
  items?: ReceivableDto[]
  content?: ReceivableDto[]
  totalElements?: number
  totalItems?: number
  totalPages?: number
  totalOutstandingReceivable?: number | string
  totalOriginalReceivable?: number | string
  totalReceivedAmount?: number | string
  page?: number
  size?: number
  number?: number
}

export const PAYABLE_STATUS_LABELS: Record<PayableStatus, string> = {
  OPEN: 'Đang nợ',
  PARTIALLY_PAID: 'Đã trả một phần',
  SETTLED: 'Đã tất toán',
  CANCELLED: 'Đã hủy',
}

export const RECEIVABLE_STATUS_LABELS: Record<ReceivableStatus, string> = {
  OPEN: 'Chưa thu',
  PARTIALLY_COLLECTED: 'Đã thu một phần',
  SETTLED: 'Đã thu xong',
  CANCELLED: 'Đã hủy',
}

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Tiền mặt tại quỹ',
  BANK_TRANSFER: 'Chuyển khoản ngân hàng',
}

// ============================================================================
// P0-FIN-03 & P0-FIN-04: Operating Expenses & Approval Workflow (FE-P0-12 / BR-FIN-07)
// ============================================================================

export type ExpenseStatus = 'DRAFT' | 'CONFIRMED' | 'VOIDED'

export interface ExpenseDto {
  id: string
  branchId: string
  expenseCode: string
  expenseDate: string
  categoryId: string
  categoryCode?: string
  categoryName?: string
  amount: number | string
  isPaid: boolean
  paymentAccountId?: string | null
  paymentAccountName?: string | null
  payableId?: string | null
  evidenceFileId?: string | null
  evidenceDownloadUrl?: string | null
  status: ExpenseStatus
  reviewFlag: boolean
  reason: string
  createdBy: string
  createdByName?: string
  confirmedBy?: string | null
  confirmedByName?: string | null
  confirmedAt?: string | null
  voidedBy?: string | null
  voidedByName?: string | null
  voidedAt?: string | null
  voidReason?: string | null
  createdAt: string
}

export interface ExpenseDetailDto extends ExpenseDto {
  linkedCashLedgerEntry?: CashLedgerEntryDto | null
}

export interface CreateExpenseRequest {
  expenseDate: string
  categoryId: string
  amount: number | string
  isPaid: boolean
  paymentAccountId?: string | null
  evidenceFileId?: string | null
  reason: string
  confirmImmediately?: boolean
}

export interface VoidExpenseRequest {
  reason: string
}

export interface BranchExpenseConfigDto {
  branchId: string
  alertThresholdAmount: number | string
  evidenceRequiredThreshold: number | string
  createdAt?: string
  updatedAt?: string
}

export interface UpdateExpenseConfigRequest {
  alertThresholdAmount: number | string
  evidenceRequiredThreshold: number | string
}

export interface BranchExpensesPageResponse {
  items?: ExpenseDto[]
  content?: ExpenseDto[]
  totalElements?: number
  totalItems?: number
  totalPages?: number
  page?: number
  size?: number
  number?: number
}

export const EXPENSE_STATUS_LABELS: Record<ExpenseStatus, string> = {
  DRAFT: 'Chờ duyệt (Nháp)',
  CONFIRMED: 'Đã duyệt chi',
  VOIDED: 'Đã hủy phiếu',
}

// ============================================================================
// P0-FIN-06 / FE-P0-14: Cash & Bank Account Reconciliation (BR-FIN-06 / TC-012)
// ============================================================================

export type ReconciliationDisposition = 'OPEN' | 'EXPLAINED' | 'ADJUSTED' | 'CLOSED'

export interface CashReconciliationDto {
  id: string
  branchId: string
  accountId: string
  accountName: string
  accountKind: 'CASH' | 'BANK'
  currency: string
  reconciliationDate: string
  recordedBalance: number
  observedBalance: number
  varianceAmount: number
  disposition: ReconciliationDisposition
  note: string | null
  evidenceFileId: string | null
  evidenceDownloadUrl: string | null
  adjustmentLedgerEntryId: string | null
  createdById: string
  createdByName: string | null
  confirmedById: string | null
  confirmedByName: string | null
  confirmedAt: string | null
  createdAt: string
}

export interface CreateReconciliationPayload {
  accountId: string
  reconciliationDate: string
  observedBalance: number
  note?: string
  evidenceFileId?: string
}

export interface ExplainDiscrepancyPayload {
  note: string
  evidenceFileId?: string
}

export interface AdjustReconciliationPayload {
  reason?: string
}

export interface CashReconciliationPageResponse {
  content?: CashReconciliationDto[]
  items?: CashReconciliationDto[]
  pageNumber?: number
  pageSize?: number
  totalElements?: number
  totalPages?: number
  page?: number
  size?: number
}

export const RECONCILIATION_DISPOSITION_LABELS: Record<ReconciliationDisposition, string> = {
  OPEN: 'Chờ xử lý',
  EXPLAINED: 'Đã giải trình',
  ADJUSTED: 'Đã điều chỉnh sổ',
  CLOSED: 'Đã đóng',
}

export const RECONCILIATION_DISPOSITION_BADGES: Record<
  ReconciliationDisposition,
  { label: string; badgeClass: string; dotClass: string }
> = {
  OPEN: {
    label: 'Chờ xử lý',
    badgeClass: 'bg-amber-50 text-amber-800 border-amber-300',
    dotClass: 'bg-amber-500',
  },
  EXPLAINED: {
    label: 'Đã giải trình',
    badgeClass: 'bg-blue-50 text-blue-800 border-blue-300',
    dotClass: 'bg-blue-500',
  },
  ADJUSTED: {
    label: 'Đã điều chỉnh sổ',
    badgeClass: 'bg-emerald-50 text-emerald-800 border-emerald-300',
    dotClass: 'bg-emerald-500',
  },
  CLOSED: {
    label: 'Đã đóng',
    badgeClass: 'bg-slate-100 text-slate-700 border-slate-300',
    dotClass: 'bg-slate-400',
  },
}

// ============================================================================
// P0-FIN-07: Tax Manual Obligations (BR-FIN-04, TC-012, FE-P0-15)
// ============================================================================

export type TaxObligationType = 'MON_BAI' | 'VAT' | 'CIT' | 'PIT' | 'OTHER'

export type TaxObligationStatus = 'OPEN' | 'PARTIALLY_PAID' | 'PAID' | 'CANCELLED'

export const TAX_OBLIGATION_TYPE_LABELS: Record<TaxObligationType, string> = {
  MON_BAI: 'Lệ phí môn bài',
  VAT: 'Thuế Giá trị gia tăng (GTGT)',
  CIT: 'Thuế Thu nhập doanh nghiệp (TNDN)',
  PIT: 'Thuế Thu nhập cá nhân (TNCN)',
  OTHER: 'Thuế / Phí / Lệ phí khác',
}

export const TAX_OBLIGATION_STATUS_LABELS: Record<TaxObligationStatus, string> = {
  OPEN: 'Chưa nộp',
  PARTIALLY_PAID: 'Đã nộp một phần',
  PAID: 'Đã hoàn thành',
  CANCELLED: 'Đã hủy',
}

export const TAX_OBLIGATION_STATUS_BADGES: Record<
  TaxObligationStatus,
  { label: string; badgeClass: string; dotClass: string }
> = {
  OPEN: {
    label: 'Chưa nộp',
    badgeClass: 'bg-amber-50 text-amber-800 border-amber-300',
    dotClass: 'bg-amber-500',
  },
  PARTIALLY_PAID: {
    label: 'Đã nộp một phần',
    badgeClass: 'bg-blue-50 text-blue-800 border-blue-300',
    dotClass: 'bg-blue-500',
  },
  PAID: {
    label: 'Đã hoàn thành',
    badgeClass: 'bg-emerald-50 text-emerald-800 border-emerald-300',
    dotClass: 'bg-emerald-500',
  },
  CANCELLED: {
    label: 'Đã hủy',
    badgeClass: 'bg-slate-100 text-slate-700 border-slate-300',
    dotClass: 'bg-slate-400',
  },
}

export interface TaxPaymentDto {
  id: string
  obligationId: string
  branchId: string | null
  paymentDate: string
  amount: number
  paymentMethod: 'BANK_TRANSFER' | 'CASH'
  financialAccountId: string | null
  financialAccountName?: string | null
  cashLedgerEntryId: string | null
  receiptNumber: string | null
  note: string | null
  evidenceFileId: string | null
  evidenceDownloadUrl?: string | null
  createdById: string
  createdByName?: string | null
  createdAt: string
}

export interface TaxObligationDto {
  id: string
  businessEntityId: string
  entityName?: string | null
  branchId: string | null
  branchName?: string | null
  obligationCode: string
  obligationType: TaxObligationType
  taxAuthorityName: string | null
  taxPeriodStart: string
  taxPeriodEnd: string
  dueDate: string | null
  amountDue: number
  amountPaid: number
  remainingLiability: number
  amountAllocated: number
  status: TaxObligationStatus
  note: string | null
  evidenceFileId: string | null
  evidenceDownloadUrl?: string | null
  enteredById: string
  enteredByName?: string | null
  createdAt: string
}

export interface TaxObligationDetailDto extends TaxObligationDto {
  payments: TaxPaymentDto[]
}

export interface CreateTaxObligationPayload {
  obligationCode: string
  obligationType: TaxObligationType
  taxAuthorityName?: string
  taxPeriodStart: string
  taxPeriodEnd: string
  dueDate?: string
  amountDue: number
  isBranchSpecific?: boolean
  note?: string
  evidenceFileId?: string
}

export interface PayTaxObligationPayload {
  paymentDate: string
  amount: number
  paymentMethod: 'BANK_TRANSFER' | 'CASH'
  financialAccountId?: string
  receiptNumber?: string
  note?: string
  evidenceFileId?: string
}

export interface TaxObligationPageResponse {
  content: TaxObligationDto[]
  items?: TaxObligationDto[]
  pageNumber: number
  pageSize: number
  totalElements: number
  totalPages: number
  page?: number
  size?: number
}

// ============================================================================
// P0-FIN-08 / FE-P0-16: Opening Balances Import & Cutover Batch (BR-FIN-08, GAP-01, ADR-009)
// ============================================================================

export type OpeningBatchStatus = 'DRAFT' | 'SUBMITTED' | 'POSTED'

export const OPENING_BATCH_STATUS_LABELS: Record<OpeningBatchStatus, string> = {
  DRAFT: 'Bản nháp',
  SUBMITTED: 'Chờ duyệt',
  POSTED: 'Đã chốt số dư',
}

export const OPENING_BATCH_STATUS_BADGES: Record<
  OpeningBatchStatus,
  { label: string; badgeClass: string; dotClass: string }
> = {
  DRAFT: {
    label: 'Bản nháp',
    badgeClass: 'bg-slate-100 text-slate-700 border-slate-300',
    dotClass: 'bg-slate-400',
  },
  SUBMITTED: {
    label: 'Chờ duyệt',
    badgeClass: 'bg-amber-50 text-amber-800 border-amber-300',
    dotClass: 'bg-amber-500',
  },
  POSTED: {
    label: 'Đã chốt số dư',
    badgeClass: 'bg-emerald-50 text-emerald-800 border-emerald-300',
    dotClass: 'bg-emerald-500',
  },
}

export interface OpeningAccountBalanceDto {
  id: string
  accountId: string
  accountName: string
  accountKind: 'CASH' | 'BANK'
  openingBalance: number
  cashLedgerEntryId: string | null
}

export interface OpeningBalanceBatchDto {
  id: string
  branchId: string
  cutoverDate: string
  status: OpeningBatchStatus
  totalCashAmount: number
  totalBankAmount: number
  totalInventoryValuation: number
  totalPayableAmount: number
  totalReceivableAmount: number
  totalTaxLiabilityAmount: number
  netWorkingCapital: number
  summarySnapshot: Record<string, unknown>
  note: string | null
  evidenceFileId: string | null
  evidenceDownloadUrl?: string | null
  createdById: string
  createdByName?: string | null
  postedById: string | null
  postedByName?: string | null
  postedAt: string | null
  createdAt: string
  accountBalances: OpeningAccountBalanceDto[]
}

export interface OpeningBalanceStatusResponse {
  isPosted: boolean
  cutoverDate: string | null
  activeBatch: OpeningBalanceBatchDto | null
}

export interface AccountOpeningBalanceInput {
  accountId: string
  openingBalance: number
}

export interface CreateOpeningBatchPayload {
  cutoverDate: string
  note?: string
  evidenceFileId?: string
  accountBalances: AccountOpeningBalanceInput[]
}





