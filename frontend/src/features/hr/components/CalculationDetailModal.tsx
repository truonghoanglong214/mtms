import React from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Calculator,
  Calendar,
  Clock,
  Coins,
  TrendingUp,
  TrendingDown,
  Info,
  CheckCircle2,
  FileSpreadsheet,
} from 'lucide-react'
import { Badge } from '@/shared/components/ui/badge'
import { Button } from '@/shared/components/ui/button'
import { formatVND, formatHours, formatWorkdays, formatPercent } from '../api/payrollApi'
import {
  ADJUSTMENT_CODE_CONFIG,
  type PayrollLine,
  type RateSlice,
} from '../types/payroll.types'

interface CalculationDetailModalProps {
  isOpen: boolean
  line: PayrollLine | null
  onClose: () => void
}

export const CalculationDetailModal: React.FC<CalculationDetailModalProps> = ({
  isOpen,
  line,
  onClose,
}) => {
  if (!isOpen || !line) return null

  const isHourly = line.rateType === 'HOURLY'
  const calcSnapshot = line.calculationSnapshot || {}
  const policySnapshot = line.policySnapshot || {}

  // Slices for hourly rate
  const slices: RateSlice[] = calcSnapshot.slices || []

  // Monthly values for prorated monthly rate (TC-014)
  const agreedRate =
    Number(calcSnapshot.agreedMonthlyRate) ||
    Number(policySnapshot.agreedMonthlyRate) ||
    Number(policySnapshot.currentRate) ||
    0
  const standardWorkdays =
    Number(calcSnapshot.standardWorkdays) ||
    Number(policySnapshot.standardWorkdays) ||
    26
  const actualWorkdays = Number(line.payableWorkdays) || 0
  const workdayRatio = standardWorkdays > 0 ? actualWorkdays / standardWorkdays : 0

  // Adjustments summary
  const adjustments = line.adjustments || []
  const positiveAdjustments = adjustments.filter((a) => a.signedAmount > 0)
  const negativeAdjustments = adjustments.filter((a) => a.signedAmount < 0)

  const totalPositive = positiveAdjustments.reduce(
    (sum, a) => sum + Number(a.signedAmount),
    0
  )
  const totalNegative = negativeAdjustments.reduce(
    (sum, a) => sum + Number(a.signedAmount),
    0
  )

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-sm overflow-y-auto">
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 10 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 10 }}
          transition={{ duration: 0.15 }}
          className="relative w-full max-w-2xl bg-white rounded-2xl shadow-2xl border border-stone-200 overflow-hidden my-8"
        >
          {/* Header */}
          <div className="px-6 py-4 border-b border-stone-200 bg-stone-50 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-2.5 rounded-xl bg-[#304638] text-white">
                <Calculator className="h-5 w-5" />
              </div>
              <div>
                <div className="flex items-center space-x-2">
                  <h3 className="text-base font-bold text-stone-900">
                    Phân rã công thức tính lương
                  </h3>
                  <Badge
                    variant="outline"
                    className="border-stone-300 text-stone-600 bg-white font-mono text-[11px]"
                  >
                    {isHourly ? 'Đa lát giờ' : 'Quy đổi công'}
                  </Badge>
                </div>
                <p className="text-xs text-stone-500">
                  Chi tiết cách tính lương cơ bản và các khoản điều chỉnh hợp đồng
                </p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg text-stone-400 hover:text-stone-600 hover:bg-stone-200/60 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="p-6 space-y-6 max-h-[75vh] overflow-y-auto">
            {/* Employee Quick Info Card */}
            <div className="bg-stone-50 rounded-xl p-4 border border-stone-200 flex flex-wrap items-center justify-between gap-4">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 rounded-full bg-[#819B78]/20 text-[#304638] flex items-center justify-center font-bold text-sm">
                  {line.employeeName.charAt(0)}
                </div>
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-stone-900 text-sm">
                      {line.employeeName}
                    </span>
                    <Badge variant="secondary" className="font-mono text-[10px]">
                      {line.staffCode}
                    </Badge>
                  </div>
                  <p className="text-xs text-stone-500">{line.positionName}</p>
                </div>
              </div>

              <div className="flex items-center space-x-2">
                {isHourly ? (
                  <Badge className="bg-sky-50 text-sky-800 border-sky-200 flex items-center space-x-1">
                    <Clock className="h-3 w-3 mr-1" />
                    <span>Lương theo giờ (HOURLY)</span>
                  </Badge>
                ) : (
                  <Badge className="bg-emerald-50 text-emerald-800 border-emerald-200 flex items-center space-x-1">
                    <Calendar className="h-3 w-3 mr-1" />
                    <span>Lương theo tháng (MONTHLY)</span>
                  </Badge>
                )}
              </div>
            </div>

            {/* CASE 1: HOURLY STAFF (TC-013 Midmonth Wage Slices) */}
            {isHourly && (
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <Clock className="h-4 w-4 text-[#304638]" />
                    <h4 className="text-sm font-bold text-stone-900">
                      Phân rã đa lát thời gian điều chỉnh mức lương
                    </h4>
                  </div>
                  <span className="text-xs text-stone-500">
                    Tổng duyệt: {formatHours(line.approvedMinutes)}
                  </span>
                </div>

                {slices.length > 0 ? (
                  <div className="border border-stone-200 rounded-xl overflow-hidden shadow-xs">
                    <table className="w-full text-xs text-left">
                      <thead className="bg-stone-100/80 text-stone-600 font-semibold uppercase tracking-wider text-[11px] border-b border-stone-200">
                        <tr>
                          <th className="px-4 py-2.5">Khoảng thời gian áp dụng</th>
                          <th className="px-4 py-2.5 text-right">Đơn giá giờ</th>
                          <th className="px-4 py-2.5 text-right">Số giờ làm</th>
                          <th className="px-4 py-2.5 text-right">Thành tiền</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-stone-100 bg-white">
                        {slices.map((slice, idx) => (
                          <tr key={idx} className="hover:bg-stone-50/50">
                            <td className="px-4 py-3 font-medium text-stone-800">
                              Từ <span className="font-mono text-stone-900">{slice.fromDate}</span> đến{' '}
                              <span className="font-mono text-stone-900">{slice.toDate}</span>
                            </td>
                            <td className="px-4 py-3 text-right font-mono text-stone-700">
                              {formatVND(slice.rate)}/h
                            </td>
                            <td className="px-4 py-3 text-right font-mono font-semibold text-stone-900">
                              {Number(slice.hours).toFixed(1)}h
                            </td>
                            <td className="px-4 py-3 text-right font-mono font-bold text-stone-900">
                              {formatVND(slice.amount)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                      <tfoot className="bg-stone-50 border-t border-stone-200 font-semibold">
                        <tr>
                          <td colSpan={3} className="px-4 py-2.5 text-stone-700 text-right">
                            Tổng Lương cơ bản theo giờ:
                          </td>
                          <td className="px-4 py-2.5 text-right font-mono font-bold text-[#304638] text-sm">
                            {formatVND(line.baseSalary)}
                          </td>
                        </tr>
                      </tfoot>
                    </table>
                  </div>
                ) : (
                  <div className="p-4 bg-amber-50/60 rounded-xl border border-amber-200 text-xs text-amber-800 space-y-1">
                    <p className="font-semibold">
                      Chính sách một mức lương đơn: {formatVND(policySnapshot.currentRate as number || 0)}/h
                    </p>
                    <p className="text-amber-700">
                      Công thức: {formatHours(line.approvedMinutes)} x{' '}
                      {formatVND(policySnapshot.currentRate as number || 0)} = {formatVND(line.baseSalary)}
                    </p>
                  </div>
                )}

                <div className="flex items-start space-x-2 text-[11px] text-stone-500 bg-stone-50 p-3 rounded-lg border border-stone-200">
                  <Info className="h-4 w-4 text-[#819B78] shrink-0 mt-0.5" />
                  <p>
                    <strong>Quy tắc tính lương theo giờ:</strong> Mỗi ca làm việc trong tháng được áp giá theo chính sách lương có hiệu lực tại ngày làm việc đó. Khi có quyết định tăng lương giữa tháng, hệ thống tự động chẻ các ca làm việc thành từng lát tương ứng mà không làm xáo trộn các ca trước.
                  </p>
                </div>
              </div>
            )}

            {/* CASE 2: MONTHLY STAFF (TC-014 Monthly Wage Proration) */}
            {!isHourly && (
              <div className="space-y-4">
                <div className="flex items-center space-x-2">
                  <Calendar className="h-4 w-4 text-[#304638]" />
                  <h4 className="text-sm font-bold text-stone-900">
                    Công thức quy đổi ngày công chuẩn
                  </h4>
                </div>

                {/* Mathematical Formula Callout */}
                <div className="bg-[#304638]/5 border border-[#304638]/20 rounded-xl p-4 text-center">
                  <p className="text-xs text-stone-600 mb-1 font-medium">Công thức tính chuẩn:</p>
                  <div className="inline-flex items-center space-x-2 text-sm font-bold text-[#304638] font-mono bg-white px-4 py-2 rounded-lg border border-stone-200 shadow-xs">
                    <span>Lương cơ bản</span>
                    <span>=</span>
                    <span>Lương thỏa thuận</span>
                    <span>×</span>
                    <span>(Công thực tế ÷ {standardWorkdays} công chuẩn)</span>
                  </div>
                </div>

                {/* Computation Matrix */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="bg-stone-50 rounded-xl p-3 border border-stone-200">
                    <span className="text-[11px] text-stone-500 block mb-1">Lương tháng thỏa thuận</span>
                    <span className="text-sm font-bold text-stone-900 font-mono">
                      {formatVND(agreedRate)}
                    </span>
                  </div>
                  <div className="bg-stone-50 rounded-xl p-3 border border-stone-200">
                    <span className="text-[11px] text-stone-500 block mb-1">Ngày công chuẩn kỳ</span>
                    <span className="text-sm font-bold text-stone-900 font-mono">
                      {standardWorkdays} ngày
                    </span>
                  </div>
                  <div className="bg-stone-50 rounded-xl p-3 border border-stone-200">
                    <span className="text-[11px] text-stone-500 block mb-1">Công thực tế đã duyệt</span>
                    <span className="text-sm font-bold text-[#304638] font-mono">
                      {formatWorkdays(actualWorkdays)}
                    </span>
                  </div>
                  <div className="bg-stone-50 rounded-xl p-3 border border-stone-200">
                    <span className="text-[11px] text-stone-500 block mb-1">Tỷ lệ công thực hưởng</span>
                    <span className="text-sm font-bold text-amber-700 font-mono">
                      {formatPercent(workdayRatio)}
                    </span>
                  </div>
                </div>

                <div className="p-3 bg-emerald-50/70 border border-emerald-200 rounded-xl flex items-center justify-between text-xs">
                  <div className="flex items-center space-x-2 text-emerald-800">
                    <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
                    <span>
                      {formatVND(agreedRate)} × ({actualWorkdays} ÷ {standardWorkdays})
                    </span>
                  </div>
                  <span className="font-bold font-mono text-emerald-900 text-sm">
                    = {formatVND(line.baseSalary)}
                  </span>
                </div>

                <div className="flex items-start space-x-2 text-[11px] text-stone-500 bg-stone-50 p-3 rounded-lg border border-stone-200">
                  <Info className="h-4 w-4 text-[#819B78] shrink-0 mt-0.5" />
                  <p>
                    <strong>Quy tắc tính lương theo tháng:</strong> Nhân viên lương tháng làm việc không đủ 26 ngày công chuẩn (do vào giữa tháng hoặc nghỉ có/không phép) sẽ được tính lương theo tỷ lệ ngày công thực tế đã được quản lý phê duyệt từ bảng chấm công.
                  </p>
                </div>
              </div>
            )}

            {/* ADJUSTMENTS SECTION */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <FileSpreadsheet className="h-4 w-4 text-[#304638]" />
                  <h4 className="text-sm font-bold text-stone-900">
                    Các khoản điều chỉnh lương ({adjustments.length})
                  </h4>
                </div>
                <span
                  className={`text-xs font-mono font-bold ${
                    line.adjustmentsTotal >= 0 ? 'text-emerald-700' : 'text-rose-700'
                  }`}
                >
                  {line.adjustmentsTotal >= 0 ? '+' : ''}
                  {formatVND(line.adjustmentsTotal)}
                </span>
              </div>

              {adjustments.length > 0 ? (
                <div className="border border-stone-200 rounded-xl overflow-hidden">
                  <table className="w-full text-xs text-left">
                    <thead className="bg-stone-100/80 text-stone-600 font-semibold uppercase tracking-wider text-[11px] border-b border-stone-200">
                      <tr>
                        <th className="px-4 py-2.5">Khoản điều chỉnh</th>
                        <th className="px-4 py-2.5">Giải trình chi tiết</th>
                        <th className="px-4 py-2.5 text-right">Số tiền</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-stone-100 bg-white">
                      {adjustments.map((adj) => {
                        const cfg = ADJUSTMENT_CODE_CONFIG[adj.adjustmentCode]
                        const isPositive = adj.signedAmount > 0
                        return (
                          <tr key={adj.id} className="hover:bg-stone-50/50">
                            <td className="px-4 py-3">
                              <Badge className={cfg?.badgeClass || 'bg-stone-100 text-stone-800'}>
                                {cfg?.label || adj.adjustmentCode}
                              </Badge>
                            </td>
                            <td className="px-4 py-3 text-stone-700 max-w-xs truncate">
                              {adj.explanation}
                            </td>
                            <td
                              className={`px-4 py-3 text-right font-mono font-bold ${
                                isPositive ? 'text-emerald-700' : 'text-rose-700'
                              }`}
                            >
                              {isPositive ? '+' : ''}
                              {formatVND(adj.signedAmount)}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="p-3 bg-stone-50 rounded-xl border border-stone-200 text-center text-xs text-stone-500">
                  Không có khoản điều chỉnh (phụ cấp, thưởng, phạt, tạm ứng) trong tháng này.
                </div>
              )}
            </div>

            {/* FINAL SALARY SUMMARY CARD */}
            <div className="bg-stone-900 text-white rounded-xl p-4 space-y-3">
              <div className="flex items-center justify-between text-xs text-stone-300 pb-2 border-b border-stone-800">
                <span>Lương cơ bản (Base Salary):</span>
                <span className="font-mono text-stone-100 font-semibold">
                  {formatVND(line.baseSalary)}
                </span>
              </div>
              <div className="flex items-center justify-between text-xs text-stone-300 pb-2 border-b border-stone-800">
                <span className="flex items-center space-x-1 text-emerald-400">
                  <TrendingUp className="h-3.5 w-3.5 mr-1" />
                  <span>(+) Thưởng & Phụ cấp:</span>
                </span>
                <span className="font-mono text-emerald-400 font-semibold">
                  +{formatVND(totalPositive)}
                </span>
              </div>
              <div className="flex items-center justify-between text-xs text-stone-300 pb-2 border-b border-stone-800">
                <span className="flex items-center space-x-1 text-rose-400">
                  <TrendingDown className="h-3.5 w-3.5 mr-1" />
                  <span>(-) Phạt & Khấu trừ tạm ứng:</span>
                </span>
                <span className="font-mono text-rose-400 font-semibold">
                  {formatVND(totalNegative)}
                </span>
              </div>
              <div className="flex items-center justify-between pt-1">
                <span className="text-sm font-bold text-white flex items-center space-x-1">
                  <Coins className="h-4 w-4 text-[#819B78] mr-1" />
                  <span>LƯƠNG THỰC LĨNH (GROSS SALARY):</span>
                </span>
                <span className="text-lg font-bold font-mono text-[#819B78]">
                  {formatVND(line.grossSalary)}
                </span>
              </div>
            </div>
          </div>

          {/* Footer */}
          <div className="px-6 py-3.5 border-t border-stone-200 bg-stone-50 flex items-center justify-end">
            <Button
              variant="outline"
              onClick={onClose}
              className="border-stone-300 text-stone-700 hover:bg-stone-100"
            >
              Đóng chi tiết
            </Button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
