import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  GitFork,
  Building2,
  Calendar,
  AlertTriangle,
  Info,
  CheckCircle2,
  Loader2,
  ShieldAlert,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { hrApi } from '../api/hrApi'
import type {
  EmployeeDetail,
  EmployeeSummary,
  EmployeeAssignment,
  AssignBranchPayload,
} from '../types/hr.types'

interface BranchOption {
  id: string
  name: string
  code?: string
}

interface AssignBranchModalProps {
  isOpen: boolean
  branchId: string
  employee: EmployeeDetail | EmployeeSummary | null
  branches: BranchOption[]
  onClose: () => void
  onSuccess: (assignment: EmployeeAssignment) => void
}

export const AssignBranchModal: React.FC<AssignBranchModalProps> = ({
  isOpen,
  branchId,
  employee,
  branches,
  onClose,
  onSuccess,
}) => {
  const [targetBranchId, setTargetBranchId] = useState('')
  const [isPrimary, setIsPrimary] = useState(true)
  const [effectiveFrom, setEffectiveFrom] = useState(
    new Date().toISOString().split('T')[0]
  )
  const [effectiveTo, setEffectiveTo] = useState('')
  const [notes, setNotes] = useState('')

  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  useEffect(() => {
    if (isOpen && employee) {
      // Default to first different branch if available
      const otherBranch = branches.find((b) => b.id !== branchId)
      setTargetBranchId(otherBranch ? otherBranch.id : branchId)
      setIsPrimary(true)
      setEffectiveFrom(new Date().toISOString().split('T')[0])
      setEffectiveTo('')
      setNotes('')
      setErrorMessage(null)
    }
  }, [isOpen, employee, branchId, branches])

  if (!isOpen || !employee) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!branchId || !employee.id) return
    setErrorMessage(null)

    if (!targetBranchId) {
      setErrorMessage('Vui lòng chọn chi nhánh đích cần phân bổ.')
      return
    }

    if (!effectiveFrom) {
      setErrorMessage('Vui lòng chọn ngày bắt đầu hiệu lực.')
      return
    }

    if (effectiveTo && effectiveTo < effectiveFrom) {
      setErrorMessage('Ngày kết thúc hiệu lực không được trước ngày bắt đầu.')
      return
    }

    const payload: AssignBranchPayload = {
      targetBranchId,
      isPrimary,
      effectiveFrom,
      effectiveTo: effectiveTo || null,
      notes: notes.trim() || null,
    }

    try {
      setIsSubmitting(true)
      const res = await hrApi.assignBranch(branchId, employee.id, payload)
      onSuccess(res)
      onClose()
    } catch (err: unknown) {
      console.error('Failed to assign branch:', err)
      const errObj = err as { response?: { data?: { detail?: string; message?: string } } }
      const backendMsg =
        errObj.response?.data?.detail ||
        errObj.response?.data?.message ||
        'Không thể phân bổ chi nhánh. Vui lòng kiểm tra lại thời gian hiệu lực và thử lại.'
      setErrorMessage(backendMsg)
    } finally {
      setIsSubmitting(false)
    }
  }

  const selectedTarget = branches.find((b) => b.id === targetBranchId)

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

        {/* Modal Content */}
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
                <GitFork className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-base font-bold tracking-tight">
                  Điều chuyển / Phân bổ chi nhánh
                </h2>
                <p className="text-xs text-[#C8CEC4]">
                  Nhân viên: <span className="font-semibold">{employee.fullName}</span> ({employee.staffCode})
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

            {/* Quy tắc TC-015 Alert Card */}
            <div className="p-3.5 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-900 space-y-1">
              <div className="flex items-center gap-1.5 font-bold text-amber-950">
                <ShieldAlert className="w-4 h-4 text-amber-700 shrink-0" />
                <span>Quy tắc phân bổ chi phí lương</span>
              </div>
              <p className="text-[11px] leading-relaxed text-amber-800">
                Tại bất kỳ ngày nào, nhân viên chỉ có tối đa <strong>1 chi nhánh chính</strong>. Khi kích hoạt chi nhánh chính mới, phân bổ cũ sẽ tự động chốt mốc kết thúc. <strong>Chi nhánh chính vào ngày cuối tháng sẽ chịu toàn bộ chi phí lương</strong> trong kỳ tính lương đó.
              </p>
            </div>

            {/* Chi nhánh đích */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                <Building2 className="w-3.5 h-3.5 text-slate-400" />
                <span>
                  Chi nhánh phân bổ đến <span className="text-rose-500">*</span>
                </span>
              </label>
              <select
                value={targetBranchId}
                onChange={(e) => setTargetBranchId(e.target.value)}
                className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
              >
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name} {b.code ? `(${b.code})` : ''}
                  </option>
                ))}
              </select>
              {selectedTarget && (
                <p className="text-[11px] text-slate-500 flex items-center gap-1">
                  <Info className="w-3 h-3 text-slate-400" />
                  <span>
                    Phân bổ làm việc tại: <strong>{selectedTarget.name}</strong>
                  </span>
                </p>
              )}
            </div>

            {/* Đặt làm chi nhánh chính */}
            <div className="p-3 rounded-xl bg-slate-50 border border-slate-200">
              <label className="flex items-start gap-2.5 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={isPrimary}
                  onChange={(e) => setIsPrimary(e.target.checked)}
                  className="w-4 h-4 mt-0.5 text-[#304638] rounded border-slate-300 focus:ring-[#304638]"
                />
                <div className="space-y-0.5">
                  <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                    <span>Đặt làm Chi nhánh chính (Primary Branch)</span>
                    {isPrimary && (
                      <Badge className="bg-emerald-600 text-white text-[10px] py-0 px-1.5">
                        Chính
                      </Badge>
                    )}
                  </span>
                  <p className="text-[11px] text-slate-500 leading-normal">
                    Nếu tắt cờ này, nhân viên sẽ là nhân sự hỗ trợ / chi viện (phân bổ phụ) và chi phí lương vẫn do chi nhánh chính gánh chịu.
                  </p>
                </div>
              </label>
            </div>

            {/* Thời gian hiệu lực */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {/* Ngày bắt đầu */}
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

              {/* Ngày kết thúc */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-slate-400" />
                  <span>Kết thúc (tùy chọn)</span>
                </label>
                <input
                  type="date"
                  value={effectiveTo}
                  min={effectiveFrom}
                  onChange={(e) => setEffectiveTo(e.target.value)}
                  placeholder="Để trống nếu vô thời hạn"
                  className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638]"
                />
              </div>
            </div>

            {/* Ghi chú điều chuyển */}
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">
                Lý do điều chuyển / Ghi chú
              </label>
              <textarea
                rows={2}
                placeholder="VD: Điều chuyển tăng cường nhân sự hỗ trợ chi nhánh mới khai trương..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs text-slate-800 focus:outline-none focus:ring-2 focus:ring-[#304638] resize-none"
              />
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
                    <span>Đang cập nhật...</span>
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="w-3.5 h-3.5 mr-1.5" />
                    <span>Xác nhận phân bổ</span>
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
