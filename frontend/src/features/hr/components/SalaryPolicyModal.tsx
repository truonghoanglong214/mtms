import React, { useState, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Coins,
  History,
  Plus,
  Calendar,
  AlertTriangle,
  Clock,
  CheckCircle2,
  Loader2,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  Search,
  BadgePercent,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { hrApi, formatVnd, parseMoneyInput } from '../api/hrApi'
import {
  ROUNDING_BLOCK_OPTIONS,
  RATE_TYPE_META,
  type RateType,
  type SalaryPolicy,
  type CreateSalaryPolicyPayload,
  type EmployeeDetail,
  type EmployeeSummary,
} from '../types/hr.types'

interface SalaryPolicyModalProps {
  isOpen: boolean
  branchId: string
  employee: EmployeeDetail | EmployeeSummary | null
  onClose: () => void
  onSuccess: (newPolicy: SalaryPolicy) => void
}

export const SalaryPolicyModal: React.FC<SalaryPolicyModalProps> = ({
  isOpen,
  branchId,
  employee,
  onClose,
  onSuccess,
}) => {
  const [policies, setPolicies] = useState<SalaryPolicy[]>([])
  const [isLoadingPolicies, setIsLoadingPolicies] = useState(false)
  const [showAddForm, setShowAddForm] = useState(false)

  // Form State
  const [rateType, setRateType] = useState<RateType>('HOURLY')
  const [rateAmountStr, setRateAmountStr] = useState('25000')
  const [standardWorkdays, setStandardWorkdays] = useState('26')
  const [roundingBlock, setRoundingBlock] = useState('15')
  const [effectiveFrom, setEffectiveFrom] = useState(
    new Date().toISOString().split('T')[0]
  )
  const [notes, setNotes] = useState('')

  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [successToast, setSuccessToast] = useState<string | null>(null)

  // Effective Policy Tester
  const [testDate, setTestDate] = useState(new Date().toISOString().split('T')[0])
  const [testResult, setTestResult] = useState<SalaryPolicy | null>(null)
  const [isTesting, setIsTesting] = useState(false)
  const [testError, setTestError] = useState<string | null>(null)

  const loadPolicies = useCallback(async () => {
    if (!branchId || !employee?.id) return
    try {
      setIsLoadingPolicies(true)
      const data = await hrApi.getSalaryPolicies(branchId, employee.id)
      // Sort newest first
      const sorted = [...data].sort((a, b) => b.effectiveFrom.localeCompare(a.effectiveFrom))
      setPolicies(sorted)
    } catch (err) {
      console.error('Failed to load salary policies:', err)
    } finally {
      setIsLoadingPolicies(false)
    }
  }, [branchId, employee?.id])

  useEffect(() => {
    if (isOpen && employee) {
      loadPolicies()
      setShowAddForm(false)
      setRateType('HOURLY')
      setRateAmountStr('25000')
      setStandardWorkdays('26')
      setRoundingBlock('15')
      setEffectiveFrom(new Date().toISOString().split('T')[0])
      setNotes('')
      setErrorMessage(null)
      setSuccessToast(null)
      setTestResult(null)
      setTestError(null)
    }
  }, [isOpen, employee, loadPolicies])

  if (!isOpen || !employee) return null

  const parsedRateAmount = parseMoneyInput(rateAmountStr)

  const handleTestEffectiveDate = async () => {
    if (!branchId || !employee?.id || !testDate) return
    try {
      setIsTesting(true)
      setTestError(null)
      const policy = await hrApi.getEffectiveSalaryPolicy(branchId, employee.id, testDate)
      setTestResult(policy)
    } catch (err: unknown) {
      console.error('Failed to test effective salary policy:', err)
      const errObj = err as { response?: { data?: { detail?: string; message?: string } } }
      setTestError(
        errObj.response?.data?.detail ||
          'Không có chính sách lương nào có hiệu lực tại ngày đã chọn.'
      )
      setTestResult(null)
    } finally {
      setIsTesting(false)
    }
  }

  const handleSubmitNewPolicy = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!branchId || !employee.id) return
    setErrorMessage(null)

    if (parsedRateAmount <= 0) {
      setErrorMessage('Mức lương phải lớn hơn 0 ₫.')
      return
    }

    if (!effectiveFrom) {
      setErrorMessage('Vui lòng chọn ngày bắt đầu hiệu lực.')
      return
    }

    const payload: CreateSalaryPolicyPayload = {
      rateType,
      rateAmount: parsedRateAmount,
      standardWorkdays:
        rateType === 'MONTHLY' ? parseFloat(standardWorkdays) || 26 : null,
      paidMinutesRoundingBlock: parseInt(roundingBlock, 10) || 15,
      effectiveFrom,
      notes: notes.trim() || null,
    }

    try {
      setIsSubmitting(true)
      const newPolicy = await hrApi.createSalaryPolicy(branchId, employee.id, payload)
      setSuccessToast('Đã thiết lập chính sách lương mới thành công!')
      setShowAddForm(false)
      onSuccess(newPolicy)
      await loadPolicies()
    } catch (err: unknown) {
      console.error('Failed to create salary policy:', err)
      const errObj = err as { response?: { data?: { detail?: string; message?: string } } }
      const backendMsg =
        errObj.response?.data?.detail ||
        errObj.response?.data?.message ||
        'Không thể thêm chính sách lương. Vui lòng kiểm tra lại mốc ngày hiệu lực.'
      setErrorMessage(backendMsg)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
        {/* Backdrop */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
          className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs"
        />

        {/* Modal Dialog */}
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 12 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 12 }}
          transition={{ duration: 0.2 }}
          className="relative w-full max-w-2xl bg-white rounded-2xl shadow-2xl overflow-hidden z-10 flex flex-col max-h-[90vh]"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 bg-[#304638] text-white">
            <div className="flex items-center space-x-3">
              <div className="p-2 rounded-xl bg-white/10 text-white">
                <Coins className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-base font-bold tracking-tight">
                  Chính sách lương theo mốc hiệu lực (P0-HR-02 / TC-013)
                </h2>
                <p className="text-xs text-[#C8CEC4]">
                  Nhân viên: <span className="font-semibold">{employee.fullName}</span> ({employee.staffCode}) • {employee.positionName}
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="p-1 rounded-lg text-slate-300 hover:text-white hover:bg-white/10 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto p-6 space-y-5">
            {/* Success Toast */}
            {successToast && (
              <div className="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span className="font-semibold flex-1">{successToast}</span>
                <button
                  type="button"
                  onClick={() => setSuccessToast(null)}
                  className="text-emerald-500 hover:text-emerald-700"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            )}

            {/* Invariant TC-013 Info Banner */}
            <div className="p-3.5 rounded-xl bg-blue-50 border border-blue-200 text-xs text-blue-900 space-y-1">
              <div className="flex items-center gap-1.5 font-bold text-blue-950">
                <ShieldCheck className="w-4 h-4 text-blue-700 shrink-0" />
                <span>Bất biến chính sách lương mốc hiệu lực (TC-013)</span>
              </div>
              <p className="text-[11px] leading-relaxed text-blue-800">
                Khi tăng/giảm lương giữa tháng, chính sách mới được lưu trữ bất biến với mốc <code>effective_from</code>. Hệ thống <strong>không hồi tố</strong> làm thay đổi đơn giá các giờ đã làm trước đó (Ví dụ TC-013: 40 giờ mức cũ + 50 giờ mức mới).
              </p>
            </div>

            {/* Action Bar */}
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
                <History className="w-3.5 h-3.5 text-[#304638]" />
                <span>Lịch sử các mức lương áp dụng ({policies.length})</span>
              </h3>
              {!showAddForm && (
                <Button
                  type="button"
                  size="sm"
                  onClick={() => setShowAddForm(true)}
                  className="bg-[#304638] hover:bg-[#25372C] text-white text-xs h-8 px-3"
                >
                  <Plus className="w-3.5 h-3.5 mr-1" />
                  <span>Thêm mức lương mới (TC-013)</span>
                </Button>
              )}
            </div>

            {/* New Policy Form (Expandable) */}
            {showAddForm && (
              <form
                onSubmit={handleSubmitNewPolicy}
                className="p-4 rounded-xl bg-slate-50 border-2 border-[#304638]/30 space-y-4 animate-in fade-in duration-200"
              >
                <div className="flex items-center justify-between border-b border-slate-200 pb-2">
                  <span className="text-xs font-bold text-slate-900 flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                    <span>Thiết lập mức lương mới</span>
                  </span>
                  <button
                    type="button"
                    onClick={() => setShowAddForm(false)}
                    className="text-xs text-slate-400 hover:text-slate-600 font-semibold"
                  >
                    Hủy bỏ
                  </button>
                </div>

                {errorMessage && (
                  <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-xs text-rose-800 flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
                    <span>{errorMessage}</span>
                  </div>
                )}

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Loại lương */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700">
                      Hình thức lương <span className="text-rose-500">*</span>
                    </label>
                    <div className="grid grid-cols-2 gap-2">
                      <button
                        type="button"
                        onClick={() => {
                          setRateType('HOURLY')
                          if (parsedRateAmount > 1000000) setRateAmountStr('25000')
                        }}
                        className={`px-3 py-2 rounded-xl text-xs font-semibold border transition-all text-center ${
                          rateType === 'HOURLY'
                            ? 'bg-blue-600 text-white border-blue-600 shadow-xs'
                            : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-100'
                        }`}
                      >
                        Theo giờ (HOURLY)
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setRateType('MONTHLY')
                          if (parsedRateAmount < 500000) setRateAmountStr('6500000')
                        }}
                        className={`px-3 py-2 rounded-xl text-xs font-semibold border transition-all text-center ${
                          rateType === 'MONTHLY'
                            ? 'bg-purple-600 text-white border-purple-600 shadow-xs'
                            : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-100'
                        }`}
                      >
                        Theo tháng (MONTHLY)
                      </button>
                    </div>
                  </div>

                  {/* Đơn giá */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700 flex items-center justify-between">
                      <span>
                        Mức lương ({rateType === 'HOURLY' ? '₫/giờ' : '₫/tháng'}){' '}
                        <span className="text-rose-500">*</span>
                      </span>
                      <span className="text-xs font-mono font-bold text-emerald-700">
                        {formatVnd(parsedRateAmount)}
                      </span>
                    </label>
                    <div className="relative">
                      <input
                        type="text"
                        required
                        value={rateAmountStr}
                        onChange={(e) => setRateAmountStr(e.target.value)}
                        placeholder="25000"
                        className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-mono font-semibold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                      />
                      <div className="absolute right-3 top-2 text-xs text-slate-400 font-semibold">
                        VND
                      </div>
                    </div>
                  </div>

                  {/* Ngày bắt đầu hiệu lực */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                      <Calendar className="w-3.5 h-3.5 text-slate-400" />
                      <span>
                        Bắt đầu hiệu lực <span className="text-rose-500">*</span>
                      </span>
                    </label>
                    <input
                      type="date"
                      required
                      value={effectiveFrom}
                      onChange={(e) => setEffectiveFrom(e.target.value)}
                      className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                    />
                  </div>

                  {/* Làm tròn phút */}
                  <div className="space-y-1.5">
                    <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      <span>Khối làm tròn phút chấm công</span>
                    </label>
                    <select
                      value={roundingBlock}
                      onChange={(e) => setRoundingBlock(e.target.value)}
                      className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                    >
                      {ROUNDING_BLOCK_OPTIONS.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* Ngày công chuẩn nếu tháng */}
                  {rateType === 'MONTHLY' && (
                    <div className="space-y-1.5 sm:col-span-2">
                      <label className="text-xs font-semibold text-slate-700 flex items-center justify-between">
                        <span>Số ngày công chuẩn của tháng (TC-014)</span>
                        <span className="text-[10px] text-slate-400">
                          Mặc định 26.00 ngày
                        </span>
                      </label>
                      <input
                        type="number"
                        step="0.5"
                        min="1"
                        max="31"
                        value={standardWorkdays}
                        onChange={(e) => setStandardWorkdays(e.target.value)}
                        className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                      />
                    </div>
                  )}

                  {/* Lý do thay đổi */}
                  <div className="space-y-1.5 sm:col-span-2">
                    <label className="text-xs font-semibold text-slate-700">
                      Lý do điều chỉnh lương
                    </label>
                    <input
                      type="text"
                      placeholder="VD: Tăng lương định kỳ quý 4, thăng cấp bậc..."
                      value={notes}
                      onChange={(e) => setNotes(e.target.value)}
                      className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                    />
                  </div>
                </div>

                <div className="flex items-center justify-end space-x-2 pt-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setShowAddForm(false)}
                    disabled={isSubmitting}
                    className="text-xs"
                  >
                    Đóng
                  </Button>
                  <Button
                    type="submit"
                    size="sm"
                    disabled={isSubmitting}
                    className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold"
                  >
                    {isSubmitting ? (
                      <>
                        <Loader2 className="w-3.5 h-3.5 mr-1.5 animate-spin" />
                        <span>Đang lưu...</span>
                      </>
                    ) : (
                      <>
                        <CheckCircle2 className="w-3.5 h-3.5 mr-1.5" />
                        <span>Áp dụng mức lương mới</span>
                      </>
                    )}
                  </Button>
                </div>
              </form>
            )}

            {/* Policies Timeline List */}
            {isLoadingPolicies ? (
              <div className="p-8 text-center text-xs text-slate-500">
                <Loader2 className="w-5 h-5 mx-auto mb-2 animate-spin text-[#304638]" />
                <span>Đang tải lịch sử chính sách lương...</span>
              </div>
            ) : policies.length === 0 ? (
              <div className="p-6 text-center rounded-xl bg-slate-50 border border-slate-200">
                <BadgePercent className="w-8 h-8 mx-auto mb-2 text-slate-300" />
                <p className="text-xs text-slate-600 font-medium">
                  Chưa có chính sách lương nào được thiết lập cho nhân viên này tại chi nhánh.
                </p>
                <button
                  type="button"
                  onClick={() => setShowAddForm(true)}
                  className="mt-2 text-xs font-semibold text-[#304638] hover:underline"
                >
                  + Thiết lập mức lương đầu tiên
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                {policies.map((p, idx) => {
                  const isCurrent = !p.effectiveTo
                  const rateMeta = RATE_TYPE_META[p.rateType]

                  return (
                    <div
                      key={p.id || idx}
                      className={`p-3.5 rounded-xl border transition-all ${
                        isCurrent
                          ? 'bg-emerald-50/50 border-emerald-300 shadow-xs ring-1 ring-emerald-400/20'
                          : 'bg-white border-slate-200 opacity-80'
                      }`}
                    >
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                        <div className="flex items-center space-x-2.5">
                          <span
                            className={`px-2 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wider ${rateMeta.badgeClass}`}
                          >
                            {p.rateType}
                          </span>
                          <span className="text-base font-bold font-mono text-slate-900">
                            {formatVnd(p.rateAmount)}
                          </span>
                          <span className="text-xs text-slate-500 font-medium">
                            {rateMeta.unit}
                          </span>
                          {isCurrent && (
                            <Badge className="bg-emerald-600 text-white text-[10px] py-0 px-1.5 ml-2">
                              Đang hiệu lực
                            </Badge>
                          )}
                        </div>

                        <div className="text-xs font-mono text-slate-600 flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5 text-slate-400" />
                          <span>{p.effectiveFrom}</span>
                          <ArrowRight className="w-3 h-3 text-slate-400" />
                          <span>{p.effectiveTo ? p.effectiveTo : 'Hiện tại (vô thời hạn)'}</span>
                        </div>
                      </div>

                      <div className="mt-2 pt-2 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-500">
                        <div className="flex items-center gap-3">
                          <span>
                            Khối làm tròn: <strong>{p.paidMinutesRoundingBlock || 15} phút</strong>
                          </span>
                          {p.rateType === 'MONTHLY' && (
                            <span>
                              Công chuẩn: <strong>{p.standardWorkdays || 26} ngày</strong>
                            </span>
                          )}
                        </div>
                        {p.notes && (
                          <div className="italic text-slate-600 truncate max-w-xs">
                            "{p.notes}"
                          </div>
                        )}
                      </div>
                    </div>
                  )
                })}
              </div>
            )}

            {/* Công cụ kiểm tra mức lương hiệu lực tại ngày cụ thể (TC-013 Tester) */}
            <div className="pt-3 border-t border-slate-200 space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-600 flex items-center gap-1.5">
                <Search className="w-3.5 h-3.5 text-blue-600" />
                <span>Kiểm tra mức lương áp dụng tại một ngày bất kỳ (TC-013 Engine Check)</span>
              </h4>

              <div className="flex items-center gap-2">
                <input
                  type="date"
                  value={testDate}
                  onChange={(e) => setTestDate(e.target.value)}
                  className="rounded-xl border border-slate-300 bg-white px-3 py-1.5 text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                />
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleTestEffectiveDate}
                  disabled={isTesting || !testDate}
                  className="text-xs h-8"
                >
                  {isTesting ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    <span>Tra cứu mức lương</span>
                  )}
                </Button>
              </div>

              {testResult && (
                <div className="p-3 rounded-xl bg-blue-50/80 border border-blue-200 text-xs text-blue-900 animate-in fade-in duration-150">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-blue-950">
                      Mức lương hiệu lực ngày {testDate}:
                    </span>
                    <span className="font-bold font-mono text-blue-800 text-sm">
                      {formatVnd(testResult.rateAmount)} / {RATE_TYPE_META[testResult.rateType].unit}
                    </span>
                  </div>
                  <p className="text-[11px] text-blue-700 mt-1">
                    Khoảng thời gian chính sách: <code>{testResult.effectiveFrom}</code> đến{' '}
                    <code>{testResult.effectiveTo || 'Hiện tại'}</code>
                    {testResult.notes ? ` • ${testResult.notes}` : ''}
                  </p>
                </div>
              )}

              {testError && (
                <div className="p-2.5 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-700">
                  {testError}
                </div>
              )}
            </div>
          </div>

          {/* Footer */}
          <div className="px-6 py-3.5 bg-slate-50 border-t border-slate-200 flex justify-end">
            <Button type="button" variant="outline" onClick={onClose} className="text-xs">
              Đóng cửa sổ
            </Button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
