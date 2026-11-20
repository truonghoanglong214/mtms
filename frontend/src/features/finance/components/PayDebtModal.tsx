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
  FileCheck,
  ShieldCheck,
  Receipt,
  Wallet,
  Landmark,
  ArrowRight,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { EvidenceDropzone } from '@/features/evidence/components/EvidenceDropzone'
import { financeApi, formatVnd, parseMoneyInput } from '../api/financeApi'
import type {
  PayableDto,
  FinancialAccountDto,
  PaymentMethod,
  PayablePaymentDto,
} from '../types/finance.types'
import type { EvidenceFileDto } from '@/features/evidence/types'

interface PayDebtModalProps {
  isOpen: boolean
  branchId: string
  payable: PayableDto | null
  accounts: FinancialAccountDto[]
  onClose: () => void
  onSuccess: (payment: PayablePaymentDto) => void
}

export const PayDebtModal: React.FC<PayDebtModalProps> = ({
  isOpen,
  branchId,
  payable,
  accounts,
  onClose,
  onSuccess,
}) => {
  const [selectedAccountId, setSelectedAccountId] = useState<string>('')
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('BANK_TRANSFER')
  const [amountStr, setAmountStr] = useState<string>('')
  const [paidOn, setPaidOn] = useState<string>(
    new Date().toISOString().split('T')[0]
  )
  const [bankReference, setBankReference] = useState<string>('')
  const [uploadedEvidence, setUploadedEvidence] = useState<EvidenceFileDto | null>(null)
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Calculate remaining debt
  const remainingDebtNum = useMemo(() => {
    if (!payable) return 0
    if (payable.remainingDebt !== undefined) {
      return typeof payable.remainingDebt === 'string'
        ? parseFloat(payable.remainingDebt) || 0
        : payable.remainingDebt
    }
    if (payable.remainingAmount !== undefined) {
      return typeof payable.remainingAmount === 'string'
        ? parseFloat(payable.remainingAmount) || 0
        : payable.remainingAmount
    }
    const orig = typeof payable.originalAmount === 'string' ? parseFloat(payable.originalAmount) || 0 : payable.originalAmount
    const paid = typeof payable.paidAmount === 'string' ? parseFloat(payable.paidAmount) || 0 : payable.paidAmount
    return Math.max(0, orig - paid)
  }, [payable])

  // Initialize or reset form when modal opens
  useEffect(() => {
    if (isOpen && payable) {
      // Auto-select first active bank or cash account
      const defaultAcc = accounts.find((a) => a.accountKind === 'BANK') || accounts[0]
      setSelectedAccountId(defaultAcc ? defaultAcc.id : '')
      setPaymentMethod(defaultAcc?.accountKind === 'CASH' ? 'CASH' : 'BANK_TRANSFER')
      setAmountStr(remainingDebtNum.toLocaleString('vi-VN'))
      setPaidOn(new Date().toISOString().split('T')[0])
      setBankReference('')
      setUploadedEvidence(null)
      setErrorMessage(null)
    }
  }, [isOpen, payable, accounts, remainingDebtNum])

  const selectedAccount = useMemo(() => {
    return accounts.find((a) => a.id === selectedAccountId)
  }, [accounts, selectedAccountId])

  const accountBalanceNum = useMemo(() => {
    if (!selectedAccount) return 0
    return typeof selectedAccount.currentBalance === 'string'
      ? parseFloat(selectedAccount.currentBalance) || 0
      : selectedAccount.currentBalance
  }, [selectedAccount])

  const parsedAmount = useMemo(() => {
    return parseMoneyInput(amountStr)
  }, [amountStr])

  // When account changes, update recommended payment method
  const handleAccountChange = (accId: string) => {
    setSelectedAccountId(accId)
    const acc = accounts.find((a) => a.id === accId)
    if (acc) {
      if (acc.accountKind === 'CASH') {
        setPaymentMethod('CASH')
      } else {
        setPaymentMethod('BANK_TRANSFER')
      }
    }
  }

  const handleSetQuickPercentage = (ratio: number) => {
    const val = Math.round(remainingDebtNum * ratio)
    setAmountStr(val.toLocaleString('vi-VN'))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!payable || !branchId) return

    setErrorMessage(null)

    if (!selectedAccountId) {
      setErrorMessage('Vui lòng chọn tài khoản nguồn thanh toán.')
      return
    }

    if (parsedAmount <= 0) {
      setErrorMessage('Số tiền thanh toán phải lớn hơn 0 ₫.')
      return
    }

    if (parsedAmount > remainingDebtNum) {
      setErrorMessage(
        `Số tiền thanh toán (${formatVnd(parsedAmount)}) không được vượt quá số nợ còn lại (${formatVnd(remainingDebtNum)}).`
      )
      return
    }

    if (accountBalanceNum < parsedAmount) {
      setErrorMessage(
        `Số dư tài khoản (${formatVnd(accountBalanceNum)}) không đủ để thanh toán ${formatVnd(parsedAmount)}.`
      )
      return
    }

    try {
      setIsSubmitting(true)

      const result = await financeApi.settlePayable(branchId, payable.id, {
        accountId: selectedAccountId,
        amount: parsedAmount,
        paidOn,
        paymentMethod,
        bankReference: bankReference.trim() || undefined,
        evidenceFileId: uploadedEvidence?.id || undefined,
      })

      onSuccess(result)
      onClose()
    } catch (err: unknown) {
      console.error('Failed to settle payable:', err)
      const apiErr = err as {
        response?: {
          data?: {
            title?: string
            detail?: string
            message?: string
            type?: string
          }
        }
      }
      const problem = apiErr.response?.data
      if (problem?.type?.includes('payment-exceeds-remaining-debt')) {
        setErrorMessage('Lỗi: Số tiền thanh toán vượt quá số nợ còn lại của nhà cung cấp.')
      } else if (problem?.type?.includes('insufficient-account-balance')) {
        setErrorMessage('Lỗi: Số dư tài khoản thanh toán không đủ để chi trả.')
      } else if (problem?.type?.includes('payable-already-settled')) {
        setErrorMessage('Lỗi: Khoản nợ này đã được thanh toán toàn bộ trước đó.')
      } else {
        setErrorMessage(
          problem?.detail ||
            problem?.message ||
            problem?.title ||
            'Không thể thực hiện thanh toán nợ. Vui lòng kiểm tra kết nối.'
        )
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!isOpen || !payable) return null

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 10 }}
          transition={{ duration: 0.2, ease: 'easeOut' }}
          className="relative w-full max-w-2xl bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col max-h-[92vh]"
        >
          {/* Header */}
          <div className="px-6 py-4.5 bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 text-white flex items-center justify-between border-b border-slate-700">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-amber-500/20 text-amber-300 border border-amber-500/30">
                <CreditCard className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-bold tracking-tight text-white flex items-center gap-2">
                  <span>Thanh Toán Công Nợ Nhà Cung Cấp</span>
                  <Badge variant="outline" className="border-amber-400 text-amber-300 bg-amber-950/40 font-mono text-[10px]">
                    TC-009
                  </Badge>
                </h3>
                <p className="text-xs text-slate-300 mt-0.5">
                  Chi trả nợ NCC ({payable.counterpartyLabel}) từ quỹ tiền mặt hoặc ngân hàng
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-700/50 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Form Body */}
          <form onSubmit={handleSubmit} className="p-6 overflow-y-auto space-y-5">
            {/* TC-009 No Double Cost Policy Banner */}
            <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-200 flex items-start gap-3">
              <ShieldCheck className="w-5 h-5 text-emerald-700 shrink-0 mt-0.5" />
              <div className="text-xs text-emerald-900 leading-relaxed">
                <span className="font-bold">Quy tắc TC-009 (No Double Cost): </span>
                Thanh toán nợ NCC làm giảm tiền quỹ và giảm nghĩa vụ nợ phải trả. Bút toán này{' '}
                <span className="font-semibold text-emerald-950 underline">không tính trùng chi phí</span> vào báo cáo lãi lỗ P&L vì giá vốn nguyên vật liệu đã hoặc sẽ được ghi nhận khi xuất kho nấu/pha chế.
              </div>
            </div>

            {/* Error Message */}
            {errorMessage && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-800 animate-in fade-in">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span className="font-medium">{errorMessage}</span>
              </div>
            )}

            {/* Payable Info Summary Box */}
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
              <div>
                <span className="text-slate-500 block text-[11px]">Nhà cung cấp / Đối tác:</span>
                <span className="font-bold text-slate-800 text-sm flex items-center gap-1.5 mt-0.5">
                  <Building2 className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                  <span className="truncate">{payable.counterpartyLabel}</span>
                </span>
              </div>
              <div>
                <span className="text-slate-500 block text-[11px]">Nợ gốc ban đầu:</span>
                <span className="font-bold text-slate-700 text-sm mt-0.5 block font-mono">
                  {formatVnd(payable.originalAmount)}
                </span>
              </div>
              <div>
                <span className="text-amber-700 font-semibold block text-[11px]">Còn phải trả hiện tại:</span>
                <span className="font-bold text-amber-700 text-base mt-0.5 block font-mono">
                  {formatVnd(remainingDebtNum)}
                </span>
              </div>
            </div>

            {/* Account Selection */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700 flex items-center justify-between">
                <span className="flex items-center gap-1.5">
                  <Wallet className="w-3.5 h-3.5 text-[#304638]" />
                  <span>Tài khoản trích tiền *</span>
                </span>
                {selectedAccount && (
                  <span className="text-[11px] font-normal text-slate-500">
                    Số dư: <span className="font-bold font-mono text-slate-700">{formatVnd(accountBalanceNum)}</span>
                  </span>
                )}
              </label>

              <select
                value={selectedAccountId}
                onChange={(e) => handleAccountChange(e.target.value)}
                className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-xs text-slate-800 font-medium focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent cursor-pointer"
                required
              >
                <option value="">-- Chọn tài khoản thanh toán --</option>
                {accounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.accountName} ({acc.accountKind === 'CASH' ? 'Tiền mặt' : 'Ngân hàng'}) — Số dư: {formatVnd(acc.currentBalance)}
                  </option>
                ))}
              </select>
            </div>

            {/* Payment Method and Date Row */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Landmark className="w-3.5 h-3.5 text-slate-500" />
                  <span>Hình thức thanh toán *</span>
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setPaymentMethod('BANK_TRANSFER')}
                    className={`px-3 py-2 text-xs font-semibold rounded-xl border transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                      paymentMethod === 'BANK_TRANSFER'
                        ? 'bg-[#304638] text-white border-[#304638] shadow-xs'
                        : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <Landmark className="w-3.5 h-3.5" />
                    <span>Chuyển khoản</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setPaymentMethod('CASH')}
                    className={`px-3 py-2 text-xs font-semibold rounded-xl border transition-all cursor-pointer flex items-center justify-center gap-1.5 ${
                      paymentMethod === 'CASH'
                        ? 'bg-[#304638] text-white border-[#304638] shadow-xs'
                        : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <Wallet className="w-3.5 h-3.5" />
                    <span>Tiền mặt</span>
                  </button>
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-slate-500" />
                  <span>Ngày thanh toán *</span>
                </label>
                <Input
                  type="date"
                  value={paidOn}
                  onChange={(e) => setPaidOn(e.target.value)}
                  className="text-xs font-mono rounded-xl h-10"
                  required
                />
              </div>
            </div>

            {/* Amount Input & Quick Shortcuts */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-slate-700">
                  Số tiền thanh toán nợ *
                </label>
                <span className="text-[11px] text-slate-500 font-mono">
                  {formatVnd(parsedAmount)}
                </span>
              </div>

              <div className="relative">
                <Input
                  type="text"
                  value={amountStr}
                  onChange={(e) => setAmountStr(e.target.value)}
                  placeholder="Nhập số tiền..."
                  className="text-base font-mono font-bold pr-10 rounded-xl h-11 text-slate-900 border-slate-300 focus:border-[#304638]"
                  required
                />
                <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400">
                  ₫
                </span>
              </div>

              {/* Quick Percentage Buttons */}
              <div className="flex items-center gap-2 pt-1">
                <span className="text-[11px] text-slate-400">Chọn nhanh:</span>
                <button
                  type="button"
                  onClick={() => handleSetQuickPercentage(0.5)}
                  className="px-2.5 py-1 rounded-lg text-xs font-medium bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors cursor-pointer"
                >
                  50% ({formatVnd(Math.round(remainingDebtNum * 0.5))})
                </button>
                <button
                  type="button"
                  onClick={() => handleSetQuickPercentage(1)}
                  className="px-2.5 py-1 rounded-lg text-xs font-semibold bg-amber-50 hover:bg-amber-100 text-amber-800 border border-amber-200 transition-colors cursor-pointer flex items-center gap-1"
                >
                  <CheckCircle2 className="w-3.5 h-3.5 text-amber-700" />
                  <span>100% (Tất toán: {formatVnd(remainingDebtNum)})</span>
                </button>
              </div>
            </div>

            {/* Bank Reference (for BANK_TRANSFER) */}
            {paymentMethod === 'BANK_TRANSFER' && (
              <div className="space-y-1.5 animate-in fade-in">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Receipt className="w-3.5 h-3.5 text-slate-500" />
                  <span>Mã giao dịch ngân hàng / Ủy nhiệm chi (Tùy chọn)</span>
                </label>
                <Input
                  type="text"
                  value={bankReference}
                  onChange={(e) => setBankReference(e.target.value)}
                  placeholder="Ví dụ: FT2611189999 hoặc MB-1029384"
                  className="text-xs font-mono rounded-xl h-9"
                />
              </div>
            )}

            {/* Evidence Dropzone */}
            <div className="space-y-2 pt-2 border-t border-slate-100">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <FileCheck className="w-3.5 h-3.5 text-[#304638]" />
                  <span>Chứng từ thanh toán / Biên lai ngân hàng (Tùy chọn)</span>
                </label>
                {uploadedEvidence && (
                  <Badge
                    variant="outline"
                    className="bg-emerald-50 text-emerald-700 border-emerald-200 text-xs font-medium"
                  >
                    Đã tải lên: {uploadedEvidence.originalFilename}
                  </Badge>
                )}
              </div>

              <EvidenceDropzone
                branchId={branchId}
                defaultVisibility="BRANCH_PRIVATE"
                disabled={isSubmitting}
                onUploadSuccess={(file) => setUploadedEvidence(file)}
              />
            </div>

            {/* Footer Buttons */}
            <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={isSubmitting}
                className="text-xs rounded-xl cursor-pointer"
              >
                Hủy bỏ
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting || parsedAmount <= 0}
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-4 py-2 rounded-xl flex items-center gap-2 shadow-xs cursor-pointer disabled:opacity-50"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang ghi sổ thanh toán...</span>
                  </>
                ) : (
                  <>
                    <span>Xác nhận thanh toán</span>
                    <ArrowRight className="w-4 h-4" />
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
