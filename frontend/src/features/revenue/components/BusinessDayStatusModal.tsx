import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Calendar,
  AlertCircle,
  Loader2,
  CheckCircle2,
  Info,
  DoorOpen,
  DoorClosed,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { revenueApi } from '../api/revenueApi'
import type { BusinessDayDto, OperationStatus } from '../types/revenue.types'

interface BusinessDayStatusModalProps {
  isOpen: boolean
  branchId: string
  branchName?: string
  businessDate: string
  currentStatus?: OperationStatus
  currentCloseReason?: string | null
  onClose: () => void
  onSuccess: (updated: BusinessDayDto) => void
}

export const BusinessDayStatusModal: React.FC<BusinessDayStatusModalProps> = ({
  isOpen,
  branchId,
  branchName,
  businessDate,
  currentStatus = 'OPEN',
  currentCloseReason = '',
  onClose,
  onSuccess,
}) => {
  const [status, setStatus] = useState<OperationStatus>(currentStatus)
  const [closeReason, setCloseReason] = useState<string>(currentCloseReason || '')
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    if (isOpen) {
      setStatus(currentStatus)
      setCloseReason(currentCloseReason || '')
      setErrorMessage(null)
    }
  }, [isOpen, currentStatus, currentCloseReason])

  const isValid = status === 'OPEN' || (status === 'CLOSED' && closeReason.trim().length > 0)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!isValid || !branchId || !businessDate) return

    setIsSubmitting(true)
    setErrorMessage(null)

    try {
      const updated = await revenueApi.setBusinessDay(branchId, businessDate, {
        operationStatus: status,
        closeReason: status === 'CLOSED' ? closeReason.trim() : null,
      })
      onSuccess(updated)
      onClose()
    } catch (err: unknown) {
      const errorObj = err as {
        response?: { data?: { detail?: string; message?: string } }
        message?: string
      }
      setErrorMessage(
        errorObj.response?.data?.detail ||
        errorObj.response?.data?.message ||
        errorObj.message ||
        'Không thể cập nhật trạng thái ngày kinh doanh.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-sm overflow-y-auto">
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 12 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 12 }}
          transition={{ duration: 0.2 }}
          className="relative w-full max-w-lg bg-[#FFFCF8] rounded-2xl shadow-2xl border border-[#E5DED4] overflow-hidden my-auto"
        >
          {/* Header */}
          <div className="bg-[#304638] text-[#F7F3EC] px-5 py-4 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-[#819B78]/25 rounded-xl text-[#F7F3EC]">
                <Calendar className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-lg font-bold text-white tracking-tight">
                  Thiết lập trạng thái ngày kinh doanh
                </h2>
                <p className="text-xs text-[#C8CEC4]">
                  Ngày: <strong className="text-white">{businessDate}</strong> {branchName ? `(${branchName})` : ''}
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="p-1.5 rounded-lg text-[#C8CEC4] hover:text-white hover:bg-white/10 transition-colors"
              aria-label="Đóng"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="p-5 sm:p-6 space-y-4">
              {errorMessage && (
                <div className="p-3.5 rounded-xl bg-[#B7423E]/10 border border-[#B7423E]/30 text-[#B7423E] flex items-start space-x-2.5 text-xs">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                  <span className="font-medium">{errorMessage}</span>
                </div>
              )}

              {/* Status Radio / Selector */}
              <div className="space-y-2">
                <label className="block text-xs font-semibold text-[#352D26] uppercase tracking-wider">
                  Trạng thái hoạt động (Operation Status)
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setStatus('OPEN')}
                    className={`p-3.5 rounded-xl border text-left transition-all flex flex-col gap-1.5 ${
                      status === 'OPEN'
                        ? 'border-emerald-600 bg-emerald-50 text-emerald-950 ring-2 ring-emerald-500/30'
                        : 'border-[#E5DED4] bg-white text-[#786C60] hover:bg-zinc-50'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold flex items-center gap-1.5">
                        <DoorOpen className="w-4 h-4 text-emerald-600" />
                        Mở cửa (OPEN)
                      </span>
                      {status === 'OPEN' && <CheckCircle2 className="w-4 h-4 text-emerald-600" />}
                    </div>
                    <span className="text-[11px] text-[#786C60]">
                      Cửa hàng mở bán bình thường, phải nộp báo cáo doanh thu.
                    </span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setStatus('CLOSED')}
                    className={`p-3.5 rounded-xl border text-left transition-all flex flex-col gap-1.5 ${
                      status === 'CLOSED'
                        ? 'border-amber-600 bg-amber-50 text-amber-950 ring-2 ring-amber-500/30'
                        : 'border-[#E5DED4] bg-white text-[#786C60] hover:bg-zinc-50'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold flex items-center gap-1.5">
                        <DoorClosed className="w-4 h-4 text-amber-600" />
                        Đóng cửa (CLOSED)
                      </span>
                      {status === 'CLOSED' && <CheckCircle2 className="w-4 h-4 text-amber-600" />}
                    </div>
                    <span className="text-[11px] text-[#786C60]">
                      Nghỉ bán có lý do (TC-005), miễn trừ nộp báo cáo doanh thu.
                    </span>
                  </button>
                </div>
              </div>

              {/* Close Reason Field (Mandatory if CLOSED) */}
              {status === 'CLOSED' && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  className="space-y-1.5"
                >
                  <label className="block text-xs font-semibold text-[#352D26] uppercase tracking-wider">
                    Lý do đóng cửa / Nghỉ bán <span className="text-[#B7423E]">*</span>
                  </label>
                  <textarea
                    rows={3}
                    required
                    value={closeReason}
                    onChange={(e) => setCloseReason(e.target.value)}
                    placeholder="Ví dụ: Nghỉ Tết Nguyên Đán, bảo trì máy pha cà phê, cúp điện lưới..."
                    className="w-full text-xs p-3 rounded-xl border border-amber-300 bg-white focus:outline-hidden focus:ring-2 focus:ring-amber-500"
                  />
                  <p className="text-[11px] text-[#786C60]">
                    * Bắt buộc nhập lý do cụ thể để kiểm toán và chốt sổ kỳ kinh doanh.
                  </p>
                </motion.div>
              )}

              {/* TC-005 Invariant Note */}
              <div className="p-3 rounded-xl bg-blue-50 border border-blue-200 text-blue-900 text-xs flex items-start gap-2">
                <Info className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
                <p className="text-[11px] text-blue-800 leading-relaxed">
                  <strong>TC-005 Quy định miễn trừ:</strong> Ngày đóng cửa hợp lệ có lý do sẽ được hệ thống loại trừ khỏi danh sách thiếu báo cáo doanh thu (`missing-dates`), cho phép thực hiện chốt sổ kỳ kế toán mà không bị chặn.
                </p>
              </div>
            </div>

            {/* Footer */}
            <div className="bg-[#F7F3EC] px-5 py-4 border-t border-[#E5DED4] flex items-center justify-end space-x-2.5">
              <Button
                type="button"
                variant="outline"
                disabled={isSubmitting}
                onClick={onClose}
                className="border-[#E5DED4] text-xs h-9"
              >
                Hủy bỏ
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting || !isValid}
                className="text-xs h-9 bg-[#304638] hover:bg-[#25372C] text-white shadow-sm font-semibold"
              >
                {isSubmitting ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin mr-1.5" />
                ) : (
                  <CheckCircle2 className="w-3.5 h-3.5 mr-1.5" />
                )}
                Lưu trạng thái
              </Button>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
