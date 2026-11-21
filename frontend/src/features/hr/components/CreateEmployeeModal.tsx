import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  UserPlus,
  AlertTriangle,
  Calendar,
  Briefcase,
  Phone,
  Mail,
  FileText,
  DollarSign,
  Clock,
  CheckCircle2,
  Loader2,
  Sparkles,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { hrApi, formatVnd, parseMoneyInput } from '../api/hrApi'
import {
  COMMON_POSITIONS,
  ROUNDING_BLOCK_OPTIONS,
  type RateType,
  type CreateEmployeePayload,
  type EmployeeDetail,
  type EmployeeSummary,
} from '../types/hr.types'

interface CreateEmployeeModalProps {
  isOpen: boolean
  branchId: string
  branchName?: string
  onClose: () => void
  onSuccess: (employee: EmployeeDetail | EmployeeSummary) => void
}

export const CreateEmployeeModal: React.FC<CreateEmployeeModalProps> = ({
  isOpen,
  branchId,
  branchName,
  onClose,
  onSuccess,
}) => {
  const [staffCode, setStaffCode] = useState('')
  const [fullName, setFullName] = useState('')
  const [positionName, setPositionName] = useState(COMMON_POSITIONS[0])
  const [customPosition, setCustomPosition] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [email, setEmail] = useState('')
  const [hiredOn, setHiredOn] = useState(new Date().toISOString().split('T')[0])
  const [notes, setNotes] = useState('')

  // Initial Salary Policy Toggle
  const [withInitialSalary, setWithInitialSalary] = useState(true)
  const [rateType, setRateType] = useState<RateType>('HOURLY')
  const [rateAmountStr, setRateAmountStr] = useState('25000')
  const [standardWorkdays, setStandardWorkdays] = useState('26')
  const [roundingBlock, setRoundingBlock] = useState('15')
  const [salaryEffectiveFrom, setSalaryEffectiveFrom] = useState(
    new Date().toISOString().split('T')[0]
  )
  const [salaryNotes, setSalaryNotes] = useState('Mức lương khởi điểm khi gia nhập')

  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Reset form when opened
  useEffect(() => {
    if (isOpen) {
      setStaffCode('')
      setFullName('')
      setPositionName(COMMON_POSITIONS[0])
      setCustomPosition('')
      setPhoneNumber('')
      setEmail('')
      const today = new Date().toISOString().split('T')[0]
      setHiredOn(today)
      setSalaryEffectiveFrom(today)
      setNotes('')
      setWithInitialSalary(true)
      setRateType('HOURLY')
      setRateAmountStr('25000')
      setStandardWorkdays('26')
      setRoundingBlock('15')
      setSalaryNotes('Mức lương khởi điểm khi gia nhập')
      setErrorMessage(null)
    }
  }, [isOpen])

  // Sync salary effective date with hiredOn
  const handleHiredOnChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value
    setHiredOn(val)
    if (salaryEffectiveFrom === hiredOn) {
      setSalaryEffectiveFrom(val)
    }
  }

  const parsedRateAmount = parseMoneyInput(rateAmountStr)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!branchId) return
    setErrorMessage(null)

    const finalPosition =
      positionName === 'CUSTOM' ? customPosition.trim() : positionName

    if (!staffCode.trim()) {
      setErrorMessage('Vui lòng nhập mã nhân viên (Staff Code).')
      return
    }
    if (!fullName.trim()) {
      setErrorMessage('Vui lòng nhập họ và tên nhân viên.')
      return
    }
    if (!finalPosition) {
      setErrorMessage('Vui lòng chọn hoặc nhập chức danh/vị trí công việc.')
      return
    }
    if (!hiredOn) {
      setErrorMessage('Vui lòng chọn ngày tuyển dụng/bắt đầu làm việc.')
      return
    }

    if (withInitialSalary) {
      if (parsedRateAmount <= 0) {
        setErrorMessage('Mức lương khởi điểm phải lớn hơn 0 ₫.')
        return
      }
      if (!salaryEffectiveFrom) {
        setErrorMessage('Vui lòng chọn ngày bắt đầu hiệu lực của chính sách lương.')
        return
      }
      const days = parseFloat(standardWorkdays)
      if (rateType === 'MONTHLY' && (isNaN(days) || days <= 0)) {
        setErrorMessage('Số ngày công chuẩn của tháng phải là số dương (ví dụ: 26).')
        return
      }
    }

    const payload: CreateEmployeePayload = {
      staffCode: staffCode.trim().toUpperCase(),
      fullName: fullName.trim(),
      positionName: finalPosition,
      phoneNumber: phoneNumber.trim() || null,
      email: email.trim() || null,
      hiredOn,
      notes: notes.trim() || null,
      initialSalaryPolicy: withInitialSalary
        ? {
            rateType,
            rateAmount: parsedRateAmount,
            standardWorkdays:
              rateType === 'MONTHLY' ? parseFloat(standardWorkdays) || 26 : null,
            paidMinutesRoundingBlock: parseInt(roundingBlock, 10) || 15,
            effectiveFrom: salaryEffectiveFrom,
            notes: salaryNotes.trim() || null,
          }
        : undefined,
    }

    try {
      setIsSubmitting(true)
      const res = await hrApi.createEmployee(branchId, payload)
      onSuccess(res)
      onClose()
    } catch (err: unknown) {
      console.error('Failed to create employee:', err)
      const errObj = err as { response?: { data?: { detail?: string; message?: string } } }
      const backendMsg =
        errObj.response?.data?.detail ||
        errObj.response?.data?.message ||
        'Không thể thêm nhân sự mới. Vui lòng kiểm tra lại dữ liệu và thử lại.'
      setErrorMessage(backendMsg)
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!isOpen) return null

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

        {/* Modal Window */}
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
                <UserPlus className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-base font-bold tracking-tight">
                  Thêm nhân sự mới
                </h2>
                <p className="text-xs text-[#C8CEC4]">
                  {branchName
                    ? `Phân bổ làm chi nhánh chính: ${branchName}`
                    : 'Tạo hồ sơ nhân viên và phân bổ chi nhánh ban đầu'}
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="p-1 rounded-lg text-slate-300 hover:text-white hover:bg-white/10 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-5">
            {errorMessage && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-xs text-rose-800">
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                <span className="flex-1 font-medium">{errorMessage}</span>
                <button
                  type="button"
                  onClick={() => setErrorMessage(null)}
                  className="text-rose-500 hover:text-rose-700"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            )}

            {/* Thông tin nhân viên */}
            <div className="space-y-4">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
                <FileText className="w-3.5 h-3.5 text-[#304638]" />
                <span>1. Hồ sơ nhân sự cơ bản</span>
              </h3>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* Mã nhân viên */}
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-700 flex items-center justify-between">
                    <span>
                      Mã nhân viên <span className="text-rose-500">*</span>
                    </span>
                    <span className="text-[10px] text-slate-400">Duy nhất</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="VD: EMP-001"
                    value={staffCode}
                    onChange={(e) => setStaffCode(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-mono font-medium text-slate-800 uppercase focus:outline-none focus:ring-2 focus:ring-[#304638]"
                  />
                </div>

                {/* Họ và tên */}
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-700">
                    Họ và tên đầy đủ <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="VD: Nguyễn Văn A"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                  />
                </div>

                {/* Chức vụ / Vị trí */}
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                    <Briefcase className="w-3.5 h-3.5 text-slate-400" />
                    <span>
                      Vị trí công việc <span className="text-rose-500">*</span>
                    </span>
                  </label>
                  <select
                    value={positionName}
                    onChange={(e) => setPositionName(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                  >
                    {COMMON_POSITIONS.map((pos) => (
                      <option key={pos} value={pos}>
                        {pos}
                      </option>
                    ))}
                    <option value="CUSTOM">Khác (tự nhập...)</option>
                  </select>
                  {positionName === 'CUSTOM' && (
                    <input
                      type="text"
                      placeholder="Nhập tên chức danh cụ thể..."
                      value={customPosition}
                      onChange={(e) => setCustomPosition(e.target.value)}
                      className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                    />
                  )}
                </div>

                {/* Ngày vào làm */}
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    <span>
                      Ngày vào làm (Hired on) <span className="text-rose-500">*</span>
                    </span>
                  </label>
                  <input
                    type="date"
                    required
                    value={hiredOn}
                    onChange={handleHiredOnChange}
                    className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                  />
                </div>

                {/* Số điện thoại */}
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                    <Phone className="w-3.5 h-3.5 text-slate-400" />
                    <span>Số điện thoại liên hệ</span>
                  </label>
                  <input
                    type="tel"
                    placeholder="VD: 0987654321"
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                  />
                </div>

                {/* Email */}
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                    <Mail className="w-3.5 h-3.5 text-slate-400" />
                    <span>Email liên hệ</span>
                  </label>
                  <input
                    type="email"
                    placeholder="VD: nva@milktea.vn"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                  />
                </div>
              </div>

              {/* Ghi chú */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700">
                  Ghi chú hồ sơ
                </label>
                <textarea
                  rows={2}
                  placeholder="Ghi chú về ca trực, thông tin bằng cấp, kinh nghiệm..."
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638] resize-none"
                />
              </div>
            </div>

            {/* Khối chính sách lương khởi điểm (TC-013 / P0-HR-02) */}
            <div className="pt-2 border-t border-slate-200">
              <div className="flex items-center justify-between mb-3">
                <label className="flex items-center gap-2 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={withInitialSalary}
                    onChange={(e) => setWithInitialSalary(e.target.checked)}
                    className="w-4 h-4 text-[#304638] rounded border-slate-300 focus:ring-[#304638]"
                  />
                  <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                    <DollarSign className="w-3.5 h-3.5 text-emerald-600" />
                    <span>2. Thiết lập chính sách lương khởi điểm ngay</span>
                  </span>
                </label>
                {withInitialSalary && (
                  <Badge variant="outline" className="text-[10px] bg-emerald-50 text-emerald-700 border-emerald-200">
                    Kích hoạt hiệu lực
                  </Badge>
                )}
              </div>

              {withInitialSalary && (
                <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-4 animate-in fade-in duration-200">
                  {/* Loại lương */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div className="space-y-1.5">
                      <label className="text-xs font-semibold text-slate-700">
                        Hình thức chi trả lương <span className="text-rose-500">*</span>
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
                          Lương theo giờ (Hourly)
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
                          Lương theo tháng (Monthly)
                        </button>
                      </div>
                    </div>

                    {/* Đơn giá lương */}
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
                          placeholder={rateType === 'HOURLY' ? '25000' : '6500000'}
                          className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-mono font-semibold text-slate-900 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                        />
                        <div className="absolute right-3 top-2 text-xs text-slate-400 font-semibold">
                          VND
                        </div>
                      </div>
                    </div>

                    {/* Ngày bắt đầu hiệu lực (effectiveFrom) */}
                    <div className="space-y-1.5">
                      <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        <span>
                          Ngày bắt đầu hiệu lực <span className="text-rose-500">*</span>
                        </span>
                      </label>
                      <input
                        type="date"
                        required
                        value={salaryEffectiveFrom}
                        onChange={(e) => setSalaryEffectiveFrom(e.target.value)}
                        className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                      />
                    </div>

                    {/* Khối làm tròn phút (paidMinutesRoundingBlock) */}
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

                    {/* Ngày công chuẩn (nếu lương tháng) */}
                    {rateType === 'MONTHLY' && (
                      <div className="space-y-1.5 sm:col-span-2">
                        <label className="text-xs font-semibold text-slate-700 flex items-center justify-between">
                          <span>Ngày công chuẩn của tháng</span>
                          <span className="text-[10px] text-slate-400">
                            Dùng để tính lương ngày công lẻ
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
                  </div>

                  <p className="text-[11px] text-slate-500 italic bg-white p-2.5 rounded-lg border border-slate-100 flex items-center gap-2">
                    <Sparkles className="w-3.5 h-3.5 text-amber-500 shrink-0" />
                    <span>
                      Chính sách lương sẽ được áp dụng cho nhân viên tại chi nhánh hiện hành. Đơn giá mới không hồi tố cho các giờ làm trước mốc hiệu lực.
                    </span>
                  </p>
                </div>
              )}
            </div>

            {/* Actions */}
            <div className="pt-3 border-t border-slate-200 flex items-center justify-end space-x-3">
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
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-5"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 mr-2 animate-spin" />
                    <span>Đang lưu hồ sơ...</span>
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="w-3.5 h-3.5 mr-1.5" />
                    <span>Tạo hồ sơ nhân viên</span>
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
