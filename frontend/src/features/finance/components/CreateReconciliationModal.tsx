import React, { useState, useEffect, useMemo } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Calendar,
  AlertTriangle,
  Scale,
  CheckCircle2,
  Loader2,
  UploadCloud,
  Building2,
  Info,
  TrendingUp,
  TrendingDown,
  Equal,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { financeApi, formatVnd, formatSignedVnd, parseMoneyInput } from '../api/financeApi'
import { EvidenceDropzone } from '@/features/evidence/components/EvidenceDropzone'
import type { EvidenceFileDto } from '@/features/evidence/types'
import type {
  FinancialAccountDto,
  CashReconciliationDto,
  CreateReconciliationPayload,
} from '../types/finance.types'

interface CreateReconciliationModalProps {
  isOpen: boolean
  branchId: string
  branchName?: string
  accounts: FinancialAccountDto[]
  onClose: () => void
  onSuccess: (reconciliation: CashReconciliationDto) => void
}

export const CreateReconciliationModal: React.FC<CreateReconciliationModalProps> = ({
  isOpen,
  branchId,
  branchName,
  accounts,
  onClose,
  onSuccess,
}) => {
  const [reconciliationDate, setReconciliationDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  )
  const [accountId, setAccountId] = useState<string>('')
  const [observedBalanceStr, setObservedBalanceStr] = useState<string>('')
  const [note, setNote] = useState<string>('')
  const [evidenceFileId, setEvidenceFileId] = useState<string | null>(null)
  const [evidenceFileName, setEvidenceFileName] = useState<string | null>(null)
  const [showDropzone, setShowDropzone] = useState<boolean>(false)

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Filter active accounts
  const activeAccounts = useMemo(() => {
    return accounts.filter((a) => a.status === 'ACTIVE' || !a.status)
  }, [accounts])

  // Selected account
  const selectedAccount = useMemo(() => {
    return activeAccounts.find((a) => a.id === accountId)
  }, [activeAccounts, accountId])

  // Recorded balance from account
  const recordedBalance = useMemo(() => {
    if (!selectedAccount) return 0
    if (typeof selectedAccount.currentBalance === 'string') {
      return parseFloat(selectedAccount.currentBalance) || 0
    }
    return selectedAccount.currentBalance || 0
  }, [selectedAccount])

  // Initialize defaults on open
  useEffect(() => {
    if (isOpen) {
      setReconciliationDate(new Date().toISOString().split('T')[0])
      setObservedBalanceStr('')
      setNote('')
      setEvidenceFileId(null)
      setEvidenceFileName(null)
      setShowDropzone(false)
      setErrorMessage(null)

      if (activeAccounts.length > 0) {
        // Default to first cash account, or first account
        const defaultCash = activeAccounts.find((a) => a.accountKind === 'CASH')
        setAccountId(defaultCash ? defaultCash.id : activeAccounts[0].id)
      }
    }
  }, [isOpen, activeAccounts])

  // When account changes, default observed balance to current recorded balance for convenience
  const handleAccountChange = (newAccId: string) => {
    setAccountId(newAccId)
    const acc = activeAccounts.find((a) => a.id === newAccId)
    if (acc) {
      const bal = typeof acc.currentBalance === 'string' ? parseFloat(acc.currentBalance) || 0 : acc.currentBalance || 0
      setObservedBalanceStr(bal.toLocaleString('vi-VN'))
    }
  }

  const observedBalance = parseMoneyInput(observedBalanceStr)
  const variance = observedBalance - recordedBalance

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
    if (!branchId) return
    setErrorMessage(null)

    if (!accountId) {
      setErrorMessage('Vui lòng chọn tài khoản kiểm kê.')
      return
    }

    if (!reconciliationDate) {
      setErrorMessage('Vui lòng chọn ngày đối soát.')
      return
    }

    if (observedBalanceStr.trim() === '') {
      setErrorMessage('Vui lòng nhập số dư kiểm kê thực tế.')
      return
    }

    try {
      setIsSubmitting(true)

      const payload: CreateReconciliationPayload = {
        accountId,
        reconciliationDate,
        observedBalance,
        note: note.trim() || undefined,
        evidenceFileId: evidenceFileId || undefined,
      }

      const res = await financeApi.createReconciliation(branchId, payload)
      onSuccess(res)
      onClose()
    } catch (err: unknown) {
      console.error('Failed to create reconciliation:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string; title?: string } }
      }
      setErrorMessage(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể ghi nhận đối soát quỹ. Có thể ngày này đã có bản ghi đối soát cho tài khoản đã chọn.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

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
            <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/70">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-[#304638]/10 text-[#304638] flex items-center justify-center font-bold">
                  <Scale className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    Đối Soát & Kiểm Kê Quỹ
                  </h3>
                  <p className="text-xs text-slate-500">
                    {branchName ? `Chi nhánh: ${branchName}` : 'Kiểm kê số dư thực tế'}
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

            {/* Form */}
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              {errorMessage && (
                <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-800 animate-in fade-in">
                  <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                  <span className="leading-relaxed">{errorMessage}</span>
                </div>
              )}

              {/* Account Selection */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Building2 className="w-3.5 h-3.5 text-slate-500" />
                  <span>Tài khoản kiểm kê <span className="text-rose-500">*</span></span>
                </label>
                <select
                  value={accountId}
                  onChange={(e) => handleAccountChange(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 font-medium focus:outline-none focus:ring-2 focus:ring-[#304638]"
                  required
                >
                  {activeAccounts.length === 0 ? (
                    <option value="">Không có tài khoản khả dụng</option>
                  ) : (
                    activeAccounts.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.accountName} ({a.accountKind === 'CASH' ? 'Tiền mặt tại quỹ' : 'Tài khoản ngân hàng'}) — Sổ sách: {formatVnd(a.currentBalance)}
                      </option>
                    ))
                  )}
                </select>
              </div>

              {/* Date & Recorded Balance Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {/* Ngày đối soát */}
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-slate-500" />
                    <span>Ngày đối soát <span className="text-rose-500">*</span></span>
                  </label>
                  <input
                    type="date"
                    value={reconciliationDate}
                    onChange={(e) => setReconciliationDate(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 font-mono focus:outline-none focus:ring-2 focus:ring-[#304638]"
                    required
                  />
                </div>

                {/* Số dư sổ sách (Readonly) */}
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                    <Info className="w-3.5 h-3.5 text-slate-500" />
                    <span>Số dư sổ sách hiện tại</span>
                  </label>
                  <div className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-mono font-bold text-slate-800">
                    {formatVnd(recordedBalance)}
                  </div>
                </div>
              </div>

              {/* Observed Balance Input */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between text-xs">
                  <label className="font-semibold text-slate-700">
                    Số dư kiểm kê thực tế (Observed) <span className="text-rose-500">*</span>
                  </label>
                  <span className="text-slate-400 text-[11px]">Đơn vị: VNĐ</span>
                </div>
                <div className="relative">
                  <input
                    type="text"
                    inputMode="numeric"
                    placeholder="Nhập số tiền kiểm đếm thực tế..."
                    value={observedBalanceStr}
                    onChange={(e) => {
                      const num = parseMoneyInput(e.target.value)
                      setObservedBalanceStr(num > 0 ? num.toLocaleString('vi-VN') : e.target.value)
                    }}
                    className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-sm font-mono font-bold text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                    required
                  />
                </div>
              </div>

              {/* Realtime Live Variance Display */}
              <div className="p-3.5 rounded-xl border transition-all">
                {observedBalanceStr.trim() === '' ? (
                  <div className="text-xs text-slate-400 flex items-center gap-2">
                    <Equal className="w-4 h-4 text-slate-400" />
                    <span>Nhập số dư thực tế để tính toán chênh lệch (Variance)</span>
                  </div>
                ) : variance === 0 ? (
                  <div className="flex items-center justify-between text-xs text-emerald-800 bg-emerald-50 -m-3.5 p-3.5 rounded-xl border border-emerald-200">
                    <div className="flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                      <span className="font-semibold">Khớp số dư (Không có chênh lệch)</span>
                    </div>
                    <Badge className="bg-emerald-600 text-white font-mono text-xs">0 ₫</Badge>
                  </div>
                ) : variance > 0 ? (
                  <div className="space-y-2 bg-emerald-50/60 -m-3.5 p-3.5 rounded-xl border border-emerald-200">
                    <div className="flex items-center justify-between text-xs text-emerald-900">
                      <div className="flex items-center gap-1.5 font-bold">
                        <TrendingUp className="w-4 h-4 text-emerald-600" />
                        <span>Thừa quỹ kiểm kê (Surplus)</span>
                      </div>
                      <Badge className="bg-emerald-600 text-white font-mono text-xs font-bold">
                        {formatSignedVnd(variance)}
                      </Badge>
                    </div>
                    <p className="text-[11px] text-emerald-700 leading-relaxed">
                      Tiền thực tế nhiều hơn sổ sách {formatVnd(variance)}. Hệ thống sẽ tạo bản ghi đối soát trạng thái <strong>CHỜ XỬ LÝ (OPEN)</strong> để xử lý.
                    </p>
                  </div>
                ) : (
                  <div className="space-y-2 bg-rose-50 -m-3.5 p-3.5 rounded-xl border border-rose-200">
                    <div className="flex items-center justify-between text-xs text-rose-900">
                      <div className="flex items-center gap-1.5 font-bold">
                        <TrendingDown className="w-4 h-4 text-rose-600" />
                        <span>Thiếu quỹ kiểm kê (Shortage)</span>
                      </div>
                      <Badge className="bg-rose-600 text-white font-mono text-xs font-bold">
                        {formatSignedVnd(variance)}
                      </Badge>
                    </div>
                    <div className="p-2 rounded-lg bg-rose-100/70 border border-rose-200 text-[11px] text-rose-800 flex items-start gap-1.5">
                      <AlertTriangle className="w-3.5 h-3.5 text-rose-600 shrink-0 mt-0.5" />
                      <span>
                        <strong>Cảnh báo chênh lệch:</strong> Chênh lệch thiếu {formatVnd(Math.abs(variance))} sẽ tạo bản ghi đối soát trạng thái <strong>CHỜ XỬ LÝ</strong>. Không biến mất âm thầm và yêu cầu giải trình hoặc duyệt điều chỉnh sổ sách.
                      </span>
                    </div>
                  </div>
                )}
              </div>

              {/* Note Textarea */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700">
                  Ghi chú / Diễn giải đợt kiểm kê (Tùy chọn)
                </label>
                <textarea
                  rows={2}
                  placeholder="Ví dụ: Kiểm đếm quỹ tiền mặt cuối ngày ca tối 18/11..."
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638] resize-none"
                />
              </div>

              {/* Evidence Upload */}
              <div className="space-y-2 pt-1 border-t border-slate-100">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                    <UploadCloud className="w-3.5 h-3.5 text-slate-500" />
                    <span>Biên bản kiểm quỹ / Sao kê ngân hàng</span>
                  </label>
                  {!evidenceFileId && !showDropzone && (
                    <button
                      type="button"
                      onClick={() => setShowDropzone(true)}
                      className="text-xs font-medium text-[#304638] hover:text-[#25372C] flex items-center gap-1 cursor-pointer"
                    >
                      <UploadCloud className="w-3.5 h-3.5" />
                      <span>Tải biên bản / sao kê</span>
                    </button>
                  )}
                </div>

                {evidenceFileId && (
                  <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs text-emerald-900 shadow-xs">
                    <div className="flex items-center gap-2 min-w-0">
                      <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                      <span className="font-medium truncate max-w-xs">
                        {evidenceFileName || 'Biên bản kiểm kê đã đính kèm'}
                      </span>
                      <Badge variant="outline" className="border-emerald-300 text-emerald-700 text-[10px] shrink-0">
                        Đã lưu chứng từ
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
                  Hủy bỏ
                </Button>
                <Button
                  type="submit"
                  disabled={isSubmitting || !accountId || observedBalanceStr.trim() === ''}
                  className="bg-[#304638] hover:bg-[#25372C] text-white rounded-xl text-xs font-semibold px-4 h-9 shadow-xs flex items-center gap-2 cursor-pointer"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      <span>Đang ghi nhận...</span>
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>Lưu đối soát quỹ</span>
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
