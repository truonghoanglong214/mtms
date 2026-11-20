import React, { useState, useEffect, useMemo } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  PlusCircle,
  AlertTriangle,
  Loader2,
  TrendingUp,
  TrendingDown,
  Info,
  DollarSign,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { Input } from '@/shared/components/ui/input'
import { payrollApi, formatVND } from '../api/payrollApi'
import {
  ADJUSTMENT_CODE_CONFIG,
  type AdjustmentCode,
  type PayrollLine,
} from '../types/payroll.types'

interface CreateAdjustmentModalProps {
  isOpen: boolean
  line: PayrollLine | null
  branchId: string
  onClose: () => void
  onSuccess: () => void
}

export const CreateAdjustmentModal: React.FC<CreateAdjustmentModalProps> = ({
  isOpen,
  line,
  branchId,
  onClose,
  onSuccess,
}) => {
  const [adjustmentCode, setAdjustmentCode] = useState<AdjustmentCode>('ALLOWANCE')
  const [rawAmount, setRawAmount] = useState<string>('')
  const [isOtherNegative, setIsOtherNegative] = useState<boolean>(false)
  const [explanation, setExplanation] = useState<string>('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    if (isOpen) {
      setAdjustmentCode('ALLOWANCE')
      setRawAmount('')
      setIsOtherNegative(false)
      setExplanation('')
      setErrorMessage(null)
    }
  }, [isOpen])

  // Calculate signed amount
  const numericAmount = useMemo(() => {
    const parsed = parseFloat(rawAmount.replace(/[^0-9]/g, ''))
    return isNaN(parsed) ? 0 : parsed
  }, [rawAmount])

  const signedAmount = useMemo(() => {
    if (numericAmount <= 0) return 0
    const cfg = ADJUSTMENT_CODE_CONFIG[adjustmentCode]
    if (cfg.defaultSign === 'POSITIVE') return numericAmount
    if (cfg.defaultSign === 'NEGATIVE') return -numericAmount
    return isOtherNegative ? -numericAmount : numericAmount
  }, [adjustmentCode, numericAmount, isOtherNegative])

  // Projected Gross Salary
  const projectedGross = useMemo(() => {
    if (!line) return 0
    return Number(line.grossSalary) + signedAmount
  }, [line, signedAmount])

  if (!isOpen || !line) return null

  const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value.replace(/[^0-9]/g, '')
    setRawAmount(val)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)

    if (numericAmount <= 0) {
      setErrorMessage('Vui lòng nhập số tiền điều chỉnh hợp lệ lớn hơn 0.')
      return
    }

    if (!explanation.trim()) {
      setErrorMessage('Vui lòng nhập ghi chú giải trình lý do điều chỉnh bắt buộc.')
      return
    }

    if (projectedGross < 0) {
      setErrorMessage(
        `Lỗi an toàn: Khoản khấu trừ (${formatVND(
          Math.abs(signedAmount)
        )}) vượt quá tổng lương hiện tại (${formatVND(
          line.grossSalary
        )}), khiến lương thực lĩnh bị âm.`
      )
      return
    }

    setIsSubmitting(true)
    try {
      await payrollApi.addAdjustment(branchId, line.id, {
        adjustmentCode,
        signedAmount,
        explanation: explanation.trim(),
      })
      onSuccess()
      onClose()
    } catch (err: unknown) {
      console.error('Failed to create adjustment:', err)
      const errResponse = (err as { response?: { data?: { detail?: string; message?: string } } })
        ?.response?.data
      setErrorMessage(
        errResponse?.detail ||
          errResponse?.message ||
          'Không thể thêm khoản điều chỉnh. Vui lòng thử lại hoặc kiểm tra quyền hạn.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  const selectedConfig = ADJUSTMENT_CODE_CONFIG[adjustmentCode]

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm overflow-y-auto">
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 10 }}
          transition={{ duration: 0.15 }}
          className="relative w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-stone-200 overflow-hidden my-8"
        >
          {/* Header */}
          <div className="px-6 py-4 border-b border-stone-200 bg-stone-50 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-2.5 rounded-xl bg-[#304638] text-white">
                <PlusCircle className="h-5 w-5" />
              </div>
              <div>
                <h3 className="text-base font-bold text-stone-900">
                  Thêm khoản điều chỉnh lương
                </h3>
                <p className="text-xs text-stone-500">
                  Phụ cấp trách nhiệm, thưởng chuyên cần, phạt nội quy hoặc tạm ứng
                </p>
              </div>
            </div>
            <button
              onClick={onClose}
              disabled={isSubmitting}
              className="p-1.5 rounded-lg text-stone-400 hover:text-stone-600 hover:bg-stone-200/60 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="p-6 space-y-5">
            {/* Employee Banner */}
            <div className="bg-stone-50 rounded-xl p-3 border border-stone-200 flex items-center justify-between">
              <div>
                <div className="flex items-center space-x-2">
                  <span className="font-bold text-stone-900 text-sm">
                    {line.employeeName}
                  </span>
                  <Badge variant="secondary" className="font-mono text-[10px]">
                    {line.staffCode}
                  </Badge>
                </div>
                <span className="text-xs text-stone-500">{line.positionName}</span>
              </div>
              <div className="text-right">
                <span className="text-[11px] text-stone-500 block">Lương hiện tại:</span>
                <span className="text-sm font-bold font-mono text-[#304638]">
                  {formatVND(line.grossSalary)}
                </span>
              </div>
            </div>

            {/* Error Message Alert */}
            {errorMessage && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl flex items-start space-x-2 text-rose-800 text-xs">
                <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
                <div className="flex-1 font-medium">{errorMessage}</div>
              </div>
            )}

            {/* 1. Chọn loại điều chỉnh */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-stone-700 block">
                Loại điều chỉnh <span className="text-rose-500">*</span>
              </label>
              <select
                value={adjustmentCode}
                onChange={(e) => setAdjustmentCode(e.target.value as AdjustmentCode)}
                className="w-full h-10 px-3 bg-white border border-stone-300 rounded-xl text-xs font-medium text-stone-900 focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent"
              >
                {(Object.keys(ADJUSTMENT_CODE_CONFIG) as AdjustmentCode[]).map((code) => {
                  const item = ADJUSTMENT_CODE_CONFIG[code]
                  const signText =
                    item.defaultSign === 'POSITIVE'
                      ? '(+ Thưởng/Phụ cấp)'
                      : item.defaultSign === 'NEGATIVE'
                      ? '(- Phạt/Tạm ứng)'
                      : '(Tùy chỉnh +/-)'
                  return (
                    <option key={code} value={code}>
                      {item.label} {signText}
                    </option>
                  )
                })}
              </select>
              <p className="text-[11px] text-stone-500">{selectedConfig.description}</p>
            </div>

            {/* If OTHER, allow toggle sign */}
            {adjustmentCode === 'OTHER' && (
              <div className="p-3 bg-stone-50 rounded-xl border border-stone-200 flex items-center justify-between">
                <span className="text-xs font-medium text-stone-700">Chiều điều chỉnh:</span>
                <div className="flex space-x-2">
                  <button
                    type="button"
                    onClick={() => setIsOtherNegative(false)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1 transition-all ${
                      !isOtherNegative
                        ? 'bg-emerald-600 text-white shadow-xs'
                        : 'bg-white text-stone-600 border border-stone-200'
                    }`}
                  >
                    <TrendingUp className="h-3.5 w-3.5 mr-1" />
                    <span>Cộng thêm (+)</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsOtherNegative(true)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1 transition-all ${
                      isOtherNegative
                        ? 'bg-rose-600 text-white shadow-xs'
                        : 'bg-white text-stone-600 border border-stone-200'
                    }`}
                  >
                    <TrendingDown className="h-3.5 w-3.5 mr-1" />
                    <span>Khấu trừ (-)</span>
                  </button>
                </div>
              </div>
            )}

            {/* 2. Nhập số tiền */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-stone-700 block">
                Số tiền điều chỉnh (VND) <span className="text-rose-500">*</span>
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-stone-400">
                  <DollarSign className="h-4 w-4" />
                </div>
                <Input
                  type="text"
                  value={rawAmount ? new Intl.NumberFormat('vi-VN').format(numericAmount) : ''}
                  onChange={handleAmountChange}
                  placeholder="Ví dụ: 500.000"
                  className="pl-9 font-mono font-bold text-stone-900 h-10 text-sm"
                />
              </div>

              {/* Dynamic Signed Preview */}
              {numericAmount > 0 && (
                <div className="flex items-center justify-between p-2.5 bg-stone-50 rounded-lg border border-stone-200 text-xs">
                  <span className="text-stone-600 font-medium">Khoản ghi nhận thực tế:</span>
                  <span
                    className={`font-mono font-bold text-sm ${
                      signedAmount > 0 ? 'text-emerald-700' : 'text-rose-700'
                    }`}
                  >
                    {signedAmount > 0 ? '+' : ''}
                    {formatVND(signedAmount)}
                  </span>
                </div>
              )}
            </div>

            {/* 3. Ghi chú giải trình */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-stone-700 block">
                Ghi chú giải trình lý do <span className="text-rose-500">*</span>
              </label>
              <textarea
                value={explanation}
                onChange={(e) => setExplanation(e.target.value)}
                rows={3}
                placeholder="Nhập lý do chi tiết (ví dụ: Phụ cấp trách nhiệm ca trưởng tuần 2, Thưởng hoàn thành doanh số tháng, Phạt đi muộn 3 lần theo nội quy...)"
                className="w-full p-3 bg-white border border-stone-300 rounded-xl text-xs text-stone-900 focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent placeholder:text-stone-400 resize-none"
              />
            </div>

            {/* Projected Impact Preview Card */}
            {numericAmount > 0 && (
              <div className="p-3 bg-[#304638]/5 rounded-xl border border-[#304638]/20 flex items-center justify-between text-xs">
                <div className="flex items-center space-x-1.5 text-stone-700">
                  <Info className="h-4 w-4 text-[#819B78]" />
                  <span>Lương thực lĩnh sau điều chỉnh:</span>
                </div>
                <span
                  className={`font-mono font-bold text-sm ${
                    projectedGross >= 0 ? 'text-[#304638]' : 'text-rose-600'
                  }`}
                >
                  {formatVND(projectedGross)}
                </span>
              </div>
            )}

            {/* Actions */}
            <div className="pt-2 flex items-center justify-end space-x-3">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={isSubmitting}
                className="border-stone-300 text-stone-700 hover:bg-stone-100"
              >
                Hủy
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting || numericAmount <= 0}
                className="bg-[#304638] hover:bg-[#25372C] text-white font-semibold"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Đang lưu...
                  </>
                ) : (
                  'Lưu khoản điều chỉnh'
                )}
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
