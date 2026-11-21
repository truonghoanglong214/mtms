import React, { useState, useEffect, useMemo } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Plus,
  Calendar,
  AlertTriangle,
  Receipt,
  FileText,
  CreditCard,
  Wallet,
  Building2,
  CheckCircle2,
  Loader2,
  UploadCloud,
  ShieldAlert,
  Info,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { financeApi, formatVnd, parseMoneyInput } from '../api/financeApi'
import { EvidenceDropzone } from '@/features/evidence/components/EvidenceDropzone'
import type { EvidenceFileDto } from '@/features/evidence/types'
import type {
  FinancialAccountDto,
  FinancialCategoryDto,
  ExpenseDto,
  CreateExpenseRequest,
} from '../types/finance.types'

interface CreateExpenseModalProps {
  isOpen: boolean
  branchId: string
  branchName?: string
  accounts: FinancialAccountDto[]
  categories: FinancialCategoryDto[]
  onClose: () => void
  onSuccess: (expense: ExpenseDto) => void
}

const DEFAULT_ALERT_THRESHOLD = 2000000 // 2,000,000 VND (BR-FIN-07)
const EVIDENCE_RECOMMENDED_THRESHOLD = 500000 // 500,000 VND

export const CreateExpenseModal: React.FC<CreateExpenseModalProps> = ({
  isOpen,
  branchId,
  branchName,
  accounts,
  categories,
  onClose,
  onSuccess,
}) => {
  const [expenseDate, setExpenseDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  )
  const [categoryId, setCategoryId] = useState<string>('')
  const [amountStr, setAmountStr] = useState<string>('')
  const [isPaid, setIsPaid] = useState<boolean>(true)
  const [paymentAccountId, setPaymentAccountId] = useState<string>('')
  const [reason, setReason] = useState<string>('')
  const [evidenceFileId, setEvidenceFileId] = useState<string | null>(null)
  const [evidenceFileName, setEvidenceFileName] = useState<string | null>(null)
  const [showDropzone, setShowDropzone] = useState<boolean>(false)

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [submitMode, setSubmitMode] = useState<'draft' | 'confirm' | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Filter OPEX categories
  const opexCategories = useMemo(() => {
    return categories.filter(
      (c) => c.categoryGroup === 'OPERATING_EXPENSE' && (c.isActive ?? true)
    )
  }, [categories])

  // Active payment accounts for branch
  const activeAccounts = useMemo(() => {
    return accounts.filter((a) => a.status === 'ACTIVE' || !a.status)
  }, [accounts])

  // Initialize defaults when modal opens
  useEffect(() => {
    if (isOpen) {
      setExpenseDate(new Date().toISOString().split('T')[0])
      setAmountStr('')
      setReason('')
      setEvidenceFileId(null)
      setEvidenceFileName(null)
      setShowDropzone(false)
      setErrorMessage(null)
      setIsPaid(true)

      if (opexCategories.length > 0) {
        setCategoryId(opexCategories[0].id)
      } else if (categories.length > 0) {
        setCategoryId(categories[0].id)
      }

      if (activeAccounts.length > 0) {
        // Prefer cash account by default
        const cashAcc = activeAccounts.find((a) => a.accountKind === 'CASH')
        setPaymentAccountId(cashAcc ? cashAcc.id : activeAccounts[0].id)
      }
    }
  }, [isOpen, opexCategories, categories, activeAccounts])

  const parsedAmount = parseMoneyInput(amountStr)
  const isLargeExpense = parsedAmount >= DEFAULT_ALERT_THRESHOLD
  const isEvidenceRecommended = parsedAmount >= EVIDENCE_RECOMMENDED_THRESHOLD

  // Selected account detail and balance check
  const selectedAccount = useMemo(() => {
    return activeAccounts.find((a) => a.id === paymentAccountId)
  }, [activeAccounts, paymentAccountId])

  const accountBalance = selectedAccount
    ? typeof selectedAccount.currentBalance === 'string'
      ? parseFloat(selectedAccount.currentBalance) || 0
      : selectedAccount.currentBalance
    : 0

  const isBalanceInsufficient = isPaid && parsedAmount > 0 && accountBalance < parsedAmount

  const handleQuickAmount = (val: number) => {
    setAmountStr(val.toLocaleString('vi-VN'))
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

  const handleSubmit = async (confirmImmediately: boolean) => {
    if (!branchId) return
    setErrorMessage(null)

    if (!expenseDate) {
      setErrorMessage('Vui lòng chọn ngày phát sinh chi phí.')
      return
    }

    if (!categoryId) {
      setErrorMessage('Vui lòng chọn danh mục chi phí vận hành (OPEX).')
      return
    }

    if (parsedAmount <= 0) {
      setErrorMessage('Số tiền chi phải lớn hơn 0 ₫.')
      return
    }

    if (isPaid && !paymentAccountId) {
      setErrorMessage('Vui lòng chọn tài khoản thanh toán tiền mặt hoặc ngân hàng.')
      return
    }

    if (isPaid && confirmImmediately && isBalanceInsufficient) {
      setErrorMessage(
        `Số dư tài khoản thanh toán (${formatVnd(accountBalance)}) không đủ để duyệt khoản chi ${formatVnd(parsedAmount)}. Vui lòng nạp thêm tiền quỹ hoặc chọn hình thức "Ghi nợ trả sau".`
      )
      return
    }

    if (!reason.trim()) {
      setErrorMessage('Vui lòng nhập diễn giải lý do chi tiêu.')
      return
    }

    try {
      setIsSubmitting(true)
      setSubmitMode(confirmImmediately ? 'confirm' : 'draft')

      const req: CreateExpenseRequest = {
        expenseDate,
        categoryId,
        amount: parsedAmount,
        isPaid,
        paymentAccountId: isPaid ? paymentAccountId : undefined,
        evidenceFileId: evidenceFileId || undefined,
        reason: reason.trim(),
        confirmImmediately,
      }

      const res = await financeApi.createExpense(branchId, req)
      onSuccess(res)
      onClose()
    } catch (err: unknown) {
      console.error('Failed to create expense:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string; title?: string } }
      }
      setErrorMessage(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          apiErr.response?.data?.title ||
          'Không thể tạo phiếu chi. Vui lòng kiểm tra lại thông tin.'
      )
    } finally {
      setIsSubmitting(false)
      setSubmitMode(null)
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
                <Receipt className="w-5 h-5 text-emerald-400" />
              </div>
              <div>
                <h3 className="text-base font-bold text-white tracking-tight flex items-center gap-2">
                  <span>Lập Phiếu Chi Vận Hành (OPEX)</span>
                </h3>
                <p className="text-xs text-slate-400">
                  {branchName ? `Chi nhánh: ${branchName}` : 'Ghi nhận chi phí hoạt động trực tiếp'}
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

          {/* Form Content */}
          <div className="p-5 sm:p-6 space-y-4 max-h-[80vh] overflow-y-auto">
            {/* Error Notice */}
            {errorMessage && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-800 shadow-xs">
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span className="flex-1 font-medium leading-relaxed">{errorMessage}</span>
                <button
                  type="button"
                  onClick={() => setErrorMessage(null)}
                  className="text-rose-500 hover:text-rose-700"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            )}

            {/* Threshold Alert Banner (Realtime BR-FIN-07) */}
            {isLargeExpense && (
              <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-300 flex items-start gap-2.5 text-xs text-amber-900 shadow-xs animate-in fade-in duration-200">
                <ShieldAlert className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
                <div className="space-y-0.5">
                  <p className="font-bold text-amber-950 flex items-center gap-1.5">
                    <span>Khoản chi lớn cần Chủ quán phê duyệt</span>
                    <Badge className="bg-amber-500 text-white text-[10px] py-0 px-1.5">
                      review_flag = true
                    </Badge>
                  </p>
                  <p className="text-[11px] text-amber-800 leading-normal">
                    Số tiền chi vượt hạn mức cảnh báo (≥ 2.000.000 ₫). Phiếu chi sẽ được bật cờ rà soát đặc biệt cho Chủ quán theo dõi.
                  </p>
                </div>
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Ngày chi */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-slate-500" />
                  <span>Ngày chi phí <span className="text-rose-500">*</span></span>
                </label>
                <input
                  type="date"
                  value={expenseDate}
                  onChange={(e) => setExpenseDate(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 font-mono focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent"
                  required
                />
              </div>

              {/* Danh mục chi phí (OPEX Only) */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 flex items-center justify-between">
                  <span className="flex items-center gap-1.5">
                    <FileText className="w-3.5 h-3.5 text-slate-500" />
                    <span>Danh mục chi phí <span className="text-rose-500">*</span></span>
                  </span>
                  <Badge variant="secondary" className="text-[10px] font-mono text-slate-600">
                    OPEX
                  </Badge>
                </label>
                <select
                  value={categoryId}
                  onChange={(e) => setCategoryId(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 font-medium focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent"
                  required
                >
                  {opexCategories.length === 0 ? (
                    categories.length === 0 ? (
                      <option value="">Chưa có danh mục</option>
                    ) : (
                      categories.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.categoryName} ({c.categoryCode})
                        </option>
                      ))
                    )
                  ) : (
                    opexCategories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.categoryName} ({c.categoryCode})
                      </option>
                    ))
                  )}
                </select>
              </div>
            </div>

            {/* Số tiền chi */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-700">
                  Số tiền chi (VND) <span className="text-rose-500">*</span>
                </label>
                {parsedAmount > 0 && (
                  <span className="text-xs font-bold text-[#B96849] font-mono">
                    {formatVnd(parsedAmount)}
                  </span>
                )}
              </div>
              <div className="relative">
                <Input
                  type="text"
                  placeholder="Nhập số tiền chi, ví dụ: 350.000"
                  value={amountStr}
                  onChange={(e) => setAmountStr(e.target.value)}
                  className="text-sm font-semibold font-mono h-10 pr-10"
                  required
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-bold text-slate-400">
                  ₫
                </span>
              </div>

              {/* Quick Amount Buttons */}
              <div className="flex items-center gap-1.5 flex-wrap pt-0.5">
                <span className="text-[11px] text-slate-400">Gợi ý nhanh:</span>
                {[50000, 100000, 200000, 500000, 1000000, 2000000, 5000000].map((val) => (
                  <button
                    key={val}
                    type="button"
                    onClick={() => handleQuickAmount(val)}
                    className="px-2 py-0.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-[11px] font-mono font-medium text-slate-700 transition-colors cursor-pointer"
                  >
                    {val >= 1000000 ? `${val / 1000000}M` : `${val / 1000}k`}
                  </button>
                ))}
              </div>
            </div>

            {/* Hình thức thanh toán: Thanh toán ngay vs Ghi nợ trả sau */}
            <div className="p-3.5 rounded-xl border border-slate-200 bg-slate-50/70 space-y-3">
              <label className="text-xs font-semibold text-slate-700 block">
                Hình thức thanh toán
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setIsPaid(true)}
                  className={`py-2 px-3 rounded-xl border text-xs font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer ${
                    isPaid
                      ? 'border-[#304638] bg-[#304638] text-white shadow-xs'
                      : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-100'
                  }`}
                >
                  <Wallet className="w-4 h-4" />
                  <span>Đã thanh toán ngay</span>
                </button>
                <button
                  type="button"
                  onClick={() => setIsPaid(false)}
                  className={`py-2 px-3 rounded-xl border text-xs font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer ${
                    !isPaid
                      ? 'border-amber-600 bg-amber-600 text-white shadow-xs'
                      : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-100'
                  }`}
                >
                  <CreditCard className="w-4 h-4" />
                  <span>Ghi nợ trả sau</span>
                </button>
              </div>

              {/* Mode A: Thanh toán ngay -> Chọn tài khoản thanh toán */}
              {isPaid ? (
                <div className="space-y-1.5 pt-1 animate-in fade-in duration-150">
                  <div className="flex items-center justify-between text-xs">
                    <label className="text-slate-600 font-medium flex items-center gap-1.5">
                      <Building2 className="w-3.5 h-3.5 text-slate-400" />
                      <span>Tài khoản chi tiền <span className="text-rose-500">*</span></span>
                    </label>
                    {selectedAccount && (
                      <span className="text-[11px] font-mono text-slate-500">
                        Số dư hiện tại:{' '}
                        <strong className={isBalanceInsufficient ? 'text-rose-600' : 'text-emerald-700'}>
                          {formatVnd(accountBalance)}
                        </strong>
                      </span>
                    )}
                  </div>
                  <select
                    value={paymentAccountId}
                    onChange={(e) => setPaymentAccountId(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 font-medium focus:outline-none focus:ring-2 focus:ring-[#304638]"
                    required={isPaid}
                  >
                    {activeAccounts.length === 0 ? (
                      <option value="">Không có tài khoản khả dụng</option>
                    ) : (
                      activeAccounts.map((a) => (
                        <option key={a.id} value={a.id}>
                          {a.accountName} ({a.accountKind === 'CASH' ? 'Tiền mặt tại quỹ' : 'Tài khoản ngân hàng'}) — {formatVnd(a.currentBalance)}
                        </option>
                      ))
                    )}
                  </select>

                  {isBalanceInsufficient && (
                    <p className="text-[11px] text-rose-600 flex items-center gap-1 font-medium mt-1">
                      <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
                      <span>Cảnh báo: Số dư quỹ không đủ để chi. Bút toán sẽ bị từ chối nếu duyệt ngay.</span>
                    </p>
                  )}
                </div>
              ) : (
                /* Mode B: Ghi nợ chi phí */
                <div className="p-2.5 rounded-lg bg-amber-50 border border-amber-200 text-xs text-amber-800 flex items-start gap-2">
                  <Info className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                  <p className="text-[11px] leading-relaxed">
                    Khoản chi này sẽ tự động tạo một khoản nợ phải trả trong <strong>Sổ Công nợ NCC</strong> và chưa trừ tiền quỹ ngay tại thời điểm này.
                  </p>
                </div>
              )}
            </div>

            {/* Diễn giải lý do chi */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">
                Diễn giải lý do chi tiêu <span className="text-rose-500">*</span>
              </label>
              <textarea
                rows={2}
                placeholder="Ví dụ: Mua đá viên và túi ly dùng cho ca chiều ngày 18/11"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent resize-none"
                required
              />
            </div>

            {/* Hóa đơn & Chứng từ đính kèm (Evidence file upload) */}
            <div className="space-y-2 pt-1 border-t border-slate-100">
              <div className="flex items-center justify-between">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Receipt className="w-3.5 h-3.5 text-slate-500" />
                  <span>Hóa đơn / Biên lai thanh toán</span>
                  {isEvidenceRecommended && (
                    <span className="text-[10px] text-amber-600 font-medium">
                      (Khoản chi ≥ 500k nên có hóa đơn)
                    </span>
                  )}
                </label>
                {!evidenceFileId && !showDropzone && (
                  <button
                    type="button"
                    onClick={() => setShowDropzone(true)}
                    className="text-xs font-medium text-[#304638] hover:text-[#25372C] flex items-center gap-1 cursor-pointer"
                  >
                    <UploadCloud className="w-3.5 h-3.5" />
                    <span>Tải ảnh hóa đơn</span>
                  </button>
                )}
              </div>

              {/* Uploaded File Info Card */}
              {evidenceFileId && (
                <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs text-emerald-900 shadow-xs">
                  <div className="flex items-center gap-2 min-w-0">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span className="font-medium truncate max-w-xs">
                      {evidenceFileName || 'Hóa đơn đã đính kèm'}
                    </span>
                    <Badge variant="outline" className="border-emerald-300 text-emerald-700 text-[10px] shrink-0">
                      Đã lưu chứng từ
                    </Badge>
                  </div>
                  <button
                    type="button"
                    onClick={handleRemoveEvidence}
                    className="text-slate-400 hover:text-rose-600 p-1 cursor-pointer"
                    title="Gỡ hóa đơn"
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
          </div>

          {/* Footer Actions */}
          <div className="px-5 py-4 bg-slate-50 border-t border-slate-200 flex flex-col-reverse sm:flex-row items-center justify-end gap-2.5">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={isSubmitting}
              className="w-full sm:w-auto text-xs font-semibold rounded-xl cursor-pointer"
            >
              Hủy bỏ
            </Button>

            {/* Action 1: Lưu bản nháp */}
            <Button
              type="button"
              variant="outline"
              onClick={() => handleSubmit(false)}
              disabled={isSubmitting}
              className="w-full sm:w-auto border-slate-300 text-slate-700 hover:bg-slate-100 text-xs font-semibold rounded-xl cursor-pointer"
            >
              {isSubmitting && submitMode === 'draft' ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin mr-1.5" />
                  <span>Đang lưu nháp...</span>
                </>
              ) : (
                <span>Lưu bản nháp</span>
              )}
            </Button>

            {/* Action 2: Duyệt chi ngay */}
            <Button
              type="button"
              onClick={() => handleSubmit(true)}
              disabled={isSubmitting || (isPaid && isBalanceInsufficient)}
              className="w-full sm:w-auto bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold rounded-xl shadow-xs flex items-center justify-center gap-1.5 cursor-pointer"
            >
              {isSubmitting && submitMode === 'confirm' ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin mr-1.5" />
                  <span>Đang duyệt chi...</span>
                </>
              ) : (
                <>
                  <Plus className="w-4 h-4" />
                  <span>Duyệt chi ngay</span>
                </>
              )}
            </Button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
export default CreateExpenseModal
