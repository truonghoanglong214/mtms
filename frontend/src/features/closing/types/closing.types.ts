export interface RevenueCheckDto {
  totalDaysInMonth: number
  reportedDays: number
  missingDates: string[]
  passed: boolean
}

export interface PayrollCheckDto {
  periodExists: boolean
  status: string
  totalGrossSalary: number
  passed: boolean
}

export interface ReconciliationCheckDto {
  totalAccounts: number
  reconciledCount: number
  hasOpenDiscrepancies: boolean
  passed: boolean
}

export interface InventoryCheckDto {
  consumptionIssuesCount: number
  passed: boolean
}

export interface ClosingPrerequisitesDto {
  branchId: string
  monthStart: string
  canClose: boolean
  revenueCheck: RevenueCheckDto
  payrollCheck: PayrollCheckDto
  reconciliationCheck: ReconciliationCheckDto
  inventoryCheck: InventoryCheckDto
  status: 'OPEN' | 'CLOSING' | 'CLOSED' | 'REOPEN_REQUESTED' | 'REOPENED' | 'SUPERSEDED'
  blockReason?: string
}

export interface NetRevenueDto {
  total: number
  cashRevenue: number
  bankRevenue: number
  appNetRevenue: number
}

export interface CostOfGoodsSoldDto {
  total: number
  verifiedNoDoubleCost: boolean
}

export interface OperatingExpensesDto {
  payrollAccrual: number
  otherOpex: number
  totalOpex: number
}

export interface TaxObligationsDto {
  total: number
}

export interface CashFlowComparisonDto {
  cashInflow: number
  cashOutflow: number
  netCashFlow: number
  accrualCashVariance: number
  varianceExplanation: string
}

export interface ManagerialPnLReportDto {
  branchId: string
  branchName: string
  monthStart: string
  isProvisional: boolean
  depreciationDisclaimer: string
  netRevenue: NetRevenueDto
  costOfGoodsSold: CostOfGoodsSoldDto
  grossProfit: number
  operatingExpenses: OperatingExpensesDto
  taxObligations: TaxObligationsDto
  managerialOperatingProfit: number
  cashFlowComparison: CashFlowComparisonDto
}

export interface ClosePeriodCommand {
  monthStart: string
  note?: string
}

export interface CloseComponentDto {
  id: string
  componentCode: string
  signedAmount: number
  provenance: string
}

export interface MonthlyCloseDto {
  id: string
  branchId: string
  monthStart: string
  revision: number
  status: 'OPEN' | 'CLOSING' | 'CLOSED' | 'REOPEN_REQUESTED' | 'REOPENED' | 'SUPERSEDED'
  managerialProfit: number
  isProvisional: boolean
  closedBy?: string
  closedAt?: string
  snapshot?: string
  note?: string
  components: CloseComponentDto[]
}

export interface CloseStatusDto {
  branchId: string
  monthStart: string
  status: 'OPEN' | 'CLOSING' | 'CLOSED' | 'REOPEN_REQUESTED' | 'REOPENED' | 'SUPERSEDED'
  isClosed: boolean
  closedAt?: string
}
