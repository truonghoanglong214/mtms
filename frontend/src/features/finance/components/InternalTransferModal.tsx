import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  ArrowRightLeft,
  AlertCircle,
  Loader2,
  Calendar,
  FileCheck,
  ShieldCheck,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { EvidenceDropzone } from '@/features/evidence/components/EvidenceDropzone'
import {
  financeApi,
  formatVnd,
  parseMoneyInput,
} from '../api/financeApi'
import type {
  FinancialAccountDto,
  InternalTransferResponse,
} from '../types/finance.types'
import type { EvidenceFileDto } from '@/features/evidence/types'

interface InternalTransferModalProps {
  isOpen: boolean
  branchId: string
  branchName?: string
  accounts: FinancialAccountDto[]
  onClose: () => void
  onSuccess: (result: InternalTransferResponse) => void
}

export const InternalTransferModal: React.FC<InternalTransferModalProps> = ({
  isOpen,
  branchId,
  branchName,
  accounts,
  onClose,
  onSuccess,
}) => {
  const [fromAccountId, setFromAccountId] = useState<string>('')
  const [toAccountId, setToAccountId] = useState<string>('')
  const [amountStr, setAmountStr] = useState<string>('')
  const [businessDate, setBusinessDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  )
  const [note, setNote] = useState<string>('')
  const [uploadedEvidence, setUploadedEvidence] = useState<EvidenceFileDto | null>(null)
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Initialize accounts on open
  useEffect(() => {
    if (isOpen) {
      setErrorMessage(null)
      setUploadedEvidence(null)
      setAmountStr('')
      setBusinessDate(new Date().toISOString().split('T')[0])
      setNote('')

      const cashAccount = accounts.find((a) => a.accountKind === 'CASH' && a.status === 'ACTIVE')
      const bankAccount = accounts.find((a) => a.accountKind === 'BANK' && a.status === 'ACTIVE')

      if (cashAccount) {
        setFromAccountId(cashAccount.id)
      } else if (accounts.length > 0) {
        setFromAccountId(accounts[0].id)
      }

      if (bankAccount) {
        setToAccountId(bankAccount.id)
      } else if (accounts.length > 1) {
        setToAccountId(accounts[1].id)
      }
    }
  }, [isOpen, accounts])

  if (!isOpen) return null

  const activeAccounts = accounts.filter((a) => a.status === 'ACTIVE')
  const fromAccount = activeAccounts.find((a) => a.id === fromAccountId)
  const toAccount = activeAccounts.find((a) => a.id === toAccountId)

  const sourceBalance = fromAccount
    ? typeof fromAccount.currentBalance === 'string'
      ? parseFloat(fromAccount.currentBalance) || 0
      : fromAccount.currentBalance || 0
    : 0

  const numericAmount = parseMoneyInput(amountStr)

  const isInsufficientBalance = numericAmount > 0 && numericAmount > sourceBalance

  const handleAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value.replace(/[^\d]/g, '')
    if (!val) {
      setAmountStr('')
      return
    }
    const num = parseInt(val, 10)
    setAmountStr(new Intl.NumberFormat('vi-VN').format(num))
  }

  const setFastAmount = (value: number) => {
    setAmountStr(new Intl.NumberFormat('vi-VN').format(value))
  }

  const handleFillAllBalance = () => {
    if (sourceBalance > 0) {
      setAmountStr(new Intl.NumberFormat('vi-VN').format(Math.floor(sourceBalance)))
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)

    if (!fromAccountId || !toAccountId) {
      setErrorMessage('Vui lòng chọn đầy đủ tài khoản nguồn và tài khoản đích.')
      return
    }

    if (fromAccountId === toAccountId) {
      setErrorMessage('Tài khoản nguồn và tài khoản đích không được trùng nhau.')
      return
    }

    if (numericAmount <= 0) {
      setErrorMessage('Số tiền chuyển khoản phải lớn hơn 0 ₫.')
      return
    }

    if (isInsufficientBalance) {
      setErrorMessage(
        `Số dư tài khoản chuyển không đủ. Hiện có: ${formatVnd(sourceBalance)}.`
      )
      return
    }

    if (!businessDate) {
      setErrorMessage('Vui lòng chọn ngày kinh doanh ghi nhận.')
      return
    }

    try {
      setIsSubmitting(true)
      const res = await financeApi.recordTransfer(branchId, {
        fromAccountId,
        toAccountId,
        amount: numericAmount,
        businessDate,
        note: note.trim() || undefined,
        evidenceFileId: uploadedEvidence?.id || null,
      })

      onSuccess(res)
      onClose()
    } catch (err: unknown) {
      console.error('Transfer failed:', err)
      const apiErr = err as {
        response?: {
          data?: {
            title?: string
            detail?: string
            message?: string
          }
        }
      }
      setErrorMessage(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể thực hiện chuyển tiền nội bộ. Vui lòng kiểm tra lại thông tin.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto bg-black/50 backdrop-blur-xs">
        <motion.div
          initial={{ opacity: 0, scale: 0.98, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.98, y: 10 }}
          transition={{ duration: 0.18 }}
          className="relative w-full max-w-2xl rounded-2xl bg-white shadow-2xl border border-slate-200 overflow-hidden my-6 flex flex-col max-h-[92vh]"
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-slate-50/70 shrink-0">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#304638] text-white shadow-xs">
                <ArrowRightLeft className="h-5 w-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg font-bold text-slate-900">
                    Chuyển Tiền Nội Bộ Chi Nhánh
                  </h2>
                </div>
                <p className="text-xs text-slate-500">
                  Chi nhánh: <span className="font-semibold text-slate-700">{branchName || branchId}</span> · Cân đối tiền mặt két và ngân hàng.
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="rounded-lg p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
              aria-label="Đóng"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Form Body */}
          <form onSubmit={handleSubmit} className="p-6 space-y-5 overflow-y-auto flex-1">
            {/* TC-010 Business Invariant Banner */}
            <div className="p-4 rounded-xl bg-emerald-50/70 border border-emerald-200/80 flex items-start gap-3 text-xs leading-relaxed text-emerald-900">
              <ShieldCheck className="w-5 h-5 text-emerald-700 shrink-0 mt-0.5" />
              <div>
                <strong className="font-semibold block text-emerald-950">
                  Quy định kế toán bất biến:
                </strong>
                Chuyển quỹ nội bộ cân đối tiền giữa két và ngân hàng, bảo toàn tổng tài sản tiền và không làm thay đổi doanh thu hay chi phí trên báo cáo lãi lỗ (P&L).
              </div>
            </div>

            {errorMessage && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-800">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span>{errorMessage}</span>
              </div>
            )}

            {/* Account Selection Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Source Account (from) */}
              <div className="space-y-1.5 p-3.5 rounded-xl border border-slate-200 bg-slate-50/50">
                <label className="text-xs font-semibold text-slate-700 flex items-center justify-between">
                  <span>Tài khoản chuyển (Nguồn đi) *</span>
                  {fromAccount && (
                    <span className="font-mono text-emerald-700 font-bold text-[11px]">
                      Dư: {formatVnd(sourceBalance)}
                    </span>
                  )}
                </label>
                <select
                  value={fromAccountId}
                  onChange={(e) => setFromAccountId(e.target.value)}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                  required
                >
                  <option value="" disabled>
                    -- Chọn tài khoản chuyển --
                  </option>
                  {activeAccounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.accountName} ({a.accountKind}) — Dư: {formatVnd(a.currentBalance)}
                    </option>
                  ))}
                </select>
                <p className="text-[11px] text-slate-500">
                  Tiền sẽ được trừ khỏi tài khoản này ngay lập tức.
                </p>
              </div>

              {/* Destination Account (to) */}
              <div className="space-y-1.5 p-3.5 rounded-xl border border-slate-200 bg-slate-50/50">
                <label className="text-xs font-semibold text-slate-700 flex items-center justify-between">
                  <span>Tài khoản nhận (Đích đến) *</span>
                  {toAccount && (
                    <span className="font-mono text-sky-700 font-bold text-[11px]">
                      Dư hiện tại: {formatVnd(toAccount.currentBalance)}
                    </span>
                  )}
                </label>
                <select
                  value={toAccountId}
                  onChange={(e) => setToAccountId(e.target.value)}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                  required
                >
                  <option value="" disabled>
                    -- Chọn tài khoản nhận --
                  </option>
                  {activeAccounts
                    .filter((a) => a.id !== fromAccountId)
                    .map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.accountName} ({a.accountKind}) — Dư: {formatVnd(a.currentBalance)}
                      </option>
                    ))}
                </select>
                <p className="text-[11px] text-slate-500">
                  Tiền sẽ được cộng vào tài khoản này với cùng số tiền.
                </p>
              </div>
            </div>

            {/* Transfer Amount */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-700">
                  Số tiền chuyển (VND) *
                </label>
                {isInsufficientBalance && (
                  <span className="text-xs text-rose-600 font-medium flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5" />
                    Số dư không đủ để thực hiện giao dịch!
                  </span>
                )}
              </div>

              <div className="relative">
                <Input
                  type="text"
                  placeholder="0"
                  value={amountStr}
                  onChange={handleAmountChange}
                  className={`text-lg font-mono font-bold pl-3 pr-10 ${
                    isInsufficientBalance
                      ? 'border-rose-400 focus-visible:ring-rose-400 bg-rose-50/30'
                      : 'border-slate-200'
                  }`}
                  required
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400">
                  ₫
                </span>
              </div>

              {/* Fast Amount Selection Pills */}
              <div className="flex flex-wrap items-center gap-1.5 pt-1">
                <span className="text-[11px] text-slate-400">Chọn nhanh:</span>
                {[1000000, 2000000, 5000000, 10000000].map((val) => (
                  <button
                    key={val}
                    type="button"
                    onClick={() => setFastAmount(val)}
                    className="px-2 py-0.5 rounded-md text-[11px] font-mono bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors cursor-pointer"
                  >
                    {formatVnd(val)}
                  </button>
                ))}
                {sourceBalance > 0 && (
                  <button
                    type="button"
                    onClick={handleFillAllBalance}
                    className="px-2 py-0.5 rounded-md text-[11px] font-mono bg-emerald-100 hover:bg-emerald-200 text-emerald-800 font-semibold transition-colors cursor-pointer"
                  >
                    Toàn bộ số dư két ({formatVnd(sourceBalance)})
                  </button>
                )}
              </div>
            </div>

            {/* Date and Note Row */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-slate-500" />
                  <span>Ngày kinh doanh *</span>
                </label>
                <Input
                  type="date"
                  value={businessDate}
                  onChange={(e) => setBusinessDate(e.target.value)}
                  className="text-xs font-mono"
                  required
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700">
                  Ghi chú / Diễn giải
                </label>
                <Input
                  type="text"
                  placeholder="Ví dụ: Nộp tiền mặt bán hàng ca sáng vào Techcombank"
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  className="text-xs"
                />
              </div>
            </div>

            {/* Evidence Dropzone */}
            <div className="space-y-2 pt-2 border-t border-slate-100">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <FileCheck className="w-3.5 h-3.5 text-[#304638]" />
                  <span>Biên lai nộp tiền / Ủy nhiệm chi (Tùy chọn)</span>
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
                className="text-xs"
              >
                Hủy bỏ
              </Button>
              <Button
                type="submit"
                disabled={
                  isSubmitting ||
                  !fromAccountId ||
                  !toAccountId ||
                  fromAccountId === toAccountId ||
                  numericAmount <= 0 ||
                  isInsufficientBalance
                }
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-5 flex items-center gap-2"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>Đang ghi sổ giao dịch...</span>
                  </>
                ) : (
                  <>
                    <ArrowRightLeft className="w-3.5 h-3.5" />
                    <span>Xác nhận chuyển {formatVnd(numericAmount)}</span>
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
