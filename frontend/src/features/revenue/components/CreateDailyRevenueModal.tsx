import React, { useState, useEffect, useMemo, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  AlertCircle,
  Loader2,
  Calendar,
  DollarSign,
  QrCode,
  Smartphone,
  SlidersHorizontal,
  FileCheck2,
  Paperclip,
  CheckCircle2,
  Info,
  Trash2,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { revenueApi, formatVnd, parseMoneyInput } from '../api/revenueApi'
import { EvidenceDropzone } from '@/features/evidence/components/EvidenceDropzone'
import type { EvidenceFileDto } from '@/features/evidence/types'
import type {
  DailyRevenueReportDto,
  CreateDailyRevenueCommand,
} from '../types/revenue.types'

interface CreateDailyRevenueModalProps {
  isOpen: boolean
  branchId: string
  branchName?: string
  initialDate?: string
  onClose: () => void
  onSuccess: (report: DailyRevenueReportDto) => void
}

export const CreateDailyRevenueModal: React.FC<CreateDailyRevenueModalProps> = ({
  isOpen,
  branchId,
  branchName,
  initialDate,
  onClose,
  onSuccess,
}) => {
  const todayStr = useMemo(() => new Date().toISOString().split('T')[0], [])

  const [businessDate, setBusinessDate] = useState<string>(initialDate || todayStr)
  const [cashRaw, setCashRaw] = useState<string>('')
  const [bankRaw, setBankRaw] = useState<string>('')
  const [deliveryAppRaw, setDeliveryAppRaw] = useState<string>('')
  const [adjustmentRaw, setAdjustmentRaw] = useState<string>('')
  const [note, setNote] = useState<string>('')
  const [attachedFile, setAttachedFile] = useState<EvidenceFileDto | null>(null)

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [submitMode, setSubmitMode] = useState<'DRAFT' | 'SUBMIT'>('SUBMIT')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Reset form when opened or initialDate changes
  useEffect(() => {
    if (isOpen) {
      setBusinessDate(initialDate || todayStr)
      setCashRaw('')
      setBankRaw('')
      setDeliveryAppRaw('')
      setAdjustmentRaw('')
      setNote('')
      setAttachedFile(null)
      setErrorMessage(null)
    }
  }, [isOpen, initialDate, todayStr])

  // Parse monetary values
  const cashAmount = useMemo(() => parseMoneyInput(cashRaw), [cashRaw])
  const bankTransferAmount = useMemo(() => parseMoneyInput(bankRaw), [bankRaw])
  const deliveryAppNetAmount = useMemo(() => parseMoneyInput(deliveryAppRaw), [deliveryAppRaw])
  const otherAdjustmentAmount = useMemo(() => {
    if (!adjustmentRaw.trim()) return 0
    // Allow negative numbers for adjustment
    const cleaned = adjustmentRaw.replace(/[^\d.-]/g, '')
    const val = parseFloat(cleaned)
    return isNaN(val) ? 0 : val
  }, [adjustmentRaw])

  // Real-time live sum
  const totalRevenue = useMemo(() => {
    return cashAmount + bankTransferAmount + deliveryAppNetAmount + otherAdjustmentAmount
  }, [cashAmount, bankTransferAmount, deliveryAppNetAmount, otherAdjustmentAmount])

  // Form validity
  const isValid = useMemo(() => {
    if (!businessDate) return false
    if (cashAmount < 0 || bankTransferAmount < 0 || deliveryAppNetAmount < 0) return false
    if (totalRevenue < 0) return false
    return true
  }, [businessDate, cashAmount, bankTransferAmount, deliveryAppNetAmount, totalRevenue])

  const handleUploadSuccess = useCallback((file: EvidenceFileDto) => {
    setAttachedFile(file)
  }, [])

  const handleSubmit = async (submitImmediately: boolean) => {
    if (!isValid || !branchId) return
    setIsSubmitting(true)
    setSubmitMode(submitImmediately ? 'SUBMIT' : 'DRAFT')
    setErrorMessage(null)

    try {
      const command: CreateDailyRevenueCommand = {
        businessDate,
        cashAmount,
        bankTransferAmount,
        deliveryAppNetAmount,
        otherAdjustmentAmount,
        evidenceFileId: attachedFile ? attachedFile.id : null,
        note: note.trim() || undefined,
        submitImmediately,
      }

      const report = await revenueApi.createDailyRevenue(branchId, command)
      onSuccess(report)
      onClose()
    } catch (err: unknown) {
      const errorObj = err as {
        response?: { data?: { detail?: string; title?: string; message?: string } }
        message?: string
      }
      const msg =
        errorObj.response?.data?.detail ||
        errorObj.response?.data?.message ||
        errorObj.response?.data?.title ||
        errorObj.message ||
        'Không thể ghi nhận báo cáo doanh thu. Vui lòng kiểm tra lại.'
      setErrorMessage(msg)
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
          className="relative w-full max-w-2xl bg-[#FFFCF8] rounded-2xl shadow-2xl border border-[#E5DED4] overflow-hidden my-auto"
        >
          {/* Header */}
          <div className="bg-[#304638] text-[#F7F3EC] px-5 py-4 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-[#819B78]/25 rounded-xl text-[#F7F3EC]">
                <DollarSign className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-lg font-bold text-white tracking-tight">
                  Nhập doanh thu ngày (P0-REV-04)
                </h2>
                <p className="text-xs text-[#C8CEC4]">
                  {branchName ? `Chi nhánh: ${branchName}` : 'Ghi nhận doanh thu tổng hợp cuối ca/ngày'}
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

          <div className="p-5 sm:p-6 space-y-5 max-h-[80vh] overflow-y-auto">
            {/* Error Banner */}
            {errorMessage && (
              <div className="p-3.5 rounded-xl bg-[#B7423E]/10 border border-[#B7423E]/30 text-[#B7423E] flex items-start space-x-2.5 text-xs">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                <span className="font-medium">{errorMessage}</span>
              </div>
            )}

            {/* Date Selection */}
            <div>
              <label className="block text-xs font-semibold text-[#352D26] uppercase tracking-wider mb-1.5">
                Ngày kinh doanh (Business Date) <span className="text-[#B7423E]">*</span>
              </label>
              <div className="relative">
                <Calendar className="w-4 h-4 text-[#786C60] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                <Input
                  type="date"
                  value={businessDate}
                  onChange={(e) => setBusinessDate(e.target.value)}
                  className="pl-9 h-11 bg-white border-[#E5DED4] font-medium text-sm text-[#352D26]"
                  max={todayStr}
                />
              </div>
            </div>

            {/* Core Revenue Fields (Mobile-friendly large touch targets) */}
            <div className="space-y-3.5 bg-[#F7F3EC]/70 p-4 rounded-xl border border-[#E5DED4]/80">
              <div className="text-xs font-bold text-[#352D26] flex items-center justify-between">
                <span>CÁC KHOẢN THU PHÂN RÃ THEO KÊNH</span>
                <span className="text-[11px] font-normal text-[#786C60]">Đơn vị: VNĐ</span>
              </div>

              {/* 1. Tiền mặt */}
              <div className="bg-white p-3 rounded-lg border border-[#E5DED4] shadow-xs">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-semibold text-[#352D26] flex items-center gap-1.5">
                    <DollarSign className="w-3.5 h-3.5 text-[#304638]" />
                    Tiền mặt tại quầy (Cash)
                  </span>
                  <span className="text-xs font-bold text-[#304638]">
                    {formatVnd(cashAmount)}
                  </span>
                </div>
                <Input
                  type="number"
                  inputMode="numeric"
                  placeholder="0"
                  min="0"
                  value={cashRaw}
                  onChange={(e) => setCashRaw(e.target.value)}
                  className="h-11 text-base font-semibold border-[#E5DED4]"
                />
              </div>

              {/* 2. Chuyển khoản */}
              <div className="bg-white p-3 rounded-lg border border-[#E5DED4] shadow-xs">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-semibold text-[#352D26] flex items-center gap-1.5">
                    <QrCode className="w-3.5 h-3.5 text-[#B96849]" />
                    Chuyển khoản VietQR / Ngân hàng (Bank Transfer)
                  </span>
                  <span className="text-xs font-bold text-[#B96849]">
                    {formatVnd(bankTransferAmount)}
                  </span>
                </div>
                <Input
                  type="number"
                  inputMode="numeric"
                  placeholder="0"
                  min="0"
                  value={bankRaw}
                  onChange={(e) => setBankRaw(e.target.value)}
                  className="h-11 text-base font-semibold border-[#E5DED4]"
                />
              </div>

              {/* 3. Doanh thu App NET */}
              <div className="bg-white p-3 rounded-lg border border-[#E5DED4] shadow-xs">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-semibold text-[#352D26] flex items-center gap-1.5">
                    <Smartphone className="w-3.5 h-3.5 text-blue-600" />
                    Doanh thu App giao hàng NET (GrabFood / ShopeeFood)
                  </span>
                  <span className="text-xs font-bold text-blue-700">
                    {formatVnd(deliveryAppNetAmount)}
                  </span>
                </div>
                <Input
                  type="number"
                  inputMode="numeric"
                  placeholder="0"
                  min="0"
                  value={deliveryAppRaw}
                  onChange={(e) => setDeliveryAppRaw(e.target.value)}
                  className="h-11 text-base font-semibold border-[#E5DED4]"
                />
              </div>

              {/* 4. Điều chỉnh khác */}
              <div className="bg-white p-3 rounded-lg border border-[#E5DED4] shadow-xs">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs font-semibold text-[#352D26] flex items-center gap-1.5">
                    <SlidersHorizontal className="w-3.5 h-3.5 text-[#786C60]" />
                    Điều chỉnh khác (+/- VNĐ)
                  </span>
                  <span className={`text-xs font-bold ${otherAdjustmentAmount < 0 ? 'text-[#B7423E]' : 'text-[#786C60]'}`}>
                    {formatVnd(otherAdjustmentAmount)}
                  </span>
                </div>
                <Input
                  type="number"
                  placeholder="0 (chênh lệch thừa/thiếu, coupon hoàn tiền)"
                  value={adjustmentRaw}
                  onChange={(e) => setAdjustmentRaw(e.target.value)}
                  className="h-11 text-base font-semibold border-[#E5DED4]"
                />
              </div>
            </div>

            {/* Invariant Banner TC-007 */}
            <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-900 flex items-start space-x-2.5 text-xs">
              <Info className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <div className="space-y-0.5">
                <span className="font-bold text-amber-950">Quy định hạch toán App NET (TC-007):</span>
                <p className="text-amber-800 text-[11px] leading-relaxed">
                  Doanh thu App NET được ghi nhận vào kết quả kinh doanh ngày nhưng <strong>tuyệt đối không tự động cộng vào tài khoản ngân hàng</strong>. Tiền thực tế ứng dụng chuyển về sẽ được đối soát riêng.
                </p>
              </div>
            </div>

            {/* Total Live Sum Card */}
            <div className="p-4 rounded-xl bg-gradient-to-br from-[#304638] to-[#25372C] text-[#F7F3EC] shadow-md flex items-center justify-between">
              <div>
                <span className="text-[11px] uppercase tracking-wider text-[#C8CEC4] font-medium block">
                  Tổng doanh thu thực tính (TC-006)
                </span>
                <span className="text-xs text-[#819B78]">
                  = Tiền mặt + Chuyển khoản + App NET + Điều chỉnh
                </span>
              </div>
              <div className="text-right">
                <span className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
                  {formatVnd(totalRevenue)}
                </span>
              </div>
            </div>

            {/* Evidence Attachment Section */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-[#352D26] uppercase tracking-wider flex items-center gap-1.5">
                  <Paperclip className="w-3.5 h-3.5 text-[#786C60]" />
                  Bằng chứng đính kèm (Bill Z-report / Sao kê)
                </label>
                {attachedFile && (
                  <Badge variant="outline" className="text-emerald-700 bg-emerald-50 border-emerald-200 text-[10px]">
                    <CheckCircle2 className="w-3 h-3 mr-1" /> Đã đính kèm
                  </Badge>
                )}
              </div>

              {attachedFile ? (
                <div className="p-3 bg-white rounded-xl border border-emerald-200 flex items-center justify-between shadow-xs">
                  <div className="flex items-center space-x-2.5 overflow-hidden">
                    <div className="p-2 bg-emerald-50 text-emerald-700 rounded-lg shrink-0">
                      <FileCheck2 className="w-4 h-4" />
                    </div>
                    <div className="truncate">
                      <p className="text-xs font-medium text-[#352D26] truncate">
                        {attachedFile.originalFilename}
                      </p>
                      <p className="text-[10px] text-[#786C60] font-mono">
                        {(attachedFile.byteSize / 1024).toFixed(1)} KB
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setAttachedFile(null)}
                    className="p-1.5 text-zinc-400 hover:text-red-500 rounded-lg hover:bg-red-50 transition-colors"
                    title="Xóa đính kèm"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ) : (
                <EvidenceDropzone
                  branchId={branchId}
                  defaultVisibility="BRANCH_PRIVATE"
                  onUploadSuccess={handleUploadSuccess}
                  className="p-3"
                />
              )}
            </div>

            {/* Note */}
            <div>
              <label className="block text-xs font-semibold text-[#352D26] uppercase tracking-wider mb-1.5">
                Ghi chú ca / ngày kinh doanh
              </label>
              <textarea
                rows={2}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Ví dụ: Doanh thu ca tối thứ Hai, khách đông giờ tan tầm..."
                className="w-full text-xs p-3 rounded-xl border border-[#E5DED4] bg-white focus:outline-hidden focus:ring-2 focus:ring-[#B96849]"
              />
            </div>
          </div>

          {/* Footer Actions */}
          <div className="bg-[#F7F3EC] px-5 py-4 border-t border-[#E5DED4] flex flex-col sm:flex-row items-center justify-between gap-3">
            <p className="text-[11px] text-[#786C60] text-center sm:text-left">
              Chỉ được có <strong className="text-[#352D26]">duy nhất 1 báo cáo hiện hành</strong> mỗi ngày kinh doanh.
            </p>
            <div className="flex items-center space-x-2.5 w-full sm:w-auto">
              <Button
                type="button"
                variant="outline"
                disabled={isSubmitting}
                onClick={onClose}
                className="flex-1 sm:flex-none border-[#E5DED4] text-xs h-10"
              >
                Hủy bỏ
              </Button>
              <Button
                type="button"
                variant="secondary"
                disabled={isSubmitting || !isValid}
                onClick={() => handleSubmit(false)}
                className="flex-1 sm:flex-none text-xs h-10 bg-amber-100 hover:bg-amber-200 text-amber-900 border-amber-300"
              >
                {isSubmitting && submitMode === 'DRAFT' ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin mr-1.5" />
                ) : null}
                Lưu nháp (DRAFT)
              </Button>
              <Button
                type="button"
                disabled={isSubmitting || !isValid}
                onClick={() => handleSubmit(true)}
                className="flex-1 sm:flex-none text-xs h-10 bg-[#B96849] hover:bg-[#A0573B] text-white shadow-sm font-semibold"
              >
                {isSubmitting && submitMode === 'SUBMIT' ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin mr-1.5" />
                ) : (
                  <FileCheck2 className="w-3.5 h-3.5 mr-1.5" />
                )}
                Gửi duyệt (SUBMIT)
              </Button>
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
