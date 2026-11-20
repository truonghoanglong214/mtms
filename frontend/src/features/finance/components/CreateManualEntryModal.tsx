import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  FileSpreadsheet,
  ArrowDownLeft,
  ArrowUpRight,
  AlertCircle,
  CheckCircle2,
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
  FinancialCategoryDto,
  EntryType,
  CashLedgerEntryDto,
} from '../types/finance.types'
import type { EvidenceFileDto } from '@/features/evidence/types'

interface CreateManualEntryModalProps {
  isOpen: boolean
  branchId: string
  branchName?: string
  accounts: FinancialAccountDto[]
  categories: FinancialCategoryDto[]
  onClose: () => void
  onSuccess: (result: CashLedgerEntryDto) => void
}

export const CreateManualEntryModal: React.FC<CreateManualEntryModalProps> = ({
  isOpen,
  branchId,
  branchName,
  accounts,
  categories,
  onClose,
  onSuccess,
}) => {
  const [accountId, setAccountId] = useState<string>('')
  const [entryType, setEntryType] = useState<EntryType>('OUTFLOW')
  const [categoryId, setCategoryId] = useState<string>('')
  const [amountStr, setAmountStr] = useState<string>('')
  const [businessDate, setBusinessDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  )
  const [memo, setMemo] = useState<string>('')
  const [uploadedEvidence, setUploadedEvidence] = useState<EvidenceFileDto | null>(null)
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const activeAccounts = accounts.filter((a) => a.status === 'ACTIVE')
  const selectedAccount = activeAccounts.find((a) => a.id === accountId)

  // Filter categories matching selected flow direction
  const filteredCategories = categories.filter((c) => {
    if (!c.isActive) return false
    return c.flowDirection === entryType || c.flowDirection === 'BOTH'
  })

  // Reset fields on modal open
  useEffect(() => {
    if (isOpen) {
      setErrorMessage(null)
      setUploadedEvidence(null)
      setAmountStr('')
      setBusinessDate(new Date().toISOString().split('T')[0])
      setMemo('')

      if (activeAccounts.length > 0) {
        setAccountId(activeAccounts[0].id)
      }
    }
  }, [isOpen, activeAccounts])

  // Select first category when entryType changes
  useEffect(() => {
    if (filteredCategories.length > 0) {
      // Don't auto-reset if already matching
      const currentMatches = filteredCategories.some((c) => c.id === categoryId)
      if (!currentMatches) {
        setCategoryId(filteredCategories[0].id)
      }
    } else {
      setCategoryId('')
    }
  }, [entryType, filteredCategories, categoryId])

  if (!isOpen) return null

  const numericAmount = parseMoneyInput(amountStr)
  const accountBalance = selectedAccount
    ? typeof selectedAccount.currentBalance === 'string'
      ? parseFloat(selectedAccount.currentBalance) || 0
      : selectedAccount.currentBalance || 0
    : 0

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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)

    if (!accountId) {
      setErrorMessage('Vui lòng chọn tài khoản ghi sổ.')
      return
    }

    if (!categoryId) {
      setErrorMessage('Vui lòng chọn danh mục thu/chi.')
      return
    }

    if (numericAmount <= 0) {
      setErrorMessage('Số tiền giao dịch phải lớn hơn 0 ₫.')
      return
    }

    if (!businessDate) {
      setErrorMessage('Vui lòng chọn ngày kinh doanh.')
      return
    }

    if (!memo.trim()) {
      setErrorMessage('Vui lòng nhập lý do / diễn giải chi tiết cho bút toán.')
      return
    }

    try {
      setIsSubmitting(true)
      const res = await financeApi.recordEntry(branchId, {
        accountId,
        categoryId,
        entryType,
        amount: numericAmount,
        businessDate,
        memo: memo.trim(),
        evidenceFileId: uploadedEvidence?.id || null,
      })

      onSuccess(res)
      onClose()
    } catch (err: unknown) {
      console.error('Manual entry failed:', err)
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
          'Không thể ghi nhận bút toán trực tiếp. Vui lòng kiểm tra lại thông tin.'
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
                <FileSpreadsheet className="h-5 w-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-lg font-bold text-slate-900">
                    Ghi Sổ Dòng Tiền Trực Tiếp
                  </h2>
                  <Badge variant="secondary" className="font-mono text-xs">
                    P0-FIN-02
                  </Badge>
                  <Badge
                    variant="outline"
                    className="border-emerald-600 text-emerald-800 bg-emerald-50 text-xs font-mono"
                  >
                    TC-011
                  </Badge>
                </div>
                <p className="text-xs text-slate-500">
                  Chi nhánh: <span className="font-semibold text-slate-700">{branchName || branchId}</span> · Ghi nhận chi phí vận hành trực tiếp hoặc điều chỉnh vốn.
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

          {/* Form */}
          <form onSubmit={handleSubmit} className="p-6 space-y-5 overflow-y-auto flex-1">
            {/* Business Policy Banner */}
            <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-600 space-y-1">
              <div className="flex items-center gap-1.5 font-semibold text-slate-800">
                <ShieldCheck className="w-4 h-4 text-[#819B78]" />
                <span>Quy định ghi sổ trực tiếp (BR-FIN-01 / BR-FIN-09):</span>
              </div>
              <p className="leading-relaxed">
                Mọi bút toán ghi trực tiếp đều có tính bất biến. Giao dịch vốn góp / rút vốn chủ sở hữu không được tính vào doanh thu bán hàng hay chi phí vận hành P&L.
              </p>
            </div>

            {errorMessage && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-800">
                <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span>{errorMessage}</span>
              </div>
            )}

            {/* Entry Type Toggle (INFLOW / OUTFLOW) */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">
                Hướng dòng tiền *
              </label>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setEntryType('INFLOW')}
                  className={`p-3 rounded-xl border flex items-center justify-center gap-2 text-xs font-semibold transition-all cursor-pointer ${
                    entryType === 'INFLOW'
                      ? 'bg-emerald-50 border-emerald-500 text-emerald-800 ring-2 ring-emerald-500/20'
                      : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                  }`}
                >
                  <ArrowDownLeft className="w-4 h-4 text-emerald-600" />
                  <span>Dòng tiền vào (+ Thu tiền)</span>
                </button>

                <button
                  type="button"
                  onClick={() => setEntryType('OUTFLOW')}
                  className={`p-3 rounded-xl border flex items-center justify-center gap-2 text-xs font-semibold transition-all cursor-pointer ${
                    entryType === 'OUTFLOW'
                      ? 'bg-rose-50 border-rose-500 text-rose-800 ring-2 ring-rose-500/20'
                      : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'
                  }`}
                >
                  <ArrowUpRight className="w-4 h-4 text-rose-600" />
                  <span>Dòng tiền ra (- Chi tiền)</span>
                </button>
              </div>
            </div>

            {/* Account & Category Row */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Account */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 flex items-center justify-between">
                  <span>Tài khoản ghi sổ *</span>
                  {selectedAccount && (
                    <span className="font-mono text-xs font-bold text-slate-700">
                      Dư: {formatVnd(accountBalance)}
                    </span>
                  )}
                </label>
                <select
                  value={accountId}
                  onChange={(e) => setAccountId(e.target.value)}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                  required
                >
                  <option value="" disabled>
                    -- Chọn tài khoản --
                  </option>
                  {activeAccounts.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.accountName} ({a.accountKind}) — Dư: {formatVnd(a.currentBalance)}
                    </option>
                  ))}
                </select>
              </div>

              {/* Category */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700">
                  Danh mục thu/chi ({entryType === 'INFLOW' ? 'Thu' : 'Chi'}) *
                </label>
                <select
                  value={categoryId}
                  onChange={(e) => setCategoryId(e.target.value)}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                  required
                >
                  <option value="" disabled>
                    -- Chọn danh mục --
                  </option>
                  {filteredCategories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.categoryName} ({c.categoryCode})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Amount */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-700">
                Số tiền ({entryType === 'INFLOW' ? 'Thu' : 'Chi'}) (VND) *
              </label>

              <div className="relative">
                <Input
                  type="text"
                  placeholder="0"
                  value={amountStr}
                  onChange={handleAmountChange}
                  className={`text-lg font-mono font-bold pl-3 pr-10 ${
                    entryType === 'INFLOW'
                      ? 'text-emerald-700'
                      : 'text-rose-700'
                  }`}
                  required
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400">
                  ₫
                </span>
              </div>

              {/* Quick Amount Buttons */}
              <div className="flex flex-wrap items-center gap-1.5 pt-1">
                <span className="text-[11px] text-slate-400">Chọn nhanh:</span>
                {[500000, 1000000, 2000000, 5000000, 10000000].map((val) => (
                  <button
                    key={val}
                    type="button"
                    onClick={() => setFastAmount(val)}
                    className="px-2 py-0.5 rounded-md text-[11px] font-mono bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors cursor-pointer"
                  >
                    {formatVnd(val)}
                  </button>
                ))}
              </div>
            </div>

            {/* Date and Memo Row */}
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
                  Diễn giải / Lý do nghiệp vụ *
                </label>
                <Input
                  type="text"
                  placeholder="Ví dụ: Mua đá bi bổ sung phục vụ giờ cao điểm"
                  value={memo}
                  onChange={(e) => setMemo(e.target.value)}
                  className="text-xs"
                  required
                />
              </div>
            </div>

            {/* Evidence Dropzone */}
            <div className="space-y-2 pt-2 border-t border-slate-100">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <FileCheck className="w-3.5 h-3.5 text-[#304638]" />
                  <span>Chứng từ / Hóa đơn đính kèm (Tùy chọn)</span>
                </label>
                {uploadedEvidence && (
                  <Badge
                    variant="outline"
                    className="bg-emerald-50 text-emerald-700 border-emerald-200 text-xs font-medium"
                  >
                    Đã đính kèm: {uploadedEvidence.originalFilename}
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
                  !accountId ||
                  !categoryId ||
                  numericAmount <= 0 ||
                  !memo.trim()
                }
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-5 flex items-center gap-2"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>Đang ghi sổ...</span>
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>
                      Ghi nhận {entryType === 'INFLOW' ? 'Thu' : 'Chi'}{' '}
                      {formatVnd(numericAmount)}
                    </span>
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
