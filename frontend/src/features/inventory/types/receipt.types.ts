/**
 * TypeScript Data Transfer Objects and Commands for P0-INV-03 / FE-P0-26 (Goods Receipt & Payables).
 * Strictly mirrors docs/api/contracts/P0-INV-03.md.
 */

export type PaymentStatus = 'UNPAID' | 'PARTIALLY_PAID' | 'PAID'
export type ReceiptStatus = 'DRAFT' | 'POSTED' | 'VOIDED'
export type PayableStatus = 'OPEN' | 'SETTLED' | 'DISPUTED' | 'CANCELLED'

export interface PurchaseReceiptLineDto {
  id: string
  receiptId: string
  lineNo: number
  itemId: string
  itemCode?: string
  itemName?: string
  baseUnitCode?: string
  baseUnitName?: string
  packName: string | null
  declaredPackQuantity: string | null
  conversionSnapshot: string
  receivedBaseQuantity: string
  baseUnitCost: string
  lineTotal: string
  lotCode: string | null
  expiresOn: string | null
}

export interface PurchaseReceiptDto {
  id: string
  branchId: string
  receiptNumber: string
  supplierId: string | null
  supplierName: string | null
  receivedOn: string // YYYY-MM-DD
  invoiceNumber: string | null
  invoiceTotal: string
  paymentStatus: PaymentStatus
  payableId: string | null
  status: ReceiptStatus
  totalLines?: number
  notes?: string | null
  postedBy?: string | null
  postedByName?: string | null
  postedAt?: string | null
  createdAt?: string
  lines?: PurchaseReceiptLineDto[]
}

export interface CreateReceiptLineCommand {
  itemId: string
  packName?: string | null
  declaredPackQuantity?: string | null
  conversionSnapshot?: string
  receivedBaseQuantity: string
  baseUnitCost: string
  lineTotal: string
  lotCode?: string | null
  expiresOn?: string | null
}

export interface CreateReceiptCommand {
  supplierId: string | null
  receivedOn: string // YYYY-MM-DD
  invoiceNumber?: string | null
  paymentStatus: PaymentStatus
  dueDate?: string | null
  notes?: string | null
  autoPost?: boolean
  lines: CreateReceiptLineCommand[]
}

export interface PayableDto {
  id: string
  branchId: string
  counterpartyLabel: string
  sourceType: string
  sourceId: string | null
  incurredDate: string
  dueDate: string | null
  originalAmount: string
  paidAmount: string
  remainingAmount: string
  status: PayableStatus
  createdAt: string
}

export interface ReceiptListResponse {
  items: PurchaseReceiptDto[]
  totalItems: number
}

export interface PayablesResponse {
  items: PayableDto[]
  totalItems: number
  totalOutstandingDebt: string
}

export interface ReceiptFilterParams {
  status?: ReceiptStatus
  supplierId?: string
  startDate?: string
  endDate?: string
  search?: string
  page?: number
  size?: number
}

export interface PayableFilterParams {
  status?: PayableStatus
  search?: string
  page?: number
  size?: number
}
