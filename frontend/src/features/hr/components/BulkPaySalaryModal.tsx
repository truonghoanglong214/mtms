import React, { useState, useEffect, useMemo } from 'react'
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
  CheckSquare,
  Square,
  Users,
  CreditCard,
  Banknote,
  CheckCircle2,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { financeApi } from '@/features/finance/api/financeApi'
import type { FinancialAccountDto } from '@/features/finance/types/finance.types'
import { paymentApi } from '../api/paymentApi'
import { formatVND } from '../api/payrollApi'
import type { PayrollPeriod, PayrollLine } from '../types/payroll.types'
import type { PaymentMethod, BulkPaymentItem } from '../types/payment.types'

interface BulkPaySalaryModalProps {
  isOpen: boolean
  period: PayrollPeriod | null
  branchId: string
  branchName?: string
  onClose: () => void
  onSuccess: () => void
}

export const BulkPaySalaryModal: React.FC<BulkPaySalaryModalProps> = ({
  isOpen,
  period,
  branchId,
  branchName,
  onClose,
  onSuccess,
}) => {
  const [accounts, setAccounts] = useState<FinancialAccountDto[]>([])
  const [selectedAccountId, setSelectedAccountId] = useState<string>('')
  const [paidOn, setPaidOn] = useState<string>(() => new Date().toISOString().split('T')[0])
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('BANK_TRANSFER')
  const [note, setNote] = useState<string>('')

  // Map of selected line IDs and custom amount overrides
  const [selectedLineIds, setSelectedLineIds] = useState<Record<string, boolean>>({})
  const [customAmounts, setCustomAmounts] = useState<Record<string, number>>({})

  const [isLoadingAccounts, setIsLoadingAccounts] = useState<boolean>(false)
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Filter lines that still have remaining salary debt
  const unpaidLines: (PayrollLine & { remainingDebt: number })[] = useMemo(() => {
    if (!period?.lines) return []
    return period.lines
      .map((line) => {
        const gross = Number(line.grossSalary) || 0
        const paid = Number(line.paidAmount) || 0
        const remainingDebt = Math.max(0, gross - paid)
        return { ...line, remainingDebt }
      })
      .filter((l) => l.remainingDebt > 0)
  }, [period?.lines])

  // Fetch accounts on modal open & initialize selections
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

        // Default to bank account
        if (activeAccounts.length > 0) {
          const bankAcc = activeAccounts.find((a) => a.accountKind === 'BANK')
          const chosen = bankAcc || activeAccounts[0]
          setSelectedAccountId(chosen.id)
          setPaymentMethod(chosen.accountKind === 'CASH' ? 'CASH' : 'BANK_TRANSFER')
        }
      })
      .catch((err) => {
        console.error('Failed to load accounts:', err)
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

  // Select all unpaid lines by default
  useEffect(() => {
    if (unpaidLines.length > 0) {
      const initialSelected: Record<string, boolean> = {}
      const initialAmounts: Record<string, number> = {}
      unpaidLines.forEach((l) => {
        initialSelected[l.id] = true
        initialAmounts[l.id] = l.remainingDebt
      })
      setSelectedLineIds(initialSelected)
      setCustomAmounts(initialAmounts)
      setNote(`Chi trả lương hàng loạt kỳ tháng ${period?.monthStart || ''}`)
      setPaidOn(new Date().toISOString().split('T')[0])
    }
  }, [unpaidLines, period?.monthStart])

  if (!isOpen || !period) return null

  const selectedAccount = accounts.find((a) => a.id === selectedAccountId)
  const availableBalance = selectedAccount ? Number(selectedAccount.currentBalance) || 0 : 0

  // Calculate totals
  const selectedItems: BulkPaymentItem[] = unpaidLines
    .filter((l) => selectedLineIds[l.id])
    .map((l) => ({
      payrollLineId: l.id,
      amount: customAmounts[l.id] !== undefined ? customAmounts[l.id] : l.remainingDebt,
      externalReference: `UNC-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${l.staffCode}`,
    }))

  const totalPaymentAmount = selectedItems.reduce((acc, cur) => acc + (Number(cur.amount) || 0), 0)
  const isOverBalance = totalPaymentAmount > availableBalance
  const hasZeroOrNegativeAmount = selectedItems.some((item) => item.amount <= 0)
  const isAnyAmountOverRemaining = unpaidLines.some(
    (l) => selectedLineIds[l.id] && (customAmounts[l.id] || 0) > l.remainingDebt
  )
  const isInvalid =
    selectedItems.length === 0 ||
    isOverBalance ||
    hasZeroOrNegativeAmount ||
    isAnyAmountOverRemaining ||
    !selectedAccountId

  const handleToggleSelectAll = () => {
    if (Object.values(selectedLineIds).filter(Boolean).length === unpaidLines.length) {
      setSelectedLineIds({})
    } else {
      const allSelected: Record<string, boolean> = {}
      unpaidLines.forEach((l) => {
        allSelected[l.id] = true
      })
      setSelectedLineIds(allSelected)
    }
  }

  const handleToggleLine = (lineId: string) => {
    setSelectedLineIds((prev) => ({
      ...prev,
      [lineId]: !prev[lineId],
    }))
  }

  const handleAmountChange = (lineId: string, val: string, maxRemaining: number) => {
    const num = Math.min(maxRemaining, Math.max(0, Number(val) || 0))
    setCustomAmounts((prev) => ({
      ...prev,
      [lineId]: num,
    }))
  }

  const handleAccountChange = (accId: string) => {
    setSelectedAccountId(accId)
    const acc = accounts.find((a) => a.id === accId)
    if (acc) {
      setPaymentMethod(acc.accountKind === 'CASH' ? 'CASH' : 'BANK_TRANSFER')
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)

    if (selectedItems.length === 0) {
      setErrorMessage('Vui lòng chọn ít nhất 1 nhân viên để chi trả lương.')
      return
    }

    if (totalPaymentAmount <= 0) {
      setErrorMessage('Tổng số tiền chi trả phải lớn hơn 0 ₫.')
      return
    }

    if (totalPaymentAmount > availableBalance) {
      setErrorMessage(
        `Số dư khả dụng của quỹ (${formatVND(availableBalance)}) không đủ để chi ${formatVND(
          totalPaymentAmount
        )}.`
      )
      return
    }

    setIsSubmitting(true)
    try {
      await paymentApi.bulkPaySalary(branchId, period.id, {
        accountId: selectedAccountId,
        paidOn,
        paymentMethod,
        items: selectedItems,
        note: note.trim() || undefined,
      })

      onSuccess()
      onClose()
    } catch (err: unknown) {
      console.error('Failed to bulk pay salaries:', err)
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

      if (errResponse?.type === 'urn:mtms:error:insufficient-funds') {
        setErrorMessage('Lỗi tài khoản: Số dư quỹ tiền mặt/ngân hàng không đủ để chi trả hàng loạt.')
      } else if (errResponse?.type === 'urn:mtms:error:payroll-payment-exceeds-remaining') {
        setErrorMessage('Lỗi: Có dòng lương có số tiền chi trả vượt quá mức lương còn nợ.')
      } else {
        setErrorMessage(
          errResponse?.detail ||
            errResponse?.message ||
            'Không thể thực hiện chi trả lương hàng loạt. Vui lòng kiểm tra lại.'
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
          className="bg-white rounded-3xl border border-stone-200 shadow-2xl max-w-3xl w-full overflow-hidden flex flex-col max-h-[92vh]"
        >
          {/* Header */}
          <div className="px-6 py-4 border-b border-stone-200 bg-stone-50/80 flex items-center justify-between shrink-0">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-2xl bg-emerald-100 text-emerald-800 flex items-center justify-center">
                <Users className="h-5 w-5" />
              </div>
              <div>
                <div className="flex items-center space-x-2">
                  <h3 className="font-bold text-stone-900 text-base">
                    Chi trả lương hàng loạt (Bulk Payment)
                  </h3>
                </div>
                <p className="text-xs text-stone-500">
                  Thanh toán đồng loạt cho nhân viên còn nợ lương tại {branchName || 'chi nhánh'}
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

          {/* Form */}
          <form onSubmit={handleSubmit} className="p-6 space-y-4 overflow-y-auto flex-1">
            {/* Error Banner */}
            {errorMessage && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl flex items-start space-x-2.5 text-rose-800 text-xs">
                <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
                <div className="flex-1 font-medium">{errorMessage}</div>
              </div>
            )}

            {/* Account & Source Funds */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-stone-50 p-4 rounded-2xl border border-stone-200">
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
                      <span>Đang tải...</span>
                    </span>
                  )}
                </label>

                <select
                  value={selectedAccountId}
                  onChange={(e) => handleAccountChange(e.target.value)}
                  disabled={isLoadingAccounts || isSubmitting}
                  className="w-full h-10 px-3 bg-white border border-stone-300 rounded-xl text-xs font-medium text-stone-900 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                >
                  {accounts.map((acc) => (
                    <option key={acc.id} value={acc.id}>
                      {acc.accountName} [{acc.accountKind === 'CASH' ? 'Tiền mặt' : 'Ngân hàng'}]
                    </option>
                  ))}
                </select>

                <div className="text-[11px] text-stone-500 pt-0.5 flex items-center justify-between">
                  <span>Số dư khả dụng:</span>
                  <span className={`font-mono font-bold ${isOverBalance ? 'text-rose-700' : 'text-emerald-700'}`}>
                    {formatVND(availableBalance)}
                  </span>
                </div>
              </div>

              {/* Paid On Date & Method */}
              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-stone-700 flex items-center space-x-1">
                    <Calendar className="h-3.5 w-3.5 text-stone-500" />
                    <span>Ngày thực chi *</span>
                  </label>
                  <Input
                    type="date"
                    value={paidOn}
                    onChange={(e) => setPaidOn(e.target.value)}
                    disabled={isSubmitting}
                    className="h-10 text-xs border-stone-300 bg-white"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-stone-700 flex items-center space-x-1">
                    <CreditCard className="h-3.5 w-3.5 text-stone-500" />
                    <span>Phương thức</span>
                  </label>
                  <div className="grid grid-cols-2 gap-1">
                    <button
                      type="button"
                      onClick={() => setPaymentMethod('BANK_TRANSFER')}
                      className={`h-10 px-1 rounded-xl border text-[11px] font-medium flex items-center justify-center space-x-1 ${
                        paymentMethod === 'BANK_TRANSFER'
                          ? 'border-[#304638] bg-[#304638]/10 text-[#304638] font-bold'
                          : 'border-stone-200 text-stone-600 bg-white'
                      }`}
                    >
                      <Building2 className="h-3 w-3" />
                      <span>CK</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => setPaymentMethod('CASH')}
                      className={`h-10 px-1 rounded-xl border text-[11px] font-medium flex items-center justify-center space-x-1 ${
                        paymentMethod === 'CASH'
                          ? 'border-[#304638] bg-[#304638]/10 text-[#304638] font-bold'
                          : 'border-stone-200 text-stone-600 bg-white'
                      }`}
                    >
                      <Banknote className="h-3 w-3" />
                      <span>Tiền mặt</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            {/* Note */}
            <div className="space-y-1">
              <label className="text-xs font-bold text-stone-700">Ghi chú đợt chi</label>
              <Input
                type="text"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="VD: Chi trả toàn bộ bảng lương tháng 11"
                disabled={isSubmitting}
                className="h-9 text-xs border-stone-300"
              />
            </div>

            {/* Employee Selection Table */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <button
                    type="button"
                    onClick={handleToggleSelectAll}
                    disabled={unpaidLines.length === 0 || isSubmitting}
                    className="flex items-center space-x-1.5 text-xs font-bold text-stone-700 hover:text-[#304638]"
                  >
                    {Object.values(selectedLineIds).filter(Boolean).length === unpaidLines.length &&
                    unpaidLines.length > 0 ? (
                      <CheckSquare className="h-4 w-4 text-[#304638]" />
                    ) : (
                      <Square className="h-4 w-4 text-stone-400" />
                    )}
                    <span>
                      Chọn tất cả ({selectedItems.length}/{unpaidLines.length})
                    </span>
                  </button>
                </div>

                <span className="text-xs text-stone-500">
                  {unpaidLines.length === 0 ? 'Tất cả nhân viên đã được tất toán!' : 'Danh sách nhân viên còn nợ lương'}
                </span>
              </div>

              <div className="border border-stone-200 rounded-2xl overflow-hidden shadow-2xs max-h-60 overflow-y-auto">
                <table className="w-full text-xs text-left">
                  <thead className="bg-stone-100/80 text-stone-600 font-semibold uppercase tracking-wider text-[11px] sticky top-0 z-10 border-b border-stone-200">
                    <tr>
                      <th className="px-3 py-2.5 w-8"></th>
                      <th className="px-3 py-2.5">Nhân viên</th>
                      <th className="px-3 py-2.5 text-right">Lương thực lĩnh</th>
                      <th className="px-3 py-2.5 text-right">Đã chi</th>
                      <th className="px-3 py-2.5 text-right">Còn nợ</th>
                      <th className="px-3 py-2.5 text-right w-36">Chi đợt này (VND)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-stone-100 bg-white">
                    {unpaidLines.length > 0 ? (
                      unpaidLines.map((l) => {
                        const isSelected = !!selectedLineIds[l.id]
                        const currentVal = customAmounts[l.id] !== undefined ? customAmounts[l.id] : l.remainingDebt

                        return (
                          <tr
                            key={l.id}
                            className={`transition-colors ${
                              isSelected ? 'bg-emerald-50/30' : 'hover:bg-stone-50/60'
                            }`}
                          >
                            <td className="px-3 py-2">
                              <input
                                type="checkbox"
                                checked={isSelected}
                                onChange={() => handleToggleLine(l.id)}
                                disabled={isSubmitting}
                                className="rounded text-[#304638] focus:ring-[#304638] h-4 w-4"
                              />
                            </td>
                            <td className="px-3 py-2">
                              <div className="font-bold text-stone-900">{l.employeeName}</div>
                              <div className="text-[10px] text-stone-500 font-mono">
                                {l.staffCode} · {l.positionName}
                              </div>
                            </td>
                            <td className="px-3 py-2 text-right font-mono text-stone-800">
                              {formatVND(l.grossSalary)}
                            </td>
                            <td className="px-3 py-2 text-right font-mono text-emerald-700">
                              {formatVND(l.paidAmount)}
                            </td>
                            <td className="px-3 py-2 text-right font-mono font-bold text-amber-800">
                              {formatVND(l.remainingDebt)}
                            </td>
                            <td className="px-3 py-2 text-right">
                              <Input
                                type="number"
                                min="1"
                                max={l.remainingDebt}
                                value={isSelected ? currentVal : ''}
                                onChange={(e) => handleAmountChange(l.id, e.target.value, l.remainingDebt)}
                                disabled={!isSelected || isSubmitting}
                                className="h-7 text-xs font-mono font-bold text-right border-stone-300"
                              />
                            </td>
                          </tr>
                        )
                      })
                    ) : (
                      <tr>
                        <td colSpan={6} className="px-4 py-8 text-center text-stone-500">
                          <CheckCircle2 className="h-6 w-6 text-emerald-600 mx-auto mb-1" />
                          <span>Tất cả nhân sự trong kỳ lương này đã được chi trả đầy đủ!</span>
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Financial Summary Box */}
            <div className="bg-stone-50 border border-stone-200 rounded-2xl p-4 space-y-2">
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
                <div className="bg-white p-2.5 rounded-xl border border-stone-200">
                  <span className="text-[10px] text-stone-500 uppercase font-medium block">
                    Nhân sự chi đợt này
                  </span>
                  <span className="text-sm font-bold font-mono text-stone-900">
                    {selectedItems.length} người
                  </span>
                </div>

                <div className="bg-white p-2.5 rounded-xl border border-stone-200">
                  <span className="text-[10px] text-stone-500 uppercase font-medium block">
                    Tổng tiền chi đợt này
                  </span>
                  <span className="text-sm font-bold font-mono text-emerald-800">
                    {formatVND(totalPaymentAmount)}
                  </span>
                </div>

                <div className="bg-white p-2.5 rounded-xl border border-stone-200">
                  <span className="text-[10px] text-stone-500 uppercase font-medium block">
                    Số dư quỹ hiện có
                  </span>
                  <span className="text-sm font-bold font-mono text-stone-900">
                    {formatVND(availableBalance)}
                  </span>
                </div>

                <div
                  className={`p-2.5 rounded-xl border ${
                    isOverBalance
                      ? 'bg-rose-50 border-rose-300 text-rose-900'
                      : 'bg-emerald-50 border-emerald-300 text-emerald-900'
                  }`}
                >
                  <span className="text-[10px] uppercase font-bold block">
                    Dư sau khi chi
                  </span>
                  <span className="text-sm font-bold font-mono">
                    {formatVND(availableBalance - totalPaymentAmount)}
                  </span>
                </div>
              </div>

              {isOverBalance && (
                <div className="text-xs text-rose-700 font-semibold text-center pt-1">
                  Cảnh báo: Tổng tiền chi trả ({formatVND(totalPaymentAmount)}) vượt quá số dư khả dụng
                  của quỹ ({formatVND(availableBalance)}). Vui lòng điều chỉnh giảm bớt nhân viên hoặc nạp thêm quỹ.
                </div>
              )}
            </div>

            {/* Invariant Alert TC-016 */}
            <div className="p-3 bg-emerald-50/80 border border-emerald-200 rounded-xl flex items-start space-x-2 text-emerald-900 text-xs">
              <ShieldCheck className="h-4 w-4 text-emerald-700 shrink-0 mt-0.5" />
              <div className="text-[11px] leading-relaxed">
                <strong>Bảo toàn hạch toán:</strong> Toàn bộ các khoản chi trên sẽ sinh bút toán
                xuất quỹ vào ngày <strong>{paidOn}</strong> và cập nhật lũy kế đã trả của bảng lương mà
                không làm biến động số dư quỹ của tháng trước.
              </div>
            </div>

            {/* Actions */}
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
                disabled={isSubmitting || isInvalid}
                className="h-9 px-5 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-semibold shadow-xs"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />
                    <span>Đang xuất quỹ hàng loạt...</span>
                  </>
                ) : (
                  <>
                    <DollarSign className="h-3.5 w-3.5 mr-1.5" />
                    <span>Xác nhận chi trả ({formatVND(totalPaymentAmount)})</span>
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
