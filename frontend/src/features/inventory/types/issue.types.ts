/**
 * TypeScript Data Transfer Objects and Commands for P0-INV-04 & P0-INV-05 / FE-P0-27
 * (Manual Consumption Issue & Moving WAC Posting Engine / TC-020, TC-021, TC-022).
 * Strictly mirrors docs/api/contracts/P0-INV-04-05.md.
 */

export type StockDocumentType = 'CONSUMPTION_ISSUE' | 'SCRAP_DISPOSAL' | 'ADJUSTMENT'

export type StockDocumentStatus = 'DRAFT' | 'SUBMITTED' | 'POSTED' | 'CANCELLED'

export interface StockDocumentLineDto {
  id: string
  documentId: string
  lineNo: number
  itemId: string
  itemCode?: string
  itemName?: string
  unitCode?: string
  unitName?: string
  lotId: string | null
  lotCode?: string | null
  issueQuantity: string // numeric string e.g. "5.000"
  bookedUnitCost: string // numeric string e.g. "110000.00"
  bookedTotal: string // numeric string e.g. "550000.00"
  notes: string | null
}

export interface StockDocumentDto {
  id: string
  branchId: string
  documentNumber: string
  documentType: StockDocumentType
  documentDate: string // YYYY-MM-DD
  status: StockDocumentStatus
  reason: string | null
  totalLines: number
  totalQuantity: string // numeric string e.g. "5.000"
  totalBookedCost: string // numeric string e.g. "550000.00"
  createdBy?: string
  createdByName?: string | null
  approvedBy?: string | null
  approvedByName?: string | null
  postedAt?: string | null
  createdAt?: string
  lines?: StockDocumentLineDto[]
}

export interface CreateStockDocumentLineCommand {
  itemId: string
  issueQuantity: string // numeric string > 0
  lotId?: string | null
  notes?: string | null
}

export interface CreateStockDocumentCommand {
  documentDate: string // YYYY-MM-DD
  documentType: StockDocumentType
  reason?: string | null
  autoPost?: boolean
  lines: CreateStockDocumentLineCommand[]
}

export interface IssueListResponse {
  items: StockDocumentDto[]
  totalItems: number
}

export interface IssueFilterParams {
  status?: StockDocumentStatus
  documentType?: StockDocumentType
  startDate?: string
  endDate?: string
  search?: string
  page?: number
  size?: number
}
