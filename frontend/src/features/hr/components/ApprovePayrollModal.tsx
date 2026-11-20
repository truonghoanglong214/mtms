import React, { useState } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  ShieldCheck,
  ShieldAlert,
  AlertTriangle,
  Lock,
  Loader2,
  Users,
  Coins,
  Building2,
  Calendar,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { useAuthStore } from '@/features/auth/authStore'
import { payrollApi, formatVND } from '../api/payrollApi'
import type { PayrollPeriod } from '../types/payroll.types'

interface ApprovePayrollModalProps {
  isOpen: boolean
  period: PayrollPeriod | null
  branchId: string
  branchName?: string
  onClose: () => void
  onSuccess: () => void
}

export const ApprovePayrollModal: React.FC<ApprovePayrollModalProps> = ({
  isOpen,
  period,
  branchId,
  branchName,
  onClose,
  onSuccess,
}) => {
  const user = useAuthStore((state) => state.user)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  if (!isOpen || !period) return null

  // Format month MM/YYYY
  const formatMonthYear = (dateStr: string) => {
    try {
      const [year, month] = dateStr.split('-')
      return `Tháng ${month}/${year}`
    } catch {
      return dateStr
    }
  }

  // Security Authority Check (TC-017 / HR-INV-02)
  const isOwnerOrAdmin =
    user?.grants?.some(
      (g) => g.roleKey === 'BRANCH_OWNER' || g.roleKey === 'SYSTEM_ADMIN'
    ) || false

  const isManagerOnly =
    !isOwnerOrAdmin &&
    (user?.grants?.some((g) => g.roleKey === 'MANAGER') ?? false)

  const handleApprove = async () => {
    setErrorMessage(null)

    if (isManagerOnly) {
      setErrorMessage(
        'Quy tắc an toàn TC-017 & HR-INV-02: Quản lý cửa hàng (Manager) KHÔNG có quyền phê duyệt bảng lương. Thao tác này bắt buộc phải do Chủ quán (Branch Owner) hoặc Quản trị viên thực hiện.'
      )
      return
    }

    setIsSubmitting(true)
    try {
      await payrollApi.approvePayrollPeriod(branchId, period.id)
      onSuccess()
      onClose()
    } catch (err: unknown) {
      console.error('Failed to approve payroll period:', err)
      const errResponse = (
        err as { response?: { data?: { detail?: string; message?: string; type?: string } } }
      )?.response?.data

      if (errResponse?.type === 'urn:mtms:error:self-approval-denied') {
        setErrorMessage(
          'Từ chối bảo mật TC-017: Quản lý không thể tự phê duyệt bảng lương của chi nhánh. Bắt buộc Chủ quán ký duyệt.'
        )
      } else {
        setErrorMessage(
          errResponse?.detail ||
            errResponse?.message ||
            'Không thể phê duyệt bảng lương. Vui lòng kiểm tra quyền hạn hoặc trạng thái kỳ lương.'
        )
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm overflow-y-auto">
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 10 }}
          transition={{ duration: 0.15 }}
          className="relative w-full max-w-xl bg-white rounded-2xl shadow-2xl border border-stone-200 overflow-hidden my-8"
        >
          {/* Header */}
          <div className="px-6 py-4 border-b border-stone-200 bg-stone-50 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-2.5 rounded-xl bg-emerald-700 text-white shadow-xs">
                <ShieldCheck className="h-5 w-5" />
              </div>
              <div>
                <div className="flex items-center space-x-2">
                  <h3 className="text-base font-bold text-stone-900">
                    Phê duyệt & Khóa bảng lương
                  </h3>
                  <Badge className="bg-emerald-100 text-emerald-800 border-emerald-300 font-mono text-[10px]">
                    TC-017
                  </Badge>
                </div>
                <p className="text-xs text-stone-500">
                  Chốt chi phí lương chính thức và tạo bản lưu snapshot bất biến
                </p>
              </div>
            </div>
            <button
              onClick={onClose}
              disabled={isSubmitting}
              className="p-1.5 rounded-lg text-stone-400 hover:text-stone-600 hover:bg-stone-200/60 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="p-6 space-y-5">
            {/* TC-017 Role Guard Warning Banner for Manager */}
            {isManagerOnly && (
              <div className="p-4 bg-rose-50 border border-rose-300 rounded-xl flex items-start space-x-3 text-rose-900">
                <ShieldAlert className="h-5 w-5 text-rose-600 shrink-0 mt-0.5" />
                <div className="space-y-1">
                  <h5 className="font-bold text-xs uppercase tracking-wide text-rose-800">
                    Cảnh báo kiểm soát cấm tự duyệt (TC-017)
                  </h5>
                  <p className="text-xs text-rose-700 leading-relaxed">
                    Bạn đang đăng nhập với vai trò <strong>Quản lý cửa hàng (Manager)</strong>. Theo quy tắc kiểm soát nội bộ MTMS (TC-017 / HR-INV-02), Quản lý KHÔNG có thẩm quyền duyệt bảng lương của chính chi nhánh mình điều hành.
                  </p>
                  <p className="text-[11px] text-rose-600 font-semibold">
                    Vui lòng thông báo cho <strong>Chủ quán (Branch Owner)</strong> hoặc <strong>Quản trị viên</strong> đăng nhập để thực hiện phê duyệt.
                  </p>
                </div>
              </div>
            )}

            {/* Invariant Snapshot Lock Notice */}
            <div className="p-4 bg-amber-50/80 border border-amber-200 rounded-xl flex items-start space-x-3 text-amber-900">
              <Lock className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
              <div className="space-y-1 text-xs">
                <h5 className="font-bold text-amber-800">
                  Cơ chế khóa Snapshot bất biến (BR-HR-05 & TC-015)
                </h5>
                <p className="text-amber-800 leading-relaxed">
                  Khi đã phê duyệt, bảng lương sẽ chuyển sang trạng thái <strong>ĐÃ CHỐT DUYỆT (APPROVED)</strong> và bị khóa vĩnh viễn:
                </p>
                <ul className="list-disc list-inside text-[11px] text-amber-700 space-y-0.5 pl-1">
                  <li>Không thể tính lại (Recalculate) từ chấm công.</li>
                  <li>Không thể thêm, sửa hoặc xóa các khoản điều chỉnh thủ công.</li>
                  <li>Dữ liệu đóng vai trò căn cứ hạch toán chi phí lương vào Sổ quỹ và Báo cáo P&L quản trị.</li>
                </ul>
              </div>
            </div>

            {/* Error Message */}
            {errorMessage && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl flex items-start space-x-2 text-rose-800 text-xs">
                <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
                <div className="flex-1 font-medium">{errorMessage}</div>
              </div>
            )}

            {/* Summary Information Grid */}
            <div className="bg-stone-50 rounded-xl p-4 border border-stone-200 space-y-3">
              <div className="flex items-center justify-between text-xs pb-2 border-b border-stone-200">
                <span className="flex items-center space-x-1.5 text-stone-600">
                  <Calendar className="h-3.5 w-3.5 text-stone-500" />
                  <span>Kỳ bảng lương:</span>
                </span>
                <span className="font-bold text-stone-900">
                  {formatMonthYear(period.monthStart)}
                </span>
              </div>

              {branchName && (
                <div className="flex items-center justify-between text-xs pb-2 border-b border-stone-200">
                  <span className="flex items-center space-x-1.5 text-stone-600">
                    <Building2 className="h-3.5 w-3.5 text-stone-500" />
                    <span>Chi nhánh:</span>
                  </span>
                  <span className="font-semibold text-stone-800">{branchName}</span>
                </div>
              )}

              <div className="flex items-center justify-between text-xs pb-2 border-b border-stone-200">
                <span className="flex items-center space-x-1.5 text-stone-600">
                  <Users className="h-3.5 w-3.5 text-stone-500" />
                  <span>Số lượng nhân viên:</span>
                </span>
                <span className="font-bold font-mono text-stone-900">
                  {period.employeeCount} nhân sự
                </span>
              </div>

              <div className="flex items-center justify-between text-xs pb-2 border-b border-stone-200">
                <span className="text-stone-600">Tổng lương cơ bản:</span>
                <span className="font-mono text-stone-800">
                  {formatVND(period.totalBaseSalary)}
                </span>
              </div>

              <div className="flex items-center justify-between text-xs pb-2 border-b border-stone-200">
                <span className="text-stone-600">Tổng điều chỉnh ròng (Thưởng/Phạt):</span>
                <span
                  className={`font-mono font-semibold ${
                    period.totalAdjustments >= 0 ? 'text-emerald-700' : 'text-rose-700'
                  }`}
                >
                  {period.totalAdjustments >= 0 ? '+' : ''}
                  {formatVND(period.totalAdjustments)}
                </span>
              </div>

              <div className="flex items-center justify-between pt-1">
                <span className="text-sm font-bold text-stone-900 flex items-center space-x-1">
                  <Coins className="h-4 w-4 text-[#304638] mr-1" />
                  <span>Tổng quỹ lương thực lĩnh (Gross):</span>
                </span>
                <span className="text-base font-bold font-mono text-[#304638]">
                  {formatVND(period.totalGrossSalary)}
                </span>
              </div>
            </div>
          </div>

          {/* Footer Actions */}
          <div className="px-6 py-4 border-t border-stone-200 bg-stone-50 flex items-center justify-end space-x-3">
            <Button
              variant="outline"
              onClick={onClose}
              disabled={isSubmitting}
              className="border-stone-300 text-stone-700 hover:bg-stone-100"
            >
              Hủy
            </Button>
            <Button
              onClick={handleApprove}
              disabled={isSubmitting || isManagerOnly}
              className={`font-semibold text-white ${
                isManagerOnly
                  ? 'bg-stone-300 text-stone-500 cursor-not-allowed'
                  : 'bg-emerald-700 hover:bg-emerald-800 shadow-sm'
              }`}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Đang phê duyệt...
                </>
              ) : (
                <>
                  <ShieldCheck className="h-4 w-4 mr-2" />
                  Chủ quán xác nhận phê duyệt & Khóa bảng lương
                </>
              )}
            </Button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
