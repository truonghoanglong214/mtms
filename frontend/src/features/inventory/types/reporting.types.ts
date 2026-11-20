/**
 * MTMS Inventory Reporting & COGS Types
 * P0-INV-06: Inventory UI & Movements Ledger (BR-INV-01 / TC-021 / FE-P0-28)
 * P0-INV-07: Inventory-to-COGS Period Read Model (BR-INV-02 / TC-020)
 * Reference: docs/api/contracts/P0-INV-06-07.md
 */

export type StockMovementType = 'OPENING_BALANCE' | 'PURCHASE_RECEIPT' | 'MANUAL_ISSUE'

export interface TopValuedItemDto {
  itemId: string
  itemCode: string
  itemName: string
  unitCode: string
  unitName?: string
  quantity: string
  avgUnitCost: string
  inventoryValue: string
}

export interface InventoryDashboardDto {
  branchId: string
  branchName: string
  totalOnHandQuantity: string
  totalInventoryValuation: string
  totalItemsTracked: number
  lowStockItemCount: number
  periodReceiptsTotal: string
  periodIssuesTotal: string
  topValuedItems: TopValuedItemDto[]
}

export interface StockMovementItemDto {
  id: string
  branchId: string
  itemId: string
  itemCode: string
  itemName: string
  unitCode: string
  unitName?: string
  businessDate: string
  movementType: StockMovementType
  signedQuantity: string
  signedValue: string
  bookedAvgCost: string
  sourceType: string
  sourceId: string
  sourceRef: string
  createdByName?: string
  createdAt: string
}

export interface StockMovementPageResponse {
  items: StockMovementItemDto[]
  totalItems: number
}

export interface StockMovementQueryParams {
  movementType?: StockMovementType
  itemId?: string
  startDate?: string
  endDate?: string
  search?: string
  page?: number
  size?: number
}

export interface CogsItemDetailDto {
  itemId: string
  itemCode: string
  itemName: string
  unitCode: string
  unitName?: string
  beginningQty: string
  beginningValue: string
  purchasedQty: string
  purchasedValue: string
  issuedQty: string
  cogsValue: string
  endingQty: string
  endingWac: string
  endingValue: string
}

export interface CogsReportDto {
  branchId: string
  branchName: string
  period?: string
  startDate: string
  endDate: string
  beginningStockValue: string
  periodPurchasesTotal: string
  periodCogsTotal: string
  periodAdjustmentsTotal: string
  calculatedEndingStockValue: string
  actualEndingStockValue: string
  reconciliationDiscrepancy: string
  reconciled: boolean
  noDoubleCostVerified: boolean
  accountingExplanation: string
  items: CogsItemDetailDto[]
}
