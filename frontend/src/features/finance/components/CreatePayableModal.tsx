import React, { useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Plus,
  Building2,
  Calendar,
  DollarSign,
  FileText,
  AlertCircle,
  Loader2,
  FileSpreadsheet,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { financeApi, formatVnd, parseMoneyInput } from '../api/financeApi'
import type { PayableDto, CreatePayableRequest } from '../types/finance.types'

interface CreatePayableModalProps {
  isOpen: boolean
  branchId: string
  branchName?: string
  onClose: () => void
  onSuccess: (payable: PayableDto) => void
}

export const CreatePayableModal: React.FC<CreatePayableModalProps> = ({
  isOpen,
  branchId,
  branchName,
  onClose,
  onSuccess,
}) => {
  const [counterpartyLabel, setCounterpartyLabel] = useState<string>('')
  const [amountStr, setAmountStr] = useState<string>('')
  const [incurredDate, setIncurredDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  )
  const [dueDate, setDueDate] = useState<string>('')
  const [notes, setNotes] = useState<string>('')
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const parsedAmount = parseMoneyInput(amountStr)

  const handleSetQuickAmount = (val: number) => {
    setAmountStr(val.toLocaleString('vi-VN'))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!branchId) return

    setErrorMessage(null)

    if (!counterpartyLabel.trim()) {
      setErrorMessage('Vui lòng nhập tên nhà cung cấp hoặc đối tác.')
      return
    }

    if (parsedAmount <= 0) {
      setErrorMessage('Số tiền nợ gốc phải lớn hơn 0 ₫.')
      return
    }

    try {
      setIsSubmitting(true)
      const req: CreatePayableRequest = {
        counterpartyLabel: counterpartyLabel.trim(),
        originalAmount: parsedAmount,
        incurredDate,
        dueDate: dueDate.trim() || undefined,
        notes: notes.trim() || undefined,
      }

      const result = await financeApi.createPayable(branchId, req)
      onSuccess(result)
      onClose()
      // Reset form
      setCounterpartyLabel('')
      setAmountStr('')
      setDueDate('')
      setNotes('')
    } catch (err: unknown) {
      console.error('Failed to create manual payable:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string } }
      }
      setErrorMessage(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tạo khoản nợ phải trả. Vui lòng thử lại.'
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
          transition={{ duration: 0.2, ease: 'easeOut' }}
          className="relative w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col"
        >
          {/* Header */}
          <div className="px-6 py-4.5 bg-slate-900 text-white flex items-center justify-between border-b border-slate-800">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-slate-800 text-amber-400 border border-slate-700">
                <FileSpreadsheet className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold tracking-tight text-white flex items-center gap-2">
                  <span>Ghi Nhận Khoản Nợ Phải Trả Thủ Công</span>
                </h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  Thêm công nợ ngoài phiếu nhập kho {branchName ? `tại ${branchName}` : ''} (tiền thuê, sửa chữa, bảo dưỡng thiết bị...)
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="p-6 space-y-4 text-xs">
            {errorMessage && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 flex items-start gap-2 animate-in fade-in">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span className="font-medium">{errorMessage}</span>
              </div>
            )}

            {/* Counterparty Label */}
            <div className="space-y-1.5">
              <label className="font-semibold text-slate-700 flex items-center gap-1.5">
                <Building2 className="w-3.5 h-3.5 text-slate-500" />
                <span>Nhà cung cấp / Đối tác công nợ *</span>
              </label>
              <Input
                type="text"
                placeholder="Ví dụ: Công ty TNHH Cơ điện Lạnh ABC, Chủ nhà cho thuê..."
                value={counterpartyLabel}
                onChange={(e) => setCounterpartyLabel(e.target.value)}
                className="text-xs rounded-xl h-10"
                required
              />
            </div>

            {/* Amount */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="font-semibold text-slate-700 flex items-center gap-1.5">
                  <DollarSign className="w-3.5 h-3.5 text-[#304638]" />
                  <span>Số tiền nợ gốc ban đầu *</span>
                </label>
                <span className="text-[11px] font-mono font-bold text-slate-600">
                  {formatVnd(parsedAmount)}
                </span>
              </div>
              <div className="relative">
                <Input
                  type="text"
                  placeholder="Nhập số tiền (VD: 15.000.000)..."
                  value={amountStr}
                  onChange={(e) => setAmountStr(e.target.value)}
                  className="text-sm font-mono font-bold pr-10 rounded-xl h-10"
                  required
                />
                <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400">
                  ₫
                </span>
              </div>

              {/* Quick buttons */}
              <div className="flex items-center gap-1.5 pt-1">
                <span className="text-[11px] text-slate-400">Gợi ý:</span>
                {[5000000, 10000000, 20000000, 50000000].map((v) => (
                  <button
                    key={v}
                    type="button"
                    onClick={() => handleSetQuickAmount(v)}
                    className="px-2 py-0.5 rounded-md text-[11px] font-mono bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors cursor-pointer"
                  >
                    {formatVnd(v)}
                  </button>
                ))}
              </div>
            </div>

            {/* Dates row */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="font-semibold text-slate-700 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-slate-500" />
                  <span>Ngày phát sinh *</span>
                </label>
                <Input
                  type="date"
                  value={incurredDate}
                  onChange={(e) => setIncurredDate(e.target.value)}
                  className="text-xs font-mono rounded-xl h-9"
                  required
                />
              </div>

              <div className="space-y-1.5">
                <label className="font-semibold text-slate-700 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-slate-500" />
                  <span>Hạn thanh toán (Tùy chọn)</span>
                </label>
                <Input
                  type="date"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                  className="text-xs font-mono rounded-xl h-9"
                />
              </div>
            </div>

            {/* Notes */}
            <div className="space-y-1.5">
              <label className="font-semibold text-slate-700 flex items-center gap-1.5">
                <FileText className="w-3.5 h-3.5 text-slate-500" />
                <span>Diễn giải / Ghi chú</span>
              </label>
              <textarea
                rows={3}
                placeholder="Ví dụ: Tiền bảo trì máy pha cà phê định kỳ tháng 11..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="w-full rounded-xl border border-slate-300 p-2.5 text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-[#304638]"
              />
            </div>

            {/* Footer */}
            <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={isSubmitting}
                className="text-xs rounded-xl"
              >
                Hủy bỏ
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting || parsedAmount <= 0}
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-4 py-2 rounded-xl flex items-center gap-1.5 shadow-xs cursor-pointer"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang lưu...</span>
                  </>
                ) : (
                  <>
                    <Plus className="w-4 h-4" />
                    <span>Lưu khoản nợ</span>
                  </>
                )}
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
