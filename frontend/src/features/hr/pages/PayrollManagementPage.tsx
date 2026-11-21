import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Calendar,
  Building2,
  DollarSign,
  Coins,
  Users,
  RefreshCw,
  ShieldCheck,
  Lock,
  AlertTriangle,
  FileCheck,
  CheckCircle2,
  Sparkles,
  Wallet,
  CreditCard,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { Card, CardContent } from '@/shared/components/ui/card'
import { useAuthStore } from '@/features/auth/authStore'
import { apiClient } from '@/shared/api/apiClient'
import type { ApiResponse } from '@/shared/types'
import { payrollApi, formatVND } from '../api/payrollApi'
import { PayrollLinesTable } from '../components/PayrollLinesTable'
import { CalculationDetailModal } from '../components/CalculationDetailModal'
import { CreateAdjustmentModal } from '../components/CreateAdjustmentModal'
import { ApprovePayrollModal } from '../components/ApprovePayrollModal'
import { PaySalaryModal } from '../components/PaySalaryModal'
import { BulkPaySalaryModal } from '../components/BulkPaySalaryModal'
import { PaymentHistoryModal } from '../components/PaymentHistoryModal'
import { EmployeePayslipModal } from '../components/EmployeePayslipModal'
import {
  PAYROLL_STATUS_CONFIG,
  type PayrollPeriod,
  type PayrollLine,
} from '../types/payroll.types'

interface BranchSummary {
  id: string
  code?: string
  name: string
  status?: string
}

export const PayrollManagementPage: React.FC = () => {
  const activeBranchId = useAuthStore((state) => state.activeBranchId)
  const setActiveBranch = useAuthStore((state) => state.setActiveBranch)

  // Branch Selection
  const [branches, setBranches] = useState<BranchSummary[]>([])
  const [selectedBranchId, setSelectedBranchId] = useState<string>(activeBranchId || '')

  // Month & Year Selection
  const now = new Date()
  const [selectedYear, setSelectedYear] = useState<number>(now.getFullYear())
  const [selectedMonth, setSelectedMonth] = useState<number>(now.getMonth() + 1)

  // Computed monthStart string (YYYY-MM-01)
  const monthStart = useMemo(() => {
    return `${selectedYear}-${String(selectedMonth).padStart(2, '0')}-01`
  }, [selectedYear, selectedMonth])

  // Current Payroll Period State
  const [currentPeriod, setCurrentPeriod] = useState<PayrollPeriod | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [isCalculating, setIsCalculating] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  // Modals
  const [selectedLineForDetail, setSelectedLineForDetail] = useState<PayrollLine | null>(null)
  const [selectedLineForAdjustment, setSelectedLineForAdjustment] = useState<PayrollLine | null>(null)
  const [isApproveModalOpen, setIsApproveModalOpen] = useState<boolean>(false)
  const [selectedLineForPay, setSelectedLineForPay] = useState<PayrollLine | null>(null)
  const [selectedLineForPayments, setSelectedLineForPayments] = useState<PayrollLine | null>(null)
  const [selectedLineForPayslip, setSelectedLineForPayslip] = useState<PayrollLine | null>(null)
  const [isBulkPayModalOpen, setIsBulkPayModalOpen] = useState<boolean>(false)

  // Selected Branch Name
  const selectedBranchName = useMemo(() => {
    const b = branches.find((item) => item.id === selectedBranchId)
    return b ? b.name : ''
  }, [branches, selectedBranchId])

  // 1. Fetch Branches
  useEffect(() => {
    let isMounted = true
    apiClient
      .get<ApiResponse<BranchSummary[]>>('/organizations/branches')
      .then((res) => {
        if (!isMounted) return
        const list = res.data?.data || []
        setBranches(list)
        if (!selectedBranchId && list.length > 0) {
          setSelectedBranchId(list[0].id)
          setActiveBranch(list[0].id, list[0].name)
        }
      })
      .catch((err) => {
        console.error('Failed to load branches:', err)
      })

    return () => {
      isMounted = false
    }
  }, [selectedBranchId, setActiveBranch])

  useEffect(() => {
    if (activeBranchId && activeBranchId !== selectedBranchId) {
      setSelectedBranchId(activeBranchId)
    }
  }, [activeBranchId, selectedBranchId])

  // 2. Fetch Payroll Period Data
  const fetchPayrollPeriod = useCallback(async () => {
    if (!selectedBranchId) return
    setIsLoading(true)
    setErrorMessage(null)
    try {
      const data = await payrollApi.getCurrentPayrollPeriod(selectedBranchId, monthStart)
      setCurrentPeriod(data)
    } catch (err: unknown) {
      console.warn('Current payroll period not found or error:', err)
      // If 404, we set currentPeriod to null so user can trigger "Tính toán bảng lương"
      setCurrentPeriod(null)
    } finally {
      setIsLoading(false)
    }
  }, [selectedBranchId, monthStart])

  useEffect(() => {
    fetchPayrollPeriod()
  }, [fetchPayrollPeriod])

  // Recalculate / Generate Payroll Action
  const handleRecalculatePayroll = async () => {
    if (!selectedBranchId) return
    setIsCalculating(true)
    setErrorMessage(null)
    setSuccessMessage(null)
    try {
      const result = await payrollApi.generateOrRecalculatePayroll(selectedBranchId, monthStart)
      setCurrentPeriod(result)
      setSuccessMessage(
        `Đã tính toán thành công bảng lương Tháng ${selectedMonth}/${selectedYear} với ${result.employeeCount} nhân sự.`
      )
      setTimeout(() => setSuccessMessage(null), 5000)
    } catch (err: unknown) {
      console.error('Failed to generate payroll:', err)
      const errResponse = (err as { response?: { data?: { detail?: string; message?: string } } })
        ?.response?.data
      setErrorMessage(
        errResponse?.detail ||
          errResponse?.message ||
          'Không thể tính toán bảng lương. Vui lòng kiểm tra lại dữ liệu chấm công đã duyệt.'
      )
    } finally {
      setIsCalculating(false)
    }
  }

  // Delete Adjustment Action
  const handleDeleteAdjustment = async (adjId: string) => {
    if (!selectedBranchId) return
    try {
      await payrollApi.deleteAdjustment(selectedBranchId, adjId)
      await fetchPayrollPeriod()
      setSuccessMessage('Đã xóa khoản điều chỉnh thành công.')
      setTimeout(() => setSuccessMessage(null), 4000)
    } catch (err: unknown) {
      console.error('Failed to delete adjustment:', err)
      const errResponse = (err as { response?: { data?: { detail?: string; message?: string } } })
        ?.response?.data
      setErrorMessage(
        errResponse?.detail ||
          errResponse?.message ||
          'Không thể xóa khoản điều chỉnh. Kỳ bảng lương có thể đã được duyệt khóa.'
      )
    }
  }

  const periodStatus = currentPeriod?.status || 'DRAFT'
  const isApproved = periodStatus === 'APPROVED'
  const statusCfg = PAYROLL_STATUS_CONFIG[periodStatus]

  // Calculated totals across lines
  const totalGrossSalary = Number(currentPeriod?.totalGrossSalary) || 0
  const totalBaseSalary = Number(currentPeriod?.totalBaseSalary) || 0
  const totalPaidAmount = useMemo(() => {
    if (!currentPeriod?.lines) return 0
    return currentPeriod.lines.reduce((sum, l) => sum + (Number(l.paidAmount) || 0), 0)
  }, [currentPeriod?.lines])

  const totalRemainingDebt = Math.max(0, totalGrossSalary - totalPaidAmount)
  const hasUnpaidLines = useMemo(() => {
    if (!currentPeriod?.lines) return false
    return currentPeriod.lines.some(
      (l) => (Number(l.grossSalary) || 0) > (Number(l.paidAmount) || 0)
    )
  }, [currentPeriod?.lines])

  return (
    <div className="p-4 sm:p-6 space-y-6 max-w-7xl mx-auto">
      {/* Top Header & Page Title */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2.5">
            <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-stone-900">
              Bảng lương & Chi trả
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-stone-500 mt-1">
            Động cơ tính lương đa lát, chốt khóa snapshot bất biến & xuất quỹ chi trả liên kết sổ quỹ
          </p>
        </div>

        {/* Global Action Buttons */}
        <div className="flex flex-wrap items-center gap-2.5">
          {/* Recalculate button */}
          {!isApproved && (
            <Button
              onClick={handleRecalculatePayroll}
              disabled={isCalculating || isLoading || !selectedBranchId}
              variant="outline"
              className="border-stone-300 text-stone-700 hover:bg-stone-100 h-9 text-xs font-semibold"
            >
              <RefreshCw
                className={`h-3.5 w-3.5 mr-1.5 ${isCalculating ? 'animate-spin' : ''}`}
              />
              <span>
                {currentPeriod ? 'Tính lại bảng lương (Recalculate)' : 'Tính toán bảng lương'}
              </span>
            </Button>
          )}

          {/* Bulk Pay button */}
          {isApproved && hasUnpaidLines && (
            <Button
              onClick={() => setIsBulkPayModalOpen(true)}
              className="bg-emerald-700 hover:bg-emerald-800 text-white shadow-xs h-9 text-xs font-semibold"
            >
              <Wallet className="h-4 w-4 mr-1.5" />
              <span>Chi trả lương hàng loạt</span>
            </Button>
          )}

          {/* Approve button (Owner / Admin) */}
          {currentPeriod && !isApproved && (
            <Button
              onClick={() => setIsApproveModalOpen(true)}
              disabled={isLoading || isCalculating || (currentPeriod?.lines?.length ?? 0) === 0}
              className="bg-emerald-700 hover:bg-emerald-800 text-white shadow-xs h-9 text-xs font-semibold"
            >
              <ShieldCheck className="h-4 w-4 mr-1.5" />
              <span>Phê duyệt bảng lương (Chủ quán)</span>
            </Button>
          )}

          {isApproved && (
            <div className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold">
              <Lock className="h-3.5 w-3.5 text-emerald-600" />
              <span>Đã khóa Snapshot bất biến</span>
            </div>
          )}
        </div>
      </div>

      {/* Selectors Bar: Branch Selector + Month/Year Selector */}
      <div className="bg-white p-4 rounded-2xl border border-stone-200 shadow-xs flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-4">
          {/* Branch Selector */}
          <div className="flex items-center space-x-2">
            <Building2 className="h-4 w-4 text-stone-500" />
            <span className="text-xs font-semibold text-stone-700">Chi nhánh:</span>
            <select
              value={selectedBranchId}
              onChange={(e) => {
                setSelectedBranchId(e.target.value)
                const target = branches.find((b) => b.id === e.target.value)
                if (target) setActiveBranch(target.id, target.name)
              }}
              className="h-9 px-3 bg-stone-50 border border-stone-300 rounded-xl text-xs font-medium text-stone-900 focus:outline-none focus:ring-2 focus:ring-[#304638]"
            >
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name} {b.code ? `(${b.code})` : ''}
                </option>
              ))}
            </select>
          </div>

          {/* Month/Year Selector */}
          <div className="flex items-center space-x-2">
            <Calendar className="h-4 w-4 text-stone-500" />
            <span className="text-xs font-semibold text-stone-700">Kỳ lương:</span>
            <select
              value={selectedMonth}
              onChange={(e) => setSelectedMonth(Number(e.target.value))}
              className="h-9 px-2.5 bg-stone-50 border border-stone-300 rounded-xl text-xs font-medium text-stone-900 focus:outline-none focus:ring-2 focus:ring-[#304638]"
            >
              {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                <option key={m} value={m}>
                  Tháng {m}
                </option>
              ))}
            </select>
            <select
              value={selectedYear}
              onChange={(e) => setSelectedYear(Number(e.target.value))}
              className="h-9 px-2.5 bg-stone-50 border border-stone-300 rounded-xl text-xs font-medium text-stone-900 focus:outline-none focus:ring-2 focus:ring-[#304638]"
            >
              {[2025, 2026, 2027].map((y) => (
                <option key={y} value={y}>
                  Năm {y}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Period Status Badge */}
        <div className="flex items-center space-x-2">
          <span className="text-xs text-stone-500">Trạng thái:</span>
          {currentPeriod ? (
            <Badge
              className={`${statusCfg.badgeClass} flex items-center space-x-1.5 px-3 py-1 font-medium text-xs`}
            >
              <span className={`w-2 h-2 rounded-full ${statusCfg.dotClass}`} />
              <span>{statusCfg.label}</span>
            </Badge>
          ) : (
            <Badge variant="outline" className="text-stone-500 border-stone-300 text-xs">
              Chưa khởi tạo
            </Badge>
          )}
        </div>
      </div>

      {/* Success & Error Banners */}
      {successMessage && (
        <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center space-x-2.5 text-emerald-800 text-xs">
          <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
          <span className="font-medium">{successMessage}</span>
        </div>
      )}

      {errorMessage && (
        <div className="p-3.5 bg-rose-50 border border-rose-200 rounded-xl flex items-start space-x-2.5 text-rose-800 text-xs">
          <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5 text-rose-600" />
          <div className="flex-1 font-medium">{errorMessage}</div>
        </div>
      )}

      {/* Approved Invariant Snapshot Notification Banner */}
      {isApproved && (
        <div className="p-4 bg-emerald-50/90 border border-emerald-300 rounded-2xl flex items-start space-x-3 text-emerald-900">
          <ShieldCheck className="h-5 w-5 text-emerald-700 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <h4 className="font-bold text-xs uppercase tracking-wide text-emerald-800">
              Bảng lương đã chốt duyệt & Khóa bất biến
            </h4>
            <p className="text-xs text-emerald-800 leading-relaxed">
              Bảng lương Tháng {selectedMonth}/{selectedYear} tại chi nhánh{' '}
              <strong>{selectedBranchName}</strong> đã được phê duyệt bởi{' '}
              <strong>{currentPeriod?.approverName || 'Chủ quán (Branch Owner)'}</strong>
              {currentPeriod?.approvedAt ? ` lúc ${new Date(currentPeriod.approvedAt).toLocaleString('vi-VN')}` : ''}.
              Toàn bộ chính sách lương và công thức tính toán đã được snapshot bất biến. Bạn có thể tiến hành{' '}
              <strong>chi trả lương từng người hoặc hàng loạt</strong> vào sổ quỹ.
            </p>
          </div>
        </div>
      )}

      {/* Draft Notification Banner */}
      {currentPeriod && !isApproved && (
        <div className="p-4 bg-amber-50/70 border border-amber-200 rounded-2xl flex items-start space-x-3 text-amber-900">
          <Sparkles className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
          <div className="space-y-1">
            <h4 className="font-bold text-xs text-amber-800">
              Bản nháp đang soạn thảo (DRAFT)
            </h4>
            <p className="text-xs text-amber-700 leading-relaxed">
              Dữ liệu được trích xuất từ bảng chấm công đã duyệt. Bạn có thể thêm phụ cấp/thưởng/phạt cho từng nhân viên hoặc bấm <strong>"Tính lại bảng lương"</strong> nếu có thêm giờ chấm công mới. Sau khi rà soát kỹ lưỡng, Chủ quán sẽ thực hiện phê duyệt để chốt kỳ.
            </p>
          </div>
        </div>
      )}

      {/* KPI Cards: Tổng quỹ lương, Lương cơ bản, Đã thanh toán, Còn nợ lại, Số lượng nhân sự */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3.5">
        {/* KPI 1: Tổng quỹ lương */}
        <Card className="border-stone-200 shadow-2xs">
          <CardContent className="p-3.5 flex items-center justify-between">
            <div>
              <span className="text-[10px] font-semibold uppercase tracking-wider text-stone-500 block mb-1">
                Tổng quỹ lương (Gross)
              </span>
              <span className="text-lg font-bold font-mono text-[#304638]">
                {formatVND(totalGrossSalary)}
              </span>
            </div>
            <div className="w-9 h-9 rounded-xl bg-[#304638]/10 text-[#304638] flex items-center justify-center">
              <DollarSign className="h-4.5 w-4.5" />
            </div>
          </CardContent>
        </Card>

        {/* KPI 2: Lương cơ bản */}
        <Card className="border-stone-200 shadow-2xs">
          <CardContent className="p-3.5 flex items-center justify-between">
            <div>
              <span className="text-[10px] font-semibold uppercase tracking-wider text-stone-500 block mb-1">
                Tổng lương cơ bản
              </span>
              <span className="text-lg font-bold font-mono text-stone-900">
                {formatVND(totalBaseSalary)}
              </span>
            </div>
            <div className="w-9 h-9 rounded-xl bg-stone-100 text-stone-700 flex items-center justify-center">
              <Coins className="h-4.5 w-4.5" />
            </div>
          </CardContent>
        </Card>

        {/* KPI 3: Đã thanh toán (P0-HR-07) */}
        <Card className="border-stone-200 shadow-2xs bg-emerald-50/30">
          <CardContent className="p-3.5 flex items-center justify-between">
            <div>
              <span className="text-[10px] font-semibold uppercase tracking-wider text-emerald-800 block mb-1">
                Đã thanh toán (Paid)
              </span>
              <span className="text-lg font-bold font-mono text-emerald-700">
                {formatVND(totalPaidAmount)}
              </span>
            </div>
            <div className="w-9 h-9 rounded-xl bg-emerald-100 text-emerald-800 flex items-center justify-center">
              <CheckCircle2 className="h-4.5 w-4.5" />
            </div>
          </CardContent>
        </Card>

        {/* KPI 4: Công nợ lương còn lại (P0-HR-07) */}
        <Card className="border-stone-200 shadow-2xs bg-amber-50/30">
          <CardContent className="p-3.5 flex items-center justify-between">
            <div>
              <span className="text-[10px] font-semibold uppercase tracking-wider text-amber-800 block mb-1">
                Công nợ lương còn lại
              </span>
              <span className={`text-lg font-bold font-mono ${totalRemainingDebt > 0 ? 'text-amber-900' : 'text-stone-500'}`}>
                {formatVND(totalRemainingDebt)}
              </span>
            </div>
            <div className="w-9 h-9 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center">
              <CreditCard className="h-4.5 w-4.5" />
            </div>
          </CardContent>
        </Card>

        {/* KPI 5: Số lượng nhân sự */}
        <Card className="border-stone-200 shadow-2xs">
          <CardContent className="p-3.5 flex items-center justify-between">
            <div>
              <span className="text-[10px] font-semibold uppercase tracking-wider text-stone-500 block mb-1">
                Nhân sự hưởng lương
              </span>
              <span className="text-lg font-bold font-mono text-stone-900">
                {currentPeriod?.employeeCount || 0} NV
              </span>
            </div>
            <div className="w-9 h-9 rounded-xl bg-stone-100 text-stone-700 flex items-center justify-center">
              <Users className="h-4.5 w-4.5" />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Table Section */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold text-stone-800">
            Danh sách bảng lương & Tiến độ chi trả chi nhánh
          </h3>
          {currentPeriod && (
            <span className="text-xs text-stone-500">
              Lần hiệu chỉnh: <strong>Rev #{currentPeriod.revision}</strong>
            </span>
          )}
        </div>

        {currentPeriod ? (
          <PayrollLinesTable
            lines={currentPeriod.lines || []}
            periodStatus={periodStatus}
            onViewCalculation={(line) => setSelectedLineForDetail(line)}
            onAddAdjustment={(line) => setSelectedLineForAdjustment(line)}
            onDeleteAdjustment={handleDeleteAdjustment}
            onPaySalary={(line) => setSelectedLineForPay(line)}
            onViewPayments={(line) => setSelectedLineForPayments(line)}
            onViewPayslip={(line) => setSelectedLineForPayslip(line)}
          />
        ) : (
          <div className="bg-white rounded-2xl border border-stone-200 p-12 text-center shadow-xs">
            <div className="max-w-md mx-auto space-y-3">
              <div className="w-12 h-12 rounded-2xl bg-stone-100 text-stone-400 flex items-center justify-center mx-auto">
                <FileCheck className="h-6 w-6" />
              </div>
              <h3 className="text-base font-bold text-stone-800">
                Chưa có bảng lương Tháng {selectedMonth}/{selectedYear}
              </h3>
              <p className="text-xs text-stone-500 leading-relaxed">
                Bảng lương cho kỳ này tại chi nhánh <strong>{selectedBranchName}</strong> chưa được tạo. Bấm nút bên dưới để hệ thống quét dữ liệu chấm công đã duyệt và tự động tính toán.
              </p>
              <div className="pt-2">
                <Button
                  onClick={handleRecalculatePayroll}
                  disabled={isCalculating || !selectedBranchId}
                  className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-4 h-9"
                >
                  <RefreshCw
                    className={`h-3.5 w-3.5 mr-1.5 ${isCalculating ? 'animate-spin' : ''}`}
                  />
                  <span>Tính toán bảng lương ngay</span>
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Modals */}
      <CalculationDetailModal
        isOpen={!!selectedLineForDetail}
        line={selectedLineForDetail}
        onClose={() => setSelectedLineForDetail(null)}
      />

      <CreateAdjustmentModal
        isOpen={!!selectedLineForAdjustment}
        line={selectedLineForAdjustment}
        branchId={selectedBranchId}
        onClose={() => setSelectedLineForAdjustment(null)}
        onSuccess={() => {
          fetchPayrollPeriod()
          setSuccessMessage('Đã thêm khoản điều chỉnh lương thành công.')
          setTimeout(() => setSuccessMessage(null), 4000)
        }}
      />

      <ApprovePayrollModal
        isOpen={isApproveModalOpen}
        period={currentPeriod}
        branchId={selectedBranchId}
        branchName={selectedBranchName}
        onClose={() => setIsApproveModalOpen(false)}
        onSuccess={() => {
          fetchPayrollPeriod()
          setSuccessMessage('Bảng lương đã được phê duyệt và khóa bất biến thành công!')
          setTimeout(() => setSuccessMessage(null), 5000)
        }}
      />

      {/* Pay Salary to Individual Line Modal (P0-HR-07) */}
      <PaySalaryModal
        isOpen={!!selectedLineForPay}
        line={selectedLineForPay}
        branchId={selectedBranchId}
        onClose={() => setSelectedLineForPay(null)}
        onSuccess={() => {
          fetchPayrollPeriod()
          setSuccessMessage('Đã xuất quỹ chi trả lương thành công.')
          setTimeout(() => setSuccessMessage(null), 5000)
        }}
      />

      {/* Bulk Pay Salary Modal (P0-HR-07) */}
      <BulkPaySalaryModal
        isOpen={isBulkPayModalOpen}
        period={currentPeriod}
        branchId={selectedBranchId}
        branchName={selectedBranchName}
        onClose={() => setIsBulkPayModalOpen(false)}
        onSuccess={() => {
          fetchPayrollPeriod()
          setSuccessMessage('Đã thực hiện chi trả lương hàng loạt thành công!')
          setTimeout(() => setSuccessMessage(null), 5000)
        }}
      />

      {/* Payment History Modal (P0-HR-07) */}
      <PaymentHistoryModal
        isOpen={!!selectedLineForPayments}
        line={selectedLineForPayments}
        branchId={selectedBranchId}
        onClose={() => setSelectedLineForPayments(null)}
      />

      {/* Employee Payslip Modal (P0-HR-08 / FE-P0-23) */}
      <EmployeePayslipModal
        isOpen={!!selectedLineForPayslip}
        line={selectedLineForPayslip}
        branchId={selectedBranchId}
        branchName={selectedBranchName}
        monthStart={monthStart}
        onClose={() => setSelectedLineForPayslip(null)}
      />
    </div>
  )
}
