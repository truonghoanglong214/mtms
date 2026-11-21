import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Scale,
  AlertTriangle,
  CheckCircle2,
  Loader2,
  TrendingUp,
  TrendingDown,
  Building2,
  Calendar,
  ShieldAlert,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { financeApi, formatVnd, formatSignedVnd } from '../api/financeApi'
import type {
  CashReconciliationDto,
  AdjustReconciliationPayload,
} from '../types/finance.types'

interface AdjustVarianceModalProps {
  isOpen: boolean
  branchId: string
  reconciliation: CashReconciliationDto | null
  onClose: () => void
  onSuccess: (updated: CashReconciliationDto) => void
}

export const AdjustVarianceModal: React.FC<AdjustVarianceModalProps> = ({
  isOpen,
  branchId,
  reconciliation,
  onClose,
  onSuccess,
}) => {
  const [reason, setReason] = useState<string>('')
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    if (isOpen && reconciliation) {
      setReason('')
      setErrorMessage(null)
    }
  }, [isOpen, reconciliation])

  if (!reconciliation) return null

  const isSurplus = reconciliation.varianceAmount > 0
  const isShortage = reconciliation.varianceAmount < 0
  const absVariance = Math.abs(reconciliation.varianceAmount)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!branchId || !reconciliation) return
    setErrorMessage(null)

    try {
      setIsSubmitting(true)

      const payload: AdjustReconciliationPayload = {
        reason: reason.trim() || undefined,
      }

      const res = await financeApi.adjustReconciliation(branchId, reconciliation.id, payload)
      onSuccess(res)
      onClose()
    } catch (err: unknown) {
      console.error('Failed to adjust reconciliation:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string; title?: string } }
      }
      setErrorMessage(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể duyệt điều chỉnh sổ quỹ. Vui lòng kiểm tra quyền hạn Chủ cửa hàng (Owner).'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs overflow-y-auto">
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 10 }}
            transition={{ duration: 0.18, ease: 'easeOut' }}
            className="w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden my-6"
          >
            {/* Header */}
            <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-amber-50/60">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center font-bold">
                  <Scale className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    Duyệt Điều Chỉnh Sổ Quỹ
                  </h3>
                  <p className="text-xs text-slate-500">
                    Quyền hạn Owner / Manager • Đồng bộ số dư sổ sách về số kiểm đếm thực tế
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={onClose}
                className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100 transition-colors cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Reconciliation Snapshot Summary Card */}
            <div className="px-6 pt-5 pb-2">
              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-2 text-xs">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5 text-slate-600 font-medium">
                    <Building2 className="w-3.5 h-3.5 text-slate-400" />
                    <span className="font-semibold text-slate-900">{reconciliation.accountName}</span>
                    <Badge variant="outline" className="text-[10px] font-mono">
                      {reconciliation.accountKind === 'CASH' ? 'Tiền mặt' : 'Ngân hàng'}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-1 text-slate-500 font-mono text-[11px]">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    <span>{reconciliation.reconciliationDate}</span>
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-2 pt-2 border-t border-slate-200/80 text-center">
                  <div>
                    <span className="text-[10px] text-slate-500 block">Số dư hiện tại</span>
                    <span className="font-mono font-semibold text-slate-800">
                      {formatVnd(reconciliation.recordedBalance)}
                    </span>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-500 block">Số kiểm đếm</span>
                    <span className="font-mono font-semibold text-slate-800">
                      {formatVnd(reconciliation.observedBalance)}
                    </span>
                  </div>
                  <div>
                    <span className="text-[10px] text-slate-500 block">Chênh lệch</span>
                    <span
                      className={`font-mono font-bold ${
                        isShortage ? 'text-rose-600' : isSurplus ? 'text-emerald-700' : 'text-slate-600'
                      }`}
                    >
                      {formatSignedVnd(reconciliation.varianceAmount)}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Explanatory Posting Box */}
            <div className="px-6 py-2">
              <div
                className={`p-3.5 rounded-xl border text-xs space-y-2 ${
                  isSurplus
                    ? 'bg-emerald-50/70 border-emerald-200 text-emerald-900'
                    : 'bg-rose-50/70 border-rose-200 text-rose-900'
                }`}
              >
                <div className="flex items-center gap-2 font-bold">
                  {isSurplus ? (
                    <TrendingUp className="w-4 h-4 text-emerald-600 shrink-0" />
                  ) : (
                    <TrendingDown className="w-4 h-4 text-rose-600 shrink-0" />
                  )}
                  <span>
                    Bút toán điều chỉnh sẽ tự động hạch toán vào sổ quỹ:
                  </span>
                </div>
                <div className="bg-white/80 p-2.5 rounded-lg border border-slate-200/60 font-mono text-[11px] space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-500">Loại bút toán:</span>
                    <span className="font-semibold text-slate-800">
                      {isSurplus ? 'Dòng tiền vào (+ INFLOW)' : 'Dòng tiền ra (- OUTFLOW)'}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-slate-500">Mã danh mục:</span>
                    <Badge variant="outline" className="font-mono text-[10px]">
                      {isSurplus ? 'ADJ_SURPLUS' : 'ADJ_SHORTAGE'}
                    </Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-slate-500">Số tiền hạch toán:</span>
                    <span className="font-bold text-slate-900">{formatVnd(absVariance)}</span>
                  </div>
                  <div className="flex items-center justify-between pt-1 border-t border-slate-100">
                    <span className="text-slate-500">Số dư mới sau điều chỉnh:</span>
                    <span className="font-bold text-emerald-700">{formatVnd(reconciliation.observedBalance)}</span>
                  </div>
                </div>

                <div className="flex items-start gap-1.5 text-[11px] leading-relaxed pt-1">
                  <ShieldAlert className="w-3.5 h-3.5 shrink-0 mt-0.5" />
                  <span>
                    Bút toán này sẽ khóa dòng và cập nhật tức thì vào số dư khả dụng của tài khoản <strong>{reconciliation.accountName}</strong>. Thao tác có lưu dấu kiểm toán vết người duyệt.
                  </span>
                </div>
              </div>
            </div>

            {/* Form */}
            <form onSubmit={handleSubmit} className="px-6 pb-6 pt-2 space-y-4">
              {errorMessage && (
                <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-800 animate-in fade-in">
                  <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                  <span className="leading-relaxed">{errorMessage}</span>
                </div>
              )}

              {/* Reason / Approval Note */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700">
                  Lý do / Căn cứ phê duyệt điều chỉnh sổ
                </label>
                <textarea
                  rows={2}
                  placeholder="Ví dụ: Chấp thuận hạch toán bù thiếu quỹ do thối nhầm tiền lẻ hoặc chênh lệch làm tròn..."
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-amber-500 resize-none"
                />
              </div>

              {/* Action Buttons */}
              <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2.5">
                <Button
                  type="button"
                  variant="outline"
                  onClick={onClose}
                  disabled={isSubmitting}
                  className="rounded-xl text-xs h-9"
                >
                  Hủy bỏ
                </Button>
                <Button
                  type="submit"
                  disabled={isSubmitting}
                  className={`rounded-xl text-xs font-semibold px-4 h-9 shadow-xs flex items-center gap-2 cursor-pointer text-white ${
                    isSurplus ? 'bg-emerald-700 hover:bg-emerald-800' : 'bg-amber-600 hover:bg-amber-700'
                  }`}
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      <span>Đang hạch toán...</span>
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>Xác nhận điều chỉnh số dư</span>
                    </>
                  )}
                </Button>
              </div>
            </form>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}
