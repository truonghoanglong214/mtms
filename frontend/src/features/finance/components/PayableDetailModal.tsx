import React, { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Building2,
  Calendar,
  CreditCard,
  FileText,
  Download,
  Loader2,
  Receipt,
  Clock,
  CheckCircle2,
  AlertCircle,
  DollarSign,
  ArrowRight,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { financeApi, formatVnd } from '../api/financeApi'
import { evidenceApi } from '@/features/evidence/evidenceApi'
import {
  PAYMENT_METHOD_LABELS,
  type PayableDto,
  type PayableDetailDto,
  type PayablePaymentDto,
} from '../types/finance.types'

interface PayableDetailModalProps {
  isOpen: boolean
  branchId: string
  payable: PayableDto | null
  onClose: () => void
  onOpenPayModal?: (payable: PayableDto) => void
}

export const PayableDetailModal: React.FC<PayableDetailModalProps> = ({
  isOpen,
  branchId,
  payable,
  onClose,
  onOpenPayModal,
}) => {
  const [detail, setDetail] = useState<PayableDetailDto | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [error, setError] = useState<string | null>(null)
  const [downloadingFileId, setDownloadingFileId] = useState<string | null>(null)

  const loadDetail = useCallback(async () => {
    if (!branchId || !payable?.id) return
    setIsLoading(true)
    setError(null)
    try {
      const data = await financeApi.getPayableDetail(branchId, payable.id)
      setDetail(data)
    } catch (err: unknown) {
      console.warn('Could not fetch detail from server, falling back to passed object:', err)
      // Graceful fallback to initial payable object if detail API endpoint is mocked or offline
      setDetail({
        ...payable,
        payments: [],
      })
    } finally {
      setIsLoading(false)
    }
  }, [branchId, payable])

  useEffect(() => {
    if (isOpen && payable) {
      loadDetail()
    } else {
      setDetail(null)
    }
  }, [isOpen, payable, loadDetail])

  const handleDownloadEvidence = async (fileId: string) => {
    try {
      setDownloadingFileId(fileId)
      const tokenRes = await evidenceApi.getEvidenceDownloadToken(fileId)
      const url = evidenceApi.getEvidenceContentUrl(fileId, tokenRes.token)
      window.open(url, '_blank')
    } catch (err: unknown) {
      console.error('Failed to download evidence:', err)
      alert('Không thể tải chứng từ. Vui lòng thử lại sau.')
    } finally {
      setDownloadingFileId(null)
    }
  }

  if (!isOpen || !payable) return null

  const displayData = detail || payable
  const origAmount = typeof displayData.originalAmount === 'string' ? parseFloat(displayData.originalAmount) || 0 : displayData.originalAmount
  const paidAmount = typeof displayData.paidAmount === 'string' ? parseFloat(displayData.paidAmount) || 0 : displayData.paidAmount
  const remainingDebt = displayData.remainingDebt !== undefined
    ? (typeof displayData.remainingDebt === 'string' ? parseFloat(displayData.remainingDebt) || 0 : displayData.remainingDebt)
    : (displayData.remainingAmount !== undefined
      ? (typeof displayData.remainingAmount === 'string' ? parseFloat(displayData.remainingAmount) || 0 : displayData.remainingAmount)
      : Math.max(0, origAmount - paidAmount))

  const progressPercent = origAmount > 0 ? Math.min(100, Math.round((paidAmount / origAmount) * 100)) : 0
  const isSettled = displayData.status === 'SETTLED' || remainingDebt <= 0

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SETTLED':
        return (
          <Badge className="bg-emerald-100 text-emerald-800 border-emerald-300 font-semibold text-xs flex items-center gap-1">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
            <span>Đã tất toán</span>
          </Badge>
        )
      case 'PARTIALLY_PAID':
        return (
          <Badge className="bg-amber-100 text-amber-900 border-amber-300 font-semibold text-xs flex items-center gap-1">
            <Clock className="w-3.5 h-3.5 text-amber-700" />
            <span>Đã trả một phần</span>
          </Badge>
        )
      case 'CANCELLED':
        return (
          <Badge variant="outline" className="bg-slate-100 text-slate-600 border-slate-300 font-semibold text-xs">
            Đã hủy
          </Badge>
        )
      case 'OPEN':
      default:
        return (
          <Badge className="bg-amber-50 text-amber-800 border-amber-200 font-semibold text-xs flex items-center gap-1">
            <Clock className="w-3.5 h-3.5 text-amber-600" />
            <span>Đang nợ</span>
          </Badge>
        )
    }
  }

  const paymentsList: PayablePaymentDto[] = detail?.payments || []

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 10 }}
          transition={{ duration: 0.2, ease: 'easeOut' }}
          className="relative w-full max-w-3xl bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[90vh]"
        >
          {/* Header */}
          <div className="px-6 py-4.5 bg-slate-900 text-white flex items-center justify-between border-b border-slate-800">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-slate-800 text-amber-400 border border-slate-700">
                <FileText className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-base font-bold tracking-tight text-white">
                    Chi Tiết Khoản Nợ Phải Trả
                  </h3>
                  {getStatusBadge(displayData.status)}
                </div>
                <p className="text-xs text-slate-400 mt-0.5">
                  Mã khoản nợ: <span className="font-mono text-slate-300">{displayData.id}</span>
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Modal Content */}
          <div className="p-6 overflow-y-auto space-y-6">
            {error && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-800 flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {/* Key Information Summary Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Partner Card */}
              <div className="p-4 rounded-xl border border-slate-200 bg-slate-50/60 space-y-2">
                <div className="text-xs font-semibold text-slate-500 flex items-center gap-1.5">
                  <Building2 className="w-3.5 h-3.5 text-slate-400" />
                  <span>Đối tác / Nhà cung cấp</span>
                </div>
                <div className="text-base font-bold text-slate-900">
                  {displayData.counterpartyLabel}
                </div>
                <div className="flex items-center gap-2 pt-1 text-xs text-slate-600">
                  <span className="text-slate-400">Nguồn gốc:</span>
                  <Badge variant="outline" className="text-[11px] font-mono bg-white text-slate-700">
                    {displayData.sourceType === 'PURCHASE_RECEIPT'
                      ? 'Phiếu nhập kho hàng'
                      : displayData.sourceType === 'MANUAL_PAYABLE'
                      ? 'Tạo nợ thủ công'
                      : displayData.sourceType}
                  </Badge>
                  {displayData.sourceId && (
                    <span className="font-mono text-[11px] text-slate-500">
                      #{displayData.sourceId.slice(0, 8)}
                    </span>
                  )}
                </div>
              </div>

              {/* Timing Card */}
              <div className="p-4 rounded-xl border border-slate-200 bg-slate-50/60 space-y-2">
                <div className="text-xs font-semibold text-slate-500 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-slate-400" />
                  <span>Thời hạn & Kỷ hạn thanh toán</span>
                </div>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div>
                    <span className="text-slate-400 block text-[11px]">Ngày ghi nhận:</span>
                    <span className="font-bold text-slate-800 font-mono">
                      {displayData.incurredDate || '—'}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[11px]">Hạn tất toán:</span>
                    <span className="font-bold text-slate-800 font-mono">
                      {displayData.dueDate || 'Không giới hạn'}
                    </span>
                  </div>
                </div>
                {displayData.notes && (
                  <div className="pt-1 text-xs text-slate-600 border-t border-slate-200">
                    <span className="text-slate-400">Ghi chú:</span> {displayData.notes}
                  </div>
                )}
              </div>
            </div>

            {/* Financial Amounts & Settlement Progress Card */}
            <div className="p-4 rounded-xl border border-slate-200 bg-white shadow-xs space-y-3">
              <div className="flex items-center justify-between text-xs font-semibold text-slate-700">
                <span className="flex items-center gap-1.5">
                  <DollarSign className="w-4 h-4 text-[#304638]" />
                  <span>Tiến độ thanh toán công nợ</span>
                </span>
                <span className="font-bold text-slate-900 font-mono">
                  {progressPercent}% hoàn thành
                </span>
              </div>

              {/* Progress bar */}
              <div className="w-full h-3 bg-slate-100 rounded-full overflow-hidden">
                <div
                  className={`h-full transition-all duration-500 ${
                    isSettled ? 'bg-emerald-600' : 'bg-amber-500'
                  }`}
                  style={{ width: `${progressPercent}%` }}
                />
              </div>

              <div className="grid grid-cols-3 gap-2 pt-1 text-center">
                <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-100">
                  <span className="text-[11px] text-slate-500 block">Nợ gốc ban đầu</span>
                  <span className="font-bold text-slate-800 text-sm font-mono mt-0.5 block">
                    {formatVnd(origAmount)}
                  </span>
                </div>
                <div className="p-2.5 rounded-lg bg-emerald-50/60 border border-emerald-100">
                  <span className="text-[11px] text-emerald-700 block">Đã thanh toán</span>
                  <span className="font-bold text-emerald-800 text-sm font-mono mt-0.5 block">
                    {formatVnd(paidAmount)}
                  </span>
                </div>
                <div className="p-2.5 rounded-lg bg-amber-50/60 border border-amber-100">
                  <span className="text-[11px] text-amber-700 block">Còn phải trả</span>
                  <span className="font-bold text-amber-800 text-sm font-mono mt-0.5 block">
                    {formatVnd(remainingDebt)}
                  </span>
                </div>
              </div>
            </div>

            {/* Payments History Table */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h4 className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                  <Receipt className="w-4 h-4 text-[#304638]" />
                  <span>Lịch sử các đợt thanh toán ({paymentsList.length})</span>
                </h4>
                {isLoading && (
                  <span className="text-xs text-slate-400 flex items-center gap-1">
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>Đang tải...</span>
                  </span>
                )}
              </div>

              {paymentsList.length === 0 ? (
                <div className="p-6 rounded-xl border border-dashed border-slate-200 text-center text-xs text-slate-500 space-y-1">
                  <Clock className="w-6 h-6 text-slate-400 mx-auto mb-1.5" />
                  <p className="font-medium text-slate-700">Chưa có đợt thanh toán nào được ghi nhận</p>
                  <p className="text-[11px]">Bấm "Thanh toán nợ ngay" bên dưới để trích tiền thanh toán đợt đầu tiên.</p>
                </div>
              ) : (
                <div className="rounded-xl border border-slate-200 overflow-hidden shadow-xs">
                  <table className="w-full text-xs">
                    <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-semibold">
                      <tr>
                        <th className="py-2.5 px-3 text-left">Ngày trả</th>
                        <th className="py-2.5 px-3 text-left">Tài khoản trích tiền</th>
                        <th className="py-2.5 px-3 text-left">Phương thức</th>
                        <th className="py-2.5 px-3 text-left">Mã GD / Tham chiếu</th>
                        <th className="py-2.5 px-3 text-right">Số tiền</th>
                        <th className="py-2.5 px-3 text-center">Chứng từ</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {paymentsList.map((p) => (
                        <tr key={p.id} className="hover:bg-slate-50/80 transition-colors">
                          <td className="py-2.5 px-3 font-mono font-medium text-slate-800">
                            {p.paidOn}
                          </td>
                          <td className="py-2.5 px-3 text-slate-700">
                            <span className="font-semibold block">{p.accountName || 'Tài khoản quỹ'}</span>
                            <span className="text-[10px] text-slate-400 font-mono">{p.accountKind}</span>
                          </td>
                          <td className="py-2.5 px-3">
                            <Badge variant="outline" className="text-[10px] bg-slate-50">
                              {PAYMENT_METHOD_LABELS[p.paymentMethod] || p.paymentMethod}
                            </Badge>
                          </td>
                          <td className="py-2.5 px-3 font-mono text-slate-600">
                            {p.bankReference || '—'}
                          </td>
                          <td className="py-2.5 px-3 text-right font-mono font-bold text-emerald-700">
                            {formatVnd(p.amount)}
                          </td>
                          <td className="py-2.5 px-3 text-center">
                            {p.evidenceFileId ? (
                              <button
                                type="button"
                                onClick={() => handleDownloadEvidence(p.evidenceFileId!)}
                                disabled={downloadingFileId === p.evidenceFileId}
                                className="inline-flex items-center gap-1 px-2 py-1 rounded-md text-[11px] font-medium bg-slate-100 hover:bg-slate-200 text-slate-700 cursor-pointer transition-colors"
                              >
                                {downloadingFileId === p.evidenceFileId ? (
                                  <Loader2 className="w-3 h-3 animate-spin" />
                                ) : (
                                  <Download className="w-3 h-3 text-[#304638]" />
                                )}
                                <span>Xem</span>
                              </button>
                            ) : (
                              <span className="text-slate-400 text-[11px]">—</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>

          {/* Footer */}
          <div className="px-6 py-4 bg-slate-50 border-t border-slate-200 flex items-center justify-between">
            <span className="text-xs text-slate-500">
              {isSettled ? (
                <span className="text-emerald-700 font-medium flex items-center gap-1">
                  <CheckCircle2 className="w-4 h-4" /> Khoản nợ đã được tất toán hoàn tất
                </span>
              ) : (
                <span>Còn phải trả: <strong className="text-amber-800 font-mono">{formatVnd(remainingDebt)}</strong></span>
              )}
            </span>

            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                className="text-xs rounded-xl"
              >
                Đóng
              </Button>
              {!isSettled && onOpenPayModal && (
                <Button
                  type="button"
                  onClick={() => {
                    onClose()
                    onOpenPayModal(displayData)
                  }}
                  className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-4 py-2 rounded-xl flex items-center gap-1.5 shadow-xs cursor-pointer"
                >
                  <CreditCard className="w-4 h-4" />
                  <span>Thanh toán nợ ngay</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </Button>
              )}
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
