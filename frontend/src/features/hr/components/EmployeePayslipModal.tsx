import React, { useState, useEffect, useRef } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Printer,
  Calendar,
  Building2,
  Clock,
  Coins,
  BadgePercent,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { formatVND, formatHours, formatWorkdays } from '../api/payrollApi'
import { paymentApi } from '../api/paymentApi'
import type { PayrollLine } from '../types/payroll.types'
import type { PayslipDto } from '../types/payment.types'
import {
  ADJUSTMENT_CODE_CONFIG,
} from '../types/payroll.types'
import {
  PAYMENT_PROGRESS_CONFIG,
  getPaymentProgressStatus,
} from '../types/payment.types'

interface EmployeePayslipModalProps {
  isOpen: boolean
  line: PayrollLine | null
  branchId: string
  branchName?: string
  monthStart?: string
  onClose: () => void
}

export const EmployeePayslipModal: React.FC<EmployeePayslipModalProps> = ({
  isOpen,
  line,
  branchId,
  branchName,
  monthStart,
  onClose,
}) => {
  const [payslipData, setPayslipData] = useState<PayslipDto | null>(null)
  const payslipRef = useRef<HTMLDivElement>(null)

  // Fetch detailed payslip data from backend if available, otherwise fallback to line props
  useEffect(() => {
    if (!isOpen || !line || !branchId) return

    let isMounted = true
    paymentApi
      .getLinePayslip(branchId, line.id)
      .then((data) => {
        if (!isMounted) return
        setPayslipData(data)
      })
      .catch((err) => {
        // Fallback to local line data if endpoint is pending backend deployment
        console.warn('Backend getLinePayslip fallback to line snapshot:', err)
      })

    return () => {
      isMounted = false
    }
  }, [isOpen, line, branchId])

  if (!isOpen || !line) return null

  // Use payslipData or fallback to line
  const effectiveLine = payslipData || line
  const grossSalary = Number(effectiveLine.grossSalary) || 0
  const paidAmount = Number(effectiveLine.paidAmount) || 0
  const remainingDebt = Math.max(0, grossSalary - paidAmount)
  const progressStatus = getPaymentProgressStatus(grossSalary, paidAmount)
  const progressCfg = PAYMENT_PROGRESS_CONFIG[progressStatus]

  const isHourly = effectiveLine.rateType === 'HOURLY'
  const slices = (effectiveLine.calculationSnapshot?.slices || []) as Array<{
    fromDate: string
    toDate: string
    rate: number
    hours: number
    amount: number
  }>

  const adjustments = effectiveLine.adjustments || []

  // Month formatting
  const formatMonth = (str?: string) => {
    if (!str) return '—'
    const parts = str.split('-')
    if (parts.length >= 2) return `Tháng ${parts[1]}/${parts[0]}`
    return str
  }

  const handlePrint = () => {
    window.print()
  }

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-stone-900/60 backdrop-blur-xs print:p-0 print:bg-white">
        {/* Print Styles */}
        <style dangerouslySetInnerHTML={{ __html: `
          @media print {
            body * {
              visibility: hidden;
            }
            #printable-payslip, #printable-payslip * {
              visibility: visible;
            }
            #printable-payslip {
              position: absolute;
              left: 0;
              top: 0;
              width: 100%;
              padding: 24px;
              margin: 0;
              box-shadow: none !important;
              border: none !important;
            }
            .no-print {
              display: none !important;
            }
          }
        `}} />

        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="bg-white rounded-3xl border border-stone-200 shadow-2xl max-w-2xl w-full overflow-hidden flex flex-col max-h-[92vh] print:max-h-none print:shadow-none print:border-none"
        >
          {/* Header Bar (Hidden on print) */}
          <div className="px-6 py-3.5 border-b border-stone-200 bg-stone-50 flex items-center justify-between shrink-0 no-print">
            <div className="flex items-center space-x-2">
              <BadgePercent className="h-4 w-4 text-[#304638]" />
              <h3 className="font-bold text-stone-900 text-sm">
                Phiếu lương nhân viên · {line.employeeName}
              </h3>
            </div>

            <div className="flex items-center space-x-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handlePrint}
                className="h-8 px-3 text-xs border-stone-300 text-stone-700 hover:bg-stone-100 flex items-center space-x-1.5"
              >
                <Printer className="h-3.5 w-3.5" />
                <span>In phiếu lương / PDF</span>
              </Button>

              <button
                type="button"
                onClick={onClose}
                className="p-1 rounded-xl text-stone-400 hover:text-stone-700 hover:bg-stone-200 transition-colors"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
          </div>

          {/* Printable Payslip Body */}
          <div
            id="printable-payslip"
            ref={payslipRef}
            className="p-6 sm:p-8 space-y-6 overflow-y-auto flex-1 bg-white text-stone-800"
          >
            {/* Header Document & Brand */}
            <div className="border-b-2 border-stone-800 pb-4 flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
              <div>
                <div className="flex items-center space-x-2">
                  <div className="w-8 h-8 rounded-full bg-[#304638] text-white flex items-center justify-center font-bold text-xs">
                    LT
                  </div>
                  <div>
                    <h2 className="font-bold text-sm tracking-tight text-stone-900 uppercase">
                      Lục Trà Tinh Hoa
                    </h2>
                    <p className="text-[10px] text-stone-500 font-mono">
                      HỆ THỐNG QUẢN LÝ VẬN HÀNH MTMS
                    </p>
                  </div>
                </div>

                <div className="mt-3 text-xs text-stone-600 space-y-0.5">
                  <div className="flex items-center space-x-1.5">
                    <Building2 className="h-3 w-3 text-stone-400" />
                    <span>Chi nhánh: <strong>{branchName || 'Chi nhánh kinh doanh'}</strong></span>
                  </div>
                  <div className="flex items-center space-x-1.5">
                    <Calendar className="h-3 w-3 text-stone-400" />
                    <span>Kỳ bảng lương: <strong>{formatMonth(monthStart)}</strong></span>
                  </div>
                </div>
              </div>

              <div className="sm:text-right">
                <span className="text-lg font-black tracking-tight text-stone-900 uppercase block">
                  PHIẾU LƯƠNG NHÂN VIÊN
                </span>
                <span className="text-[11px] font-mono text-stone-500 block">
                  Mã phiếu: PL-{effectiveLine.staffCode}-{new Date().getFullYear()}
                </span>
                <div className="mt-2 sm:flex sm:justify-end">
                  <Badge className={`${progressCfg.badgeClass} text-[10px] px-2 py-0.5 font-semibold`}>
                    <span className={`w-1.5 h-1.5 rounded-full ${progressCfg.dotClass} mr-1.5`} />
                    {progressCfg.label}
                  </Badge>
                </div>
              </div>
            </div>

            {/* Employee Information */}
            <div className="bg-stone-50/80 rounded-2xl p-4 border border-stone-200">
              <h4 className="text-[11px] font-bold uppercase tracking-wider text-stone-500 mb-2.5">
                Thông tin nhân sự
              </h4>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                <div>
                  <span className="text-stone-400 block text-[10px]">Họ và tên</span>
                  <span className="font-bold text-stone-900 text-sm">
                    {effectiveLine.employeeName}
                  </span>
                </div>
                <div>
                  <span className="text-stone-400 block text-[10px]">Mã nhân viên</span>
                  <span className="font-mono font-bold text-stone-800">
                    {effectiveLine.staffCode}
                  </span>
                </div>
                <div>
                  <span className="text-stone-400 block text-[10px]">Chức danh / Vị trí</span>
                  <span className="font-medium text-stone-800">
                    {effectiveLine.positionName}
                  </span>
                </div>
                <div>
                  <span className="text-stone-400 block text-[10px]">Hình thức tính lương</span>
                  <span className="font-medium text-stone-800">
                    {isHourly ? 'Lương giờ (Part-time)' : 'Lương tháng (Full-time)'}
                  </span>
                </div>
              </div>
            </div>

            {/* Work Time Breakdown */}
            <div className="space-y-2">
              <h4 className="text-[11px] font-bold uppercase tracking-wider text-stone-500 flex items-center space-x-1.5">
                <Clock className="h-3.5 w-3.5 text-[#304638]" />
                <span>1. Thời gian làm việc được phê duyệt</span>
              </h4>

              <div className="bg-white border border-stone-200 rounded-xl p-3 flex flex-wrap items-center justify-between gap-4 text-xs">
                <div>
                  <span className="text-stone-500 text-[11px] block">Tổng giờ làm việc:</span>
                  <span className="font-mono font-bold text-sm text-stone-900">
                    {formatHours(effectiveLine.approvedMinutes)}
                  </span>
                </div>

                {!isHourly && (
                  <div>
                    <span className="text-stone-500 text-[11px] block">Ngày công quy đổi:</span>
                    <span className="font-mono font-bold text-sm text-stone-900">
                      {formatWorkdays(effectiveLine.payableWorkdays)}
                    </span>
                  </div>
                )}

                <div>
                  <span className="text-stone-500 text-[11px] block">Lương cơ bản tính ra:</span>
                  <span className="font-mono font-bold text-sm text-stone-900">
                    {formatVND(effectiveLine.baseSalary)}
                  </span>
                </div>
              </div>

              {/* Midmonth Wage Slices Breakdown (TC-013) */}
              {isHourly && slices.length > 0 && (
                <div className="border border-stone-200 rounded-xl overflow-hidden mt-2 text-xs">
                  <div className="bg-stone-100/80 px-3 py-1.5 font-semibold text-[11px] text-stone-600 border-b border-stone-200">
                    Chi tiết các lát đơn giá giữa tháng
                  </div>
                  <table className="w-full text-xs text-left">
                    <thead className="bg-stone-50 text-stone-500 border-b border-stone-200 text-[10px] uppercase font-semibold">
                      <tr>
                        <th className="px-3 py-1.5">Khoảng thời gian</th>
                        <th className="px-3 py-1.5 text-right">Đơn giá / giờ</th>
                        <th className="px-3 py-1.5 text-right">Số giờ làm</th>
                        <th className="px-3 py-1.5 text-right">Thành tiền</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-stone-100">
                      {slices.map((slice, idx) => (
                        <tr key={idx} className="hover:bg-stone-50/50">
                          <td className="px-3 py-1.5 font-mono text-[11px]">
                            {slice.fromDate} → {slice.toDate}
                          </td>
                          <td className="px-3 py-1.5 text-right font-mono">
                            {formatVND(slice.rate)}
                          </td>
                          <td className="px-3 py-1.5 text-right font-mono">
                            {slice.hours?.toFixed(2) || '0'}h
                          </td>
                          <td className="px-3 py-1.5 text-right font-mono font-semibold">
                            {formatVND(slice.amount)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Income & Adjustments Table */}
            <div className="space-y-2">
              <h4 className="text-[11px] font-bold uppercase tracking-wider text-stone-500 flex items-center space-x-1.5">
                <Coins className="h-3.5 w-3.5 text-[#304638]" />
                <span>2. Bảng kê thu nhập, phụ cấp & khấu trừ</span>
              </h4>

              <div className="border border-stone-200 rounded-xl overflow-hidden text-xs">
                <table className="w-full text-left">
                  <thead className="bg-stone-100/80 text-stone-600 font-semibold uppercase text-[10px] border-b border-stone-200">
                    <tr>
                      <th className="px-3 py-2">Khoản mục</th>
                      <th className="px-3 py-2">Diễn giải / Ghi chú</th>
                      <th className="px-3 py-2 text-right">Số tiền (VND)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-stone-100">
                    {/* Base Salary Row */}
                    <tr>
                      <td className="px-3 py-2 font-medium text-stone-900">
                        Lương cơ bản theo công / giờ
                      </td>
                      <td className="px-3 py-2 text-stone-500 text-[11px]">
                        {isHourly ? 'Tính theo giờ làm duyệt và đơn giá' : 'Quy đổi công chuẩn tháng'}
                      </td>
                      <td className="px-3 py-2 text-right font-mono font-semibold text-stone-900">
                        {formatVND(effectiveLine.baseSalary)}
                      </td>
                    </tr>

                    {/* Adjustments Rows */}
                    {adjustments.map((adj) => {
                      const cfg = ADJUSTMENT_CODE_CONFIG[adj.adjustmentCode]
                      const isPos = adj.signedAmount > 0
                      return (
                        <tr key={adj.id}>
                          <td className="px-3 py-2 font-medium">
                            <span
                              className={
                                isPos ? 'text-emerald-800' : 'text-rose-800'
                              }
                            >
                              {cfg?.label || adj.adjustmentCode}
                            </span>
                          </td>
                          <td className="px-3 py-2 text-stone-500 text-[11px]">
                            {adj.explanation}
                          </td>
                          <td
                            className={`px-3 py-2 text-right font-mono font-semibold ${
                              isPos ? 'text-emerald-700' : 'text-rose-700'
                            }`}
                          >
                            {isPos ? '+' : ''}
                            {formatVND(adj.signedAmount)}
                          </td>
                        </tr>
                      )
                    })}

                    {/* Total Adjustments Summary */}
                    {adjustments.length > 0 && (
                      <tr className="bg-stone-50 font-semibold text-stone-700">
                        <td colSpan={2} className="px-3 py-1.5 text-right text-[11px]">
                          Tổng các khoản điều chỉnh:
                        </td>
                        <td
                          className={`px-3 py-1.5 text-right font-mono ${
                            effectiveLine.adjustmentsTotal >= 0
                              ? 'text-emerald-700'
                              : 'text-rose-700'
                          }`}
                        >
                          {effectiveLine.adjustmentsTotal >= 0 ? '+' : ''}
                          {formatVND(effectiveLine.adjustmentsTotal)}
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Net Salary & Payment Summary (Highlight Box) */}
            <div className="bg-[#304638]/5 border-2 border-[#304638]/20 rounded-2xl p-4 sm:p-5 space-y-3">
              <div className="flex items-center justify-between border-b border-[#304638]/10 pb-3">
                <span className="font-bold text-sm uppercase tracking-wide text-stone-900">
                  Lương thực lĩnh (Gross Salary):
                </span>
                <span className="text-xl sm:text-2xl font-black font-mono text-[#304638]">
                  {formatVND(grossSalary)}
                </span>
              </div>

              <div className="grid grid-cols-2 gap-4 text-xs pt-1">
                <div className="flex items-center justify-between">
                  <span className="text-stone-500">Đã thanh toán:</span>
                  <span className="font-mono font-bold text-emerald-700">
                    {formatVND(paidAmount)}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-stone-500">Số tiền còn lại:</span>
                  <span className="font-mono font-bold text-amber-800">
                    {formatVND(remainingDebt)}
                  </span>
                </div>
              </div>
            </div>

            {/* Signatures */}
            <div className="grid grid-cols-3 gap-4 pt-4 text-center text-xs">
              <div className="space-y-12">
                <span className="font-bold text-stone-700 block">Người lập biểu</span>
                <span className="text-[10px] text-stone-400 italic block">(Ký và ghi rõ họ tên)</span>
              </div>

              <div className="space-y-12">
                <span className="font-bold text-stone-700 block">Kế toán / Phê duyệt</span>
                <span className="text-[10px] text-stone-400 italic block">(Ký và ghi rõ họ tên)</span>
              </div>

              <div className="space-y-12">
                <span className="font-bold text-stone-700 block">Người nhận tiền</span>
                <span className="text-[10px] text-stone-400 italic block">(Ký xác nhận)</span>
              </div>
            </div>

            {/* Legal Notice Footer */}
            <div className="border-t border-stone-200 pt-3 text-[10px] text-stone-400 text-center leading-relaxed">
              Phiếu lương điện tử được tạo tự động từ hệ thống MTMS. Bảo mật thông tin lương theo quy chế
              nội bộ. Mọi thắc mắc về số giờ làm hoặc đơn giá vui lòng phản hồi quản lý chi nhánh trong vòng 03 ngày làm việc.
            </div>
          </div>

          {/* Modal Footer (No-print) */}
          <div className="px-6 py-3 border-t border-stone-200 bg-stone-50 flex items-center justify-between no-print">
            <span className="text-xs text-stone-500">
              Chế độ bảo mật: Dữ liệu phân quyền theo nhân sự & chi nhánh
            </span>

            <div className="flex items-center space-x-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={handlePrint}
                className="h-8 px-3 text-xs border-stone-300 flex items-center space-x-1.5"
              >
                <Printer className="h-3.5 w-3.5" />
                <span>In / Xuất PDF</span>
              </Button>

              <Button
                type="button"
                onClick={onClose}
                className="h-8 px-4 text-xs bg-[#304638] hover:bg-[#25372C] text-white"
              >
                Đóng
              </Button>
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
