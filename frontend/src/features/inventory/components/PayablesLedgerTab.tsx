import React, { useState, useEffect, useCallback } from 'react'
import {
  Search,
  RefreshCw,
  AlertCircle,
  CreditCard,
  CheckCircle2,
  Clock,
  ChevronLeft,
  ChevronRight,
  TrendingDown,
  Info,
  Loader2,
  Receipt,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { receiptApi } from '../api/receiptApi'
import type { PayableDto, PayableStatus, PayableFilterParams } from '../types/receipt.types'

interface PayablesLedgerTabProps {
  branchId: string
  branchName?: string
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

export const PayablesLedgerTab: React.FC<PayablesLedgerTabProps> = ({
  branchId,
  branchName,
}) => {
  const [payables, setPayables] = useState<PayableDto[]>([])
  const [totalItems, setTotalItems] = useState(0)
  const [totalOutstandingDebt, setTotalOutstandingDebt] = useState('0.00')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Filters & Pagination
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [page, setPage] = useState(0)
  const pageSize = 15

  const fetchPayables = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    setError(null)
    try {
      const params: PayableFilterParams = {
        status: statusFilter !== 'ALL' ? (statusFilter as PayableStatus) : undefined,
        search: searchTerm.trim() || undefined,
        page,
        size: pageSize,
      }
      const res = await receiptApi.getBranchPayables(branchId, params)
      setPayables(res.items || [])
      setTotalItems(res.totalItems || 0)
      setTotalOutstandingDebt(res.totalOutstandingDebt || '0.00')
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải sổ cái công nợ nhà cung cấp.'
      )
    } finally {
      setIsLoading(false)
    }
  }, [branchId, statusFilter, searchTerm, page])

  useEffect(() => {
    fetchPayables()
  }, [fetchPayables])

  const openCount = payables.filter((p) => p.status === 'OPEN').length
  const settledCount = payables.filter((p) => p.status === 'SETTLED').length
  const totalPages = Math.ceil(totalItems / pageSize)

  return (
    <div className="space-y-5">
      {/* KPI Cards Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Outstanding Debt KPI */}
        <div className="rounded-2xl border border-rose-200 bg-gradient-to-br from-rose-50/80 via-white to-white p-5 shadow-xs">
          <div className="flex items-center justify-between text-rose-800 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">
              Tổng nợ NCC đang chờ thanh toán
            </span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-rose-100 text-rose-700">
              <TrendingDown className="h-4 w-4" />
            </span>
          </div>
          <div className="text-2xl font-bold font-mono text-rose-950">
            {formatVnd(totalOutstandingDebt)}
          </div>
          <p className="text-[11px] text-slate-500 mt-1.5">
            Dư nợ phải trả theo thời gian thực tại chi nhánh {branchName || ''}.
          </p>
        </div>

        {/* Open Accounts KPI */}
        <div className="rounded-2xl border border-amber-200 bg-gradient-to-br from-amber-50/70 via-white to-white p-5 shadow-xs">
          <div className="flex items-center justify-between text-amber-800 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">
              Khoản nợ chưa tất toán (OPEN)
            </span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-amber-100 text-amber-700">
              <Clock className="h-4 w-4" />
            </span>
          </div>
          <div className="text-2xl font-bold font-mono text-amber-950">
            {openCount} / {totalItems}
          </div>
          <p className="text-[11px] text-slate-500 mt-1.5">
            Các khoản công nợ phát sinh từ phiếu nhập kho mua hàng.
          </p>
        </div>

        {/* Settled Accounts KPI */}
        <div className="rounded-2xl border border-emerald-200 bg-gradient-to-br from-emerald-50/70 via-white to-white p-5 shadow-xs">
          <div className="flex items-center justify-between text-emerald-800 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">
              Đã thanh toán (SETTLED)
            </span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-100 text-emerald-700">
              <CheckCircle2 className="h-4 w-4" />
            </span>
          </div>
          <div className="text-2xl font-bold font-mono text-emerald-950">
            {settledCount} khoản
          </div>
          <p className="text-[11px] text-slate-500 mt-1.5">
            Các khoản công nợ nhà cung cấp đã được thanh toán hoàn tất.
          </p>
        </div>
      </div>

      {/* TC-008 Policy Banner */}
      <div className="flex items-start gap-3 rounded-xl border border-emerald-200 bg-emerald-50/70 p-4 text-xs text-emerald-950">
        <Info className="h-4 w-4 shrink-0 text-emerald-700 mt-0.5" />
        <div className="space-y-1">
          <span className="font-bold text-emerald-950">
            Nguyên tắc bảo toàn dòng tiền & chi phí giá vốn:
          </span>
          <p className="text-emerald-900 leading-relaxed text-[11px]">
            Khi chi nhánh nhận hàng trực tiếp và chọn hình thức <strong>Chưa thanh toán (Ghi nợ NCC)</strong>, hệ thống tự động ghi nhận số nợ vào sổ cái này và tăng tài sản tồn kho.
            Quỹ tiền mặt/ngân hàng của quán <strong>KHÔNG BỊ TRỪ</strong> và chi phí giá vốn (COGS) trong báo cáo P&L <strong>KHÔNG BỊ TÍNH</strong> cho đến khi lập phiếu chi thanh toán nợ hoặc xuất dùng nguyên liệu.
          </p>
        </div>
      </div>

      {/* Control Bar: Filter, Search & Refresh */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2 flex-1">
          <div className="relative min-w-[240px] flex-1 max-w-sm">
            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-slate-400" />
            <Input
              type="text"
              placeholder="Tìm theo nhà cung cấp, mã phiếu..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value)
                setPage(0)
              }}
              className="pl-8 text-xs bg-white"
            />
          </div>

          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value)
              setPage(0)
            }}
            className="text-xs rounded-lg border border-slate-200 bg-white px-3 py-2 text-slate-700 focus:outline-none focus:ring-1 focus:ring-emerald-700"
          >
            <option value="ALL">Tất cả trạng thái nợ</option>
            <option value="OPEN">Còn nợ (OPEN)</option>
            <option value="SETTLED">Đã tất toán (SETTLED)</option>
          </select>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={fetchPayables}
            disabled={isLoading}
            className="text-xs h-9 text-slate-600 hover:text-slate-900 border-slate-200"
            title="Tải lại sổ nợ"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          </Button>
        </div>
      </div>

      {/* Error notice */}
      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-xs text-red-800">
          <AlertCircle className="h-4 w-4 shrink-0 text-red-600" />
          <span>{error}</span>
          <Button
            type="button"
            variant="link"
            size="sm"
            onClick={fetchPayables}
            className="text-xs text-red-900 underline ml-auto p-0 h-auto"
          >
            Thử lại
          </Button>
        </div>
      )}

      {/* Payables Ledger Table */}
      <div className="rounded-xl border border-slate-200 bg-white overflow-hidden shadow-2xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-700 font-semibold border-b border-slate-200">
              <tr>
                <th className="py-3 px-4 min-w-[200px]">Nhà cung cấp</th>
                <th className="py-3 px-4 min-w-[140px]">Nguồn phát sinh</th>
                <th className="py-3 px-4 min-w-[110px]">Ngày ghi nợ</th>
                <th className="py-3 px-4 min-w-[110px]">Hạn thanh toán</th>
                <th className="py-3 px-4 min-w-[130px] text-right">Nợ gốc</th>
                <th className="py-3 px-4 min-w-[130px] text-right">Đã thanh toán</th>
                <th className="py-3 px-4 min-w-[140px] text-right">Còn phải trả</th>
                <th className="py-3 px-4 min-w-[110px]">Trạng thái</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {isLoading && payables.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-400">
                    <Loader2 className="h-6 w-6 animate-spin mx-auto text-emerald-800 mb-2" />
                    <span>Đang tải sổ cái công nợ nhà cung cấp...</span>
                  </td>
                </tr>
              ) : payables.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-500">
                    <div className="flex flex-col items-center justify-center gap-2">
                      <CreditCard className="h-10 w-10 text-slate-300" />
                      <span className="font-medium text-slate-700">
                        Chi nhánh hiện không có khoản nợ NCC nào
                      </span>
                      <p className="text-xs text-slate-400 max-w-sm">
                        Khi nhập kho và chọn &ldquo;Chưa thanh toán (Ghi nợ)&rdquo;, các khoản nợ sẽ tự động xuất hiện tại đây.
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                payables.map((item) => {
                  const isOpen = item.status === 'OPEN'
                  const isSettled = item.status === 'SETTLED'

                  return (
                    <tr key={item.id} className="hover:bg-slate-50/80 transition-colors">
                      {/* Counterparty */}
                      <td className="py-3 px-4">
                        <span className="font-bold text-slate-900 block">
                          {item.counterpartyLabel}
                        </span>
                        <span className="text-[10px] text-slate-400 font-mono">
                          ID: {item.id.substring(0, 8)}...
                        </span>
                      </td>

                      {/* Source */}
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-1.5 text-slate-700">
                          <Receipt className="h-3.5 w-3.5 text-slate-400" />
                          <span className="font-medium">
                            {item.sourceType === 'PURCHASE_RECEIPT'
                              ? 'Phiếu nhập kho'
                              : item.sourceType}
                          </span>
                        </div>
                      </td>

                      {/* Incurred Date */}
                      <td className="py-3 px-4 font-mono text-slate-600">
                        {item.incurredDate}
                      </td>

                      {/* Due Date */}
                      <td className="py-3 px-4 font-mono text-slate-600">
                        {item.dueDate ? (
                          <span className="text-slate-800 font-medium">
                            {item.dueDate}
                          </span>
                        ) : (
                          <span className="text-slate-400">—</span>
                        )}
                      </td>

                      {/* Original Amount */}
                      <td className="py-3 px-4 text-right font-mono text-slate-700">
                        {formatVnd(item.originalAmount)}
                      </td>

                      {/* Paid Amount */}
                      <td className="py-3 px-4 text-right font-mono text-emerald-700">
                        {formatVnd(item.paidAmount)}
                      </td>

                      {/* Remaining Amount */}
                      <td className="py-3 px-4 text-right font-mono font-bold text-rose-900">
                        {formatVnd(item.remainingAmount)}
                      </td>

                      {/* Status */}
                      <td className="py-3 px-4">
                        {isOpen ? (
                          <Badge className="bg-amber-100 text-amber-900 border-amber-300 text-[11px] font-semibold gap-1">
                            <Clock className="h-3 w-3" />
                            <span>Còn nợ</span>
                          </Badge>
                        ) : isSettled ? (
                          <Badge className="bg-emerald-100 text-emerald-900 border-emerald-300 text-[11px] font-semibold gap-1">
                            <CheckCircle2 className="h-3 w-3" />
                            <span>Đã tất toán</span>
                          </Badge>
                        ) : (
                          <Badge variant="outline" className="text-[11px]">
                            {item.status}
                          </Badge>
                        )}
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Footer / Pagination */}
        {totalItems > 0 && (
          <div className="bg-slate-50 border-t border-slate-200 px-4 py-3 flex items-center justify-between text-xs text-slate-500">
            <div>
              Hiển thị <strong>{payables.length}</strong> trên tổng số <strong>{totalItems}</strong> khoản nợ
            </div>

            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={page === 0 || isLoading}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="h-7 px-2 text-xs"
              >
                <ChevronLeft className="h-3.5 w-3.5" />
                <span>Trước</span>
              </Button>

              <span className="font-mono text-slate-700">
                Trang {page + 1} / {totalPages || 1}
              </span>

              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={page + 1 >= totalPages || isLoading}
                onClick={() => setPage((p) => p + 1)}
                className="h-7 px-2 text-xs"
              >
                <span>Sau</span>
                <ChevronRight className="h-3.5 w-3.5" />
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
