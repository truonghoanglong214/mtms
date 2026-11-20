import React, { useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  CheckCircle2,
  AlertTriangle,
  ShieldAlert,
  Loader2,
  Calendar,
  User,
  Clock,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { useAuthStore } from '@/features/auth/authStore'
import {
  timesheetApi,
  formatMinutesToHours,
  formatWorkdays,
} from '../api/timesheetApi'
import type { TimesheetEntry } from '../types/timesheet.types'

export type ApprovalModalMode = 'APPROVE' | 'REJECT' | 'BULK_APPROVE'

interface TimesheetApprovalModalProps {
  isOpen: boolean
  branchId: string
  mode: ApprovalModalMode
  entries: TimesheetEntry[]
  onClose: () => void
  onSuccess: () => void
}

export const TimesheetApprovalModal: React.FC<TimesheetApprovalModalProps> = ({
  isOpen,
  branchId,
  mode,
  entries,
  onClose,
  onSuccess,
}) => {
  const user = useAuthStore((state) => state.user)
  const [noteOrReason, setNoteOrReason] = useState(
    mode === 'REJECT' ? '' : 'Xác nhận đúng giờ làm thực tế'
  )
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // TC-017 Guard Logic
  const isOwnerOrAdmin =
    user?.grants?.some(
      (g) => g.roleKey === 'BRANCH_OWNER' || g.roleKey === 'SYSTEM_ADMIN'
    ) || false

  const isManagerOnly =
    !isOwnerOrAdmin &&
    (user?.grants?.some((g) => g.roleKey === 'MANAGER') ?? false)

  // Check if any selected entry matches current user's displayName or identifier
  const hasPotentialSelfApproval =
    isManagerOnly &&
    entries.some(
      (e) =>
        e.employeeName.trim().toLowerCase() ===
          user?.displayName?.trim().toLowerCase() ||
        (user?.id && e.employeeId === user.id)
    )

  const isRejectMode = mode === 'REJECT'
  const isBulk = mode === 'BULK_APPROVE' || entries.length > 1

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)

    if (isRejectMode && !noteOrReason.trim()) {
      setErrorMessage('Vui lòng nhập lý do từ chối để nhân viên nắm rõ.')
      return
    }

    if (!isRejectMode && hasPotentialSelfApproval && isManagerOnly) {
      setErrorMessage(
        'Quy tắc an toàn TC-017: Quản lý không thể tự phê duyệt giờ làm của chính mình. Vui lòng bỏ chọn bản ghi của bạn.'
      )
      return
    }

    setIsSubmitting(true)
    try {
      if (isRejectMode) {
        const target = entries[0]
        if (!target) return
        await timesheetApi.rejectTimesheet(branchId, target.id, {
          rejectionReason: noteOrReason.trim(),
        })
      } else if (mode === 'BULK_APPROVE') {
        const entryIds = entries.map((item) => item.id)
        await timesheetApi.bulkApproveTimesheets(branchId, {
          entryIds,
          approvalNote: noteOrReason.trim() || undefined,
        })
      } else {
        const target = entries[0]
        if (!target) return
        await timesheetApi.approveTimesheet(branchId, target.id, {
          approvalNote: noteOrReason.trim() || undefined,
        })
      }

      onSuccess()
      onClose()
    } catch (err: unknown) {
      console.error('Approval action error:', err)
      const errorObj = err as {
        response?: { data?: { detail?: string; type?: string; message?: string } }
      }
      const problem = errorObj.response?.data
      if (
        problem?.type === 'urn:mtms:error:self-approval-denied' ||
        problem?.detail?.includes('self-approval') ||
        problem?.message?.includes('self-approval')
      ) {
        setErrorMessage(
          'Quy tắc an toàn TC-017: Quản lý không thể tự phê duyệt giờ làm của chính mình! Bản ghi của Quản lý phải do Chủ quán (Owner) duyệt.'
        )
      } else {
        setErrorMessage(
          problem?.detail ||
            problem?.message ||
            'Thao tác không thành công. Vui lòng thử lại.'
        )
      }
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
          className="relative w-full max-w-xl bg-white rounded-2xl shadow-2xl border border-stone-200 overflow-hidden flex flex-col my-8"
        >
          {/* Header */}
          <div
            className={`p-6 border-b flex items-center justify-between text-white ${
              isRejectMode ? 'bg-[#9E2A2B]' : 'bg-[#304638]'
            }`}
          >
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-white/10 backdrop-blur-sm">
                {isRejectMode ? (
                  <AlertTriangle className="h-6 w-6 text-rose-200" />
                ) : (
                  <CheckCircle2 className="h-6 w-6 text-emerald-300" />
                )}
              </div>
              <div>
                <h2 className="text-lg font-bold tracking-tight">
                  {isRejectMode
                    ? 'Từ chối Giờ làm việc'
                    : isBulk
                    ? `Phê duyệt Hàng loạt (${entries.length} dòng)`
                    : 'Phê duyệt Giờ làm việc'}
                </h2>
                <p className="text-xs text-white/80">
                  {isRejectMode
                    ? 'Yêu cầu nhân viên hoặc quản lý nhập lại thông tin ca'
                    : 'Kiểm soát bất biến và xác nhận tính lương theo TC-017'}
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

          <form onSubmit={handleSubmit} className="p-6 space-y-5">
            {/* TC-017 Warning Banner */}
            <div className="rounded-xl border border-amber-200 bg-amber-50/80 p-4 flex gap-3 text-amber-900 text-xs leading-relaxed">
              <ShieldAlert className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-amber-950">
                  Quy tắc an toàn TC-017 & Bất biến Quản lý:
                </p>
                <p className="mt-0.5 text-amber-800">
                  Quản lý cửa hàng không thể tự phê duyệt giờ làm của chính
                  mình; bản ghi của Quản lý sẽ do Chủ quán (Branch Owner) hoặc
                  Quản trị viên phê duyệt. Mọi hành động duyệt đều được ghi vết
                  kiểm toán không thể đảo ngược.
                </p>
              </div>
            </div>

            {/* Error Message */}
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

            {/* Self-approval blocking notice */}
            {hasPotentialSelfApproval && !isRejectMode && (
              <div className="p-3.5 rounded-xl bg-red-50 border border-red-200 text-red-900 text-xs flex gap-2.5 items-start font-medium">
                <AlertTriangle className="h-4 w-4 text-red-600 shrink-0 mt-0.5" />
                <div>
                  <span className="font-bold">Cảnh báo vi phạm TC-017:</span> Danh
                  sách đang chứa bản ghi chấm công của chính bạn. Với vai trò Quản lý,
                  bạn không có thẩm quyền tự duyệt công của mình.
                </div>
              </div>
            )}

            {/* Target Entry Preview */}
            <div className="bg-stone-50 rounded-xl p-4 border border-stone-200 space-y-2.5 text-xs">
              <div className="text-[11px] font-semibold uppercase tracking-wider text-stone-500">
                {isBulk ? 'Danh sách bản ghi thao tác' : 'Chi tiết dòng công'}
              </div>

              {!isBulk && entries[0] ? (
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="flex items-center gap-1.5 font-semibold text-stone-800 text-sm">
                      <User className="h-4 w-4 text-stone-500" />
                      {entries[0].employeeName}
                      <span className="font-mono text-stone-500 font-normal">
                        ({entries[0].staffCode})
                      </span>
                    </span>
                    <Badge variant="outline" className="border-stone-300">
                      {entries[0].positionName}
                    </Badge>
                  </div>

                  <div className="grid grid-cols-2 gap-3 pt-2 border-t border-stone-200">
                    <div className="flex items-center gap-1.5 text-stone-600">
                      <Calendar className="h-3.5 w-3.5 text-stone-400" />
                      <span>Ngày: </span>
                      <strong className="text-stone-800">
                        {entries[0].workDate}
                      </strong>
                    </div>
                    <div className="flex items-center gap-1.5 text-stone-600">
                      <Clock className="h-3.5 w-3.5 text-stone-400" />
                      <span>Giờ duyệt: </span>
                      <strong className="text-[#304638]">
                        {formatMinutesToHours(entries[0].approvedMinutes)}
                      </strong>
                      <span className="text-stone-400">
                        ({formatWorkdays(entries[0].payableWorkdays)})
                      </span>
                    </div>
                  </div>

                  {entries[0].note && (
                    <div className="pt-2 text-stone-600 italic border-t border-dashed border-stone-200">
                      Ghi chú ca: &ldquo;{entries[0].note}&rdquo;
                    </div>
                  )}
                </div>
              ) : (
                <div className="max-h-40 overflow-y-auto space-y-1.5 pr-1 divide-y divide-stone-200">
                  {entries.map((item) => (
                    <div
                      key={item.id}
                      className="flex items-center justify-between pt-1.5 text-stone-700"
                    >
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{item.employeeName}</span>
                        <span className="text-[10px] text-stone-500">
                          {item.workDate}
                        </span>
                      </div>
                      <div className="font-medium text-[#304638]">
                        {formatMinutesToHours(item.approvedMinutes)} ·{' '}
                        {formatWorkdays(item.payableWorkdays)}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Note / Rejection Reason Input */}
            <div className="space-y-1.5">
              <label
                htmlFor="approvalNoteInput"
                className="block text-xs font-semibold text-stone-700"
              >
                {isRejectMode ? (
                  <span className="text-rose-700">
                    Lý do từ chối bản ghi <span className="text-rose-500">*</span>
                  </span>
                ) : (
                  'Ghi chú phê duyệt (Tùy chọn)'
                )}
              </label>
              <textarea
                id="approvalNoteInput"
                rows={3}
                value={noteOrReason}
                onChange={(e) => setNoteOrReason(e.target.value)}
                placeholder={
                  isRejectMode
                    ? 'Ví dụ: Nhân viên về sớm chưa xin phép; Cần kiểm tra lại ca làm việc...'
                    : 'Ghi chú xác nhận giờ làm thực tế...'
                }
                className="w-full text-xs p-3 rounded-xl border border-stone-300 focus:outline-none focus:ring-2 focus:ring-[#304638] focus:border-transparent resize-none"
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
                disabled={
                  isSubmitting ||
                  (!isRejectMode && hasPotentialSelfApproval && isManagerOnly)
                }
                className={`text-xs gap-1.5 ${
                  isRejectMode
                    ? 'bg-rose-600 hover:bg-rose-700 text-white'
                    : 'bg-[#304638] hover:bg-[#25372C] text-white'
                }`}
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Đang xử lý...
                  </>
                ) : isRejectMode ? (
                  <>
                    <AlertTriangle className="h-4 w-4" />
                    Xác nhận Từ chối
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="h-4 w-4" />
                    {isBulk ? 'Duyệt tất cả bản ghi' : 'Xác nhận Phê duyệt'}
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
