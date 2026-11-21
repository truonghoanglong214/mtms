import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  FileText,
  AlertTriangle,
  CheckCircle2,
  Loader2,
  UploadCloud,
  Calendar,
  Building2,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { financeApi, formatVnd, formatSignedVnd } from '../api/financeApi'
import { EvidenceDropzone } from '@/features/evidence/components/EvidenceDropzone'
import type { EvidenceFileDto } from '@/features/evidence/types'
import type {
  CashReconciliationDto,
  ExplainDiscrepancyPayload,
} from '../types/finance.types'

interface ExplainDiscrepancyModalProps {
  isOpen: boolean
  branchId: string
  reconciliation: CashReconciliationDto | null
  onClose: () => void
  onSuccess: (updated: CashReconciliationDto) => void
}

export const ExplainDiscrepancyModal: React.FC<ExplainDiscrepancyModalProps> = ({
  isOpen,
  branchId,
  reconciliation,
  onClose,
  onSuccess,
}) => {
  const [note, setNote] = useState<string>('')
  const [evidenceFileId, setEvidenceFileId] = useState<string | null>(null)
  const [evidenceFileName, setEvidenceFileName] = useState<string | null>(null)
  const [showDropzone, setShowDropzone] = useState<boolean>(false)

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    if (isOpen && reconciliation) {
      setNote(reconciliation.note || '')
      setEvidenceFileId(reconciliation.evidenceFileId || null)
      setEvidenceFileName(reconciliation.evidenceFileId ? 'Chứng từ hiện tại' : null)
      setShowDropzone(false)
      setErrorMessage(null)
    }
  }, [isOpen, reconciliation])

  if (!reconciliation) return null

  const handleUploadSuccess = (file: EvidenceFileDto) => {
    setEvidenceFileId(file.id)
    setEvidenceFileName(file.originalFilename)
    setShowDropzone(false)
  }

  const handleRemoveEvidence = () => {
    setEvidenceFileId(null)
    setEvidenceFileName(null)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!branchId || !reconciliation) return
    setErrorMessage(null)

    if (!note.trim()) {
      setErrorMessage('Vui lòng nhập nội dung giải trình lý do chênh lệch.')
      return
    }

    try {
      setIsSubmitting(true)

      const payload: ExplainDiscrepancyPayload = {
        note: note.trim(),
        evidenceFileId: evidenceFileId || undefined,
      }

      const res = await financeApi.explainDiscrepancy(branchId, reconciliation.id, payload)
      onSuccess(res)
      onClose()
    } catch (err: unknown) {
      console.error('Failed to explain discrepancy:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string; title?: string } }
      }
      setErrorMessage(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể cập nhật giải trình đối soát. Vui lòng thử lại.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  const isShortage = reconciliation.varianceAmount < 0
  const isSurplus = reconciliation.varianceAmount > 0

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
            <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-blue-50/50">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-blue-100 text-blue-700 flex items-center justify-center font-bold">
                  <FileText className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    Giải Trình Chênh Lệch Quỹ
                  </h3>
                  <p className="text-xs text-slate-500">
                    Cập nhật lý do và chứng từ đính kèm để chuyển trạng thái sang <strong>ĐÃ GIẢI TRÌNH</strong>
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
                    <span className="text-[10px] text-slate-500 block">Số dư sổ sách</span>
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

            {/* Form */}
            <form onSubmit={handleSubmit} className="px-6 pb-6 pt-2 space-y-4">
              {errorMessage && (
                <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-800 animate-in fade-in">
                  <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                  <span className="leading-relaxed">{errorMessage}</span>
                </div>
              )}

              {/* Note / Explanation Textarea */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 flex items-center justify-between">
                  <span>Nội dung giải trình nguyên nhân chênh lệch <span className="text-rose-500">*</span></span>
                  <span className="text-[11px] text-slate-400 font-normal">Bắt buộc</span>
                </label>
                <textarea
                  rows={3}
                  placeholder="Ví dụ: Khách quẹt thẻ thanh toán ngân hàng lúc 23h50 nên tiền chưa kịp nổi về tài khoản, hoặc thối nhầm tiền lẻ cho khách..."
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                  required
                />
              </div>

              {/* Evidence Upload */}
              <div className="space-y-2 pt-1 border-t border-slate-100">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                    <UploadCloud className="w-3.5 h-3.5 text-slate-500" />
                    <span>Chứng từ giải trình bổ sung (Biên lai / Sao kê...)</span>
                  </label>
                  {!evidenceFileId && !showDropzone && (
                    <button
                      type="button"
                      onClick={() => setShowDropzone(true)}
                      className="text-xs font-medium text-blue-600 hover:text-blue-800 flex items-center gap-1 cursor-pointer"
                    >
                      <UploadCloud className="w-3.5 h-3.5" />
                      <span>Tải tệp đính kèm</span>
                    </button>
                  )}
                </div>

                {evidenceFileId && (
                  <div className="p-3 rounded-xl bg-blue-50 border border-blue-200 flex items-center justify-between text-xs text-blue-900 shadow-xs">
                    <div className="flex items-center gap-2 min-w-0">
                      <CheckCircle2 className="w-4 h-4 text-blue-600 shrink-0" />
                      <span className="font-medium truncate max-w-xs">
                        {evidenceFileName || 'Chứng từ giải trình đã đính kèm'}
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={handleRemoveEvidence}
                      className="text-slate-400 hover:text-rose-600 p-1 cursor-pointer"
                      title="Gỡ chứng từ"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                )}

                {showDropzone && !evidenceFileId && (
                  <div className="rounded-xl border border-slate-200 p-2 bg-slate-50 relative">
                    <button
                      type="button"
                      onClick={() => setShowDropzone(false)}
                      className="absolute top-2 right-2 text-slate-400 hover:text-slate-600 p-1 z-10"
                      title="Đóng tải tệp"
                    >
                      <X className="w-4 h-4" />
                    </button>
                    <EvidenceDropzone
                      branchId={branchId}
                      onUploadSuccess={handleUploadSuccess}
                      defaultVisibility="BRANCH_PRIVATE"
                    />
                  </div>
                )}
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
                  Đóng
                </Button>
                <Button
                  type="submit"
                  disabled={isSubmitting || !note.trim()}
                  className="bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-semibold px-4 h-9 shadow-xs flex items-center gap-2 cursor-pointer"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      <span>Đang lưu giải trình...</span>
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>Lưu giải trình (EXPLAINED)</span>
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
