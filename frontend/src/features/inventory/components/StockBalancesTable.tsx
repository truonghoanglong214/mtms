import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Search,
  RefreshCw,
  AlertCircle,
  Package,
  Layers,
  Coins,
  ChevronLeft,
  ChevronRight,
  TrendingUp,
} from 'lucide-react'
import { Input } from '@/shared/components/ui/input'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { openingApi } from '../api/openingApi'
import type { StockBalanceDto } from '../types/opening.types'

interface StockBalancesTableProps {
  branchId: string
  branchName?: string
  isOpeningCommitted?: boolean
  onOpenOpeningWizard?: () => void
}

function formatVnd(value: string | number): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 2,
  }).format(num)
}

function formatQuantity(value: string | number): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0'
  return new Intl.NumberFormat('vi-VN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 3,
  }).format(num)
}

export const StockBalancesTable: React.FC<StockBalancesTableProps> = ({
  branchId,
  branchName,
  isOpeningCommitted = false,
  onOpenOpeningWizard,
}) => {
  const [items, setItems] = useState<StockBalanceDto[]>([])
  const [totalItems, setTotalItems] = useState(0)
  const [totalBranchValue, setTotalBranchValue] = useState('0.00')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Filters & Pagination
  const [searchTerm, setSearchTerm] = useState('')
  const [page, setPage] = useState(0)
  const pageSize = 15

  const fetchBalances = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    setError(null)
    try {
      const res = await openingApi.getStockBalances(branchId, {
        search: searchTerm.trim() || undefined,
        page,
        size: pageSize,
      })
      setItems(res.items || [])
      setTotalItems(res.totalItems || 0)
      setTotalBranchValue(res.totalBranchValue || '0.00')
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải số dư tồn kho chi nhánh.'
      )
    } finally {
      setIsLoading(false)
    }
  }, [branchId, searchTerm, page])

  useEffect(() => {
    fetchBalances()
  }, [fetchBalances])

  // Total quantity on page
  const pageTotalQuantity = useMemo(() => {
    return items.reduce((acc, curr) => acc + (parseFloat(curr.quantity) || 0), 0)
  }, [items])

  return (
    <div className="space-y-4">
      {/* Top Filter and Actions */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-white p-3.5 rounded-2xl border border-slate-200 shadow-xs">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <Input
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value)
              setPage(0)
            }}
            placeholder="Tìm theo mã NVL hoặc tên nguyên vật liệu..."
            className="pl-9 h-9 text-xs rounded-xl bg-slate-50 border-slate-200 focus:bg-white"
          />
        </div>

        <div className="flex items-center gap-2 self-end sm:self-auto">
          <Button
            variant="outline"
            size="sm"
            onClick={fetchBalances}
            disabled={isLoading}
            className="h-9 px-3 text-xs gap-1.5 rounded-xl border-slate-200 text-slate-700 hover:bg-slate-50"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            <span>Làm mới</span>
          </Button>

          {!isOpeningCommitted && onOpenOpeningWizard && (
            <Button
              size="sm"
              onClick={onOpenOpeningWizard}
              className="h-9 px-3 text-xs gap-1.5 rounded-xl bg-[#304638] hover:bg-[#25372C] text-white shadow-xs"
            >
              <Package className="h-3.5 w-3.5" />
              <span>Khởi tạo tồn đầu kỳ</span>
            </Button>
          )}
        </div>
      </div>

      {/* Summary KPI Badges */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="bg-white rounded-xl p-3 border border-slate-200 shadow-2xs flex items-center gap-3">
          <div className="h-9 w-9 rounded-lg bg-emerald-50 text-emerald-800 flex items-center justify-center shrink-0">
            <Package className="h-4.5 w-4.5" />
          </div>
          <div>
            <div className="text-[11px] text-slate-500 font-medium">Tổng mặt hàng tồn</div>
            <div className="text-sm font-bold text-slate-900">
              {totalItems} <span className="text-xs font-normal text-slate-500">mặt hàng</span>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-xl p-3 border border-slate-200 shadow-2xs flex items-center gap-3">
          <div className="h-9 w-9 rounded-lg bg-blue-50 text-blue-800 flex items-center justify-center shrink-0">
            <Layers className="h-4.5 w-4.5" />
          </div>
          <div>
            <div className="text-[11px] text-slate-500 font-medium">Tổng lượng tồn trên trang</div>
            <div className="text-sm font-bold text-slate-900 font-mono">
              {formatQuantity(pageTotalQuantity)}
            </div>
          </div>
        </div>

        <div className="bg-white rounded-xl p-3 border border-slate-200 shadow-2xs flex items-center gap-3">
          <div className="h-9 w-9 rounded-lg bg-emerald-50 text-emerald-800 flex items-center justify-center shrink-0">
            <Coins className="h-4.5 w-4.5" />
          </div>
          <div>
            <div className="text-[11px] text-slate-500 font-medium">
              Tổng giá trị tồn kho WAC {branchName ? `(${branchName})` : ''}
            </div>
            <div className="text-sm font-bold text-emerald-800 font-mono">
              {formatVnd(totalBranchValue)}
            </div>
          </div>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="p-3.5 rounded-xl border border-red-200 bg-red-50 text-red-800 flex items-center gap-2.5 text-xs">
          <AlertCircle className="h-4 w-4 shrink-0 text-red-600" />
          <span>{error}</span>
        </div>
      )}

      {/* Main Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-600 font-semibold">
                <th className="py-3 px-3.5 w-12 text-center">#</th>
                <th className="py-3 px-3.5">Mã NVL</th>
                <th className="py-3 px-3.5">Tên nguyên vật liệu</th>
                <th className="py-3 px-3.5 text-center">ĐVT Chuẩn</th>
                <th className="py-3 px-3.5 text-right">Tồn khả dụng</th>
                <th className="py-3 px-3.5 text-right">Đơn giá Moving WAC</th>
                <th className="py-3 px-3.5 text-right">Tổng giá trị tồn kho</th>
                <th className="py-3 px-3.5 text-right">Cập nhật lúc</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                Array.from({ length: 5 }).map((_, idx) => (
                  <tr key={idx} className="animate-pulse">
                    <td className="py-3 px-3.5 text-center"><div className="h-4 w-4 bg-slate-200 rounded mx-auto" /></td>
                    <td className="py-3 px-3.5"><div className="h-4 w-20 bg-slate-200 rounded" /></td>
                    <td className="py-3 px-3.5"><div className="h-4 w-48 bg-slate-200 rounded" /></td>
                    <td className="py-3 px-3.5"><div className="h-4 w-12 bg-slate-200 rounded mx-auto" /></td>
                    <td className="py-3 px-3.5"><div className="h-4 w-16 bg-slate-200 rounded ml-auto" /></td>
                    <td className="py-3 px-3.5"><div className="h-4 w-20 bg-slate-200 rounded ml-auto" /></td>
                    <td className="py-3 px-3.5"><div className="h-4 w-24 bg-slate-200 rounded ml-auto" /></td>
                    <td className="py-3 px-3.5"><div className="h-4 w-20 bg-slate-200 rounded ml-auto" /></td>
                  </tr>
                ))
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500">
                    <div className="max-w-sm mx-auto space-y-2">
                      <div className="h-10 w-10 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
                        <Package className="h-5 w-5" />
                      </div>
                      <p className="font-semibold text-slate-700">
                        {searchTerm ? 'Không tìm thấy nguyên vật liệu nào phù hợp' : 'Chưa có dữ liệu tồn kho'}
                      </p>
                      <p className="text-[11px] text-slate-500">
                        {searchTerm
                          ? 'Thử điều chỉnh từ khóa tìm kiếm mã hoặc tên mặt hàng.'
                          : isOpeningCommitted
                          ? 'Chi nhánh chưa có mặt hàng nào phát sinh số dư tồn.'
                          : 'Vui lòng thực hiện mở sổ tồn kho đầu kỳ để khởi tạo số dư và giá vốn WAC.'}
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                items.map((row, idx) => (
                  <tr
                    key={row.id}
                    className="hover:bg-slate-50/70 transition-colors text-slate-800"
                  >
                    <td className="py-3 px-3.5 text-center text-slate-400 font-mono text-[11px]">
                      {page * pageSize + idx + 1}
                    </td>
                    <td className="py-3 px-3.5 font-mono font-semibold text-emerald-800">
                      {row.itemCode}
                    </td>
                    <td className="py-3 px-3.5 font-medium text-slate-900">
                      {row.itemName}
                    </td>
                    <td className="py-3 px-3.5 text-center">
                      <Badge variant="outline" className="text-[11px] font-mono border-slate-200 bg-slate-50 text-slate-700">
                        {row.unitName || row.unitCode}
                      </Badge>
                    </td>
                    <td className="py-3 px-3.5 text-right font-mono font-bold text-slate-900">
                      {formatQuantity(row.quantity)}
                    </td>
                    <td className="py-3 px-3.5 text-right font-mono text-slate-700">
                      {formatVnd(row.avgUnitCost)}
                    </td>
                    <td className="py-3 px-3.5 text-right font-mono font-bold text-emerald-800">
                      {formatVnd(row.inventoryValue)}
                    </td>
                    <td className="py-3 px-3.5 text-right text-slate-500 font-mono text-[11px]">
                      {row.updatedAt ? new Date(row.updatedAt).toLocaleDateString('vi-VN') : '—'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Footer & Pagination */}
        <div className="p-3.5 border-t border-slate-200 bg-slate-50/60 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500">
          <div className="flex items-center gap-1.5 font-medium">
            <TrendingUp className="h-3.5 w-3.5 text-emerald-700" />
            <span>
              Hiển thị {items.length} trên tổng số {totalItems} mặt hàng tồn kho
            </span>
          </div>

          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0 || isLoading}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="h-8 px-2.5 rounded-lg border-slate-200 text-slate-600 disabled:opacity-40"
            >
              <ChevronLeft className="h-4 w-4" />
              <span>Trang trước</span>
            </Button>
            <span className="font-mono text-xs px-2">
              Trang {page + 1} / {Math.max(1, Math.ceil(totalItems / pageSize))}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={(page + 1) * pageSize >= totalItems || isLoading}
              onClick={() => setPage((p) => p + 1)}
              className="h-8 px-2.5 rounded-lg border-slate-200 text-slate-600 disabled:opacity-40"
            >
              <span>Trang sau</span>
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
export default StockBalancesTable
