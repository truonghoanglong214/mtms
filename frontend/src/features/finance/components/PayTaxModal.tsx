import React, { useState, useEffect, useMemo } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  CreditCard,
  Building2,
  Calendar,
  AlertCircle,
  CheckCircle2,
  Loader2,
  Receipt,
  Wallet,
  UploadCloud,
  Info,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { EvidenceDropzone } from '@/features/evidence/components/EvidenceDropzone'
import { financeApi, formatVnd, parseMoneyInput } from '../api/financeApi'
import {
  TAX_OBLIGATION_TYPE_LABELS,
  type TaxObligationDto,
  type TaxObligationDetailDto,
  type FinancialAccountDto,
  type PayTaxObligationPayload,
} from '../types/finance.types'
import type { EvidenceFileDto } from '@/features/evidence/types'

interface PayTaxModalProps {
  isOpen: boolean
  branchId: string
  obligation: TaxObligationDto | null
  accounts: FinancialAccountDto[]
  onClose: () => void
  onSuccess: (updatedDetail: TaxObligationDetailDto) => void
}

export const PayTaxModal: React.FC<PayTaxModalProps> = ({
  isOpen,
  branchId,
  obligation,
  accounts,
  onClose,
  onSuccess,
}) => {
  const [paymentDate, setPaymentDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  )
  const [paymentMethod, setPaymentMethod] = useState<'BANK_TRANSFER' | 'CASH'>('BANK_TRANSFER')
  const [selectedAccountId, setSelectedAccountId] = useState<string>('')
  const [amountStr, setAmountStr] = useState<string>('')
  const [receiptNumber, setReceiptNumber] = useState<string>('')
  const [note, setNote] = useState<string>('')

  // Evidence
  const [evidenceFileId, setEvidenceFileId] = useState<string | null>(null)
  const [evidenceFileName, setEvidenceFileName] = useState<string | null>(null)
  const [showDropzone, setShowDropzone] = useState<boolean>(false)

  // Status & Error
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Filter accounts according to payment method
  const filteredAccounts = useMemo(() => {
    return accounts.filter((a) => {
      const isActive = a.status === 'ACTIVE' || !a.status
      if (!isActive) return false
      if (paymentMethod === 'BANK_TRANSFER') return a.accountKind === 'BANK'
      if (paymentMethod === 'CASH') return a.accountKind === 'CASH'
      return true
    })
  }, [accounts, paymentMethod])

  const selectedAccount = useMemo(() => {
    return accounts.find((a) => a.id === selectedAccountId)
  }, [accounts, selectedAccountId])

  const accountBalanceNum = useMemo(() => {
    if (!selectedAccount) return 0
    return typeof selectedAccount.currentBalance === 'string'
      ? parseFloat(selectedAccount.currentBalance) || 0
      : selectedAccount.currentBalance
  }, [selectedAccount])

  const remainingLiabilityNum = useMemo(() => {
    if (!obligation) return 0
    if (typeof obligation.remainingLiability === 'number') {
      return obligation.remainingLiability
    }
    const due = typeof obligation.amountDue === 'string' ? parseFloat(obligation.amountDue) || 0 : obligation.amountDue
    const paid = typeof obligation.amountPaid === 'string' ? parseFloat(obligation.amountPaid) || 0 : obligation.amountPaid
    return Math.max(0, due - paid)
  }, [obligation])

  // Initialize form when opened
  useEffect(() => {
    if (isOpen && obligation) {
      setPaymentDate(new Date().toISOString().split('T')[0])
      setPaymentMethod('BANK_TRANSFER')

      // Select default account
      const bankAcc = accounts.find((a) => a.accountKind === 'BANK' && (a.status === 'ACTIVE' || !a.status))
      const fallbackAcc = accounts.find((a) => a.status === 'ACTIVE' || !a.status) || accounts[0]
      setSelectedAccountId(bankAcc ? bankAcc.id : (fallbackAcc ? fallbackAcc.id : ''))

      setAmountStr(remainingLiabilityNum.toLocaleString('vi-VN'))
      setReceiptNumber('')
      setNote(`Nộp thuế ${obligation.obligationCode} vào Ngân sách Nhà nước`)
      setEvidenceFileId(null)
      setEvidenceFileName(null)
      setShowDropzone(false)
      setErrorMessage(null)
    }
  }, [isOpen, obligation, accounts, remainingLiabilityNum])

  // Switch default account when payment method toggles
  const handleMethodChange = (method: 'BANK_TRANSFER' | 'CASH') => {
    setPaymentMethod(method)
    const matchingAcc = accounts.find(
      (a) => a.accountKind === (method === 'BANK_TRANSFER' ? 'BANK' : 'CASH') && (a.status === 'ACTIVE' || !a.status)
    )
    if (matchingAcc) {
      setSelectedAccountId(matchingAcc.id)
    }
  }

  const parsedPaymentAmount = useMemo(() => {
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

  const handleUploadSuccess = (file: EvidenceFileDto) => {
    setEvidenceFileId(file.id)
    setEvidenceFileName(file.originalFilename)
    setShowDropzone(false)
  }

  const handleRemoveEvidence = () => {
    setEvidenceFileId(null)
    setEvidenceFileName(null)
  }

  const isBalanceInsufficient = selectedAccount && parsedPaymentAmount > accountBalanceNum
  const isAmountExceedingLiability = parsedPaymentAmount > remainingLiabilityNum

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)

    if (!obligation) return

    if (parsedPaymentAmount <= 0) {
      setErrorMessage('Số tiền nộp thuế phải lớn hơn 0.')
      return
    }

    if (parsedPaymentAmount > remainingLiabilityNum) {
      setErrorMessage(
        `Số tiền nộp (${formatVnd(parsedPaymentAmount)}) vượt quá số thuế còn phải nộp (${formatVnd(remainingLiabilityNum)}).`
      )
      return
    }

    if (!selectedAccountId) {
      setErrorMessage('Vui lòng chọn tài khoản nguồn chi tiền nộp thuế.')
      return
    }

    if (isBalanceInsufficient) {
      setErrorMessage(
        `Số dư tài khoản không đủ để thực hiện thanh toán (Hiện có: ${formatVnd(accountBalanceNum)}, Cần: ${formatVnd(parsedPaymentAmount)}).`
      )
      return
    }

    setIsSubmitting(true)

    try {
      const payload: PayTaxObligationPayload = {
        paymentDate,
        amount: parsedPaymentAmount,
        paymentMethod,
        financialAccountId: selectedAccountId,
        receiptNumber: receiptNumber.trim() || undefined,
        note: note.trim() || undefined,
        evidenceFileId: evidenceFileId || undefined,
      }

      const res = await financeApi.payTaxObligation(branchId, obligation.id, payload)
      onSuccess(res)
      onClose()
    } catch (err: unknown) {
      console.error('Failed to pay tax obligation:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string; title?: string } }
      }
      setErrorMessage(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          apiErr.response?.data?.title ||
          'Không thể ghi nhận nộp thuế. Vui lòng kiểm tra lại số dư và thông tin.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!isOpen || !obligation) return null

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
              <div className="w-9 h-9 rounded-xl bg-emerald-700 text-white flex items-center justify-center shadow-xs">
                <Receipt className="w-5 h-5 text-emerald-200" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
                  <span>Ghi Nhận Nộp Thuế Vào Ngân Sách</span>
                </h3>
                <p className="text-xs text-slate-400">
                  Mã nghĩa vụ: <strong className="text-white font-mono">{obligation.obligationCode}</strong> — {TAX_OBLIGATION_TYPE_LABELS[obligation.obligationType]}
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
            {/* Obligation Summary Cards */}
            <div className="grid grid-cols-3 gap-2.5 p-3 rounded-xl bg-slate-50 border border-slate-200">
              <div className="text-center p-2 rounded-lg bg-white border border-slate-100">
                <span className="text-[11px] text-slate-500 block">Số phải nộp</span>
                <span className="text-xs sm:text-sm font-bold text-slate-800 font-mono">
                  {formatVnd(obligation.amountDue)}
                </span>
              </div>
              <div className="text-center p-2 rounded-lg bg-white border border-slate-100">
                <span className="text-[11px] text-slate-500 block">Đã nộp trước</span>
                <span className="text-xs sm:text-sm font-bold text-emerald-700 font-mono">
                  {formatVnd(obligation.amountPaid)}
                </span>
              </div>
              <div className="text-center p-2 rounded-lg bg-amber-50 border border-amber-200">
                <span className="text-[11px] text-amber-700 font-semibold block">Còn phải nộp</span>
                <span className="text-xs sm:text-sm font-bold text-amber-900 font-mono">
                  {formatVnd(remainingLiabilityNum)}
                </span>
              </div>
            </div>

            {/* Error Banner */}
            {errorMessage && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-start gap-2.5 shadow-xs">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span className="font-medium">{errorMessage}</span>
              </div>
            )}

            {/* Row 1: Số tiền nộp & Ngày nộp */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
              {/* Số tiền thanh toán */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-slate-700">
                    Số tiền nộp (VNĐ) <span className="text-rose-500">*</span>
                  </label>
                  <button
                    type="button"
                    onClick={() => setAmountStr(remainingLiabilityNum.toLocaleString('vi-VN'))}
                    className="text-[11px] font-medium text-[#304638] hover:underline cursor-pointer"
                  >
                    Nộp toàn bộ
                  </button>
                </div>
                <div className="relative">
                  <Input
                    type="text"
                    placeholder="0"
                    value={amountStr}
                    onChange={handleAmountChange}
                    className={`text-right font-mono text-sm font-semibold pr-10 ${
                      isAmountExceedingLiability ? 'border-rose-500 text-rose-700' : 'text-emerald-800'
                    }`}
                    required
                  />
                  <span className="absolute right-3 top-2.5 text-xs font-medium text-slate-400">
                    ₫
                  </span>
                </div>
                {parsedPaymentAmount > 0 && (
                  <p className="text-[11px] font-mono text-emerald-700 text-right">
                    {formatVnd(parsedPaymentAmount)}
                  </p>
                )}
              </div>

              {/* Ngày nộp tiền */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-slate-500" />
                  <span>Ngày nộp tiền <span className="text-rose-500">*</span></span>
                </label>
                <Input
                  type="date"
                  value={paymentDate}
                  onChange={(e) => setPaymentDate(e.target.value)}
                  className="text-xs"
                  required
                />
              </div>
            </div>

            {/* Row 2: Phương thức thanh toán (BANK_TRANSFER vs CASH) */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700 block">
                Phương thức nộp thuế <span className="text-rose-500">*</span>
              </label>
              <div className="grid grid-cols-2 gap-2.5">
                <button
                  type="button"
                  onClick={() => handleMethodChange('BANK_TRANSFER')}
                  className={`p-3 rounded-xl border text-xs font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer ${
                    paymentMethod === 'BANK_TRANSFER'
                      ? 'border-[#304638] bg-[#304638] text-white shadow-xs'
                      : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  <CreditCard className="w-4 h-4" />
                  <span>Chuyển khoản / KBNN điện tử</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleMethodChange('CASH')}
                  className={`p-3 rounded-xl border text-xs font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer ${
                    paymentMethod === 'CASH'
                      ? 'border-[#304638] bg-[#304638] text-white shadow-xs'
                      : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  <Wallet className="w-4 h-4" />
                  <span>Tiền mặt tại kho bạc / ngân hàng</span>
                </button>
              </div>
            </div>

            {/* Row 3: Chọn tài khoản nguồn chi */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between text-xs">
                <label className="text-slate-700 font-semibold flex items-center gap-1.5">
                  <Building2 className="w-3.5 h-3.5 text-slate-500" />
                  <span>Tài khoản trích tiền <span className="text-rose-500">*</span></span>
                </label>
                {selectedAccount && (
                  <span className="text-[11px] font-mono text-slate-500">
                    Số dư hiện tại:{' '}
                    <strong className={isBalanceInsufficient ? 'text-rose-600' : 'text-emerald-700'}>
                      {formatVnd(accountBalanceNum)}
                    </strong>
                  </span>
                )}
              </div>
              <select
                value={selectedAccountId}
                onChange={(e) => setSelectedAccountId(e.target.value)}
                className={`w-full rounded-xl border px-3 py-2 text-xs font-medium focus:outline-none focus:ring-2 focus:ring-[#304638] ${
                  isBalanceInsufficient ? 'border-rose-400 bg-rose-50/50' : 'border-slate-300 bg-white text-slate-800'
                }`}
                required
              >
                {filteredAccounts.length === 0 ? (
                  <option value="">Không có tài khoản phù hợp ({paymentMethod === 'BANK_TRANSFER' ? 'Ngân hàng' : 'Tiền mặt'})</option>
                ) : (
                  filteredAccounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.accountName} ({a.accountKind === 'BANK' ? 'Ngân hàng' : 'Tiền mặt'}) — Số dư: {formatVnd(a.currentBalance)}
                    </option>
                  ))
                )}
              </select>
              {isBalanceInsufficient && (
                <p className="text-[11px] text-rose-600 font-medium">
                  Cảnh báo: Số dư tài khoản không đủ để chi trả số tiền này!
                </p>
              )}
            </div>

            {/* Row 4: Số biên lai / Mã UNC */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                <Receipt className="w-3.5 h-3.5 text-slate-500" />
                <span>Số biên lai / Số UNC / Mã giao dịch NSNN</span>
              </label>
              <Input
                type="text"
                placeholder="Ví dụ: UNC-20260415-099 hoặc GNT-778899"
                value={receiptNumber}
                onChange={(e) => setReceiptNumber(e.target.value)}
                className="font-mono text-xs"
              />
            </div>

            {/* Ghi chú */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">
                Ghi chú nộp thuế
              </label>
              <textarea
                rows={2}
                placeholder="Diễn giải chi tiết nội dung nộp thuế..."
                value={note}
                onChange={(e) => setNote(e.target.value)}
                className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent resize-none"
              />
            </div>

            {/* Chứng từ thanh toán (Evidence Dropzone: Giấy nộp tiền NSNN) */}
            <div className="space-y-2 pt-2 border-t border-slate-100">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Receipt className="w-3.5 h-3.5 text-slate-500" />
                  <span>Chứng từ nộp NSNN (Giấy nộp tiền / UNC có dấu ngân hàng)</span>
                </label>
                {!evidenceFileId && !showDropzone && (
                  <button
                    type="button"
                    onClick={() => setShowDropzone(true)}
                    className="text-xs font-medium text-[#304638] hover:text-[#25372C] flex items-center gap-1 cursor-pointer"
                  >
                    <UploadCloud className="w-3.5 h-3.5" />
                    <span>Tải chứng từ nộp</span>
                  </button>
                )}
              </div>

              {/* Uploaded File Info Card */}
              {evidenceFileId && (
                <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs text-emerald-900 shadow-xs">
                  <div className="flex items-center gap-2 min-w-0">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span className="font-medium truncate max-w-xs">
                      {evidenceFileName || 'Chứng từ nộp thuế đã tải lên'}
                    </span>
                    <Badge variant="outline" className="border-emerald-300 text-emerald-700 text-[10px] shrink-0">
                      Đã đính kèm
                    </Badge>
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

            {/* Ledger Impact Notice */}
            <div className="p-3 rounded-xl bg-blue-50/70 border border-blue-200 text-blue-900 text-xs flex items-start gap-2.5">
              <Info className="w-4 h-4 text-blue-600 shrink-0 mt-0.5" />
              <div>
                <span className="font-semibold">Định khoản tự động:</span> Khi xác nhận nộp, hệ thống sẽ tự động ghi sổ một bút toán xuất quỹ (OUTFLOW) mã nguồn <strong className="font-mono">TAX_PAYMENT</strong> vào Sổ quỹ tài khoản chi và giảm số dư tài khoản tương ứng.
              </div>
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
                disabled={isSubmitting || isBalanceInsufficient || isAmountExceedingLiability}
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-5 flex items-center gap-1.5"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>Đang trích tiền nộp thuế...</span>
                  </>
                ) : (
                  <>
                    <Receipt className="w-3.5 h-3.5" />
                    <span>Xác Nhận Nộp Thuế</span>
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
