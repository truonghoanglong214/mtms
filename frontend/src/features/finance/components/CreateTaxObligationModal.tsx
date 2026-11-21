import React, { useState, useEffect, useMemo } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Plus,
  Calendar,
  AlertCircle,
  CheckCircle2,
  Loader2,
  UploadCloud,
  Landmark,
  FileText,
  Building2,
  Sparkles,
  Info,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { financeApi, formatVnd, parseMoneyInput } from '../api/financeApi'
import { EvidenceDropzone } from '@/features/evidence/components/EvidenceDropzone'
import type { EvidenceFileDto } from '@/features/evidence/types'
import {
  TAX_OBLIGATION_TYPE_LABELS,
  type TaxObligationDto,
  type TaxObligationType,
  type CreateTaxObligationPayload,
} from '../types/finance.types'

interface CreateTaxObligationModalProps {
  isOpen: boolean
  branchId: string
  branchName?: string
  onClose: () => void
  onSuccess: (obligation: TaxObligationDto) => void
}

export const CreateTaxObligationModal: React.FC<CreateTaxObligationModalProps> = ({
  isOpen,
  branchId,
  branchName,
  onClose,
  onSuccess,
}) => {
  const currentYear = new Date().getFullYear()
  const currentQuarter = Math.floor(new Date().getMonth() / 3) + 1

  const [obligationCode, setObligationCode] = useState<string>('')
  const [obligationType, setObligationType] = useState<TaxObligationType>('VAT')
  const [taxAuthorityName, setTaxAuthorityName] = useState<string>('')
  const [taxPeriodStart, setTaxPeriodStart] = useState<string>('')
  const [taxPeriodEnd, setTaxPeriodEnd] = useState<string>('')
  const [dueDate, setDueDate] = useState<string>('')
  const [amountStr, setAmountStr] = useState<string>('')
  const [isBranchSpecific, setIsBranchSpecific] = useState<boolean>(true)
  const [note, setNote] = useState<string>('')

  // Evidence
  const [evidenceFileId, setEvidenceFileId] = useState<string | null>(null)
  const [evidenceFileName, setEvidenceFileName] = useState<string | null>(null)
  const [showDropzone, setShowDropzone] = useState<boolean>(false)

  // Status & Errors
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Suggest default dates based on obligationType
  const generateSuggestedCode = (type: TaxObligationType) => {
    const codeMap: Record<TaxObligationType, string> = {
      MON_BAI: `TAX-${currentYear}-MB`,
      VAT: `TAX-${currentYear}-Q${currentQuarter}-VAT`,
      CIT: `TAX-${currentYear}-Q${currentQuarter}-CIT`,
      PIT: `TAX-${currentYear}-Q${currentQuarter}-PIT`,
      OTHER: `TAX-${currentYear}-${Date.now().toString().slice(-4)}`,
    }
    return codeMap[type] || `TAX-${currentYear}-${type}`
  }

  // Reset form when modal opens
  useEffect(() => {
    if (isOpen) {
      const today = new Date()
      const year = today.getFullYear()
      const month = today.getMonth()

      // Default quarter start and end
      const qStart = new Date(year, Math.floor(month / 3) * 3, 1)
      const qEnd = new Date(year, Math.floor(month / 3) * 3 + 3, 0)
      const due = new Date(year, Math.floor(month / 3) * 3 + 3, 30)

      const fmt = (d: Date) => d.toISOString().split('T')[0]

      setObligationType('VAT')
      setObligationCode(`TAX-${year}-Q${Math.floor(month / 3) + 1}-VAT`)
      setTaxAuthorityName('')
      setTaxPeriodStart(fmt(qStart))
      setTaxPeriodEnd(fmt(qEnd))
      setDueDate(fmt(due))
      setAmountStr('')
      setIsBranchSpecific(true)
      setNote('')
      setEvidenceFileId(null)
      setEvidenceFileName(null)
      setShowDropzone(false)
      setErrorMessage(null)
    }
  }, [isOpen])

  const parsedAmount = useMemo(() => {
    return parseMoneyInput(amountStr)
  }, [amountStr])

  const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value
    const parsed = parseMoneyInput(raw)
    if (parsed === 0 && raw === '') {
      setAmountStr('')
    } else {
      setAmountStr(parsed.toLocaleString('vi-VN'))
    }
  }

  const handleTypeChange = (type: TaxObligationType) => {
    setObligationType(type)
    // Update code if code is empty or looks like standard generated code
    if (!obligationCode || obligationCode.startsWith('TAX-')) {
      setObligationCode(generateSuggestedCode(type))
    }
  }

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
    setErrorMessage(null)

    if (!obligationCode.trim()) {
      setErrorMessage('Vui lòng nhập mã nghĩa vụ thuế.')
      return
    }

    if (parsedAmount <= 0) {
      setErrorMessage('Số tiền thuế phải nộp phải lớn hơn 0.')
      return
    }

    if (!taxPeriodStart || !taxPeriodEnd) {
      setErrorMessage('Vui lòng chọn đầy đủ kỳ tính thuế (từ ngày - đến ngày).')
      return
    }

    if (taxPeriodEnd < taxPeriodStart) {
      setErrorMessage('Ngày kết thúc kỳ tính thuế phải sau hoặc bằng ngày bắt đầu.')
      return
    }

    setIsSubmitting(true)

    try {
      const payload: CreateTaxObligationPayload = {
        obligationCode: obligationCode.trim().toUpperCase(),
        obligationType,
        taxAuthorityName: taxAuthorityName.trim() || undefined,
        taxPeriodStart,
        taxPeriodEnd,
        dueDate: dueDate || undefined,
        amountDue: parsedAmount,
        isBranchSpecific,
        note: note.trim() || undefined,
        evidenceFileId: evidenceFileId || undefined,
      }

      const res = await financeApi.createTaxObligation(branchId, payload)
      onSuccess(res)
      onClose()
    } catch (err: unknown) {
      console.error('Failed to create tax obligation:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string; title?: string } }
      }
      setErrorMessage(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          apiErr.response?.data?.title ||
          'Không thể khai báo nghĩa vụ thuế. Vui lòng kiểm tra lại thông tin.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-900/60 backdrop-blur-xs overflow-y-auto">
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 15 }}
          transition={{ duration: 0.2, ease: 'easeOut' }}
          className="relative w-full max-w-2xl bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden my-auto"
        >
          {/* Header */}
          <div className="px-5 py-4 bg-slate-900 text-white flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-xl bg-[#304638] text-white flex items-center justify-center shadow-xs">
                <Landmark className="w-5 h-5 text-emerald-400" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
                  <span>Khai Báo Nghĩa Vụ Thuế & Lệ Phí</span>
                </h3>
                <p className="text-xs text-slate-400">
                  {branchName ? `Chi nhánh: ${branchName}` : 'Ghi nhận nghĩa vụ nộp ngân sách nhà nước'}
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="text-slate-400 hover:text-white p-1 rounded-lg transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="p-5 space-y-4 max-h-[80vh] overflow-y-auto">
            {/* Business Policy Note (BR-FIN-04) */}
            <div className="p-3 rounded-xl bg-amber-50/80 border border-amber-200 text-amber-900 text-xs flex items-start gap-2.5">
              <Info className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <div>
                <span className="font-semibold">Quy chuẩn kế toán thuế:</span> Nghĩa vụ thuế
                được ghi nhận thủ công theo số liệu tờ khai hoặc thông báo chính thức từ cơ quan thuế,
                không sử dụng công thức tự động tính toán sai quy định.
              </div>
            </div>

            {/* Error Banner */}
            {errorMessage && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-start gap-2.5 shadow-xs">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span className="font-medium">{errorMessage}</span>
              </div>
            )}

            {/* Row 1: Loại thuế & Mã nghĩa vụ */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              {/* Loại thuế */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 block">
                  Loại nghĩa vụ thuế <span className="text-rose-500">*</span>
                </label>
                <select
                  value={obligationType}
                  onChange={(e) => handleTypeChange(e.target.value as TaxObligationType)}
                  className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 font-medium focus:outline-none focus:ring-2 focus:ring-[#304638]"
                >
                  {(Object.keys(TAX_OBLIGATION_TYPE_LABELS) as TaxObligationType[]).map((key) => (
                    <option key={key} value={key}>
                      {TAX_OBLIGATION_TYPE_LABELS[key]}
                    </option>
                  ))}
                </select>
              </div>

              {/* Mã nghĩa vụ */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-slate-700">
                    Mã hồ sơ / tờ khai <span className="text-rose-500">*</span>
                  </label>
                  <button
                    type="button"
                    onClick={() => setObligationCode(generateSuggestedCode(obligationType))}
                    className="text-[11px] font-medium text-[#304638] hover:underline flex items-center gap-1 cursor-pointer"
                  >
                    <Sparkles className="w-3 h-3" />
                    <span>Gợi ý mã</span>
                  </button>
                </div>
                <Input
                  type="text"
                  placeholder="Ví dụ: TAX-2026-Q1-VAT"
                  value={obligationCode}
                  onChange={(e) => setObligationCode(e.target.value.toUpperCase())}
                  className="font-mono text-xs"
                  required
                />
              </div>
            </div>

            {/* Row 2: Số tiền phải nộp & Hạn nộp */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              {/* Số tiền phải nộp */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 block">
                  Số tiền phải nộp (VNĐ) <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <Input
                    type="text"
                    placeholder="0"
                    value={amountStr}
                    onChange={handleAmountChange}
                    className="text-right font-mono text-sm font-semibold pr-10 text-emerald-800"
                    required
                  />
                  <span className="absolute right-3 top-2.5 text-xs font-medium text-slate-400">
                    ₫
                  </span>
                </div>
                {parsedAmount > 0 && (
                  <p className="text-[11px] font-mono text-emerald-700 text-right">
                    {formatVnd(parsedAmount)}
                  </p>
                )}
              </div>

              {/* Hạn nộp tiền */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-slate-500" />
                  <span>Hạn nộp ngân sách</span>
                </label>
                <Input
                  type="date"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                  className="text-xs"
                />
              </div>
            </div>

            {/* Row 3: Kỳ tính thuế (Từ ngày - Đến ngày) */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700">
                  Kỳ thuế: Từ ngày <span className="text-rose-500">*</span>
                </label>
                <Input
                  type="date"
                  value={taxPeriodStart}
                  onChange={(e) => setTaxPeriodStart(e.target.value)}
                  className="text-xs"
                  required
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700">
                  Kỳ thuế: Đến ngày <span className="text-rose-500">*</span>
                </label>
                <Input
                  type="date"
                  value={taxPeriodEnd}
                  onChange={(e) => setTaxPeriodEnd(e.target.value)}
                  className="text-xs"
                  required
                />
              </div>
            </div>

            {/* Row 4: Cơ quan thuế */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                <Building2 className="w-3.5 h-3.5 text-slate-500" />
                <span>Cơ quan quản lý thuế trực tiếp</span>
              </label>
              <Input
                type="text"
                placeholder="Ví dụ: Chi cục Thuế Quận 1, Cục Thuế TP.HCM"
                value={taxAuthorityName}
                onChange={(e) => setTaxAuthorityName(e.target.value)}
                className="text-xs"
              />
            </div>

            {/* Phạm vi chi nhánh / Pháp nhân (Branch scope) */}
            <div className="p-3 rounded-xl border border-slate-200 bg-slate-50/80 flex items-center justify-between">
              <div>
                <label htmlFor="isBranchSpecificCheckbox" className="text-xs font-semibold text-slate-800 block cursor-pointer">
                  {isBranchSpecific ? 'Nghĩa vụ gắn riêng cho chi nhánh này' : 'Nghĩa vụ thuộc pháp nhân chung'}
                </label>
                <p className="text-[11px] text-slate-500 mt-0.5">
                  {isBranchSpecific
                    ? `Khoản thuế sẽ được tính phân bổ và theo dõi trực tiếp tại chi nhánh ${branchName || ''}.`
                    : 'Khoản thuế thuộc cấp công ty / pháp nhân dùng chung (ví dụ: Thuế TNDN, Thuế GTGT toàn công ty).'}
                </p>
              </div>
              <input
                id="isBranchSpecificCheckbox"
                type="checkbox"
                checked={isBranchSpecific}
                onChange={(e) => setIsBranchSpecific(e.target.checked)}
                className="w-4 h-4 rounded text-[#304638] focus:ring-[#304638] cursor-pointer"
              />
            </div>

            {/* Ghi chú */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">
                Ghi chú & Căn cứ phát sinh
              </label>
              <textarea
                rows={2}
                placeholder="Ví dụ: Căn cứ tờ khai thuế GTGT quý 1 năm 2026 đã nộp qua cổng thuedientu"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent resize-none"
              />
            </div>

            {/* Chứng từ & Tờ khai đính kèm (Evidence Dropzone) */}
            <div className="space-y-2 pt-2 border-t border-slate-100">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <FileText className="w-3.5 h-3.5 text-slate-500" />
                  <span>Tờ khai thuế / Thông báo nộp thuế (Chứng từ đính kèm)</span>
                </label>
                {!evidenceFileId && !showDropzone && (
                  <button
                    type="button"
                    onClick={() => setShowDropzone(true)}
                    className="text-xs font-medium text-[#304638] hover:text-[#25372C] flex items-center gap-1 cursor-pointer"
                  >
                    <UploadCloud className="w-3.5 h-3.5" />
                    <span>Tải tệp đính kèm</span>
                  </button>
                )}
              </div>

              {/* Uploaded File Info Card */}
              {evidenceFileId && (
                <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs text-emerald-900 shadow-xs">
                  <div className="flex items-center gap-2 min-w-0">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span className="font-medium truncate max-w-xs">
                      {evidenceFileName || 'Tài liệu đã đính kèm'}
                    </span>
                    <Badge variant="outline" className="border-emerald-300 text-emerald-700 text-[10px] shrink-0">
                      Đã tải lên
                    </Badge>
                  </div>
                  <button
                    type="button"
                    onClick={handleRemoveEvidence}
                    className="text-slate-400 hover:text-rose-600 p-1 cursor-pointer"
                    title="Gỡ tài liệu"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
              )}

              {/* Dropzone Component */}
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

            {/* Footer Buttons */}
            <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-2.5">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={isSubmitting}
                className="text-xs"
              >
                Hủy bỏ
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting}
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-5 flex items-center gap-1.5"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>Đang lưu...</span>
                  </>
                ) : (
                  <>
                    <Plus className="w-3.5 h-3.5" />
                    <span>Tạo Nghĩa Vụ Thuế</span>
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
