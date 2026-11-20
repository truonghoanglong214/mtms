import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Package,
  Coins,
  AlertTriangle,
  ArrowDownLeft,
  ArrowUpRight,
  TrendingUp,
  RefreshCw,
  Calendar,
  Layers,
  Sparkles,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { reportingApi } from '../api/reportingApi'
import type { InventoryDashboardDto } from '../types/reporting.types'

interface InventoryDashboardTabProps {
  branchId: string
  branchName?: string
  onNavigateToLedger?: () => void
  onNavigateToCogs?: () => void
}

function formatVnd(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === '') return '0 ₫'
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(num)
}

function formatQuantity(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === '') return '0'
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0'
  return new Intl.NumberFormat('vi-VN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 3,
  }).format(num)
}

function getStartOfCurrentMonth(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}-01`
}

function getToday(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export const InventoryDashboardTab: React.FC<InventoryDashboardTabProps> = ({
  branchId,
  branchName,
  onNavigateToLedger,
  onNavigateToCogs,
}) => {
  const [startDate, setStartDate] = useState<string>(getStartOfCurrentMonth())
  const [endDate, setEndDate] = useState<string>(getToday())
  const [data, setData] = useState<InventoryDashboardDto | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchDashboard = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    setError(null)
    try {
      const res = await reportingApi.getInventoryDashboard(branchId, startDate, endDate)
      setData(res)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải dữ liệu báo cáo tổng quan kho.'
      )
    } finally {
      setIsLoading(false)
    }
  }, [branchId, startDate, endDate])

  useEffect(() => {
    fetchDashboard()
  }, [fetchDashboard])

  // Valuation numeric for proportion calculations
  const totalValuationNum = useMemo(() => {
    if (!data?.totalInventoryValuation) return 0
    const val = parseFloat(data.totalInventoryValuation)
    return isNaN(val) || val <= 0 ? 0 : val
  }, [data?.totalInventoryValuation])

  return (
    <div className="space-y-6">
      {/* Date Range Filter Header */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs">
        <div className="flex items-center gap-2">
          <Calendar className="h-4 w-4 text-[#304638]" />
          <span className="text-xs font-semibold text-slate-800">
            Kỳ phân tích thống kê:
          </span>
          <span className="text-xs text-slate-500 font-mono">
            {startDate} → {endDate}
          </span>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center gap-1.5 text-xs text-slate-600">
            <span>Từ:</span>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="h-8 px-2.5 text-xs rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-1 focus:ring-[#304638]"
            />
          </div>
          <div className="flex items-center gap-1.5 text-xs text-slate-600">
            <span>Đến:</span>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="h-8 px-2.5 text-xs rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-1 focus:ring-[#304638]"
            />
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={fetchDashboard}
            disabled={isLoading}
            className="h-8 px-3 text-xs gap-1.5 rounded-xl border-slate-200 hover:bg-slate-50"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            <span>Làm mới</span>
          </Button>
        </div>
      </div>

      {/* Error Notice */}
      {error && (
        <div className="p-4 rounded-2xl border border-rose-200 bg-rose-50 text-rose-800 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-rose-600 shrink-0" />
            <span>{error}</span>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={fetchDashboard}
            className="h-7 text-xs border-rose-300 text-rose-700 hover:bg-rose-100"
          >
            Thử lại
          </Button>
        </div>
      )}

      {/* 4 Distinct KPI Highlight Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1: On-hand Qty */}
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-2xs hover:shadow-xs transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Tổng số lượng tồn kho
            </span>
            <span className="p-2 rounded-xl bg-emerald-50 text-emerald-800">
              <Package className="h-4.5 w-4.5" />
            </span>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold font-mono tracking-tight text-slate-900">
              {isLoading ? (
                <div className="h-7 w-24 bg-slate-200 rounded animate-pulse" />
              ) : (
                formatQuantity(data?.totalOnHandQuantity)
              )}
            </div>
            <div className="mt-1 flex items-center gap-1.5 text-xs text-slate-500">
              <Layers className="h-3.5 w-3.5 text-slate-400" />
              <span>Theo dõi </span>
              <strong className="text-slate-800 font-semibold">
                {data?.totalItemsTracked ?? 0}
              </strong>
              <span> mặt hàng</span>
            </div>
          </div>
        </div>

        {/* KPI 2: Total Inventory Valuation */}
        <div className="rounded-2xl border border-emerald-200 bg-gradient-to-br from-emerald-50/60 to-white p-5 shadow-2xs hover:shadow-xs transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-emerald-900 uppercase tracking-wider">
              Tổng giá trị tồn kho WAC
            </span>
            <span className="p-2 rounded-xl bg-[#304638] text-white">
              <Coins className="h-4.5 w-4.5" />
            </span>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold font-mono tracking-tight text-emerald-900">
              {isLoading ? (
                <div className="h-7 w-32 bg-slate-200 rounded animate-pulse" />
              ) : (
                formatVnd(data?.totalInventoryValuation)
              )}
            </div>
            <div className="mt-1 flex items-center gap-1.5 text-xs text-emerald-700">
              <Sparkles className="h-3.5 w-3.5" />
              <span>Giá vốn bình quân di động tức thời</span>
            </div>
          </div>
        </div>

        {/* KPI 3: Low Stock Warnings */}
        <div className="rounded-2xl border border-amber-200 bg-gradient-to-br from-amber-50/50 to-white p-5 shadow-2xs hover:shadow-xs transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-amber-900 uppercase tracking-wider">
              Cảnh báo sắp hết hàng
            </span>
            <span className="p-2 rounded-xl bg-amber-100 text-amber-800">
              <AlertTriangle className="h-4.5 w-4.5" />
            </span>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-bold font-mono tracking-tight text-amber-950">
              {isLoading ? (
                <div className="h-7 w-16 bg-slate-200 rounded animate-pulse" />
              ) : (
                data?.lowStockItemCount ?? 0
              )}
              <span className="text-sm font-normal text-amber-800 ml-1.5">mặt hàng</span>
            </div>
            <div className="mt-1 text-xs text-amber-700">
              {(data?.lowStockItemCount ?? 0) > 0 ? (
                <span className="font-medium text-amber-800">
                  Cần lên kế hoạch nhập hàng bổ sung
                </span>
              ) : (
                <span className="text-emerald-700 font-medium">Mức tồn kho an toàn</span>
              )}
            </div>
          </div>
        </div>

        {/* KPI 4: Purchases vs Issues in Period */}
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-2xs hover:shadow-xs transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
              Biến động giá trị trong kỳ
            </span>
            <span className="p-2 rounded-xl bg-slate-100 text-slate-700">
              <TrendingUp className="h-4.5 w-4.5" />
            </span>
          </div>
          <div className="mt-2.5 space-y-1.5">
            <div className="flex items-center justify-between text-xs">
              <span className="flex items-center gap-1 text-slate-600">
                <ArrowDownLeft className="h-3.5 w-3.5 text-emerald-600" />
                <span>Nhập kho:</span>
              </span>
              <span className="font-mono font-bold text-emerald-800">
                {isLoading ? '...' : formatVnd(data?.periodReceiptsTotal)}
              </span>
            </div>
            <div className="flex items-center justify-between text-xs">
              <span className="flex items-center gap-1 text-slate-600">
                <ArrowUpRight className="h-3.5 w-3.5 text-rose-600" />
                <span>Xuất dùng:</span>
              </span>
              <span className="font-mono font-bold text-rose-800">
                {isLoading ? '...' : formatVnd(data?.periodIssuesTotal)}
              </span>
            </div>
            <div className="pt-1 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-500">
              <span>Định giá:</span>
              <span className="font-medium text-slate-700">Không tính trùng chi phí</span>
            </div>
          </div>
        </div>
      </div>

      {/* Top Valued Items Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
        <div className="p-4 sm:p-5 border-b border-slate-200 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 bg-slate-50/50">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <span>Mặt hàng có giá trị tồn kho lớn nhất (Top Valued Items)</span>
              <Badge variant="outline" className="border-slate-300 text-slate-700 text-[10px]">
                {data?.topValuedItems?.length ?? 0} mặt hàng
              </Badge>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Tỷ trọng giá trị đóng góp vào tài sản tồn kho tại {branchName || 'chi nhánh hiện tại'}.
            </p>
          </div>

          <div className="flex items-center gap-2 self-end sm:self-auto">
            {onNavigateToLedger && (
              <Button
                variant="outline"
                size="sm"
                onClick={onNavigateToLedger}
                className="h-8 text-xs rounded-xl border-slate-200 text-slate-700 hover:bg-slate-100"
              >
                Xem sổ chuyển động kho
              </Button>
            )}
            {onNavigateToCogs && (
              <Button
                size="sm"
                onClick={onNavigateToCogs}
                className="h-8 text-xs rounded-xl bg-[#304638] hover:bg-[#25372C] text-white shadow-2xs"
              >
                Đối soát Giá vốn COGS
              </Button>
            )}
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-600 font-semibold">
                <th className="py-3 px-4 w-12 text-center">#</th>
                <th className="py-3 px-4">Mã NVL</th>
                <th className="py-3 px-4">Tên nguyên vật liệu</th>
                <th className="py-3 px-4 text-center">ĐVT Chuẩn</th>
                <th className="py-3 px-4 text-right">Lượng tồn</th>
                <th className="py-3 px-4 text-right">Đơn giá WAC</th>
                <th className="py-3 px-4 text-right">Giá trị tồn kho</th>
                <th className="py-3 px-4 w-44">Tỷ trọng giá trị (%)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                Array.from({ length: 5 }).map((_, idx) => (
                  <tr key={idx} className="animate-pulse">
                    <td className="py-3.5 px-4 text-center">
                      <div className="h-4 w-4 bg-slate-200 rounded mx-auto" />
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="h-4 w-24 bg-slate-200 rounded" />
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="h-4 w-44 bg-slate-200 rounded" />
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <div className="h-4 w-12 bg-slate-200 rounded mx-auto" />
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="h-4 w-16 bg-slate-200 rounded ml-auto" />
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="h-4 w-20 bg-slate-200 rounded ml-auto" />
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <div className="h-4 w-24 bg-slate-200 rounded ml-auto" />
                    </td>
                    <td className="py-3.5 px-4">
                      <div className="h-3 w-full bg-slate-200 rounded" />
                    </td>
                  </tr>
                ))
              ) : !data?.topValuedItems || data.topValuedItems.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500">
                    <div className="max-w-sm mx-auto space-y-2">
                      <div className="h-10 w-10 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
                        <Package className="h-5 w-5" />
                      </div>
                      <p className="font-semibold text-slate-700">
                        Chưa có dữ liệu tồn kho phát sinh
                      </p>
                      <p className="text-[11px] text-slate-500">
                        Chi nhánh chưa có số dư tồn kho ban đầu hoặc chưa thực hiện nhập kho hàng hóa.
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                data.topValuedItems.map((item, idx) => {
                  const itemVal = parseFloat(item.inventoryValue) || 0
                  const proportion =
                    totalValuationNum > 0 ? (itemVal / totalValuationNum) * 100 : 0

                  return (
                    <tr
                      key={item.itemId || idx}
                      className="hover:bg-slate-50/70 transition-colors text-slate-800"
                    >
                      <td className="py-3.5 px-4 text-center font-mono text-[11px] text-slate-400">
                        {idx + 1}
                      </td>
                      <td className="py-3.5 px-4 font-mono font-semibold text-emerald-800">
                        {item.itemCode}
                      </td>
                      <td className="py-3.5 px-4 font-medium text-slate-900">
                        {item.itemName}
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        <Badge
                          variant="outline"
                          className="text-[11px] font-mono border-slate-200 bg-slate-50 text-slate-700"
                        >
                          {item.unitName || item.unitCode}
                        </Badge>
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono font-bold text-slate-900">
                        {formatQuantity(item.quantity)}
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono text-slate-700">
                        {formatVnd(item.avgUnitCost)}
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono font-bold text-emerald-900">
                        {formatVnd(item.inventoryValue)}
                      </td>
                      <td className="py-3.5 px-4">
                        <div className="space-y-1">
                          <div className="flex items-center justify-between text-[11px] font-mono text-slate-600">
                            <span>{proportion.toFixed(1)}%</span>
                          </div>
                          <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                            <div
                              className="h-full bg-emerald-600 rounded-full transition-all duration-300"
                              style={{ width: `${Math.min(100, Math.max(0, proportion))}%` }}
                            />
                          </div>
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
export default InventoryDashboardTab
