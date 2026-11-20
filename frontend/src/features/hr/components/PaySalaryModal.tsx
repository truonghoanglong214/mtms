import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  DollarSign,
  Wallet,
  Building2,
  Calendar,
  AlertTriangle,
  Loader2,
  ShieldCheck,
  CheckCircle2,
  FileText,
  CreditCard,
  Banknote,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { Input } from '@/shared/components/ui/input'
import { financeApi } from '@/features/finance/api/financeApi'
import type { FinancialAccountDto } from '@/features/finance/types/finance.types'
import { paymentApi } from '../api/paymentApi'
import { formatVND } from '../api/payrollApi'
import type { PayrollLine } from '../types/payroll.types'
import type { PaymentMethod } from '../types/payment.types'

interface PaySalaryModalProps {
  isOpen: boolean
  line: PayrollLine | null
  branchId: string
  onClose: () => void
  onSuccess: () => void
}

export const PaySalaryModal: React.FC<PaySalaryModalProps> = ({
  isOpen,
  line,
  branchId,
  onClose,
  onSuccess,
}) => {
  const [accounts, setAccounts] = useState<FinancialAccountDto[]>([])
  const [selectedAccountId, setSelectedAccountId] = useState<string>('')
  const [amount, setAmount] = useState<number | string>('')
  const [paidOn, setPaidOn] = useState<string>(() => new Date().toISOString().split('T')[0])
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('BANK_TRANSFER')
  const [externalReference, setExternalReference] = useState<string>('')
  const [note, setNote] = useState<string>('')

  const [isLoadingAccounts, setIsLoadingAccounts] = useState<boolean>(false)
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Derived metrics
  const grossSalary = line ? Number(line.grossSalary) || 0 : 0
  const paidAmount = line ? Number(line.paidAmount) || 0 : 0
  const remainingDebt = Math.max(0, grossSalary - paidAmount)

  // Load branch accounts on modal open
  useEffect(() => {
    if (!isOpen || !branchId) return

    let isMounted = true
    setIsLoadingAccounts(true)
    setErrorMessage(null)

    financeApi
      .getAccounts(branchId)
      .then((accs) => {
        if (!isMounted) return
        const activeAccounts = (accs || []).filter((a) => a.status !== 'INACTIVE')
        setAccounts(activeAccounts)

        // Default selection: Prefer Bank Account, else first available
        if (activeAccounts.length > 0) {
          const bankAcc = activeAccounts.find((a) => a.accountKind === 'BANK')
          const chosen = bankAcc || activeAccounts[0]
          setSelectedAccountId(chosen.id)
          setPaymentMethod(chosen.accountKind === 'CASH' ? 'CASH' : 'BANK_TRANSFER')
        }
      })
      .catch((err) => {
        console.error('Failed to load financial accounts:', err)
        if (isMounted) {
          setErrorMessage('Không thể tải danh sách tài khoản quỹ của chi nhánh.')
        }
      })
      .finally(() => {
        if (isMounted) setIsLoadingAccounts(false)
      })

    return () => {
      isMounted = false
    }
  }, [isOpen, branchId])

  // Initialize fields when line changes
  useEffect(() => {
    if (line) {
      setAmount(remainingDebt > 0 ? remainingDebt : '')
      setExternalReference(`UNC-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${line.staffCode}`)
      setNote(`Chi trả lương kỳ cho nhân viên ${line.employeeName} (${line.staffCode})`)
      setPaidOn(new Date().toISOString().split('T')[0])
    }
  }, [line, remainingDebt])

  if (!isOpen || !line) return null

  const selectedAccount = accounts.find((a) => a.id === selectedAccountId)
  const availableBalance = selectedAccount ? Number(selectedAccount.currentBalance) || 0 : 0
  const numericAmount = Number(amount) || 0

  // Validation
  const isAmountOverRemaining = numericAmount > remainingDebt
  const isAmountOverBalance = numericAmount > availableBalance
  const isInvalidAmount = numericAmount <= 0 || isAmountOverRemaining

  const handleAccountChange = (accId: string) => {
    setSelectedAccountId(accId)
    const acc = accounts.find((a) => a.id === accId)
    if (acc) {
      setPaymentMethod(acc.accountKind === 'CASH' ? 'CASH' : 'BANK_TRANSFER')
    }
  }

  const handleFillFullRemaining = () => {
    setAmount(remainingDebt)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)

    if (!selectedAccountId) {
      setErrorMessage('Vui lòng chọn tài khoản nguồn chi trả (Tiền mặt hoặc Ngân hàng).')
      return
    }

    if (numericAmount <= 0) {
      setErrorMessage('Số tiền chi trả phải lớn hơn 0 ₫.')
      return
    }

    if (numericAmount > remainingDebt) {
      setErrorMessage(
        `Số tiền chi trả vượt quá số dư lương còn nợ (${formatVND(remainingDebt)}).`
      )
      return
    }

    if (numericAmount > availableBalance) {
      setErrorMessage(
        `Số dư khả dụng của tài khoản "${selectedAccount?.accountName}" (${formatVND(
          availableBalance
        )}) không đủ để chi trả số tiền ${formatVND(numericAmount)}.`
      )
      return
    }

    setIsSubmitting(true)
    try {
      await paymentApi.paySalary(branchId, line.id, {
        accountId: selectedAccountId,
        amount: numericAmount,
        paidOn,
        paymentMethod,
        externalReference: externalReference.trim() || undefined,
        note: note.trim() || undefined,
      })

      onSuccess()
      onClose()
    } catch (err: unknown) {
      console.error('Failed to pay salary:', err)
      const errResponse = (
        err as {
          response?: {
            data?: {
              detail?: string
              message?: string
              type?: string
            }
          }
        }
      )?.response?.data

      if (errResponse?.type === 'urn:mtms:error:payroll-payment-exceeds-remaining') {
        setErrorMessage('Lỗi hạch toán: Số tiền chi trả vượt quá mức lương còn nợ của nhân viên.')
      } else if (errResponse?.type === 'urn:mtms:error:insufficient-funds') {
        setErrorMessage('Lỗi tài khoản: Số dư quỹ tiền mặt/ngân hàng không đủ để chi trả.')
      } else if (errResponse?.type === 'urn:mtms:error:payroll-not-approved') {
        setErrorMessage('Từ chối: Bảng lương chưa được phê duyệt APPROVED không thể chi trả.')
      } else {
        setErrorMessage(
          errResponse?.detail ||
            errResponse?.message ||
            'Không thể thực hiện chi trả lương. Vui lòng kiểm tra lại số dư và kết nối.'
        )
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-xs">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="bg-white rounded-3xl border border-stone-200 shadow-2xl max-w-xl w-full overflow-hidden flex flex-col max-h-[92vh]"
        >
          {/* Header */}
          <div className="px-6 py-4 border-b border-stone-200 bg-stone-50/80 flex items-center justify-between shrink-0">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-2xl bg-emerald-100 text-emerald-800 flex items-center justify-center">
                <DollarSign className="h-5 w-5" />
              </div>
              <div>
                <div className="flex items-center space-x-2">
                  <h3 className="font-bold text-stone-900 text-base">
                    Lập phiếu chi trả lương (P0-HR-07)
                  </h3>
                  <Badge variant="outline" className="border-emerald-300 text-emerald-800 text-[10px]">
                    TC-016 Invariant
                  </Badge>
                </div>
                <p className="text-xs text-stone-500">
                  Xuất quỹ tiền mặt/ngân hàng & liên kết bút toán sổ quỹ bất biến
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="p-1.5 rounded-xl text-stone-400 hover:text-stone-700 hover:bg-stone-200 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Body */}
          <form onSubmit={handleSubmit} className="p-6 space-y-4 overflow-y-auto flex-1">
            {/* Employee Summary Card */}
            <div className="bg-stone-50 border border-stone-200 rounded-2xl p-4 space-y-3">
              <div className="flex items-center justify-between border-b border-stone-200 pb-2.5">
                <div className="flex items-center space-x-2">
                  <div className="w-8 h-8 rounded-full bg-[#819B78]/20 text-[#304638] flex items-center justify-center font-bold text-xs shrink-0">
                    {line.employeeName.charAt(0)}
                  </div>
                  <div>
                    <span className="font-bold text-sm text-stone-900 block">
                      {line.employeeName}
                    </span>
                    <span className="text-xs text-stone-500 font-mono">
                      Mã NV: {line.staffCode} · {line.positionName}
                    </span>
                  </div>
                </div>

                <Badge variant="secondary" className="text-xs">
                  {line.rateType === 'HOURLY' ? 'Lương giờ' : 'Lương tháng'}
                </Badge>
              </div>

              {/* 3 Metric stats */}
              <div className="grid grid-cols-3 gap-2 pt-1 text-center">
                <div className="p-2 rounded-xl bg-white border border-stone-200">
                  <span className="text-[10px] text-stone-500 block uppercase font-medium">
                    Lương thực lĩnh
                  </span>
                  <span className="text-xs font-bold font-mono text-stone-900">
                    {formatVND(grossSalary)}
                  </span>
                </div>
                <div className="p-2 rounded-xl bg-white border border-stone-200">
                  <span className="text-[10px] text-stone-500 block uppercase font-medium">
                    Đã thanh toán
                  </span>
                  <span className="text-xs font-bold font-mono text-emerald-700">
                    {formatVND(paidAmount)}
                  </span>
                </div>
                <div className="p-2 rounded-xl bg-amber-50/80 border border-amber-200">
                  <span className="text-[10px] text-amber-800 block uppercase font-bold">
                    Còn nợ cần chi
                  </span>
                  <span className="text-xs font-bold font-mono text-amber-900">
                    {formatVND(remainingDebt)}
                  </span>
                </div>
              </div>
            </div>

            {/* Error Banner */}
            {errorMessage && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl flex items-start space-x-2.5 text-rose-800 text-xs">
                <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
                <div className="flex-1 font-medium">{errorMessage}</div>
              </div>
            )}

            {/* Account Selector */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-stone-700 flex items-center justify-between">
                <span className="flex items-center space-x-1.5">
                  <Wallet className="h-3.5 w-3.5 text-[#304638]" />
                  <span>Tài khoản xuất quỹ thanh toán *</span>
                </span>
                {isLoadingAccounts && (
                  <span className="text-[11px] text-stone-400 flex items-center space-x-1 font-normal">
                    <Loader2 className="h-3 w-3 animate-spin" />
                    <span>Đang tải số dư...</span>
                  </span>
                )}
              </label>

              <select
                value={selectedAccountId}
                onChange={(e) => handleAccountChange(e.target.value)}
                disabled={isLoadingAccounts || isSubmitting}
                className="w-full h-10 px-3 bg-white border border-stone-300 rounded-xl text-xs font-medium text-stone-900 focus:outline-none focus:ring-2 focus:ring-[#304638]"
              >
                {accounts.length === 0 ? (
                  <option value="">(Chi nhánh chưa có tài khoản quỹ hoạt động)</option>
                ) : (
                  accounts.map((acc) => (
                    <option key={acc.id} value={acc.id}>
                      {acc.accountName} [{acc.accountKind === 'CASH' ? 'Quỹ tiền mặt' : 'Ngân hàng'}] —
                      Số dư khả dụng: {formatVND(Number(acc.currentBalance) || 0)}
                    </option>
                  ))
                )}
              </select>

              {selectedAccount && (
                <div className="flex items-center justify-between text-[11px] text-stone-500 px-1 pt-0.5">
                  <span>
                    Loại quỹ:{' '}
                    <strong>{selectedAccount.accountKind === 'CASH' ? 'Tiền mặt' : 'Ngân hàng'}</strong>
                  </span>
                  <span>
                    Số dư khả dụng:{' '}
                    <strong
                      className={
                        isAmountOverBalance ? 'text-rose-700 font-bold' : 'text-emerald-700'
                      }
                    >
                      {formatVND(availableBalance)}
                    </strong>
                  </span>
                </div>
              )}
            </div>

            {/* Amount Input & Quick Shortcut */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-stone-700 flex items-center space-x-1.5">
                  <DollarSign className="h-3.5 w-3.5 text-[#304638]" />
                  <span>Số tiền chi trả (VND) *</span>
                </label>
                <button
                  type="button"
                  onClick={handleFillFullRemaining}
                  disabled={remainingDebt <= 0 || isSubmitting}
                  className="text-[11px] font-semibold text-[#304638] hover:underline flex items-center space-x-1"
                >
                  <CheckCircle2 className="h-3 w-3" />
                  <span>Chi toàn bộ số còn nợ ({formatVND(remainingDebt)})</span>
                </button>
              </div>

              <div className="relative">
                <Input
                  type="number"
                  min="1"
                  max={remainingDebt}
                  step="1000"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="Nhập số tiền cần chi trả..."
                  disabled={isSubmitting}
                  className={`h-10 text-sm font-mono font-bold pl-3 pr-12 ${
                    isAmountOverRemaining || isAmountOverBalance
                      ? 'border-rose-400 focus-visible:ring-rose-500'
                      : 'border-stone-300'
                  }`}
                />
                <span className="absolute right-3 top-2.5 text-xs font-semibold text-stone-400">
                  VNĐ
                </span>
              </div>

              {isAmountOverRemaining && (
                <p className="text-[11px] text-rose-600 font-medium">
                  Số tiền chi không được vượt quá số nợ còn lại ({formatVND(remainingDebt)}).
                </p>
              )}

              {!isAmountOverRemaining && isAmountOverBalance && (
                <p className="text-[11px] text-rose-600 font-medium">
                  Số tiền chi vượt quá số dư khả dụng của tài khoản ({formatVND(availableBalance)}).
                </p>
              )}
            </div>

            {/* Paid On Date & Payment Method */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-stone-700 flex items-center space-x-1.5">
                  <Calendar className="h-3.5 w-3.5 text-stone-500" />
                  <span>Ngày thực chi (paidOn) *</span>
                </label>
                <Input
                  type="date"
                  value={paidOn}
                  onChange={(e) => setPaidOn(e.target.value)}
                  disabled={isSubmitting}
                  className="h-9 text-xs border-stone-300"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-bold text-stone-700 flex items-center space-x-1.5">
                  <CreditCard className="h-3.5 w-3.5 text-stone-500" />
                  <span>Phương thức *</span>
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setPaymentMethod('BANK_TRANSFER')}
                    className={`h-9 px-2.5 rounded-xl border text-xs font-medium flex items-center justify-center space-x-1.5 transition-all ${
                      paymentMethod === 'BANK_TRANSFER'
                        ? 'border-[#304638] bg-[#304638]/10 text-[#304638] font-bold'
                        : 'border-stone-200 text-stone-600 hover:bg-stone-50'
                    }`}
                  >
                    <Building2 className="h-3.5 w-3.5" />
                    <span>Chuyển khoản</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setPaymentMethod('CASH')}
                    className={`h-9 px-2.5 rounded-xl border text-xs font-medium flex items-center justify-center space-x-1.5 transition-all ${
                      paymentMethod === 'CASH'
                        ? 'border-[#304638] bg-[#304638]/10 text-[#304638] font-bold'
                        : 'border-stone-200 text-stone-600 hover:bg-stone-50'
                    }`}
                  >
                    <Banknote className="h-3.5 w-3.5" />
                    <span>Tiền mặt</span>
                  </button>
                </div>
              </div>
            </div>

            {/* External Reference / UNC */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-stone-700 flex items-center space-x-1.5">
                <FileText className="h-3.5 w-3.5 text-stone-500" />
                <span>Mã chứng từ / Mã UNC chuyển khoản</span>
              </label>
              <Input
                type="text"
                value={externalReference}
                onChange={(e) => setExternalReference(e.target.value)}
                placeholder="VD: UNC-20261205-001 hoặc số phiếu chi..."
                disabled={isSubmitting}
                className="h-9 text-xs border-stone-300 font-mono"
              />
            </div>

            {/* Note */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-stone-700">
                Ghi chú nội dung thanh toán
              </label>
              <Input
                type="text"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Ghi chú chi lương (đợt 1, tất toán...)"
                disabled={isSubmitting}
                className="h-9 text-xs border-stone-300"
              />
            </div>

            {/* Invariant Alert TC-016 */}
            <div className="p-3.5 bg-emerald-50/80 border border-emerald-200 rounded-2xl flex items-start space-x-2.5 text-emerald-900 text-xs">
              <ShieldCheck className="h-4.5 w-4.5 text-emerald-700 shrink-0 mt-0.5" />
              <div className="space-y-0.5 leading-relaxed">
                <span className="font-bold text-emerald-800 block">
                  Kiểm soát hạch toán bất biến TC-016 (Accrual vs Cash Flow)
                </span>
                <span className="text-emerald-700 text-[11px] block">
                  Dòng tiền xuất quỹ sẽ được ghi nhận vào ngày thực chi (<strong>{paidOn}</strong>).
                  Hệ thống tuyệt đối <strong>không sửa đổi hồi tố</strong> số dư tiền mặt/ngân hàng của
                  kỳ tháng trước.
                </span>
              </div>
            </div>

            {/* Footer Buttons */}
            <div className="pt-2 flex items-center justify-end space-x-2">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={isSubmitting}
                className="h-9 px-4 text-xs border-stone-300"
              >
                Hủy bỏ
              </Button>

              <Button
                type="submit"
                disabled={isSubmitting || isInvalidAmount || isAmountOverBalance || !selectedAccountId}
                className="h-9 px-5 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-semibold shadow-xs"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />
                    <span>Đang xuất quỹ...</span>
                  </>
                ) : (
                  <>
                    <DollarSign className="h-3.5 w-3.5 mr-1.5" />
                    <span>Xác nhận xuất quỹ chi lương</span>
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
