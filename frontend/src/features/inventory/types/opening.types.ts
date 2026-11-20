/**
 * Opening Inventory & Valuation Types (P0-INV-02 / FE-P0-25 / BR-INV-08 / TC-024)
 * Strictly mirrors docs/api/contracts/P0-INV-02.md
 */

export interface OpeningRecordDto {
  id: string
  openingDate: string // YYYY-MM-DD
  totalItems: number
  totalQuantity: string // numeric string e.g. "150.000"
  totalValue: string // numeric string e.g. "15000000.00"
  status: 'COMMITTED' | string
  approvedBy: string
  approvedByName?: string | null
  approvedAt: string
  note?: string | null
}

export interface BranchOpeningStatusDto {
  branchId: string
  branchName: string
  isOpeningCommitted: boolean
  openingRecord: OpeningRecordDto | null
}

export interface OpeningManualRowInput {
  itemCode: string
  quantity: string
  unitCost: string
  lotCode?: string | null
  expiresOn?: string | null
}

export interface OpeningPreviewRequest {
  importBatchId?: string | null
  openingDate: string
  rows?: OpeningManualRowInput[]
}

export interface OpeningPreviewRowDto {
  rowNumber: number
  itemId: string
  itemCode: string
  itemName: string
  unitCode: string
  unitName: string
  quantity: string
  unitCost: string
  lineTotal: string
  calculatedWac: string
  lotCode: string | null
  expiresOn: string | null
  lotTrackingRequired: boolean
  status: 'VALID' | 'ERROR'
  errors: string[]
}

export interface OpeningPreviewResponse {
  branchId: string
  openingDate: string
  totalItems: number
  totalQuantity: string
  totalValuation: string
  hasErrors: boolean
  previewRows: OpeningPreviewRowDto[]
}

export interface CommitOpeningRequest {
  importBatchId?: string | null
  openingDate: string
  rows?: OpeningManualRowInput[]
  note?: string | null
}

export interface CommitOpeningResponse {
  id: string
  branchId: string
  openingDate: string
  totalItems: number
  totalQuantity: string
  totalValue: string
  status: string
  approvedBy: string
  approvedByName?: string | null
  approvedAt: string
  note?: string | null
}

export interface StockBalanceDto {
  id: string
  branchId: string
  itemId: string
  itemCode: string
  itemName: string
  unitCode: string
  unitName: string
  quantity: string
  avgUnitCost: string
  inventoryValue: string
  updatedAt: string
}

export interface StockBalancesResponse {
  items: StockBalanceDto[]
  totalItems: number
  totalBranchValue: string
}

export interface StockBalancesFilterParams {
  search?: string
  page?: number
  size?: number
}
