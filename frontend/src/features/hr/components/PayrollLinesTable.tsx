import React, { useState, useMemo } from 'react'
import {
  Search,
  Calculator,
  Plus,
  Trash2,
  Clock,
  Calendar,
  ChevronDown,
  ChevronUp,
  FileSpreadsheet,
  AlertCircle,
  Filter,
  DollarSign,
  History,
  FileText,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { Input } from '@/shared/components/ui/input'
import { formatVND, formatHours, formatWorkdays } from '../api/payrollApi'
import {
  ADJUSTMENT_CODE_CONFIG,
  type PayrollLine,
  type PayrollPeriodStatus,
  type SalaryRateType,
} from '../types/payroll.types'
import {
  PAYMENT_PROGRESS_CONFIG,
  getPaymentProgressStatus,
} from '../types/payment.types'

interface PayrollLinesTableProps {
  lines: PayrollLine[]
  periodStatus: PayrollPeriodStatus
  onViewCalculation: (line: PayrollLine) => void
  onAddAdjustment: (line: PayrollLine) => void
  onDeleteAdjustment: (adjId: string) => Promise<void>
  onPaySalary?: (line: PayrollLine) => void
  onViewPayments?: (line: PayrollLine) => void
  onViewPayslip?: (line: PayrollLine) => void
}

export const PayrollLinesTable: React.FC<PayrollLinesTableProps> = ({
  lines,
  periodStatus,
  onViewCalculation,
  onAddAdjustment,
  onDeleteAdjustment,
  onPaySalary,
  onViewPayments,
  onViewPayslip,
}) => {
  const [searchTerm, setSearchTerm] = useState('')
  const [rateTypeFilter, setRateTypeFilter] = useState<'ALL' | SalaryRateType>('ALL')
  const [expandedLineIds, setExpandedLineIds] = useState<Record<string, boolean>>({})
  const [deletingAdjId, setDeletingAdjId] = useState<string | null>(null)

  const isDraft = periodStatus === 'DRAFT'
  const isApproved = periodStatus === 'APPROVED'

  // Toggle adjustment row expansion
  const toggleExpand = (lineId: string) => {
    setExpandedLineIds((prev) => ({
      ...prev,
      [lineId]: !prev[lineId],
    }))
  }

  // Filter lines
  const filteredLines = useMemo(() => {
    return lines.filter((line) => {
      const matchSearch =
        line.employeeName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        line.staffCode.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (line.positionName && line.positionName.toLowerCase().includes(searchTerm.toLowerCase()))

      const matchRateType =
        rateTypeFilter === 'ALL' || line.rateType === rateTypeFilter

      return matchSearch && matchRateType
    })
  }, [lines, searchTerm, rateTypeFilter])

  const handleDeleteAdj = async (adjId: string) => {
    if (!window.confirm('Bạn có chắc muốn xóa khoản điều chỉnh này không?')) return
    setDeletingAdjId(adjId)
    try {
      await onDeleteAdjustment(adjId)
    } finally {
      setDeletingAdjId(null)
    }
  }

  return (
    <div className="bg-white rounded-2xl border border-stone-200 shadow-sm overflow-hidden">
      {/* Table Toolbar & Filters */}
      <div className="p-4 border-b border-stone-200 bg-stone-50/50 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="flex flex-1 items-center space-x-3">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-stone-400" />
            <Input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm theo mã NV, họ tên, chức vụ..."
              className="pl-9 h-9 text-xs bg-white border-stone-300"
            />
          </div>

          <div className="flex items-center space-x-1.5">
            <Filter className="h-3.5 w-3.5 text-stone-400" />
            <select
              value={rateTypeFilter}
              onChange={(e) => setRateTypeFilter(e.target.value as 'ALL' | SalaryRateType)}
              className="h-9 px-2.5 bg-white border border-stone-300 rounded-lg text-xs font-medium text-stone-700 focus:outline-none focus:ring-1 focus:ring-[#304638]"
            >
              <option value="ALL">Tất cả loại lương</option>
              <option value="HOURLY">Lương giờ (HOURLY)</option>
              <option value="MONTHLY">Lương tháng (MONTHLY)</option>
            </select>
          </div>
        </div>

        <div className="text-xs text-stone-500 font-medium">
          Hiển thị <strong>{filteredLines.length}</strong> / {lines.length} nhân sự
        </div>
      </div>

      {/* Main Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-xs text-left">
          <thead className="bg-stone-100 text-stone-600 font-semibold uppercase tracking-wider text-[11px] border-b border-stone-200">
            <tr>
              <th className="px-4 py-3">Nhân viên</th>
              <th className="px-3 py-3">Loại lương</th>
              <th className="px-3 py-3 text-right">Giờ làm duyệt</th>
              <th className="px-3 py-3 text-right">Công quy đổi</th>
              <th className="px-3 py-3 text-right">Lương cơ bản</th>
              <th className="px-3 py-3 text-right">Tổng điều chỉnh</th>
              <th className="px-3 py-3 text-right">Lương thực lĩnh</th>
              <th className="px-3 py-3 text-right">Đã chi</th>
              <th className="px-3 py-3 text-right">Còn nợ</th>
              <th className="px-3 py-3 text-center">Tiến độ</th>
              <th className="px-4 py-3 text-center">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-stone-100">
            {filteredLines.length > 0 ? (
              filteredLines.map((line) => {
                const isHourly = line.rateType === 'HOURLY'
                const isExpanded = !!expandedLineIds[line.id]
                const adjustmentsCount = line.adjustments?.length || 0
                const isAdjPositive = line.adjustmentsTotal > 0
                const isAdjNegative = line.adjustmentsTotal < 0

                const gross = Number(line.grossSalary) || 0
                const paid = Number(line.paidAmount) || 0
                const remaining = Math.max(0, gross - paid)
                const progress = getPaymentProgressStatus(gross, paid)
                const progressCfg = PAYMENT_PROGRESS_CONFIG[progress]

                return (
                  <React.Fragment key={line.id}>
                    <tr className="hover:bg-stone-50/70 transition-colors">
                      {/* Nhân viên */}
                      <td className="px-4 py-3">
                        <div className="flex items-center space-x-2.5">
                          <div className="w-8 h-8 rounded-full bg-[#819B78]/20 text-[#304638] flex items-center justify-center font-bold text-xs shrink-0">
                            {line.employeeName.charAt(0)}
                          </div>
                          <div>
                            <div className="flex items-center space-x-1.5">
                              <span className="font-bold text-stone-900">
                                {line.employeeName}
                              </span>
                              <Badge
                                variant="secondary"
                                className="font-mono text-[10px] px-1 py-0"
                              >
                                {line.staffCode}
                              </Badge>
                            </div>
                            <span className="text-[11px] text-stone-500">
                              {line.positionName}
                            </span>
                          </div>
                        </div>
                      </td>

                      {/* Loại lương */}
                      <td className="px-3 py-3">
                        {isHourly ? (
                          <Badge className="bg-sky-50 text-sky-800 border-sky-200 flex items-center w-fit text-[10px] font-medium">
                            <Clock className="h-3 w-3 mr-1 text-sky-600" />
                            <span>Lương giờ</span>
                          </Badge>
                        ) : (
                          <Badge className="bg-emerald-50 text-emerald-800 border-emerald-200 flex items-center w-fit text-[10px] font-medium">
                            <Calendar className="h-3 w-3 mr-1 text-emerald-600" />
                            <span>Lương tháng</span>
                          </Badge>
                        )}
                      </td>

                      {/* Giờ làm duyệt */}
                      <td className="px-3 py-3 text-right font-mono font-medium text-stone-800">
                        {formatHours(line.approvedMinutes)}
                      </td>

                      {/* Công quy đổi */}
                      <td className="px-3 py-3 text-right font-mono text-stone-700">
                        {isHourly ? '—' : formatWorkdays(line.payableWorkdays)}
                      </td>

                      {/* Lương cơ bản */}
                      <td className="px-3 py-3 text-right font-mono font-semibold text-stone-900">
                        {formatVND(line.baseSalary)}
                      </td>

                      {/* Tổng điều chỉnh & Accordion button */}
                      <td className="px-3 py-3 text-right">
                        <div className="flex items-center justify-end space-x-1.5">
                          <span
                            className={`font-mono font-bold ${
                              isAdjPositive
                                ? 'text-emerald-700'
                                : isAdjNegative
                                ? 'text-rose-700'
                                : 'text-stone-500'
                            }`}
                          >
                            {isAdjPositive ? '+' : ''}
                            {formatVND(line.adjustmentsTotal)}
                          </span>

                          {adjustmentsCount > 0 ? (
                            <button
                              type="button"
                              onClick={() => toggleExpand(line.id)}
                              className="inline-flex items-center p-1 rounded hover:bg-stone-200 text-stone-500 transition-colors"
                              title={`${adjustmentsCount} khoản điều chỉnh`}
                            >
                              <Badge
                                variant="outline"
                                className="text-[10px] px-1 py-0 text-stone-600 border-stone-300"
                              >
                                {adjustmentsCount}
                              </Badge>
                              {isExpanded ? (
                                <ChevronUp className="h-3.5 w-3.5 ml-0.5 text-stone-400" />
                              ) : (
                                <ChevronDown className="h-3.5 w-3.5 ml-0.5 text-stone-400" />
                              )}
                            </button>
                          ) : (
                            isDraft && (
                              <button
                                type="button"
                                onClick={() => onAddAdjustment(line)}
                                className="p-1 rounded text-stone-400 hover:text-[#304638] hover:bg-stone-100"
                                title="Thêm điều chỉnh"
                              >
                                <Plus className="h-3.5 w-3.5" />
                              </button>
                            )
                          )}
                        </div>
                      </td>

                      {/* Lương thực lĩnh */}
                      <td className="px-3 py-3 text-right">
                        <span className="font-mono font-bold text-xs text-[#304638] bg-[#304638]/5 px-2 py-0.5 rounded-md border border-[#304638]/20">
                          {formatVND(line.grossSalary)}
                        </span>
                      </td>

                      {/* Đã chi */}
                      <td className="px-3 py-3 text-right font-mono font-semibold text-emerald-700">
                        {paid > 0 ? formatVND(paid) : '0 ₫'}
                      </td>

                      {/* Còn nợ */}
                      <td className="px-3 py-3 text-right font-mono font-bold">
                        <span className={remaining > 0 ? 'text-amber-800' : 'text-stone-400'}>
                          {formatVND(remaining)}
                        </span>
                      </td>

                      {/* Tiến độ chi trả */}
                      <td className="px-3 py-3 text-center">
                        <Badge
                          className={`${progressCfg.badgeClass} text-[10px] px-1.5 py-0.5 font-medium inline-flex items-center space-x-1`}
                        >
                          <span className={`w-1.5 h-1.5 rounded-full ${progressCfg.dotClass}`} />
                          <span>{progressCfg.label}</span>
                        </Badge>
                      </td>

                      {/* Thao tác */}
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-center space-x-1">
                          {/* Nút chi trả lương (P0-HR-07: chỉ khả dụng khi APPROVED và còn nợ) */}
                          {isApproved && remaining > 0 && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => onPaySalary?.(line)}
                              className="h-7 px-2 text-[11px] border-emerald-300 text-emerald-800 bg-emerald-50/70 hover:bg-emerald-100 flex items-center space-x-1 shadow-2xs"
                              title="Xuất quỹ chi trả lương (TC-016)"
                            >
                              <DollarSign className="h-3 w-3 text-emerald-700" />
                              <span>Chi lương</span>
                            </Button>
                          )}

                          {/* Nút xem lịch sử thanh toán */}
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => onViewPayments?.(line)}
                            className="h-7 px-2 text-[11px] border-stone-300 text-stone-700 hover:bg-stone-100 flex items-center space-x-1"
                            title="Lịch sử các đợt chi trả"
                          >
                            <History className="h-3 w-3 text-stone-500" />
                            <span>Lịch sử</span>
                          </Button>

                          {/* Nút xem phiếu lương */}
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => onViewPayslip?.(line)}
                            className="h-7 px-2 text-[11px] border-stone-300 text-stone-700 hover:bg-stone-100 flex items-center space-x-1"
                            title="Xem và in phiếu lương nhân viên (P0-HR-08 / FE-P0-23)"
                          >
                            <FileText className="h-3 w-3 text-[#304638]" />
                            <span>Phiếu</span>
                          </Button>

                          {/* Nút xem chi tiết phân rã công thức */}
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => onViewCalculation(line)}
                            className="h-7 px-2 text-[11px] border-stone-300 text-stone-700 hover:bg-stone-100 flex items-center space-x-1"
                            title="Xem chi tiết phân rã công thức (TC-013/TC-014)"
                          >
                            <Calculator className="h-3 w-3 text-stone-500" />
                            <span>Công thức</span>
                          </Button>

                          {/* Nút thêm điều chỉnh (chỉ hiện khi DRAFT) */}
                          {isDraft && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => onAddAdjustment(line)}
                              className="h-7 px-2 text-[11px] border-stone-300 text-stone-700 hover:bg-emerald-50 hover:text-emerald-800 hover:border-emerald-300 flex items-center space-x-1"
                              title="Thêm phụ cấp, thưởng hoặc phạt"
                            >
                              <Plus className="h-3 w-3 text-emerald-600" />
                              <span>Điều chỉnh</span>
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>

                    {/* Expandable Adjustments Sub-row */}
                    {isExpanded && adjustmentsCount > 0 && (
                      <tr className="bg-stone-50/80">
                        <td colSpan={11} className="px-6 py-3 border-t border-b border-stone-200">
                          <div className="space-y-2">
                            <div className="flex items-center justify-between">
                              <span className="text-[11px] font-bold uppercase tracking-wider text-stone-600 flex items-center space-x-1">
                                <FileSpreadsheet className="h-3.5 w-3.5 mr-1 text-[#304638]" />
                                <span>Chi tiết các khoản điều chỉnh của {line.employeeName}</span>
                              </span>
                              {isDraft && (
                                <button
                                  type="button"
                                  onClick={() => onAddAdjustment(line)}
                                  className="text-xs text-[#304638] font-semibold hover:underline flex items-center space-x-1"
                                >
                                  <Plus className="h-3 w-3 mr-0.5" />
                                  <span>Thêm khoản khác</span>
                                </button>
                              )}
                            </div>

                            <div className="bg-white rounded-lg border border-stone-200 overflow-hidden shadow-2xs">
                              <table className="w-full text-xs">
                                <thead className="bg-stone-100/70 text-stone-500 font-medium border-b border-stone-200">
                                  <tr>
                                    <th className="px-3 py-1.5 text-left">Phân loại</th>
                                    <th className="px-3 py-1.5 text-left">Nội dung giải trình</th>
                                    <th className="px-3 py-1.5 text-right">Số tiền</th>
                                    {isDraft && (
                                      <th className="px-3 py-1.5 text-center w-16">Xóa</th>
                                    )}
                                  </tr>
                                </thead>
                                <tbody className="divide-y divide-stone-100">
                                  {line.adjustments.map((adj) => {
                                    const cfg = ADJUSTMENT_CODE_CONFIG[adj.adjustmentCode]
                                    const isPos = adj.signedAmount > 0
                                    return (
                                      <tr key={adj.id} className="hover:bg-stone-50/60">
                                        <td className="px-3 py-2">
                                          <Badge
                                            className={`${
                                              cfg?.badgeClass || 'bg-stone-100 text-stone-800'
                                            } text-[10px]`}
                                          >
                                            {cfg?.label || adj.adjustmentCode}
                                          </Badge>
                                        </td>
                                        <td className="px-3 py-2 text-stone-700">
                                          {adj.explanation}
                                        </td>
                                        <td
                                          className={`px-3 py-2 text-right font-mono font-bold ${
                                            isPos ? 'text-emerald-700' : 'text-rose-700'
                                          }`}
                                        >
                                          {isPos ? '+' : ''}
                                          {formatVND(adj.signedAmount)}
                                        </td>
                                        {isDraft && (
                                          <td className="px-3 py-2 text-center">
                                            <button
                                              type="button"
                                              onClick={() => handleDeleteAdj(adj.id)}
                                              disabled={deletingAdjId === adj.id}
                                              className="p-1 rounded text-stone-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
                                              title="Xóa điều chỉnh"
                                            >
                                              <Trash2 className="h-3.5 w-3.5" />
                                            </button>
                                          </td>
                                        )}
                                      </tr>
                                    )
                                  })}
                                </tbody>
                              </table>
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                )
              })
            ) : (
              <tr>
                <td colSpan={11} className="px-6 py-12 text-center">
                  <div className="max-w-sm mx-auto space-y-2">
                    <AlertCircle className="h-8 w-8 text-stone-300 mx-auto" />
                    <p className="text-sm font-semibold text-stone-700">
                      Chưa có dữ liệu bảng lương
                    </p>
                    <p className="text-xs text-stone-500">
                      {searchTerm || rateTypeFilter !== 'ALL'
                        ? 'Không tìm thấy nhân viên nào phù hợp với bộ lọc.'
                        : 'Bảng lương kỳ này chưa được tính toán hoặc chi nhánh chưa có nhân viên phân bổ.'}
                    </p>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
