import React, { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Receipt,
  FileText,
  CreditCard,
  Wallet,
  CheckCircle2,
  AlertTriangle,
  ShieldAlert,
  Loader2,
  ExternalLink,
  Ban,
  Building2,
  ArrowDownRight,
  Clock,
  History,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { financeApi, formatVnd, formatSignedVnd } from '../api/financeApi'
import { evidenceApi } from '@/features/evidence/evidenceApi'
import {
  type ExpenseDto,
  type ExpenseDetailDto,
} from '../types/finance.types'

interface ExpenseDetailModalProps {
  isOpen: boolean
  branchId: string
  expense: ExpenseDto | null
  onClose: () => void
  onExpenseUpdated?: (updated: ExpenseDto) => void
}

export const ExpenseDetailModal: React.FC<ExpenseDetailModalProps> = ({
  isOpen,
  branchId,
  expense,
  onClose,
  onExpenseUpdated,
}) => {
  const [detail, setDetail] = useState<ExpenseDetailDto | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [isConfirming, setIsConfirming] = useState<boolean>(false)
  const [isVoiding, setIsVoiding] = useState<boolean>(false)
  const [isVoidDialogOpen, setIsVoidDialogOpen] = useState<boolean>(false)
  const [voidReason, setVoidReason] = useState<string>('')
  const [downloadingEvidence, setDownloadingEvidence] = useState<boolean>(false)
  const [errorNotice, setErrorNotice] = useState<string | null>(null)
  const [successNotice, setSuccessNotice] = useState<string | null>(null)

  const loadDetail = useCallback(async () => {
    if (!branchId || !expense?.id) return
    setIsLoading(true)
    setErrorNotice(null)
    try {
      const data = await financeApi.getExpenseDetail(branchId, expense.id)
      setDetail(data)
    } catch (err: unknown) {
      console.warn('Could not load detailed expense data, fallback to list item:', err)
      setDetail({
        ...expense,
        linkedCashLedgerEntry: null,
      })
    } finally {
      setIsLoading(false)
    }
  }, [branchId, expense])

  useEffect(() => {
    if (isOpen && expense) {
      loadDetail()
      setIsVoidDialogOpen(false)
      setVoidReason('')
      setSuccessNotice(null)
      setErrorNotice(null)
    } else {
      setDetail(null)
    }
  }, [isOpen, expense, loadDetail])

  const handleDownloadEvidence = async (fileId: string) => {
    try {
      setDownloadingEvidence(true)
      const tokenRes = await evidenceApi.getEvidenceDownloadToken(fileId)
      const url = evidenceApi.getEvidenceContentUrl(fileId, tokenRes.token)
      window.open(url, '_blank')
    } catch (err: unknown) {
      console.error('Failed to get download token for evidence:', err)
      alert('Không thể mở chứng từ. Vui lòng thử lại sau.')
    } finally {
      setDownloadingEvidence(false)
    }
  }

  const handleConfirm = async () => {
    if (!branchId || !expense?.id) return
    setErrorNotice(null)
    try {
      setIsConfirming(true)
      const updated = await financeApi.confirmExpense(branchId, expense.id)
      setSuccessNotice(`Đã duyệt chi thành công phiếu ${updated.expenseCode}`)
      setDetail((prev) => (prev ? { ...prev, ...updated } : (updated as ExpenseDetailDto)))
      if (onExpenseUpdated) {
        onExpenseUpdated(updated)
      }
    } catch (err: unknown) {
      console.error('Failed to confirm expense:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string } }
      }
      setErrorNotice(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể duyệt phiếu chi. Vui lòng thử lại.'
      )
    } finally {
      setIsConfirming(false)
    }
  }

  const handleVoid = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!branchId || !expense?.id) return
    if (!voidReason.trim()) {
      setErrorNotice('Vui lòng nhập lý do hủy phiếu chi.')
      return
    }

    setErrorNotice(null)
    try {
      setIsVoiding(true)
      const updated = await financeApi.voidExpense(branchId, expense.id, {
        reason: voidReason.trim(),
      })
      setSuccessNotice(`Đã hủy phiếu chi ${updated.expenseCode}`)
      setIsVoidDialogOpen(false)
      setDetail((prev) => (prev ? { ...prev, ...updated } : (updated as ExpenseDetailDto)))
      if (onExpenseUpdated) {
        onExpenseUpdated(updated)
      }
    } catch (err: unknown) {
      console.error('Failed to void expense:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string } }
      }
      setErrorNotice(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể hủy phiếu chi. Vui lòng thử lại.'
      )
    } finally {
      setIsVoiding(false)
    }
  }

  if (!isOpen || !expense) return null

  const displayData: ExpenseDetailDto = detail || (expense as ExpenseDetailDto)
  const amount =
    typeof displayData.amount === 'string'
      ? parseFloat(displayData.amount) || 0
      : displayData.amount

  const getStatusBadge = (st: string) => {
    switch (st) {
      case 'CONFIRMED':
        return (
          <Badge className="bg-emerald-100 text-emerald-800 border-emerald-300 font-semibold text-xs flex items-center gap-1">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
            <span>Đã duyệt chi</span>
          </Badge>
        )
      case 'DRAFT':
        return (
          <Badge className="bg-amber-100 text-amber-800 border-amber-300 font-semibold text-xs flex items-center gap-1">
            <Clock className="w-3.5 h-3.5 text-amber-600" />
            <span>Chờ duyệt (Nháp)</span>
          </Badge>
        )
      case 'VOIDED':
        return (
          <Badge variant="outline" className="bg-rose-50 text-rose-700 border-rose-300 font-semibold text-xs flex items-center gap-1">
            <Ban className="w-3.5 h-3.5 text-rose-600" />
            <span>Đã hủy phiếu</span>
          </Badge>
        )
      default:
        return (
          <Badge variant="secondary" className="text-xs">
            {st}
          </Badge>
        )
    }
  }

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-900/60 backdrop-blur-xs overflow-y-auto">
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 15 }}
          transition={{ duration: 0.2, ease: 'easeOut' }}
          className="relative w-full max-w-2xl bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden my-auto max-h-[90vh] flex flex-col"
        >
          {/* Header */}
          <div className="px-6 py-4 bg-slate-900 text-white flex items-center justify-between shrink-0 border-b border-slate-800">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-[#304638] text-white flex items-center justify-center shadow-xs">
                <Receipt className="w-5 h-5 text-emerald-400" />
              </div>
              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <h3 className="text-base font-bold text-white tracking-tight font-mono">
                    {displayData.expenseCode}
                  </h3>
                  {getStatusBadge(displayData.status)}
                  {isLoading && <Loader2 className="w-3.5 h-3.5 animate-spin text-slate-400" />}
                </div>
                <p className="text-xs text-slate-400 mt-0.5">
                  Ngày chi: {displayData.expenseDate} • Danh mục: {displayData.categoryName || displayData.categoryCode}
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="text-slate-400 hover:text-white p-1 rounded-lg transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Modal Body */}
          <div className="p-6 space-y-5 overflow-y-auto flex-1">
            {/* Notices */}
            {successNotice && (
              <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center gap-2.5 text-xs text-emerald-800 shadow-xs">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span className="font-medium">{successNotice}</span>
              </div>
            )}

            {errorNotice && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2.5 text-xs text-rose-800 shadow-xs">
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
                <span className="font-medium">{errorNotice}</span>
              </div>
            )}

            {/* Abnormal Large Expense Alert Banner (BR-FIN-07) */}
            {displayData.reviewFlag && (
              <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-300 flex items-start gap-2.5 text-xs text-amber-900 shadow-xs">
                <ShieldAlert className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                <div>
                  <p className="font-bold text-amber-950 flex items-center gap-1.5">
                    <span>CẢNH BÁO CHI PHÍ LỚN CẦN RÀ SOÁT (BR-FIN-07)</span>
                    <Badge className="bg-amber-600 text-white text-[10px]">
                      review_flag = true
                    </Badge>
                  </p>
                  <p className="text-[11px] text-amber-800 mt-0.5">
                    Phiếu chi này có giá trị ≥ 2.000.000 ₫. Cần có sự phê duyệt và rà soát định kỳ của Chủ quán/Kế toán trưởng.
                  </p>
                </div>
              </div>
            )}

            {/* Voided Details Banner */}
            {displayData.status === 'VOIDED' && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-900 space-y-1">
                <div className="flex items-center gap-1.5 font-bold text-rose-800">
                  <Ban className="w-4 h-4 text-rose-600" />
                  <span>Phiếu chi đã bị hủy</span>
                </div>
                {displayData.voidReason && (
                  <p className="text-[11px] text-rose-700">
                    <strong>Lý do hủy:</strong> {displayData.voidReason}
                  </p>
                )}
                {displayData.voidedByName && (
                  <p className="text-[11px] text-rose-600">
                    Người hủy: {displayData.voidedByName} • Lúc: {displayData.voidedAt ? new Date(displayData.voidedAt).toLocaleString('vi-VN') : '—'}
                  </p>
                )}
              </div>
            )}

            {/* Primary Amount Card */}
            <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              <div>
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Số tiền chi tiêu
                </span>
                <div className="text-2xl sm:text-3xl font-extrabold text-[#B96849] font-mono mt-0.5">
                  {formatVnd(amount)}
                </div>
              </div>

              {/* Payment Mode Indicator */}
              <div className="flex items-center gap-2">
                {displayData.isPaid ? (
                  <div className="px-3 py-1.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold flex items-center gap-1.5">
                    <Wallet className="w-4 h-4 text-emerald-600" />
                    <span>Đã thanh toán ngay</span>
                  </div>
                ) : (
                  <div className="px-3 py-1.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs font-semibold flex items-center gap-1.5">
                    <CreditCard className="w-4 h-4 text-amber-600" />
                    <span>Ghi nợ chi phí (TC-009)</span>
                  </div>
                )}
              </div>
            </div>

            {/* General Info Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div className="p-3.5 rounded-xl border border-slate-100 bg-white space-y-1">
                <span className="text-slate-400 font-medium">Danh mục chi phí:</span>
                <p className="font-semibold text-slate-800 text-sm">
                  {displayData.categoryName || 'Chi phí vận hành'}
                </p>
                <span className="font-mono text-[11px] text-slate-500">
                  Mã: {displayData.categoryCode || 'OPEX'}
                </span>
              </div>

              <div className="p-3.5 rounded-xl border border-slate-100 bg-white space-y-1">
                <span className="text-slate-400 font-medium">Tài khoản thanh toán:</span>
                <p className="font-semibold text-slate-800 text-sm flex items-center gap-1.5">
                  <Building2 className="w-4 h-4 text-slate-500" />
                  <span>{displayData.paymentAccountName || (displayData.isPaid ? 'Quỹ chi nhánh' : 'Chưa thanh toán (Ghi nợ)')}</span>
                </p>
                <span className="text-[11px] text-slate-500">
                  {displayData.isPaid ? 'Trừ tiền trực tiếp vào sổ quỹ' : 'Tạo nợ trong Sổ Công nợ NCC'}
                </span>
              </div>
            </div>

            {/* Diễn giải lý do chi */}
            <div className="p-3.5 rounded-xl border border-slate-100 bg-white space-y-1 text-xs">
              <span className="text-slate-400 font-medium">Diễn giải lý do chi tiêu:</span>
              <p className="text-slate-800 font-medium leading-relaxed whitespace-pre-wrap">
                {displayData.reason}
              </p>
            </div>

            {/* Bút toán sổ quỹ liên kết (Linked Cash Ledger Entry) */}
            {displayData.linkedCashLedgerEntry && (
              <div className="p-4 rounded-xl border border-slate-200 bg-slate-50/60 space-y-2.5 text-xs">
                <div className="flex items-center justify-between font-semibold text-slate-700">
                  <span className="flex items-center gap-1.5">
                    <ArrowDownRight className="w-4 h-4 text-rose-600" />
                    <span>Bút toán Sổ quỹ tiền mặt/ngân hàng đã ghi nhận</span>
                  </span>
                  <Badge variant="outline" className="text-[10px] font-mono border-slate-300">
                    ID: {displayData.linkedCashLedgerEntry.id.slice(0, 8)}...
                  </Badge>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 pt-1 font-mono text-[11px]">
                  <div className="p-2 rounded-lg bg-white border border-slate-200">
                    <span className="text-slate-400 block text-[10px]">Hướng dòng tiền:</span>
                    <strong className="text-rose-600">
                      {formatSignedVnd(displayData.linkedCashLedgerEntry.signedAmount)}
                    </strong>
                  </div>
                  <div className="p-2 rounded-lg bg-white border border-slate-200">
                    <span className="text-slate-400 block text-[10px]">Số dư tức thời:</span>
                    <strong className="text-slate-800">
                      {formatVnd(displayData.linkedCashLedgerEntry.runningBalance)}
                    </strong>
                  </div>
                  <div className="p-2 rounded-lg bg-white border border-slate-200 col-span-2 sm:col-span-1">
                    <span className="text-slate-400 block text-[10px]">Trạng thái sổ:</span>
                    <strong className={displayData.linkedCashLedgerEntry.status === 'POSTED' ? 'text-emerald-700' : 'text-rose-600'}>
                      {displayData.linkedCashLedgerEntry.status}
                    </strong>
                  </div>
                </div>
              </div>
            )}

            {/* Hóa đơn & Chứng từ đính kèm (Evidence File) */}
            <div className="p-3.5 rounded-xl border border-slate-200 bg-white space-y-2 text-xs">
              <span className="text-slate-500 font-semibold flex items-center gap-1.5">
                <FileText className="w-4 h-4 text-slate-400" />
                <span>Hóa đơn / Biên lai chứng từ</span>
              </span>

              {displayData.evidenceFileId ? (
                <div className="flex items-center justify-between p-2.5 rounded-lg bg-slate-50 border border-slate-200">
                  <div className="flex items-center gap-2">
                    <Receipt className="w-4 h-4 text-[#304638]" />
                    <span className="font-medium text-slate-700">Chứng từ hóa đơn đính kèm</span>
                    <Badge variant="outline" className="text-[10px] font-mono text-slate-500">
                      SHA-256 Verified
                    </Badge>
                  </div>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={() => handleDownloadEvidence(displayData.evidenceFileId!)}
                    disabled={downloadingEvidence}
                    className="h-8 text-xs text-[#304638] hover:text-[#25372C] flex items-center gap-1.5 cursor-pointer"
                  >
                    {downloadingEvidence ? (
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    ) : (
                      <ExternalLink className="w-3.5 h-3.5" />
                    )}
                    <span>Xem chứng từ</span>
                  </Button>
                </div>
              ) : (
                <p className="text-[11px] text-slate-400 italic">
                  Chưa có hóa đơn hoặc biên lai được đính kèm cho phiếu chi này.
                </p>
              )}
            </div>

            {/* Audit History Box */}
            <div className="p-3.5 rounded-xl border border-slate-100 bg-slate-50/50 space-y-2 text-xs text-slate-500">
              <span className="font-semibold text-slate-600 flex items-center gap-1.5">
                <History className="w-3.5 h-3.5 text-slate-400" />
                <span>Lịch sử kiểm toán & phê duyệt</span>
              </span>
              <div className="space-y-1 font-mono text-[11px]">
                <div>
                  Người lập: <strong className="text-slate-700">{displayData.createdByName || displayData.createdBy}</strong> • Lúc:{' '}
                  {displayData.createdAt ? new Date(displayData.createdAt).toLocaleString('vi-VN') : '—'}
                </div>
                {displayData.confirmedByName && (
                  <div>
                    Người duyệt: <strong className="text-emerald-800">{displayData.confirmedByName}</strong> • Lúc:{' '}
                    {displayData.confirmedAt ? new Date(displayData.confirmedAt).toLocaleString('vi-VN') : '—'}
                  </div>
                )}
              </div>
            </div>

            {/* Voiding Sub-Dialog if open */}
            {isVoidDialogOpen && (
              <form onSubmit={handleVoid} className="p-4 rounded-xl border border-rose-300 bg-rose-50/70 space-y-3 animate-in fade-in duration-200">
                <div className="flex items-center gap-1.5 text-xs font-bold text-rose-900">
                  <AlertTriangle className="w-4 h-4 text-rose-600" />
                  <span>Xác nhận hủy phiếu chi</span>
                </div>
                <p className="text-[11px] text-rose-800 leading-normal">
                  {displayData.isPaid && displayData.status === 'CONFIRMED'
                    ? 'Khi hủy phiếu chi đã thanh toán, hệ thống sẽ tự động tạo bút toán đảo (REVERSED) hoàn lại số tiền vào quỹ và chuyển trạng thái phiếu chi thành ĐÃ HỦY (BR-FIN-07).'
                    : 'Phiếu chi này sẽ chuyển sang trạng thái ĐÃ HỦY và không thể chỉnh sửa.'}
                </p>
                <div className="space-y-1">
                  <label className="text-[11px] font-semibold text-slate-700">
                    Lý do hủy phiếu <span className="text-rose-500">*</span>
                  </label>
                  <textarea
                    rows={2}
                    value={voidReason}
                    onChange={(e) => setVoidReason(e.target.value)}
                    placeholder="Ví dụ: Nhập trùng hóa đơn, sai danh mục, nhà cung cấp trả lại tiền..."
                    className="w-full rounded-lg border border-slate-300 bg-white p-2 text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-rose-500 resize-none"
                    required
                  />
                </div>
                <div className="flex items-center justify-end gap-2 pt-1">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setIsVoidDialogOpen(false)}
                    disabled={isVoiding}
                    className="text-xs h-8 cursor-pointer"
                  >
                    Hủy thao tác
                  </Button>
                  <Button
                    type="submit"
                    size="sm"
                    disabled={isVoiding || !voidReason.trim()}
                    className="bg-rose-600 hover:bg-rose-700 text-white text-xs h-8 cursor-pointer"
                  >
                    {isVoiding ? (
                      <>
                        <Loader2 className="w-3.5 h-3.5 animate-spin mr-1" />
                        <span>Đang xử lý...</span>
                      </>
                    ) : (
                      <span>Xác nhận hủy phiếu</span>
                    )}
                  </Button>
                </div>
              </form>
            )}
          </div>

          {/* Modal Footer */}
          <div className="px-6 py-4 bg-slate-50 border-t border-slate-200 flex flex-wrap items-center justify-between gap-3 shrink-0">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onClose}
              className="text-xs rounded-xl cursor-pointer"
            >
              Đóng
            </Button>

            <div className="flex items-center gap-2">
              {/* Action: Void Button (available for DRAFT and CONFIRMED) */}
              {displayData.status !== 'VOIDED' && !isVoidDialogOpen && (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setIsVoidDialogOpen(true)}
                  disabled={isConfirming || isVoiding}
                  className="border-rose-200 text-rose-700 hover:bg-rose-50 text-xs rounded-xl cursor-pointer"
                >
                  <Ban className="w-3.5 h-3.5 mr-1" />
                  <span>Hủy phiếu chi</span>
                </Button>
              )}

              {/* Action: Confirm Button (only for DRAFT) */}
              {displayData.status === 'DRAFT' && !isVoidDialogOpen && (
                <Button
                  type="button"
                  size="sm"
                  onClick={handleConfirm}
                  disabled={isConfirming || isVoiding}
                  className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold rounded-xl flex items-center gap-1.5 shadow-xs cursor-pointer"
                >
                  {isConfirming ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      <span>Đang duyệt...</span>
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                      <span>Duyệt phiếu chi</span>
                    </>
                  )}
                </Button>
              )}
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
export default ExpenseDetailModal
