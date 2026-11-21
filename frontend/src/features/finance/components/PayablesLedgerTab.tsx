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
  Loader2,
  Receipt,
  Plus,
  Eye,
  Building2,
  ShieldCheck,
  Check,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { financeApi, formatVnd, getPayableItems } from '../api/financeApi'
import { PayDebtModal } from './PayDebtModal'
import { PayableDetailModal } from './PayableDetailModal'
import { CreatePayableModal } from './CreatePayableModal'
import type {
  PayableDto,
  FinancialAccountDto,
  PayablePaymentDto,
} from '../types/finance.types'

interface PayablesLedgerTabProps {
  branchId: string
  branchName?: string
  accounts: FinancialAccountDto[]
  onRefreshAccounts?: () => void
}

export const PayablesLedgerTab: React.FC<PayablesLedgerTabProps> = ({
  branchId,
  branchName,
  accounts,
  onRefreshAccounts,
}) => {
  const [payables, setPayables] = useState<PayableDto[]>([])
  const [totalItems, setTotalItems] = useState<number>(0)
  const [totalPages, setTotalPages] = useState<number>(1)
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [pageSize, setPageSize] = useState<number>(15)

  // Aggregated KPI numbers from backend or computed
  const [totalOutstandingDebt, setTotalOutstandingDebt] = useState<number>(0)
  const [totalOriginalDebt, setTotalOriginalDebt] = useState<number>(0)
  const [totalPaidDebt, setTotalPaidDebt] = useState<number>(0)

  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [errorNotice, setErrorNotice] = useState<string | null>(null)
  const [successToast, setSuccessToast] = useState<string | null>(null)

  // Filters
  const [searchTerm, setSearchTerm] = useState<string>('')
  const [statusFilter, setStatusFilter] = useState<string>('ALL')

  // Modals state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false)
  const [selectedPayableForPay, setSelectedPayableForPay] = useState<PayableDto | null>(null)
  const [selectedPayableForDetail, setSelectedPayableForDetail] = useState<PayableDto | null>(null)

  const fetchPayables = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    setErrorNotice(null)
    try {
      const res = await financeApi.getPayables(branchId, {
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        search: searchTerm.trim() || undefined,
        page: currentPage,
        size: pageSize,
      })

      const items = getPayableItems(res)
      setPayables(items)
      const count = res.totalElements ?? res.totalItems ?? items.length
      setTotalItems(count)
      setTotalPages(res.totalPages || Math.ceil(count / pageSize) || 1)

      // Calculate or use KPI figures
      if (res.totalOutstandingDebt !== undefined) {
        setTotalOutstandingDebt(
          typeof res.totalOutstandingDebt === 'string'
            ? parseFloat(res.totalOutstandingDebt) || 0
            : res.totalOutstandingDebt
        )
      } else {
        const sumOut = items.reduce((acc, p) => {
          const debt = p.remainingDebt ?? p.remainingAmount
          if (debt !== undefined) {
            return acc + (typeof debt === 'string' ? parseFloat(debt) || 0 : debt)
          }
          const orig = typeof p.originalAmount === 'string' ? parseFloat(p.originalAmount) || 0 : p.originalAmount
          const paid = typeof p.paidAmount === 'string' ? parseFloat(p.paidAmount) || 0 : p.paidAmount
          return acc + Math.max(0, orig - paid)
        }, 0)
        setTotalOutstandingDebt(sumOut)
      }

      if (res.totalOriginalDebt !== undefined) {
        setTotalOriginalDebt(
          typeof res.totalOriginalDebt === 'string'
            ? parseFloat(res.totalOriginalDebt) || 0
            : res.totalOriginalDebt
        )
      } else {
        const sumOrig = items.reduce((acc, p) => {
          const orig = typeof p.originalAmount === 'string' ? parseFloat(p.originalAmount) || 0 : p.originalAmount
          return acc + orig
        }, 0)
        setTotalOriginalDebt(sumOrig)
      }

      if (res.totalPaidDebt !== undefined) {
        setTotalPaidDebt(
          typeof res.totalPaidDebt === 'string'
            ? parseFloat(res.totalPaidDebt) || 0
            : res.totalPaidDebt
        )
      } else {
        const sumPaid = items.reduce((acc, p) => {
          const paid = typeof p.paidAmount === 'string' ? parseFloat(p.paidAmount) || 0 : p.paidAmount
          return acc + paid
        }, 0)
        setTotalPaidDebt(sumPaid)
      }
    } catch (err: unknown) {
      console.error('Failed to load payables:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string } }
      }
      setErrorNotice(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải sổ công nợ nhà cung cấp. Vui lòng thử lại.'
      )
    } finally {
      setIsLoading(false)
    }
  }, [branchId, statusFilter, searchTerm, currentPage, pageSize])

  useEffect(() => {
    fetchPayables()
  }, [fetchPayables])

  const handlePaymentSuccess = (payment: PayablePaymentDto) => {
    setSuccessToast(
      `Đã ghi nhận thanh toán nợ thành công số tiền ${formatVnd(payment.amount)}. Giảm tiền quỹ và giảm nợ phải trả.`
    )
    setTimeout(() => setSuccessToast(null), 6000)
    fetchPayables()
    if (onRefreshAccounts) {
      onRefreshAccounts()
    }
  }

  const handleCreateSuccess = (newPayable: PayableDto) => {
    setSuccessToast(`Đã tạo khoản nợ phải trả thành công cho: ${newPayable.counterpartyLabel}`)
    setTimeout(() => setSuccessToast(null), 5000)
    fetchPayables()
  }

  return (
    <div className="space-y-5 animate-in fade-in duration-200">
      {/* Toast Notification */}
      {successToast && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs text-emerald-800 shadow-sm">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span className="font-medium">{successToast}</span>
          </div>
          <button
            type="button"
            onClick={() => setSuccessToast(null)}
            className="text-emerald-700 hover:text-emerald-900 cursor-pointer"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Error Notice */}
      {errorNotice && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-xs text-rose-800 shadow-sm">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
          <span>{errorNotice}</span>
        </div>
      )}

      {/* 1. Three KPI Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* KPI 1: Tổng nợ phát sinh */}
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">
              Tổng nợ phát sinh
            </span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-slate-100 text-slate-600">
              <Receipt className="h-4 w-4" />
            </span>
          </div>
          <div className="text-2xl font-bold font-mono text-slate-900">
            {formatVnd(totalOriginalDebt)}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">
            Bao gồm nợ nhập nguyên vật liệu & các khoản phát sinh thủ công
          </div>
        </div>

        {/* KPI 2: Nợ đã thanh toán */}
        <div className="rounded-2xl border border-emerald-200 bg-gradient-to-br from-emerald-50/60 via-white to-white p-5 shadow-xs">
          <div className="flex items-center justify-between text-emerald-800 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">
              Nợ đã thanh toán
            </span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-100 text-emerald-700">
              <Check className="h-4 w-4" />
            </span>
          </div>
          <div className="text-2xl font-bold font-mono text-emerald-700">
            {formatVnd(totalPaidDebt)}
          </div>
          <div className="text-[11px] text-emerald-600 mt-1">
            Đã trích quỹ chi trả tiền mặt hoặc chuyển khoản ngân hàng
          </div>
        </div>

        {/* KPI 3: Còn phải trả */}
        <div className="rounded-2xl border border-amber-200 bg-gradient-to-br from-amber-50/70 via-white to-white p-5 shadow-xs">
          <div className="flex items-center justify-between text-amber-800 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">
              Còn phải trả hiện tại
            </span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-amber-100 text-amber-700">
              <Clock className="h-4 w-4" />
            </span>
          </div>
          <div className="text-2xl font-bold font-mono text-amber-700">
            {formatVnd(totalOutstandingDebt)}
          </div>
          <div className="text-[11px] text-amber-700/80 mt-1">
            Nghĩa vụ nợ cần tất toán cho các nhà cung cấp
          </div>
        </div>
      </div>

      {/* 2. TC-009 Policy Hint Banner */}
      <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 flex items-start gap-3">
        <ShieldCheck className="w-4 h-4 text-[#304638] shrink-0 mt-0.5" />
        <div className="text-xs text-slate-700 leading-relaxed">
          <strong>Nguyên tắc kế toán (Không tính trùng chi phí):</strong> Nghiệp vụ thanh toán công nợ nhà cung cấp là sự hoán đổi tài sản và nghĩa vụ nợ (giảm tiền quỹ - giảm nợ phải trả). Giá vốn nguyên vật liệu được tính khi tiêu hao/pha chế, do đó bút toán trả nợ không ghi nhận thêm chi phí vào báo cáo P&L.
        </div>
      </div>

      {/* 3. Action Toolbar & Filters */}
      <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs flex flex-col md:flex-row md:items-center md:justify-between gap-3">
        {/* Left: Filters */}
        <div className="flex flex-wrap items-center gap-2.5">
          {/* Status selector */}
          <div className="flex items-center gap-1.5">
            <span className="text-xs font-medium text-slate-500">Trạng thái:</span>
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value)
                setCurrentPage(0)
              }}
              className="text-xs font-medium text-slate-700 bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 focus:outline-none focus:ring-1 focus:ring-[#304638] cursor-pointer"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="OPEN">Đang nợ (Chưa tất toán)</option>
              <option value="SETTLED">Đã tất toán xong</option>
            </select>
          </div>

          {/* Search box */}
          <div className="relative min-w-[240px]">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
            <Input
              type="text"
              placeholder="Tìm theo tên NCC, diễn giải..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value)
                setCurrentPage(0)
              }}
              className="pl-8 text-xs h-8 rounded-lg"
            />
          </div>

          {/* Reload button */}
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => fetchPayables()}
            disabled={isLoading}
            className="text-xs text-slate-600 flex items-center gap-1.5 cursor-pointer rounded-lg h-8"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin text-[#B96849]' : ''}`} />
            <span>Làm mới</span>
          </Button>
        </div>

        {/* Right: Manual Debt Button */}
        <div>
          <Button
            type="button"
            onClick={() => setIsCreateModalOpen(true)}
            className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-3.5 py-1.5 rounded-xl flex items-center gap-1.5 shadow-xs cursor-pointer h-9"
          >
            <Plus className="w-4 h-4" />
            <span>Thêm khoản nợ thủ công</span>
          </Button>
        </div>
      </div>

      {/* 4. Payables List Table */}
      <div className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
              <tr>
                <th className="py-3 px-3.5 text-left">Ngày phát sinh</th>
                <th className="py-3 px-3.5 text-left">Hạn thanh toán</th>
                <th className="py-3 px-3.5 text-left">Nhà cung cấp / Đối tác</th>
                <th className="py-3 px-3.5 text-left">Nguồn gốc</th>
                <th className="py-3 px-3.5 text-right">Nợ gốc</th>
                <th className="py-3 px-3.5 text-right">Đã trả</th>
                <th className="py-3 px-3.5 text-right">Còn nợ</th>
                <th className="py-3 px-3.5 text-center min-w-[120px]">Tiến độ</th>
                <th className="py-3 px-3.5 text-center">Trạng thái</th>
                <th className="py-3 px-3.5 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={10} className="py-12 text-center text-slate-400">
                    <Loader2 className="w-6 h-6 animate-spin mx-auto mb-2 text-[#304638]" />
                    <span>Đang tải danh sách công nợ...</span>
                  </td>
                </tr>
              ) : payables.length === 0 ? (
                <tr>
                  <td colSpan={10} className="py-12 text-center text-slate-500">
                    <Receipt className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                    <p className="font-semibold text-slate-700">Không tìm thấy khoản nợ nào</p>
                    <p className="text-[11px] text-slate-400 mt-0.5">
                      {statusFilter !== 'ALL' || searchTerm
                        ? 'Thử thay đổi bộ lọc tìm kiếm'
                        : 'Các khoản nợ từ phiếu nhập kho hoặc tạo thủ công sẽ xuất hiện ở đây'}
                    </p>
                  </td>
                </tr>
              ) : (
                payables.map((item) => {
                  const orig = typeof item.originalAmount === 'string' ? parseFloat(item.originalAmount) || 0 : item.originalAmount
                  const paid = typeof item.paidAmount === 'string' ? parseFloat(item.paidAmount) || 0 : item.paidAmount
                  const debt = item.remainingDebt !== undefined
                    ? (typeof item.remainingDebt === 'string' ? parseFloat(item.remainingDebt) || 0 : item.remainingDebt)
                    : (item.remainingAmount !== undefined
                      ? (typeof item.remainingAmount === 'string' ? parseFloat(item.remainingAmount) || 0 : item.remainingAmount)
                      : Math.max(0, orig - paid))
                  const pct = orig > 0 ? Math.min(100, Math.round((paid / orig) * 100)) : 0
                  const isSettled = item.status === 'SETTLED' || debt <= 0

                  return (
                    <tr
                      key={item.id}
                      className="hover:bg-slate-50/70 transition-colors group"
                    >
                      {/* Ngày ghi nợ */}
                      <td className="py-3 px-3.5 font-mono text-slate-700 whitespace-nowrap">
                        {item.incurredDate}
                      </td>

                      {/* Hạn trả */}
                      <td className="py-3 px-3.5 font-mono text-slate-600 whitespace-nowrap">
                        {item.dueDate ? (
                          <span>{item.dueDate}</span>
                        ) : (
                          <span className="text-slate-400 italic">Không có</span>
                        )}
                      </td>

                      {/* Nhà cung cấp */}
                      <td className="py-3 px-3.5 text-slate-900 font-semibold">
                        <div className="flex items-center gap-1.5">
                          <Building2 className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                          <span className="truncate max-w-[200px]" title={item.counterpartyLabel}>
                            {item.counterpartyLabel}
                          </span>
                        </div>
                      </td>

                      {/* Nguồn gốc */}
                      <td className="py-3 px-3.5 whitespace-nowrap">
                        <Badge
                          variant="outline"
                          className="text-[10px] font-mono bg-slate-50 text-slate-600 border-slate-200"
                        >
                          {item.sourceType === 'PURCHASE_RECEIPT'
                            ? 'Phiếu nhập kho'
                            : item.sourceType === 'MANUAL_PAYABLE'
                            ? 'Thủ công'
                            : item.sourceType}
                        </Badge>
                      </td>

                      {/* Nợ gốc */}
                      <td className="py-3 px-3.5 text-right font-mono text-slate-700 font-medium whitespace-nowrap">
                        {formatVnd(orig)}
                      </td>

                      {/* Đã trả */}
                      <td className="py-3 px-3.5 text-right font-mono text-emerald-700 font-medium whitespace-nowrap">
                        {formatVnd(paid)}
                      </td>

                      {/* Còn nợ */}
                      <td className="py-3 px-3.5 text-right font-mono font-bold whitespace-nowrap">
                        <span className={debt > 0 ? 'text-amber-800' : 'text-slate-400'}>
                          {formatVnd(debt)}
                        </span>
                      </td>

                      {/* Tiến độ */}
                      <td className="py-3 px-3.5 text-center">
                        <div className="flex items-center justify-center gap-1.5">
                          <div className="w-16 h-2 bg-slate-100 rounded-full overflow-hidden">
                            <div
                              className={`h-full ${isSettled ? 'bg-emerald-500' : 'bg-amber-500'}`}
                              style={{ width: `${pct}%` }}
                            />
                          </div>
                          <span className="font-mono text-[10px] text-slate-500 w-8 text-right">
                            {pct}%
                          </span>
                        </div>
                      </td>

                      {/* Trạng thái */}
                      <td className="py-3 px-3.5 text-center whitespace-nowrap">
                        {isSettled ? (
                          <Badge className="bg-emerald-50 text-emerald-800 border-emerald-200 font-semibold text-[11px] gap-1">
                            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                            <span>Đã tất toán</span>
                          </Badge>
                        ) : (
                          <Badge className="bg-amber-50 text-amber-800 border-amber-200 font-semibold text-[11px] gap-1">
                            <Clock className="w-3 h-3 text-amber-600" />
                            <span>Đang nợ</span>
                          </Badge>
                        )}
                      </td>

                      {/* Thao tác */}
                      <td className="py-3 px-3.5 text-right whitespace-nowrap">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => setSelectedPayableForDetail(item)}
                            className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors cursor-pointer"
                            title="Xem chi tiết & lịch sử thanh toán"
                          >
                            <Eye className="w-4 h-4" />
                          </button>

                          {!isSettled && (
                            <Button
                              type="button"
                              size="sm"
                              onClick={() => setSelectedPayableForPay(item)}
                              className="bg-[#304638] hover:bg-[#25372C] text-white text-[11px] font-medium px-2.5 py-1 rounded-lg flex items-center gap-1 cursor-pointer h-7"
                            >
                              <CreditCard className="w-3 h-3" />
                              <span>Trả nợ</span>
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Toolbar */}
        <div className="px-4 py-3 bg-slate-50 border-t border-slate-200 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-600">
          <div className="flex items-center gap-3">
            <span>
              Tổng số: <strong className="text-slate-900">{totalItems}</strong> khoản nợ
            </span>
            <div className="flex items-center gap-1.5">
              <span>Hiển thị:</span>
              <select
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value))
                  setCurrentPage(0)
                }}
                className="rounded border border-slate-300 bg-white px-2 py-0.5 text-xs text-slate-700 cursor-pointer"
              >
                <option value={10}>10</option>
                <option value={15}>15</option>
                <option value={30}>30</option>
                <option value={50}>50</option>
              </select>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
              disabled={currentPage === 0 || isLoading}
              className="h-8 px-2.5 rounded-lg text-xs"
            >
              <ChevronLeft className="w-3.5 h-3.5 mr-1" />
              <span>Trước</span>
            </Button>
            <span className="font-mono text-xs px-2">
              Trang {currentPage + 1} / {totalPages}
            </span>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage((prev) => Math.min(totalPages - 1, prev + 1))}
              disabled={currentPage >= totalPages - 1 || isLoading}
              className="h-8 px-2.5 rounded-lg text-xs"
            >
              <span>Sau</span>
              <ChevronRight className="w-3.5 h-3.5 ml-1" />
            </Button>
          </div>
        </div>
      </div>

      {/* Modals */}
      {/* 1. Modal Pay Debt */}
      <PayDebtModal
        isOpen={Boolean(selectedPayableForPay)}
        branchId={branchId}
        payable={selectedPayableForPay}
        accounts={accounts}
        onClose={() => setSelectedPayableForPay(null)}
        onSuccess={handlePaymentSuccess}
      />

      {/* 2. Modal View Detail */}
      <PayableDetailModal
        isOpen={Boolean(selectedPayableForDetail)}
        branchId={branchId}
        payable={selectedPayableForDetail}
        onClose={() => setSelectedPayableForDetail(null)}
        onOpenPayModal={(item) => setSelectedPayableForPay(item)}
      />

      {/* 3. Modal Create Manual Payable */}
      <CreatePayableModal
        isOpen={isCreateModalOpen}
        branchId={branchId}
        branchName={branchName}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={handleCreateSuccess}
      />
    </div>
  )
}
