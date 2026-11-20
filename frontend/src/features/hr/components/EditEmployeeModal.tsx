import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  UserCheck,
  AlertTriangle,
  Briefcase,
  Phone,
  Mail,
  Calendar,
  CheckCircle2,
  Loader2,
  AlertOctagon,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { hrApi } from '../api/hrApi'
import {
  COMMON_POSITIONS,
  EMPLOYEE_STATUS_META,
  type EmployeeStatus,
  type UpdateEmployeePayload,
  type EmployeeDetail,
  type EmployeeSummary,
} from '../types/hr.types'

interface EditEmployeeModalProps {
  isOpen: boolean
  branchId: string
  employee: EmployeeDetail | EmployeeSummary | null
  onClose: () => void
  onSuccess: (updated: EmployeeDetail | EmployeeSummary) => void
}

export const EditEmployeeModal: React.FC<EditEmployeeModalProps> = ({
  isOpen,
  branchId,
  employee,
  onClose,
  onSuccess,
}) => {
  const [fullName, setFullName] = useState('')
  const [positionName, setPositionName] = useState('')
  const [customPosition, setCustomPosition] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [email, setEmail] = useState('')
  const [status, setStatus] = useState<EmployeeStatus>('ACTIVE')
  const [leftOn, setLeftOn] = useState('')
  const [notes, setNotes] = useState('')

  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    if (employee && isOpen) {
      setFullName(employee.fullName || '')
      const isPredefined = COMMON_POSITIONS.includes(employee.positionName)
      if (isPredefined) {
        setPositionName(employee.positionName)
        setCustomPosition('')
      } else {
        setPositionName('CUSTOM')
        setCustomPosition(employee.positionName || '')
      }
      setPhoneNumber(employee.phoneNumber || '')
      setEmail(employee.email || '')
      setStatus(employee.status || 'ACTIVE')
      setLeftOn(employee.leftOn || '')
      const currentNotes = 'notes' in employee ? (employee as EmployeeDetail).notes || '' : ''
      setNotes(currentNotes)
      setErrorMessage(null)
    }
  }, [employee, isOpen])

  if (!isOpen || !employee) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!branchId || !employee.id) return
    setErrorMessage(null)

    const finalPosition =
      positionName === 'CUSTOM' ? customPosition.trim() : positionName

    if (!fullName.trim()) {
      setErrorMessage('Vui lòng nhập họ và tên nhân viên.')
      return
    }
    if (!finalPosition) {
      setErrorMessage('Vui lòng chọn hoặc nhập vị trí công việc.')
      return
    }

    // Date validation
    if (status === 'TERMINATED') {
      if (!leftOn) {
        setErrorMessage('Nhân viên đã thôi việc bắt buộc phải có Ngày thôi việc (left on).')
        return
      }
      if (employee.hiredOn && leftOn < employee.hiredOn) {
        setErrorMessage(
          `Ngày thôi việc (${leftOn}) không được sớm hơn ngày tuyển dụng (${employee.hiredOn}).`
        )
        return
      }
    }

    const payload: UpdateEmployeePayload = {
      fullName: fullName.trim(),
      positionName: finalPosition,
      phoneNumber: phoneNumber.trim() || null,
      email: email.trim() || null,
      status,
      leftOn: status === 'TERMINATED' ? leftOn : null,
      notes: notes.trim() || null,
    }

    try {
      setIsSubmitting(true)
      const res = await hrApi.updateEmployee(branchId, employee.id, payload)
      onSuccess(res)
      onClose()
    } catch (err: unknown) {
      console.error('Failed to update employee:', err)
      const errObj = err as { response?: { data?: { detail?: string; message?: string } } }
      const backendMsg =
        errObj.response?.data?.detail ||
        errObj.response?.data?.message ||
        'Không thể cập nhật hồ sơ nhân sự. Vui lòng thử lại.'
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

        {/* Modal Window */}
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 12 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 12 }}
          transition={{ duration: 0.2 }}
          className="relative w-full max-w-lg bg-white rounded-2xl shadow-2xl overflow-hidden z-10 flex flex-col max-h-[90vh]"
        >
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 bg-[#304638] text-white">
            <div className="flex items-center space-x-3">
              <div className="p-2 rounded-xl bg-white/10 text-white">
                <UserCheck className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-base font-bold tracking-tight">
                  Chỉnh sửa hồ sơ nhân sự
                </h2>
                <p className="text-xs text-[#C8CEC4]">
                  Mã nhân viên: <span className="font-mono font-semibold">{employee.staffCode}</span>
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
          <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-4">
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

            {/* Họ và tên */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">
                Họ và tên đầy đủ <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
              />
            </div>

            {/* Chức vụ */}
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
                  placeholder="Nhập chức danh..."
                  value={customPosition}
                  onChange={(e) => setCustomPosition(e.target.value)}
                  className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                />
              )}
            </div>

            {/* Trạng thái làm việc */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">
                Trạng thái làm việc <span className="text-rose-500">*</span>
              </label>
              <div className="grid grid-cols-3 gap-2">
                {(['ACTIVE', 'SUSPENDED', 'TERMINATED'] as EmployeeStatus[]).map((st) => {
                  const meta = EMPLOYEE_STATUS_META[st]
                  const isSelected = status === st
                  return (
                    <button
                      key={st}
                      type="button"
                      onClick={() => setStatus(st)}
                      className={`px-3 py-2 rounded-xl text-xs font-semibold border transition-all text-center flex items-center justify-center gap-1.5 ${
                        isSelected
                          ? `${meta.badgeClass} ring-2 ring-offset-1 ring-slate-400 shadow-xs font-bold`
                          : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50'
                      }`}
                    >
                      <span className={`w-2 h-2 rounded-full ${meta.dotClass}`} />
                      <span>{meta.label}</span>
                    </button>
                  )
                })}
              </div>
            </div>

            {/* Ngày thôi việc (chỉ hiện khi TERMINATED) */}
            {status === 'TERMINATED' && (
              <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-300 space-y-2 animate-in fade-in duration-200">
                <div className="flex items-center gap-2 text-amber-800 text-xs font-semibold">
                  <AlertOctagon className="w-4 h-4 text-amber-600 shrink-0" />
                  <span>Xác nhận thôi việc nhân sự</span>
                </div>
                <div className="space-y-1">
                  <label className="text-xs text-slate-700 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-slate-400" />
                    <span>
                      Ngày nghỉ việc (Left on) <span className="text-rose-500">*</span>
                    </span>
                  </label>
                  <input
                    type="date"
                    required
                    value={leftOn}
                    onChange={(e) => setLeftOn(e.target.value)}
                    min={employee.hiredOn}
                    className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                  />
                  <p className="text-[10px] text-slate-500">
                    Ngày tuyển dụng: <strong>{employee.hiredOn}</strong>. Ngày thôi việc phải sau hoặc bằng ngày tuyển dụng.
                  </p>
                </div>
              </div>
            )}

            {/* Số điện thoại & Email */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Phone className="w-3.5 h-3.5 text-slate-400" />
                  <span>Số điện thoại</span>
                </label>
                <input
                  type="tel"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Mail className="w-3.5 h-3.5 text-slate-400" />
                  <span>Email</span>
                </label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                />
              </div>
            </div>

            {/* Ghi chú */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">
                Ghi chú bổ sung
              </label>
              <textarea
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638] resize-none"
              />
            </div>

            {/* Footer Buttons */}
            <div className="pt-3 border-t border-slate-200 flex items-center justify-end space-x-3">
              <Button
                type="button"
                variant="outline"
                onClick={onClose}
                disabled={isSubmitting}
                className="text-xs"
              >
                Hủy
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting}
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-5"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 mr-2 animate-spin" />
                    <span>Đang cập nhật...</span>
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="w-3.5 h-3.5 mr-1.5" />
                    <span>Lưu thay đổi</span>
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
