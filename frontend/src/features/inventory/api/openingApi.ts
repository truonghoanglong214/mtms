import { apiClient } from '@/shared/api/apiClient'
import type {
  BranchOpeningStatusDto,
  OpeningPreviewRequest,
  OpeningPreviewResponse,
  CommitOpeningRequest,
  CommitOpeningResponse,
  StockBalancesResponse,
  StockBalancesFilterParams,
} from '../types/opening.types'

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

export const openingApi = {
  /**
   * Get branch opening inventory status and lock details (TC-024)
   */
  async getOpeningStatus(branchId: string): Promise<BranchOpeningStatusDto> {
    const res = await apiClient.get(`/branches/${branchId}/inventory/opening`)
    return extractData<BranchOpeningStatusDto>(res)
  },

  /**
   * Validate and preview opening inventory items with live WAC & line total calculations
   */
  async previewOpening(
    branchId: string,
    data: OpeningPreviewRequest
  ): Promise<OpeningPreviewResponse> {
    const res = await apiClient.post(`/branches/${branchId}/inventory/opening/preview`, data)
    return extractData<OpeningPreviewResponse>(res)
  },

  /**
   * Commit and lock branch opening balance (Post Once / TC-024)
   * Protected with server-side Idempotency-Key
   */
  async commitOpening(
    branchId: string,
    data: CommitOpeningRequest
  ): Promise<CommitOpeningResponse> {
    const res = await apiClient.post(`/branches/${branchId}/inventory/opening/commit`, data)
    return extractData<CommitOpeningResponse>(res)
  },

  /**
   * Query branch on-hand stock balances, moving WAC, and total valuation
   */
  async getStockBalances(
    branchId: string,
    params?: StockBalancesFilterParams
  ): Promise<StockBalancesResponse> {
    const res = await apiClient.get(`/branches/${branchId}/inventory/balances`, {
      params,
    })
    return extractData<StockBalancesResponse>(res)
  },

  /**
   * Generates and downloads a starter CSV template for opening inventory physical count
   * Includes UTF-8 BOM for Microsoft Excel Vietnamese character compatibility.
   */
  downloadOpeningTemplate(): void {
    const headers = ['item_code', 'item_name', 'unit', 'quantity', 'unit_cost', 'lot_code', 'expires_on']
    const sampleRows = [
      ['ING-MILK-OT-380G', 'Sữa đặc có đường Ông Thọ đỏ lon 380g', 'Lon', '50.000', '24000.00', 'LOT-202611-01', '2027-11-16'],
      ['ING-COFFEE-01', 'Cà phê hạt Robusta rang mộc', 'Kilogram', '20.000', '150000.00', '', ''],
      ['ING-SUGAR-01', 'Đường tinh luyện Biên Hòa 1kg', 'Gói', '35.000', '22500.00', '', ''],
      ['ING-TEA-OOLONG', 'Trà Oolong Bảo Lộc loại 1', 'Kilogram', '15.000', '280000.00', 'LOT-TEA-202611', '2027-05-16'],
    ]

    const csvContent =
      '\uFEFF' +
      [headers.join(','), ...sampleRows.map((row) => row.map((val) => `"${val.replace(/"/g, '""')}"`).join(','))].join('\r\n')

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.setAttribute('href', url)
    link.setAttribute('download', `MTMS_Mau_Kiem_Ke_Ton_Kho_Dau_Ky.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  },
}
