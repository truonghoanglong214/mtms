import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Clock,
  CheckCircle2,
  AlertTriangle,
  Loader2,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import {
  timesheetApi,
  convertHoursToMinutes,
  convertMinutesToHours,
} from '../api/timesheetApi'
import type {
  TimesheetEntry,
  TimesheetStatus,
  CreateTimesheetPayload,
} from '../types/timesheet.types'

interface SimpleEmployeeOption {
  id: string
  staffCode: string
  fullName: string
  positionName: string
}

interface CreateTimesheetModalProps {
  isOpen: boolean
  branchId: string
  employees: SimpleEmployeeOption[]
  initialEntry?: TimesheetEntry | null
  defaultDate?: string
  onClose: () => void
  onSuccess: () => void
}

export const CreateTimesheetModal: React.FC<CreateTimesheetModalProps> = ({
  isOpen,
  branchId,
  employees,
  initialEntry,
  defaultDate,
  onClose,
  onSuccess,
}) => {
  const [employeeId, setEmployeeId] = useState<string>('')
  const [workDate, setWorkDate] = useState<string>(
    defaultDate || new Date().toISOString().split('T')[0]
  )
  const [hoursInput, setHoursInput] = useState<string>('8')
  const [payableWorkdaysInput, setPayableWorkdaysInput] = useState<string>('1.00')
  const [status, setStatus] = useState<TimesheetStatus>('SUBMITTED')
  const [note, setNote] = useState<string>('')

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Populate form if editing or when opened
  useEffect(() => {
    if (isOpen) {
      if (initialEntry) {
        setEmployeeId(initialEntry.employeeId)
        setWorkDate(initialEntry.workDate)
        setHoursInput(
          convertMinutesToHours(initialEntry.approvedMinutes).toString()
        )
        setPayableWorkdaysInput(initialEntry.payableWorkdays.toFixed(2))
        setStatus(initialEntry.status)
        setNote(initialEntry.note || '')
      } else {
        if (!employeeId && employees.length > 0) {
          setEmployeeId(employees[0].id)
        }
        if (defaultDate) {
          setWorkDate(defaultDate)
        }
        setHoursInput('8')
        setPayableWorkdaysInput('1.00')
        setStatus('SUBMITTED')
        setNote('Ca làm việc tiêu chuẩn')
      }
      setErrorMessage(null)
    }
  }, [isOpen, initialEntry, employees, defaultDate, employeeId])

  // Quick preset helper
  const applyPreset = (hours: number, workdays: number, label: string) => {
    setHoursInput(hours.toString())
    setPayableWorkdaysInput(workdays.toFixed(2))
    setNote(`Ca làm việc ${label}`)
  }

  const handleHoursChange = (val: string) => {
    setHoursInput(val)
    const parsed = parseFloat(val)
    if (!isNaN(parsed) && parsed > 0) {
      // Standard 8h = 1.0 workdays
      const calculatedDays = (parsed / 8).toFixed(2)
      setPayableWorkdaysInput(calculatedDays)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)

    if (!employeeId) {
      setErrorMessage('Vui lòng chọn nhân viên.')
      return
    }
    if (!workDate) {
      setErrorMessage('Vui lòng chọn ngày làm việc.')
      return
    }

    const hours = parseFloat(hoursInput)
    if (isNaN(hours) || hours <= 0) {
      setErrorMessage('Số giờ làm việc phải lớn hơn 0.')
      return
    }

    const workdays = parseFloat(payableWorkdaysInput)
    if (isNaN(workdays) || workdays <= 0) {
      setErrorMessage('Số ngày công quy đổi phải lớn hơn 0.')
      return
    }

    const approvedMinutes = convertHoursToMinutes(hours)

    const payload: CreateTimesheetPayload = {
      employeeId,
      workDate,
      approvedMinutes,
      payableWorkdays: workdays,
      status,
      note: note.trim() || null,
      rawMinutes: approvedMinutes,
    }

    setIsSubmitting(true)
    try {
      await timesheetApi.createOrUpdateTimesheet(branchId, payload)
      onSuccess()
      onClose()
    } catch (err: unknown) {
      console.error('Error saving timesheet:', err)
      const errorObj = err as {
        response?: { data?: { detail?: string; message?: string } }
      }
      setErrorMessage(
        errorObj.response?.data?.detail ||
          errorObj.response?.data?.message ||
          'Không thể lưu bảng chấm công. Vui lòng kiểm tra lại thông tin.'
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm overflow-y-auto">
        <motion.div
          initial={{ opacity: 0, scale: 0.96, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.96, y: 10 }}
          transition={{ duration: 0.18 }}
          className="relative w-full max-w-lg bg-white rounded-2xl shadow-2xl border border-stone-200 overflow-hidden flex flex-col my-8"
        >
          {/* Header */}
          <div className="bg-[#304638] p-6 border-b border-[#25372C] flex items-center justify-between text-white">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-white/10 backdrop-blur-sm">
                <Clock className="h-6 w-6 text-emerald-300" />
              </div>
              <div>
                <h2 className="text-lg font-bold tracking-tight">
                  {initialEntry ? 'Cập nhật Giờ làm việc' : 'Ghi nhận Giờ làm việc (Đơn lẻ)'}
                </h2>
                <p className="text-xs text-white/80">
                  Nhập số giờ làm và ngày công tính lương
                </p>
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="p-1.5 rounded-lg text-white/70 hover:text-white hover:bg-white/10 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="p-6 space-y-4">
            {errorMessage && (
              <motion.div
                initial={{ opacity: 0, y: -4 }}
                animate={{ opacity: 1, y: 0 }}
                className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex gap-2.5 items-start"
              >
                <AlertTriangle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
                <span>{errorMessage}</span>
              </motion.div>
            )}

            {/* Employee Selection */}
            <div>
              <label
                htmlFor="createTimesheetEmpSelect"
                className="block text-xs font-semibold text-stone-700 mb-1"
              >
                Nhân viên <span className="text-rose-500">*</span>
              </label>
              <div className="relative">
                <select
                  id="createTimesheetEmpSelect"
                  value={employeeId}
                  onChange={(e) => setEmployeeId(e.target.value)}
                  disabled={Boolean(initialEntry) || isSubmitting}
                  className="w-full text-xs p-2.5 rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent font-medium text-stone-800"
                >
                  <option value="" disabled>
                    -- Chọn nhân viên --
                  </option>
                  {employees.map((emp) => (
                    <option key={emp.id} value={emp.id}>
                      {emp.fullName} ({emp.staffCode}) — {emp.positionName}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Work Date */}
            <div>
              <label
                htmlFor="createTimesheetWorkDate"
                className="block text-xs font-semibold text-stone-700 mb-1"
              >
                Ngày làm việc <span className="text-rose-500">*</span>
              </label>
              <input
                id="createTimesheetWorkDate"
                type="date"
                value={workDate}
                onChange={(e) => setWorkDate(e.target.value)}
                disabled={Boolean(initialEntry) || isSubmitting}
                className="w-full text-xs p-2.5 rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent font-medium"
              />
            </div>

            {/* Quick Presets */}
            <div>
              <label className="block text-[11px] font-semibold text-stone-500 mb-1.5 uppercase tracking-wider">
                Mẫu ca làm việc nhanh
              </label>
              <div className="grid grid-cols-4 gap-2">
                <button
                  type="button"
                  onClick={() => applyPreset(8, 1.0, '8 tiếng')}
                  className="px-2 py-1.5 rounded-lg border border-stone-200 bg-stone-50 hover:bg-[#304638]/10 hover:border-[#304638] text-[11px] font-medium text-stone-700 text-center transition-colors"
                >
                  Ca 8h (1.0 công)
                </button>
                <button
                  type="button"
                  onClick={() => applyPreset(4, 0.5, '4 tiếng')}
                  className="px-2 py-1.5 rounded-lg border border-stone-200 bg-stone-50 hover:bg-[#304638]/10 hover:border-[#304638] text-[11px] font-medium text-stone-700 text-center transition-colors"
                >
                  Ca 4h (0.5 công)
                </button>
                <button
                  type="button"
                  onClick={() => applyPreset(6, 0.75, '6 tiếng')}
                  className="px-2 py-1.5 rounded-lg border border-stone-200 bg-stone-50 hover:bg-[#304638]/10 hover:border-[#304638] text-[11px] font-medium text-stone-700 text-center transition-colors"
                >
                  Ca 6h (0.75)
                </button>
                <button
                  type="button"
                  onClick={() => applyPreset(12, 1.5, '12 tiếng')}
                  className="px-2 py-1.5 rounded-lg border border-stone-200 bg-stone-50 hover:bg-[#304638]/10 hover:border-[#304638] text-[11px] font-medium text-stone-700 text-center transition-colors"
                >
                  Ca 12h (1.5)
                </button>
              </div>
            </div>

            {/* Hours & Workdays */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label
                  htmlFor="createTimesheetHoursInput"
                  className="block text-xs font-semibold text-stone-700 mb-1"
                >
                  Số giờ làm việc (giờ) <span className="text-rose-500">*</span>
                </label>
                <input
                  id="createTimesheetHoursInput"
                  type="number"
                  step="0.25"
                  min="0.25"
                  max="24"
                  value={hoursInput}
                  onChange={(e) => handleHoursChange(e.target.value)}
                  className="w-full text-xs p-2.5 rounded-xl border border-stone-300 focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent font-bold text-stone-900"
                />
                <span className="text-[10px] text-stone-400 mt-0.5 block">
                  Tương đương:{' '}
                  {convertHoursToMinutes(parseFloat(hoursInput) || 0)} phút
                </span>
              </div>

              <div>
                <label
                  htmlFor="createTimesheetWorkdaysInput"
                  className="block text-xs font-semibold text-stone-700 mb-1"
                >
                  Ngày công quy đổi <span className="text-rose-500">*</span>
                </label>
                <input
                  id="createTimesheetWorkdaysInput"
                  type="number"
                  step="0.05"
                  min="0.05"
                  max="3"
                  value={payableWorkdaysInput}
                  onChange={(e) => setPayableWorkdaysInput(e.target.value)}
                  className="w-full text-xs p-2.5 rounded-xl border border-stone-300 focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent font-bold text-emerald-800"
                />
                <span className="text-[10px] text-stone-400 mt-0.5 block">
                  Công tính lương
                </span>
              </div>
            </div>

            {/* Initial Status */}
            <div>
              <label
                htmlFor="createTimesheetStatusSelect"
                className="block text-xs font-semibold text-stone-700 mb-1"
              >
                Trạng thái lưu ban đầu
              </label>
              <select
                id="createTimesheetStatusSelect"
                value={status}
                onChange={(e) => setStatus(e.target.value as TimesheetStatus)}
                className="w-full text-xs p-2.5 rounded-xl border border-stone-300 bg-white focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent"
              >
                <option value="SUBMITTED">
                  Chờ duyệt (SUBMITTED) — Đưa vào hàng đợi duyệt ngay
                </option>
                <option value="DRAFT">Lưu nháp (DRAFT) — Chưa gửi duyệt</option>
              </select>
            </div>

            {/* Note */}
            <div>
              <label
                htmlFor="createTimesheetNoteInput"
                className="block text-xs font-semibold text-stone-700 mb-1"
              >
                Ghi chú ca làm việc (Tùy chọn)
              </label>
              <input
                id="createTimesheetNoteInput"
                type="text"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Ví dụ: Ca sáng 07:00 - 15:00, làm thêm 30 phút..."
                className="w-full text-xs p-2.5 rounded-xl border border-stone-300 focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent"
              />
            </div>

            {/* Actions */}
            <div className="flex items-center justify-end gap-3 pt-3 border-t border-stone-200">
              <Button
                type="button"
                variant="outline"
                disabled={isSubmitting}
                onClick={onClose}
                className="text-xs"
              >
                Hủy bỏ
              </Button>
              <Button
                type="submit"
                disabled={isSubmitting}
                className="bg-[#304638] hover:bg-[#25372C] text-white text-xs gap-1.5"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Đang lưu...
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="h-4 w-4" />
                    {initialEntry ? 'Cập nhật bản ghi' : 'Lưu chấm công'}
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
