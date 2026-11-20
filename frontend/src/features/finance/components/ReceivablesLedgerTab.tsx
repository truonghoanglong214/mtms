import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  Search,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  Clock,
  ChevronLeft,
  ChevronRight,
  Receipt,
  Plus,
  Eye,
  Building2,
  DollarSign,
  ShieldCheck,
  Check,
  X,
  Loader2,
  FileCheck,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { EvidenceDropzone } from '@/features/evidence/components/EvidenceDropzone'
import {
  financeApi,
  formatVnd,
  parseMoneyInput,
  getReceivableItems,
} from '../api/financeApi'
import type {
  ReceivableDto,
  ReceivableDetailDto,
  ReceivableReceiptDto,
  CreateReceivableRequest,
  ReceivableReceiptRequest,
  FinancialAccountDto,
  PaymentMethod,
} from '../types/finance.types'
import type { EvidenceFileDto } from '@/features/evidence/types'

interface ReceivablesLedgerTabProps {
  branchId: string
  branchName?: string
  accounts: FinancialAccountDto[]
  onRefreshAccounts?: () => void
}

export const ReceivablesLedgerTab: React.FC<ReceivablesLedgerTabProps> = ({
  branchId,
  branchName,
  accounts,
  onRefreshAccounts,
}) => {
  const [receivables, setReceivables] = useState<ReceivableDto[]>([])
  const [totalItems, setTotalItems] = useState<number>(0)
  const [totalPages, setTotalPages] = useState<number>(1)
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [pageSize, setPageSize] = useState<number>(15)

  // KPI figures
  const [totalOriginalReceivable, setTotalOriginalReceivable] = useState<number>(0)
  const [totalReceivedAmount, setTotalReceivedAmount] = useState<number>(0)
  const [totalOutstandingReceivable, setTotalOutstandingReceivable] = useState<number>(0)

  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [errorNotice, setErrorNotice] = useState<string | null>(null)
  const [successToast, setSuccessToast] = useState<string | null>(null)

  // Filters
  const [searchTerm, setSearchTerm] = useState<string>('')
  const [statusFilter, setStatusFilter] = useState<string>('ALL')

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false)
  const [selectedReceivableForReceipt, setSelectedReceivableForReceipt] = useState<ReceivableDto | null>(null)
  const [selectedReceivableForDetail, setSelectedReceivableForDetail] = useState<ReceivableDto | null>(null)

  const fetchReceivables = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    setErrorNotice(null)
    try {
      const res = await financeApi.getReceivables(branchId, {
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        search: searchTerm.trim() || undefined,
        page: currentPage,
        size: pageSize,
      })

      const items = getReceivableItems(res)
      setReceivables(items)
      const count = res.totalElements ?? res.totalItems ?? items.length
      setTotalItems(count)
      setTotalPages(res.totalPages || Math.ceil(count / pageSize) || 1)

      // Calculate or read KPIs
      if (res.totalOriginalReceivable !== undefined) {
        setTotalOriginalReceivable(
          typeof res.totalOriginalReceivable === 'string'
            ? parseFloat(res.totalOriginalReceivable) || 0
            : res.totalOriginalReceivable
        )
      } else {
        const sumOrig = items.reduce((acc, r) => {
          const val = typeof r.originalAmount === 'string' ? parseFloat(r.originalAmount) || 0 : r.originalAmount
          return acc + val
        }, 0)
        setTotalOriginalReceivable(sumOrig)
      }

      if (res.totalReceivedAmount !== undefined) {
        setTotalReceivedAmount(
          typeof res.totalReceivedAmount === 'string'
            ? parseFloat(res.totalReceivedAmount) || 0
            : res.totalReceivedAmount
        )
      } else {
        const sumReceived = items.reduce((acc, r) => {
          const val = typeof r.receivedAmount === 'string' ? parseFloat(r.receivedAmount) || 0 : r.receivedAmount
          return acc + val
        }, 0)
        setTotalReceivedAmount(sumReceived)
      }

      if (res.totalOutstandingReceivable !== undefined) {
        setTotalOutstandingReceivable(
          typeof res.totalOutstandingReceivable === 'string'
            ? parseFloat(res.totalOutstandingReceivable) || 0
            : res.totalOutstandingReceivable
        )
      } else {
        const sumOut = items.reduce((acc, r) => {
          const orig = typeof r.originalAmount === 'string' ? parseFloat(r.originalAmount) || 0 : r.originalAmount
          const rec = typeof r.receivedAmount === 'string' ? parseFloat(r.receivedAmount) || 0 : r.receivedAmount
          return acc + Math.max(0, orig - rec)
        }, 0)
        setTotalOutstandingReceivable(sumOut)
      }
    } catch (err: unknown) {
      console.error('Failed to load receivables:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string } }
      }
      setErrorNotice(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải danh sách khoản phải thu khác. Vui lòng thử lại.'
      )
    } finally {
      setIsLoading(false)
    }
  }, [branchId, statusFilter, searchTerm, currentPage, pageSize])

  useEffect(() => {
    fetchReceivables()
  }, [fetchReceivables])

  const handleCreateSuccess = (newRec: ReceivableDto) => {
    setSuccessToast(`Đã tạo khoản phải thu thành công: ${newRec.counterpartyLabel}`)
    setTimeout(() => setSuccessToast(null), 5000)
    fetchReceivables()
  }

  const handleReceiptSuccess = (receipt: ReceivableReceiptDto) => {
    setSuccessToast(
      `Đã thu hồi thành công ${formatVnd(receipt.amount)} vào quỹ. Tăng số dư tài khoản và giảm nợ phải thu.`
    )
    setTimeout(() => setSuccessToast(null), 6000)
    fetchReceivables()
    if (onRefreshAccounts) {
      onRefreshAccounts()
    }
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
        {/* KPI 1: Tổng nợ phải thu */}
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">
              Tổng phát sinh phải thu
            </span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-slate-100 text-slate-600">
              <Receipt className="h-4 w-4" />
            </span>
          </div>
          <div className="text-2xl font-bold font-mono text-slate-900">
            {formatVnd(totalOriginalReceivable)}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">
            Các khoản tạm ứng, bồi thường, hoàn cọc đối tác, thanh lý tài sản...
          </div>
        </div>

        {/* KPI 2: Đã thu hồi vào quỹ */}
        <div className="rounded-2xl border border-emerald-200 bg-gradient-to-br from-emerald-50/60 via-white to-white p-5 shadow-xs">
          <div className="flex items-center justify-between text-emerald-800 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">
              Đã thu hồi vào quỹ
            </span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-emerald-100 text-emerald-700">
              <Check className="h-4 w-4" />
            </span>
          </div>
          <div className="text-2xl font-bold font-mono text-emerald-700">
            {formatVnd(totalReceivedAmount)}
          </div>
          <div className="text-[11px] text-emerald-600 mt-1">
            Tiền thực tế đã nộp vào két chi nhánh hoặc tài khoản ngân hàng
          </div>
        </div>

        {/* KPI 3: Còn phải thu */}
        <div className="rounded-2xl border border-sky-200 bg-gradient-to-br from-sky-50/70 via-white to-white p-5 shadow-xs">
          <div className="flex items-center justify-between text-sky-800 mb-2">
            <span className="text-xs font-semibold uppercase tracking-wider">
              Còn phải thu hồi
            </span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-sky-100 text-sky-700">
              <Clock className="h-4 w-4" />
            </span>
          </div>
          <div className="text-2xl font-bold font-mono text-sky-800">
            {formatVnd(totalOutstandingReceivable)}
          </div>
          <div className="text-[11px] text-sky-700/80 mt-1">
            Số tiền đối tác/cá nhân còn thiếu cần theo dõi thu hồi
          </div>
        </div>
      </div>

      {/* 2. Policy Note */}
      <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 flex items-start gap-3">
        <ShieldCheck className="w-4 h-4 text-[#304638] shrink-0 mt-0.5" />
        <div className="text-xs text-slate-700 leading-relaxed">
          <strong>Sổ Quản Lý Phải Thu Khác:</strong> Ghi nhận các dòng phải thu ngoài doanh thu bán hàng hàng ngày (như hoàn tiền đặt cọc nhà mặt bằng, thu bồi thường hư hỏng tài sản, bán phế liệu vỏ chai hộp). Khi ghi nhận thu tiền, quỹ chi nhánh sẽ tự động tăng tương ứng.
        </div>
      </div>

      {/* 3. Action Toolbar & Filters */}
      <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs flex flex-col md:flex-row md:items-center md:justify-between gap-3">
        {/* Left: Filters */}
        <div className="flex flex-wrap items-center gap-2.5">
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
              <option value="OPEN">Chưa thu xong</option>
              <option value="SETTLED">Đã thu xong</option>
            </select>
          </div>

          <div className="relative min-w-[240px]">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
            <Input
              type="text"
              placeholder="Tìm đối tác nợ, diễn giải..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value)
                setCurrentPage(0)
              }}
              className="pl-8 text-xs h-8 rounded-lg"
            />
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => fetchReceivables()}
            disabled={isLoading}
            className="text-xs text-slate-600 flex items-center gap-1.5 cursor-pointer rounded-lg h-8"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin text-[#B96849]' : ''}`} />
            <span>Làm mới</span>
          </Button>
        </div>

        {/* Right: Button */}
        <div>
          <Button
            type="button"
            onClick={() => setIsCreateModalOpen(true)}
            className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-3.5 py-1.5 rounded-xl flex items-center gap-1.5 shadow-xs cursor-pointer h-9"
          >
            <Plus className="w-4 h-4" />
            <span>Tạo khoản phải thu</span>
          </Button>
        </div>
      </div>

      {/* 4. Table */}
      <div className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
              <tr>
                <th className="py-3 px-3.5 text-left">Ngày phát sinh</th>
                <th className="py-3 px-3.5 text-left">Hạn thu</th>
                <th className="py-3 px-3.5 text-left">Đối tác / Người nợ</th>
                <th className="py-3 px-3.5 text-right">Số tiền gốc</th>
                <th className="py-3 px-3.5 text-right">Đã thu</th>
                <th className="py-3 px-3.5 text-right">Còn phải thu</th>
                <th className="py-3 px-3.5 text-center min-w-[120px]">Tiến độ</th>
                <th className="py-3 px-3.5 text-center">Trạng thái</th>
                <th className="py-3 px-3.5 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-slate-400">
                    <Loader2 className="w-6 h-6 animate-spin mx-auto mb-2 text-[#304638]" />
                    <span>Đang tải danh sách khoản phải thu...</span>
                  </td>
                </tr>
              ) : receivables.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-slate-500">
                    <Receipt className="w-8 h-8 text-slate-300 mx-auto mb-2" />
                    <p className="font-semibold text-slate-700">Chưa có khoản phải thu nào</p>
                    <p className="text-[11px] text-slate-400 mt-0.5">
                      Bấm "Tạo khoản phải thu" để ghi nhận khoản cần thu hồi tiền vào quỹ.
                    </p>
                  </td>
                </tr>
              ) : (
                receivables.map((item) => {
                  const orig = typeof item.originalAmount === 'string' ? parseFloat(item.originalAmount) || 0 : item.originalAmount
                  const rec = typeof item.receivedAmount === 'string' ? parseFloat(item.receivedAmount) || 0 : item.receivedAmount
                  const remain = item.remainingAmount !== undefined
                    ? (typeof item.remainingAmount === 'string' ? parseFloat(item.remainingAmount) || 0 : item.remainingAmount)
                    : Math.max(0, orig - rec)
                  const pct = orig > 0 ? Math.min(100, Math.round((rec / orig) * 100)) : 0
                  const isSettled = item.status === 'SETTLED' || remain <= 0

                  return (
                    <tr key={item.id} className="hover:bg-slate-50/70 transition-colors">
                      <td className="py-3 px-3.5 font-mono text-slate-700 whitespace-nowrap">
                        {item.incurredDate}
                      </td>
                      <td className="py-3 px-3.5 font-mono text-slate-600 whitespace-nowrap">
                        {item.dueDate || <span className="text-slate-400 italic">Không có</span>}
                      </td>
                      <td className="py-3 px-3.5 text-slate-900 font-semibold">
                        <div className="flex items-center gap-1.5">
                          <Building2 className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                          <span className="truncate max-w-[200px]" title={item.counterpartyLabel}>
                            {item.counterpartyLabel}
                          </span>
                        </div>
                      </td>
                      <td className="py-3 px-3.5 text-right font-mono text-slate-700 font-medium whitespace-nowrap">
                        {formatVnd(orig)}
                      </td>
                      <td className="py-3 px-3.5 text-right font-mono text-emerald-700 font-medium whitespace-nowrap">
                        {formatVnd(rec)}
                      </td>
                      <td className="py-3 px-3.5 text-right font-mono font-bold whitespace-nowrap">
                        <span className={remain > 0 ? 'text-sky-800' : 'text-slate-400'}>
                          {formatVnd(remain)}
                        </span>
                      </td>
                      <td className="py-3 px-3.5 text-center">
                        <div className="flex items-center justify-center gap-1.5">
                          <div className="w-16 h-2 bg-slate-100 rounded-full overflow-hidden">
                            <div
                              className={`h-full ${isSettled ? 'bg-emerald-500' : 'bg-sky-500'}`}
                              style={{ width: `${pct}%` }}
                            />
                          </div>
                          <span className="font-mono text-[10px] text-slate-500 w-8 text-right">
                            {pct}%
                          </span>
                        </div>
                      </td>
                      <td className="py-3 px-3.5 text-center whitespace-nowrap">
                        {isSettled ? (
                          <Badge className="bg-emerald-50 text-emerald-800 border-emerald-200 font-semibold text-[11px] gap-1">
                            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                            <span>Đã thu xong</span>
                          </Badge>
                        ) : (
                          <Badge className="bg-sky-50 text-sky-800 border-sky-200 font-semibold text-[11px] gap-1">
                            <Clock className="w-3 h-3 text-sky-600" />
                            <span>Chưa thu xong</span>
                          </Badge>
                        )}
                      </td>
                      <td className="py-3 px-3.5 text-right whitespace-nowrap">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            type="button"
                            onClick={() => setSelectedReceivableForDetail(item)}
                            className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors cursor-pointer"
                            title="Xem chi tiết"
                          >
                            <Eye className="w-4 h-4" />
                          </button>

                          {!isSettled && (
                            <Button
                              type="button"
                              size="sm"
                              onClick={() => setSelectedReceivableForReceipt(item)}
                              className="bg-[#304638] hover:bg-[#25372C] text-white text-[11px] font-medium px-2.5 py-1 rounded-lg flex items-center gap-1 cursor-pointer h-7"
                            >
                              <DollarSign className="w-3 h-3" />
                              <span>Thu tiền</span>
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

        {/* Pagination */}
        <div className="px-4 py-3 bg-slate-50 border-t border-slate-200 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-600">
          <div className="flex items-center gap-3">
            <span>
              Tổng số: <strong className="text-slate-900">{totalItems}</strong> khoản phải thu
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
      {/* 1. Create Modal */}
      <CreateReceivableModal
        isOpen={isCreateModalOpen}
        branchId={branchId}
        branchName={branchName}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={handleCreateSuccess}
      />

      {/* 2. Receive Money Modal */}
      <RecordReceiptModal
        isOpen={Boolean(selectedReceivableForReceipt)}
        branchId={branchId}
        receivable={selectedReceivableForReceipt}
        accounts={accounts}
        onClose={() => setSelectedReceivableForReceipt(null)}
        onSuccess={handleReceiptSuccess}
      />

      {/* 3. Detail Modal */}
      <ReceivableDetailModal
        isOpen={Boolean(selectedReceivableForDetail)}
        branchId={branchId}
        receivable={selectedReceivableForDetail}
        onClose={() => setSelectedReceivableForDetail(null)}
        onOpenReceiptModal={(r) => setSelectedReceivableForReceipt(r)}
      />
    </div>
  )
}

// ============================================================================
// Submodal: Create Receivable
// ============================================================================
interface CreateReceivableModalProps {
  isOpen: boolean
  branchId: string
  branchName?: string
  onClose: () => void
  onSuccess: (rec: ReceivableDto) => void
}

const CreateReceivableModal: React.FC<CreateReceivableModalProps> = ({
  isOpen,
  branchId,
  branchName,
  onClose,
  onSuccess,
}) => {
  const [counterpartyLabel, setCounterpartyLabel] = useState('')
  const [amountStr, setAmountStr] = useState('')
  const [incurredDate, setIncurredDate] = useState(new Date().toISOString().split('T')[0])
  const [dueDate, setDueDate] = useState('')
  const [notes, setNotes] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const parsedAmount = parseMoneyInput(amountStr)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!branchId) return
    setErrorMessage(null)

    if (!counterpartyLabel.trim()) {
      setErrorMessage('Vui lòng nhập tên đối tác / người nợ.')
      return
    }

    if (parsedAmount <= 0) {
      setErrorMessage('Số tiền phải thu phải lớn hơn 0 ₫.')
      return
    }

    try {
      setIsSubmitting(true)
      const req: CreateReceivableRequest = {
        counterpartyLabel: counterpartyLabel.trim(),
        originalAmount: parsedAmount,
        incurredDate,
        dueDate: dueDate.trim() || undefined,
        notes: notes.trim() || undefined,
      }
      const res = await financeApi.createReceivable(branchId, req)
      onSuccess(res)
      onClose()
      setCounterpartyLabel('')
      setAmountStr('')
      setDueDate('')
      setNotes('')
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setErrorMessage(
        apiErr.response?.data?.detail || apiErr.response?.data?.message || 'Không thể tạo khoản phải thu.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 10 }}
          className="relative w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col"
        >
          <div className="px-6 py-4.5 bg-slate-900 text-white flex items-center justify-between border-b border-slate-800">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-slate-800 text-sky-400 border border-slate-700">
                <Receipt className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white">Ghi Nhận Khoản Phải Thu Khác</h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  Khoản tiền bên ngoài còn nợ chi nhánh {branchName ? `(${branchName})` : ''}
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="p-6 space-y-4 text-xs">
            {errorMessage && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 flex items-start gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span>{errorMessage}</span>
              </div>
            )}

            <div className="space-y-1.5">
              <label className="font-semibold text-slate-700">Đối tác / Người nợ *</label>
              <Input
                type="text"
                placeholder="Ví dụ: Công ty đối tác vận chuyển, Chủ nhà hoàn cọc..."
                value={counterpartyLabel}
                onChange={(e) => setCounterpartyLabel(e.target.value)}
                className="text-xs rounded-xl h-10"
                required
              />
            </div>

            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="font-semibold text-slate-700">Số tiền phải thu *</label>
                <span className="text-[11px] font-mono font-bold text-slate-600">
                  {formatVnd(parsedAmount)}
                </span>
              </div>
              <div className="relative">
                <Input
                  type="text"
                  placeholder="Nhập số tiền..."
                  value={amountStr}
                  onChange={(e) => setAmountStr(e.target.value)}
                  className="text-sm font-mono font-bold pr-10 rounded-xl h-10"
                  required
                />
                <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400">
                  ₫
                </span>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="font-semibold text-slate-700">Ngày phát sinh *</label>
                <Input
                  type="date"
                  value={incurredDate}
                  onChange={(e) => setIncurredDate(e.target.value)}
                  className="text-xs font-mono rounded-xl h-9"
                  required
                />
              </div>

              <div className="space-y-1.5">
                <label className="font-semibold text-slate-700">Hạn thu dự kiến</label>
                <Input
                  type="date"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                  className="text-xs font-mono rounded-xl h-9"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="font-semibold text-slate-700">Diễn giải / Lý do phải thu</label>
              <textarea
                rows={3}
                placeholder="Ví dụ: Hoàn trả tiền cọc thuê mặt bằng theo biên bản thanh lý..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="w-full rounded-xl border border-slate-300 p-2.5 text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-[#304638]"
              />
            </div>

            <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-3">
              <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting} className="text-xs rounded-xl">
                Hủy bỏ
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting || parsedAmount <= 0}
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-4 py-2 rounded-xl flex items-center gap-1.5 shadow-xs cursor-pointer"
              >
                {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
                <span>Tạo khoản phải thu</span>
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}

// ============================================================================
// Submodal: Record Receipt (Thu tiền phải thu)
// ============================================================================
interface RecordReceiptModalProps {
  isOpen: boolean
  branchId: string
  receivable: ReceivableDto | null
  accounts: FinancialAccountDto[]
  onClose: () => void
  onSuccess: (receipt: ReceivableReceiptDto) => void
}

const RecordReceiptModal: React.FC<RecordReceiptModalProps> = ({
  isOpen,
  branchId,
  receivable,
  accounts,
  onClose,
  onSuccess,
}) => {
  const [selectedAccountId, setSelectedAccountId] = useState('')
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('BANK_TRANSFER')
  const [amountStr, setAmountStr] = useState('')
  const [receivedOn, setReceivedOn] = useState(new Date().toISOString().split('T')[0])
  const [bankReference, setBankReference] = useState('')
  const [uploadedEvidence, setUploadedEvidence] = useState<EvidenceFileDto | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const remainingReceivableNum = useMemo(() => {
    if (!receivable) return 0
    if (receivable.remainingAmount !== undefined) {
      return typeof receivable.remainingAmount === 'string'
        ? parseFloat(receivable.remainingAmount) || 0
        : receivable.remainingAmount
    }
    const orig = typeof receivable.originalAmount === 'string' ? parseFloat(receivable.originalAmount) || 0 : receivable.originalAmount
    const rec = typeof receivable.receivedAmount === 'string' ? parseFloat(receivable.receivedAmount) || 0 : receivable.receivedAmount
    return Math.max(0, orig - rec)
  }, [receivable])

  useEffect(() => {
    if (isOpen && receivable) {
      const defaultAcc = accounts.find((a) => a.accountKind === 'BANK') || accounts[0]
      setSelectedAccountId(defaultAcc?.id || '')
      setPaymentMethod(defaultAcc?.accountKind === 'CASH' ? 'CASH' : 'BANK_TRANSFER')
      setAmountStr(remainingReceivableNum.toLocaleString('vi-VN'))
      setReceivedOn(new Date().toISOString().split('T')[0])
      setBankReference('')
      setUploadedEvidence(null)
      setErrorMessage(null)
    }
  }, [isOpen, receivable, accounts, remainingReceivableNum])

  const parsedAmount = parseMoneyInput(amountStr)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!receivable || !branchId) return
    setErrorMessage(null)

    if (!selectedAccountId) {
      setErrorMessage('Vui lòng chọn tài khoản nhận tiền.')
      return
    }

    if (parsedAmount <= 0) {
      setErrorMessage('Số tiền thu phải lớn hơn 0 ₫.')
      return
    }

    if (parsedAmount > remainingReceivableNum) {
      setErrorMessage(
        `Số tiền thu (${formatVnd(parsedAmount)}) không được vượt quá số còn phải thu (${formatVnd(remainingReceivableNum)}).`
      )
      return
    }

    try {
      setIsSubmitting(true)
      const req: ReceivableReceiptRequest = {
        accountId: selectedAccountId,
        amount: parsedAmount,
        receivedOn,
        paymentMethod,
        bankReference: bankReference.trim() || undefined,
        evidenceFileId: uploadedEvidence?.id || undefined,
      }
      const res = await financeApi.recordReceivableReceipt(branchId, receivable.id, req)
      onSuccess(res)
      onClose()
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setErrorMessage(
        apiErr.response?.data?.detail || apiErr.response?.data?.message || 'Không thể ghi nhận thu hồi tiền.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!isOpen || !receivable) return null

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 10 }}
          className="relative w-full max-w-xl bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col"
        >
          <div className="px-6 py-4.5 bg-slate-900 text-white flex items-center justify-between border-b border-slate-800">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-slate-800 text-emerald-400 border border-slate-700">
                <DollarSign className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white">Thu Hồi Tiền Công Nợ Phải Thu</h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  Nhập số tiền thu được từ đối tác: {receivable.counterpartyLabel}
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="p-6 space-y-4 text-xs">
            {errorMessage && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 flex items-start gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span>{errorMessage}</span>
              </div>
            )}

            {/* Account selection */}
            <div className="space-y-1.5">
              <label className="font-semibold text-slate-700">Tài khoản quỹ nhận tiền *</label>
              <select
                value={selectedAccountId}
                onChange={(e) => setSelectedAccountId(e.target.value)}
                className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-[#304638]"
                required
              >
                <option value="">-- Chọn tài khoản nhận tiền --</option>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.accountName} ({a.accountKind === 'CASH' ? 'Tiền mặt' : 'Ngân hàng'}) — Số dư: {formatVnd(a.currentBalance)}
                  </option>
                ))}
              </select>
            </div>

            {/* Amount input */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="font-semibold text-slate-700">Số tiền thu nộp quỹ *</label>
                <span className="text-[11px] font-mono text-slate-500">
                  Còn phải thu: <strong className="text-sky-700">{formatVnd(remainingReceivableNum)}</strong>
                </span>
              </div>
              <div className="relative">
                <Input
                  type="text"
                  value={amountStr}
                  onChange={(e) => setAmountStr(e.target.value)}
                  className="text-base font-mono font-bold pr-10 rounded-xl h-10"
                  required
                />
                <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400">
                  ₫
                </span>
              </div>
            </div>

            {/* Date and method */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="font-semibold text-slate-700">Hình thức nhận *</label>
                <select
                  value={paymentMethod}
                  onChange={(e) => setPaymentMethod(e.target.value as PaymentMethod)}
                  className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800"
                >
                  <option value="BANK_TRANSFER">Chuyển khoản ngân hàng</option>
                  <option value="CASH">Tiền mặt tại két</option>
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="font-semibold text-slate-700">Ngày nhận tiền *</label>
                <Input
                  type="date"
                  value={receivedOn}
                  onChange={(e) => setReceivedOn(e.target.value)}
                  className="text-xs font-mono rounded-xl h-9"
                  required
                />
              </div>
            </div>

            {paymentMethod === 'BANK_TRANSFER' && (
              <div className="space-y-1.5">
                <label className="font-semibold text-slate-700">Mã giao dịch ngân hàng / Tham chiếu</label>
                <Input
                  type="text"
                  value={bankReference}
                  onChange={(e) => setBankReference(e.target.value)}
                  placeholder="Ví dụ: FT2611181234..."
                  className="text-xs font-mono rounded-xl h-9"
                />
              </div>
            )}

            {/* Evidence dropzone */}
            <div className="space-y-2 pt-2 border-t border-slate-100">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <FileCheck className="w-3.5 h-3.5 text-[#304638]" />
                  <span>Biên lai / Chứng từ nhận tiền (Tùy chọn)</span>
                </label>
                {uploadedEvidence && (
                  <Badge variant="outline" className="bg-emerald-50 text-emerald-700 text-xs">
                    {uploadedEvidence.originalFilename}
                  </Badge>
                )}
              </div>
              <EvidenceDropzone
                branchId={branchId}
                defaultVisibility="BRANCH_PRIVATE"
                disabled={isSubmitting}
                onUploadSuccess={(file) => setUploadedEvidence(file)}
              />
            </div>

            {/* Footer */}
            <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-3">
              <Button type="button" variant="outline" onClick={onClose} disabled={isSubmitting} className="text-xs rounded-xl">
                Hủy bỏ
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting || parsedAmount <= 0}
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-4 py-2 rounded-xl flex items-center gap-1.5 shadow-xs cursor-pointer"
              >
                {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                <span>Xác nhận thu tiền</span>
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}

// ============================================================================
// Submodal: Receivable Detail
// ============================================================================
interface ReceivableDetailModalProps {
  isOpen: boolean
  branchId: string
  receivable: ReceivableDto | null
  onClose: () => void
  onOpenReceiptModal?: (r: ReceivableDto) => void
}

const ReceivableDetailModal: React.FC<ReceivableDetailModalProps> = ({
  isOpen,
  branchId,
  receivable,
  onClose,
  onOpenReceiptModal,
}) => {
  const [detail, setDetail] = useState<ReceivableDetailDto | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const loadDetail = useCallback(async () => {
    if (!branchId || !receivable?.id) return
    setIsLoading(true)
    try {
      const data = await financeApi.getReceivableDetail(branchId, receivable.id)
      setDetail(data)
    } catch {
      setDetail({
        ...receivable,
        receipts: [],
      })
    } finally {
      setIsLoading(false)
    }
  }, [branchId, receivable])

  useEffect(() => {
    if (isOpen && receivable) {
      loadDetail()
    } else {
      setDetail(null)
    }
  }, [isOpen, receivable, loadDetail])

  if (!isOpen || !receivable) return null

  const data = detail || receivable
  const orig = typeof data.originalAmount === 'string' ? parseFloat(data.originalAmount) || 0 : data.originalAmount
  const rec = typeof data.receivedAmount === 'string' ? parseFloat(data.receivedAmount) || 0 : data.receivedAmount
  const remain = Math.max(0, orig - rec)
  const isSettled = data.status === 'SETTLED' || remain <= 0

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 10 }}
          className="relative w-full max-w-2xl bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[85vh]"
        >
          <div className="px-6 py-4.5 bg-slate-900 text-white flex items-center justify-between border-b border-slate-800">
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-bold text-white">Chi Tiết Khoản Phải Thu Khác</h3>
                {isLoading && <Loader2 className="w-3.5 h-3.5 animate-spin text-sky-400" />}
              </div>
              <p className="text-xs text-slate-400 mt-0.5">Đối tác: {data.counterpartyLabel}</p>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="p-6 overflow-y-auto space-y-5 text-xs">
            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-200">
                <span className="text-slate-500 block text-[11px]">Nợ gốc ban đầu</span>
                <span className="font-bold font-mono text-sm mt-0.5 block">{formatVnd(orig)}</span>
              </div>
              <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800">
                <span className="block text-[11px]">Đã thu hồi</span>
                <span className="font-bold font-mono text-sm mt-0.5 block">{formatVnd(rec)}</span>
              </div>
              <div className="p-3 rounded-xl bg-sky-50 border border-sky-200 text-sky-800">
                <span className="block text-[11px]">Còn phải thu</span>
                <span className="font-bold font-mono text-sm mt-0.5 block">{formatVnd(remain)}</span>
              </div>
            </div>

            <div className="space-y-2 p-3.5 rounded-xl border border-slate-200 bg-slate-50/50">
              <div className="flex justify-between">
                <span className="text-slate-500">Ngày phát sinh:</span>
                <span className="font-mono font-medium text-slate-800">{data.incurredDate}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Hạn thu:</span>
                <span className="font-mono font-medium text-slate-800">{data.dueDate || 'Không giới hạn'}</span>
              </div>
              {data.notes && (
                <div className="pt-2 border-t border-slate-200 text-slate-700">
                  <span className="text-slate-500 block text-[11px]">Diễn giải / Ghi chú:</span>
                  <p className="mt-0.5">{data.notes}</p>
                </div>
              )}
            </div>

            {/* Collection history */}
            <div className="space-y-2">
              <h4 className="font-bold text-slate-700 flex items-center gap-1.5">
                <Receipt className="w-4 h-4 text-[#304638]" />
                <span>Lịch sử các đợt thu hồi tiền</span>
              </h4>
              {(!detail?.receipts || detail.receipts.length === 0) ? (
                <p className="text-slate-400 italic py-2">Chưa có lượt thu tiền nào được ghi nhận.</p>
              ) : (
                <div className="rounded-xl border border-slate-200 overflow-hidden">
                  <table className="w-full text-xs">
                    <thead className="bg-slate-50 border-b border-slate-200 text-slate-600">
                      <tr>
                        <th className="py-2 px-3 text-left">Ngày thu</th>
                        <th className="py-2 px-3 text-left">Tài khoản nhận</th>
                        <th className="py-2 px-3 text-right">Số tiền</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {detail.receipts.map((rc) => (
                        <tr key={rc.id}>
                          <td className="py-2 px-3 font-mono">{rc.receivedOn}</td>
                          <td className="py-2 px-3">{rc.accountName || 'Tài khoản quỹ'}</td>
                          <td className="py-2 px-3 text-right font-mono font-bold text-emerald-700">
                            {formatVnd(rc.amount)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>

          <div className="px-6 py-4 bg-slate-50 border-t border-slate-200 flex items-center justify-end gap-3">
            <Button type="button" variant="outline" onClick={onClose} className="text-xs rounded-xl">
              Đóng
            </Button>
            {!isSettled && onOpenReceiptModal && (
              <Button
                type="button"
                onClick={() => {
                  onClose()
                  onOpenReceiptModal(data)
                }}
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-4 py-2 rounded-xl flex items-center gap-1.5 shadow-xs cursor-pointer"
              >
                <DollarSign className="w-4 h-4" />
                <span>Thu tiền ngay</span>
              </Button>
            )}
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
