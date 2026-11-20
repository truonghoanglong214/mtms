import React, { useState, useEffect, useCallback } from 'react'
import {
  Search,
  RefreshCw,
  AlertCircle,
  FileText,
  Calendar,
  ChevronLeft,
  ChevronRight,
  ArrowDownLeft,
  ArrowUpRight,
  Coins,
  Link as LinkIcon,
  Filter,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { reportingApi } from '../api/reportingApi'
import type {
  StockMovementItemDto,
  StockMovementType,
  StockMovementQueryParams,
} from '../types/reporting.types'

interface StockMovementsLedgerTabProps {
  branchId: string
  branchName?: string
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

function renderMovementBadge(type: StockMovementType) {
  switch (type) {
    case 'PURCHASE_RECEIPT':
      return (
        <Badge className="bg-emerald-100 text-emerald-800 border border-emerald-300 font-medium text-[11px] flex items-center gap-1 w-fit">
          <ArrowDownLeft className="h-3 w-3 text-emerald-700" />
          <span>Nhập mua NCC</span>
        </Badge>
      )
    case 'MANUAL_ISSUE':
      return (
        <Badge className="bg-rose-100 text-rose-800 border border-rose-300 font-medium text-[11px] flex items-center gap-1 w-fit">
          <ArrowUpRight className="h-3 w-3 text-rose-700" />
          <span>Xuất dùng quầy</span>
        </Badge>
      )
    case 'OPENING_BALANCE':
      return (
        <Badge className="bg-blue-100 text-blue-800 border border-blue-300 font-medium text-[11px] flex items-center gap-1 w-fit">
          <Coins className="h-3 w-3 text-blue-700" />
          <span>Tồn đầu kỳ</span>
        </Badge>
      )
    default:
      return (
        <Badge variant="outline" className="text-[11px] font-mono">
          {type}
        </Badge>
      )
  }
}

export const StockMovementsLedgerTab: React.FC<StockMovementsLedgerTabProps> = ({
  branchId,
  branchName,
}) => {
  const [items, setItems] = useState<StockMovementItemDto[]>([])
  const [totalItems, setTotalItems] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Filters
  const [searchTerm, setSearchTerm] = useState('')
  const [movementType, setMovementType] = useState<string>('ALL')
  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')
  const [page, setPage] = useState(0)
  const pageSize = 15

  const fetchMovements = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    setError(null)
    try {
      const params: StockMovementQueryParams = {
        movementType:
          movementType !== 'ALL' ? (movementType as StockMovementType) : undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        search: searchTerm.trim() || undefined,
        page,
        size: pageSize,
      }
      const res = await reportingApi.getStockMovements(branchId, params)
      setItems(res.items || [])
      setTotalItems(res.totalItems || 0)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải dữ liệu sổ chuyển động kho.'
      )
    } finally {
      setIsLoading(false)
    }
  }, [branchId, movementType, startDate, endDate, searchTerm, page])

  useEffect(() => {
    fetchMovements()
  }, [fetchMovements])

  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize))

  return (
    <div className="space-y-4">
      {/* Top Filter Bar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs space-y-3">
        <div className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-3">
          {/* Search Box */}
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <Input
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value)
                setPage(0)
              }}
              placeholder="Tìm theo mã NVL, tên hàng, hoặc số chứng từ (PXK, PNK)..."
              className="pl-9 h-9 text-xs rounded-xl bg-slate-50 border-slate-200 focus:bg-white"
            />
          </div>

          {/* Quick Filters */}
          <div className="flex flex-wrap items-center gap-2">
            <div className="flex items-center gap-1.5 text-xs text-slate-600">
              <Filter className="h-3.5 w-3.5 text-slate-400" />
              <span>Loại:</span>
              <select
                value={movementType}
                onChange={(e) => {
                  setMovementType(e.target.value)
                  setPage(0)
                }}
                className="h-8 px-2.5 text-xs rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none"
              >
                <option value="ALL">Tất cả loại giao dịch</option>
                <option value="PURCHASE_RECEIPT">Nhập mua NCC (Tăng tồn)</option>
                <option value="MANUAL_ISSUE">Xuất dùng pha chế (Giảm tồn)</option>
                <option value="OPENING_BALANCE">Số dư đầu kỳ (Khởi tạo)</option>
              </select>
            </div>

            <div className="flex items-center gap-1 text-xs text-slate-600">
              <Calendar className="h-3.5 w-3.5 text-slate-400" />
              <span>Từ:</span>
              <input
                type="date"
                value={startDate}
                onChange={(e) => {
                  setStartDate(e.target.value)
                  setPage(0)
                }}
                className="h-8 px-2 text-xs rounded-xl border border-slate-200 bg-slate-50 focus:bg-white"
              />
              <span>Đến:</span>
              <input
                type="date"
                value={endDate}
                onChange={(e) => {
                  setEndDate(e.target.value)
                  setPage(0)
                }}
                className="h-8 px-2 text-xs rounded-xl border border-slate-200 bg-slate-50 focus:bg-white"
              />
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={fetchMovements}
              disabled={isLoading}
              className="h-8 px-3 text-xs gap-1.5 rounded-xl border-slate-200 hover:bg-slate-50"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
              <span>Làm mới</span>
            </Button>
          </div>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="p-3.5 rounded-xl border border-rose-200 bg-rose-50 text-rose-800 flex items-center gap-2 text-xs">
          <AlertCircle className="h-4 w-4 shrink-0 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {/* Ledger Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-600 font-semibold">
                <th className="py-3 px-3.5 w-12 text-center">#</th>
                <th className="py-3 px-3.5">Ngày hạch toán</th>
                <th className="py-3 px-3.5">Loại biến động</th>
                <th className="py-3 px-3.5">Mã & Tên nguyên vật liệu</th>
                <th className="py-3 px-3.5 text-center">ĐVT</th>
                <th className="py-3 px-3.5 text-right">Số lượng (+/-)</th>
                <th className="py-3 px-3.5 text-right">Đơn giá ghi nhận (WAC)</th>
                <th className="py-3 px-3.5 text-right">Giá trị phát sinh (+/-)</th>
                <th className="py-3 px-3.5">Số chứng từ gốc</th>
                <th className="py-3 px-3.5">Người thực hiện</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                Array.from({ length: 5 }).map((_, idx) => (
                  <tr key={idx} className="animate-pulse">
                    <td className="py-3.5 px-3.5 text-center">
                      <div className="h-4 w-4 bg-slate-200 rounded mx-auto" />
                    </td>
                    <td className="py-3.5 px-3.5">
                      <div className="h-4 w-20 bg-slate-200 rounded" />
                    </td>
                    <td className="py-3.5 px-3.5">
                      <div className="h-5 w-24 bg-slate-200 rounded" />
                    </td>
                    <td className="py-3.5 px-3.5">
                      <div className="h-4 w-36 bg-slate-200 rounded" />
                    </td>
                    <td className="py-3.5 px-3.5 text-center">
                      <div className="h-4 w-10 bg-slate-200 rounded mx-auto" />
                    </td>
                    <td className="py-3.5 px-3.5 text-right">
                      <div className="h-4 w-16 bg-slate-200 rounded ml-auto" />
                    </td>
                    <td className="py-3.5 px-3.5 text-right">
                      <div className="h-4 w-20 bg-slate-200 rounded ml-auto" />
                    </td>
                    <td className="py-3.5 px-3.5 text-right">
                      <div className="h-4 w-24 bg-slate-200 rounded ml-auto" />
                    </td>
                    <td className="py-3.5 px-3.5">
                      <div className="h-4 w-28 bg-slate-200 rounded" />
                    </td>
                    <td className="py-3.5 px-3.5">
                      <div className="h-4 w-20 bg-slate-200 rounded" />
                    </td>
                  </tr>
                ))
              ) : items.length === 0 ? (
                <tr>
                  <td colSpan={10} className="py-12 text-center text-slate-500">
                    <div className="max-w-sm mx-auto space-y-2">
                      <div className="h-10 w-10 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
                        <FileText className="h-5 w-5" />
                      </div>
                      <p className="font-semibold text-slate-700">
                        {searchTerm || movementType !== 'ALL' || startDate || endDate
                          ? 'Không tìm thấy bản ghi biến động kho nào phù hợp'
                          : 'Chưa có dữ liệu chuyển động kho'}
                      </p>
                      <p className="text-[11px] text-slate-500">
                        {searchTerm || movementType !== 'ALL' || startDate || endDate
                          ? 'Vui lòng thử xóa bớt điều kiện lọc hoặc từ khóa tìm kiếm.'
                          : `Chi nhánh ${branchName || ''} chưa phát sinh giao dịch nhập kho, xuất dùng hoặc số dư đầu kỳ.`}
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                items.map((row, idx) => {
                  const qtyNum = parseFloat(row.signedQuantity) || 0
                  const isPositive = qtyNum > 0
                  const isNegative = qtyNum < 0

                  const valNum = parseFloat(row.signedValue) || 0

                  return (
                    <tr
                      key={row.id || idx}
                      className="hover:bg-slate-50/70 transition-colors text-slate-800"
                    >
                      <td className="py-3.5 px-3.5 text-center text-slate-400 font-mono text-[11px]">
                        {page * pageSize + idx + 1}
                      </td>
                      <td className="py-3.5 px-3.5 font-mono text-slate-700 whitespace-nowrap">
                        {row.businessDate}
                      </td>
                      <td className="py-3.5 px-3.5">
                        {renderMovementBadge(row.movementType)}
                      </td>
                      <td className="py-3.5 px-3.5">
                        <div className="font-semibold text-slate-900">{row.itemName}</div>
                        <div className="font-mono text-[11px] text-slate-500">
                          {row.itemCode}
                        </div>
                      </td>
                      <td className="py-3.5 px-3.5 text-center">
                        <Badge
                          variant="outline"
                          className="text-[11px] font-mono border-slate-200 bg-slate-50 text-slate-700"
                        >
                          {row.unitName || row.unitCode}
                        </Badge>
                      </td>
                      <td
                        className={`py-3.5 px-3.5 text-right font-mono font-bold whitespace-nowrap ${
                          isPositive
                            ? 'text-emerald-700'
                            : isNegative
                            ? 'text-rose-700'
                            : 'text-slate-700'
                        }`}
                      >
                        {isPositive ? `+${formatQuantity(qtyNum)}` : formatQuantity(qtyNum)}
                      </td>
                      <td className="py-3.5 px-3.5 text-right font-mono text-slate-700 whitespace-nowrap">
                        {formatVnd(row.bookedAvgCost)}
                      </td>
                      <td
                        className={`py-3.5 px-3.5 text-right font-mono font-bold whitespace-nowrap ${
                          valNum > 0
                            ? 'text-emerald-700'
                            : valNum < 0
                            ? 'text-rose-700'
                            : 'text-slate-700'
                        }`}
                      >
                        {valNum > 0 ? `+${formatVnd(valNum)}` : formatVnd(valNum)}
                      </td>
                      <td className="py-3.5 px-3.5">
                        <div className="flex items-center gap-1.5">
                          <LinkIcon className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                          <span className="font-mono font-medium text-slate-900">
                            {row.sourceRef || '—'}
                          </span>
                        </div>
                        <div className="text-[10px] text-slate-400 uppercase font-mono">
                          {row.sourceType}
                        </div>
                      </td>
                      <td className="py-3.5 px-3.5 text-slate-600 whitespace-nowrap">
                        <span className="font-medium text-slate-800">
                          {row.createdByName || 'Hệ thống'}
                        </span>
                        <div className="text-[10px] text-slate-400 font-mono">
                          {row.createdAt ? new Date(row.createdAt).toLocaleDateString('vi-VN') : ''}
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination & Status Footer */}
        <div className="p-3.5 border-t border-slate-200 bg-slate-50/60 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500">
          <div>
            Hiển thị <span className="font-semibold text-slate-700">{items.length}</span> trên
            tổng số <span className="font-semibold text-slate-700">{totalItems}</span> giao dịch biến động
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
              Trang {page + 1} / {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={page + 1 >= totalPages || isLoading}
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
export default StockMovementsLedgerTab
