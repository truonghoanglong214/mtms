import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  History,
  Building2,
  AlertCircle,
  Loader2,
  Banknote,
  CheckCircle2,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { paymentApi } from '../api/paymentApi'
import { formatVND } from '../api/payrollApi'
import type { PayrollLine } from '../types/payroll.types'
import type { PayrollPaymentDto } from '../types/payment.types'
import { PAYMENT_METHOD_LABELS } from '../types/payment.types'

interface PaymentHistoryModalProps {
  isOpen: boolean
  line: PayrollLine | null
  branchId: string
  onClose: () => void
}

export const PaymentHistoryModal: React.FC<PaymentHistoryModalProps> = ({
  isOpen,
  line,
  branchId,
  onClose,
}) => {
  const [payments, setPayments] = useState<PayrollPaymentDto[]>([])
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    if (!isOpen || !line || !branchId) return

    let isMounted = true
    setIsLoading(true)
    setErrorMessage(null)

    paymentApi
      .getLinePayments(branchId, line.id)
      .then((data) => {
        if (!isMounted) return
        setPayments(data || [])
      })
      .catch((err) => {
        console.error('Failed to load payment history:', err)
        if (isMounted) {
          setErrorMessage('Không thể tải lịch sử chi trả lương của nhân viên.')
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false)
      })

    return () => {
      isMounted = false
    }
  }, [isOpen, line, branchId])

  if (!isOpen || !line) return null

  const grossSalary = Number(line.grossSalary) || 0
  const paidAmount = Number(line.paidAmount) || 0
  const remainingDebt = Math.max(0, grossSalary - paidAmount)

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-xs">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="bg-white rounded-3xl border border-stone-200 shadow-2xl max-w-2xl w-full overflow-hidden flex flex-col max-h-[90vh]"
        >
          {/* Header */}
          <div className="px-6 py-4 border-b border-stone-200 bg-stone-50/80 flex items-center justify-between shrink-0">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-2xl bg-[#304638]/10 text-[#304638] flex items-center justify-center">
                <History className="h-5 w-5" />
              </div>
              <div>
                <h3 className="font-bold text-stone-900 text-base">
                  Lịch sử chi trả lương
                </h3>
                <p className="text-xs text-stone-500">
                  Nhân viên: <strong>{line.employeeName}</strong> ({line.staffCode}) · {line.positionName}
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={onClose}
              className="p-1.5 rounded-xl text-stone-400 hover:text-stone-700 hover:bg-stone-200 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Body */}
          <div className="p-6 space-y-4 overflow-y-auto flex-1">
            {/* Summary Bar */}
            <div className="grid grid-cols-3 gap-3 bg-stone-50 p-3.5 rounded-2xl border border-stone-200 text-center">
              <div>
                <span className="text-[10px] text-stone-500 uppercase font-medium block">
                  Lương thực lĩnh
                </span>
                <span className="text-sm font-bold font-mono text-stone-900">
                  {formatVND(grossSalary)}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-stone-500 uppercase font-medium block">
                  Đã thanh toán
                </span>
                <span className="text-sm font-bold font-mono text-emerald-700">
                  {formatVND(paidAmount)}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-stone-500 uppercase font-medium block">
                  Còn nợ lại
                </span>
                <span className="text-sm font-bold font-mono text-amber-800">
                  {formatVND(remainingDebt)}
                </span>
              </div>
            </div>

            {/* Error Message */}
            {errorMessage && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800">
                {errorMessage}
              </div>
            )}

            {/* Content List */}
            {isLoading ? (
              <div className="py-12 flex flex-col items-center justify-center space-y-2 text-stone-400">
                <Loader2 className="h-6 w-6 animate-spin text-[#304638]" />
                <span className="text-xs">Đang tải lịch sử chi trả...</span>
              </div>
            ) : payments.length > 0 ? (
              <div className="space-y-3">
                <div className="text-xs font-bold text-stone-700 flex items-center justify-between">
                  <span>Các đợt chi trả ({payments.length} đợt)</span>
                  <span className="text-[11px] text-stone-500 font-normal">
                    Liên kết tự động vào sổ quỹ Cash Ledger
                  </span>
                </div>

                <div className="space-y-2.5">
                  {payments.map((p, idx) => (
                    <div
                      key={p.id || idx}
                      className="p-4 rounded-2xl border border-stone-200 bg-white hover:border-stone-300 transition-colors shadow-2xs space-y-2.5"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-2">
                          <span className="w-6 h-6 rounded-full bg-stone-100 text-stone-600 flex items-center justify-center text-xs font-mono font-bold">
                            #{payments.length - idx}
                          </span>
                          <span className="text-xs font-bold text-stone-900">
                            {p.paidOn}
                          </span>
                          <Badge
                            className={
                              p.paymentMethod === 'BANK_TRANSFER'
                                ? 'bg-sky-50 text-sky-800 border-sky-200 text-[10px]'
                                : 'bg-amber-50 text-amber-800 border-amber-200 text-[10px]'
                            }
                          >
                            {p.paymentMethod === 'BANK_TRANSFER' ? (
                              <Building2 className="h-3 w-3 mr-1 inline" />
                            ) : (
                              <Banknote className="h-3 w-3 mr-1 inline" />
                            )}
                            {PAYMENT_METHOD_LABELS[p.paymentMethod]}
                          </Badge>
                        </div>

                        <span className="font-mono font-bold text-sm text-emerald-700">
                          {formatVND(p.amount)}
                        </span>
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px] text-stone-600 bg-stone-50/70 p-2.5 rounded-xl">
                        <div>
                          <span className="text-stone-400">Tài khoản nguồn: </span>
                          <span className="font-medium text-stone-800">
                            {p.accountName || p.accountId}
                          </span>
                        </div>
                        <div>
                          <span className="text-stone-400">Mã UNC / Tham chiếu: </span>
                          <span className="font-mono font-medium text-stone-800">
                            {p.externalReference || '—'}
                          </span>
                        </div>

                        {p.ledgerEntryId && (
                          <div className="col-span-2 flex items-center space-x-1 text-emerald-800 font-mono text-[10px]">
                            <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600 shrink-0" />
                            <span>Bút toán sổ quỹ: {p.ledgerEntryId}</span>
                          </div>
                        )}

                        {p.note && (
                          <div className="col-span-2 text-stone-600 italic">
                            "{p.note}"
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <div className="py-12 text-center text-stone-400 space-y-2">
                <AlertCircle className="h-8 w-8 mx-auto text-stone-300" />
                <p className="text-sm font-semibold text-stone-700">
                  Chưa có đợt thanh toán nào
                </p>
                <p className="text-xs text-stone-500">
                  Nhân viên này chưa được ghi nhận chi trả lương trong kỳ này.
                </p>
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="px-6 py-3 border-t border-stone-200 bg-stone-50 flex items-center justify-end">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              className="h-8 px-4 text-xs border-stone-300"
            >
              Đóng
            </Button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
