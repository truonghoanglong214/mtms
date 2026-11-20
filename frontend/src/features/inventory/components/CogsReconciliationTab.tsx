import React, { useState, useEffect, useCallback } from 'react'
import {
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Calendar,
  Calculator,
  Plus,
  Minus,
  Equal,
  Package,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { reportingApi } from '../api/reportingApi'
import type { CogsReportDto } from '../types/reporting.types'

interface CogsReconciliationTabProps {
  branchId: string
  branchName?: string
}

function formatVnd(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === '') return '0 ₫'
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(num)
}

function formatQuantity(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === '') return '0'
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0'
  return new Intl.NumberFormat('vi-VN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 3,
  }).format(num)
}

function getStartOfCurrentMonth(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}-01`
}

function getEndOfCurrentMonth(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const lastDay = new Date(year, month, 0).getDate()
  return `${year}-${String(month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`
}

export const CogsReconciliationTab: React.FC<CogsReconciliationTabProps> = ({
  branchId,
  branchName,
}) => {
  const [startDate, setStartDate] = useState<string>(getStartOfCurrentMonth())
  const [endDate, setEndDate] = useState<string>(getEndOfCurrentMonth())
  const [data, setData] = useState<CogsReportDto | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchCogsReport = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    setError(null)
    try {
      const res = await reportingApi.getCogsReport(branchId, startDate, endDate)
      setData(res)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải báo cáo đối soát giá vốn COGS.'
      )
    } finally {
      setIsLoading(false)
    }
  }, [branchId, startDate, endDate])

  useEffect(() => {
    fetchCogsReport()
  }, [fetchCogsReport])

  return (
    <div className="space-y-6">
      {/* Date Range Selection & Header */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs">
        <div className="flex items-center gap-2">
          <Calendar className="h-4 w-4 text-[#304638]" />
          <span className="text-xs font-semibold text-slate-800">
            Kỳ đối soát kế toán:
          </span>
          <span className="text-xs text-slate-500 font-mono">
            {startDate} → {endDate}
          </span>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center gap-1.5 text-xs text-slate-600">
            <span>Từ:</span>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="h-8 px-2.5 text-xs rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-1 focus:ring-[#304638]"
            />
          </div>
          <div className="flex items-center gap-1.5 text-xs text-slate-600">
            <span>Đến:</span>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="h-8 px-2.5 text-xs rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-none focus:ring-1 focus:ring-[#304638]"
            />
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={fetchCogsReport}
            disabled={isLoading}
            className="h-8 px-3 text-xs gap-1.5 rounded-xl border-slate-200 hover:bg-slate-50"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            <span>Đối soát lại</span>
          </Button>
        </div>
      </div>

      {/* Error Notice */}
      {error && (
        <div className="p-4 rounded-2xl border border-rose-200 bg-rose-50 text-rose-800 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-4 w-4 text-rose-600 shrink-0" />
            <span>{error}</span>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={fetchCogsReport}
            className="h-7 text-xs border-rose-300 text-rose-700 hover:bg-rose-100"
          >
            Thử lại
          </Button>
        </div>
      )}

      {/* Verification Banner: TC-020 No Double Cost Invariant */}
      <div className="rounded-2xl border border-emerald-300 bg-gradient-to-r from-emerald-50 via-white to-emerald-50/40 p-5 shadow-2xs">
        <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2.5 flex-wrap">
              <span className="p-1.5 rounded-lg bg-[#304638] text-white">
                <ShieldCheck className="h-4 w-4" />
              </span>
              <h3 className="text-sm font-bold text-slate-900">
                Nguyên tắc đối soát Giá vốn (COGS) & Không trùng chi phí (TC-020)
              </h3>
              {data?.noDoubleCostVerified && (
                <Badge className="bg-emerald-700 text-white font-medium text-xs gap-1 py-0.5">
                  <CheckCircle2 className="h-3 w-3" />
                  <span>No Double Cost Verified</span>
                </Badge>
              )}
              {data?.reconciled && (
                <Badge variant="outline" className="border-emerald-600 text-emerald-800 bg-white text-xs py-0.5">
                  Toàn vẹn số dư 100%
                </Badge>
              )}
            </div>
            <p className="text-xs text-slate-600 max-w-3xl leading-relaxed">
              {data?.accountingExplanation ||
                'Giá vốn hàng bán (COGS) trong kỳ chỉ tính trên giá trị thực tế xuất dùng pha chế. Tiền nhập mua hàng được ghi nhận vào công nợ NCC và tài sản kho, tuyệt đối không bị tính trùng hai lần vào chi phí P&L.'}
            </p>
          </div>

          <div className="shrink-0 flex items-center gap-3 bg-white px-4 py-2.5 rounded-xl border border-emerald-200">
            <div className="text-right">
              <div className="text-[10px] uppercase font-semibold text-slate-500">
                Chênh lệch đối soát
              </div>
              <div className="text-sm font-bold font-mono text-emerald-800">
                {formatVnd(data?.reconciliationDiscrepancy ?? '0.00')}
              </div>
            </div>
            <div className="h-8 w-8 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold">
              ✓
            </div>
          </div>
        </div>
      </div>

      {/* Visual Mathematical Equation Card: Beginning + Purchases - COGS = Ending */}
      <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-2xs">
        <div className="flex items-center justify-between pb-4 border-b border-slate-100">
          <div className="flex items-center gap-2">
            <Calculator className="h-4 w-4 text-[#304638]" />
            <span className="text-xs font-bold text-slate-800 uppercase tracking-wider">
              Phương trình cân bằng giá trị tồn kho trong kỳ
            </span>
          </div>
          <Badge variant="outline" className="text-[10px] font-mono border-slate-200">
            TC-020 Read Model
          </Badge>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-7 gap-3 pt-4 items-center">
          {/* Term 1: Beginning */}
          <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 lg:col-span-2 text-center sm:text-left">
            <div className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">
              Tồn đầu kỳ
            </div>
            <div className="text-lg font-bold font-mono text-slate-900 mt-1">
              {isLoading ? '...' : formatVnd(data?.beginningStockValue)}
            </div>
            <div className="text-[10px] text-slate-400 mt-0.5">Số dư đầu kỳ ghi nhận</div>
          </div>

          {/* Operator + */}
          <div className="flex items-center justify-center text-slate-400 font-bold">
            <Plus className="h-5 w-5 text-emerald-600" />
          </div>

          {/* Term 2: Purchases */}
          <div className="p-3.5 rounded-xl bg-emerald-50/70 border border-emerald-200 lg:col-span-1 text-center sm:text-left">
            <div className="text-[11px] font-semibold text-emerald-900 uppercase tracking-wider">
              Tiền nhập mua
            </div>
            <div className="text-lg font-bold font-mono text-emerald-900 mt-1">
              {isLoading ? '...' : formatVnd(data?.periodPurchasesTotal)}
            </div>
            <div className="text-[10px] text-emerald-700 mt-0.5">Vào tài sản & nợ NCC</div>
          </div>

          {/* Operator - */}
          <div className="flex items-center justify-center text-slate-400 font-bold">
            <Minus className="h-5 w-5 text-rose-600" />
          </div>

          {/* Term 3: COGS (Issues) */}
          <div className="p-3.5 rounded-xl bg-rose-50/70 border border-rose-200 lg:col-span-1 text-center sm:text-left">
            <div className="text-[11px] font-semibold text-rose-900 uppercase tracking-wider">
              Giá vốn xuất dùng
            </div>
            <div className="text-lg font-bold font-mono text-rose-900 mt-1">
              {isLoading ? '...' : formatVnd(data?.periodCogsTotal)}
            </div>
            <div className="text-[10px] text-rose-700 mt-0.5">Ghi nhận vào P&L COGS</div>
          </div>

          {/* Operator = */}
          <div className="flex items-center justify-center text-slate-400 font-bold">
            <Equal className="h-5 w-5 text-slate-600" />
          </div>

          {/* Term 4: Ending Stock */}
          <div className="p-3.5 rounded-xl bg-blue-50/70 border border-blue-200 lg:col-span-1 text-center sm:text-left">
            <div className="text-[11px] font-semibold text-blue-900 uppercase tracking-wider">
              Tồn cuối kỳ
            </div>
            <div className="text-lg font-bold font-mono text-blue-950 mt-1">
              {isLoading ? '...' : formatVnd(data?.actualEndingStockValue)}
            </div>
            <div className="text-[10px] text-blue-700 mt-0.5">Giá trị tồn kho WAC</div>
          </div>
        </div>
      </div>

      {/* Detailed Material-by-Material Breakdown Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
        <div className="p-4 sm:p-5 border-b border-slate-200 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 bg-slate-50/50">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <span>Bảng kê chi tiết Xuất - Nhập - Tồn & Giá vốn từng nguyên vật liệu</span>
              <Badge variant="outline" className="border-slate-300 text-slate-700 text-[10px]">
                {data?.items?.length ?? 0} mặt hàng
              </Badge>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Phân rã số dư đầu kỳ, biến động nhập mua và xuất dùng tính giá vốn WAC tại {branchName || 'chi nhánh'}.
            </p>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-100/90 border-b border-slate-200 text-slate-700 font-semibold text-[11px]">
                <th className="py-2.5 px-3 w-10 text-center" rowSpan={2}>#</th>
                <th className="py-2.5 px-3" rowSpan={2}>Nguyên vật liệu</th>
                <th className="py-2.5 px-3 text-center" rowSpan={2}>ĐVT</th>
                <th className="py-2 px-3 text-center bg-slate-200/50 border-r border-slate-200" colSpan={2}>
                  Tồn đầu kỳ
                </th>
                <th className="py-2 px-3 text-center bg-emerald-100/40 border-r border-slate-200" colSpan={2}>
                  Nhập mua trong kỳ
                </th>
                <th className="py-2 px-3 text-center bg-rose-100/40 border-r border-slate-200" colSpan={2}>
                  Xuất dùng (Giá vốn COGS)
                </th>
                <th className="py-2 px-3 text-center bg-blue-100/40" colSpan={3}>
                  Tồn cuối kỳ
                </th>
              </tr>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-600 font-medium text-[10px]">
                <th className="py-1.5 px-2.5 text-right bg-slate-200/40">Số lượng</th>
                <th className="py-1.5 px-2.5 text-right bg-slate-200/40 border-r border-slate-200">Giá trị</th>
                <th className="py-1.5 px-2.5 text-right bg-emerald-50">Số lượng</th>
                <th className="py-1.5 px-2.5 text-right bg-emerald-50 border-r border-slate-200">Tiền nhập</th>
                <th className="py-1.5 px-2.5 text-right bg-rose-50">Số lượng</th>
                <th className="py-1.5 px-2.5 text-right bg-rose-50 border-r border-slate-200">Giá vốn (COGS)</th>
                <th className="py-1.5 px-2.5 text-right bg-blue-50">Số lượng</th>
                <th className="py-1.5 px-2.5 text-right bg-blue-50">Đơn giá WAC</th>
                <th className="py-1.5 px-2.5 text-right bg-blue-50">Giá trị tồn</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                Array.from({ length: 5 }).map((_, idx) => (
                  <tr key={idx} className="animate-pulse">
                    <td colSpan={12} className="py-3 px-3">
                      <div className="h-4 bg-slate-200 rounded w-full" />
                    </td>
                  </tr>
                ))
              ) : !data?.items || data.items.length === 0 ? (
                <tr>
                  <td colSpan={12} className="py-12 text-center text-slate-500">
                    <div className="max-w-sm mx-auto space-y-2">
                      <div className="h-10 w-10 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
                        <Package className="h-5 w-5" />
                      </div>
                      <p className="font-semibold text-slate-700">
                        Chưa có dữ liệu biến động kho trong kỳ
                      </p>
                      <p className="text-[11px] text-slate-500">
                        Không có mặt hàng nào phát sinh số dư hoặc giao dịch xuất nhập trong khoảng ngày đã chọn.
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                data.items.map((row, idx) => (
                  <tr
                    key={row.itemId || idx}
                    className="hover:bg-slate-50/70 transition-colors text-slate-800"
                  >
                    <td className="py-2.5 px-3 text-center font-mono text-[11px] text-slate-400">
                      {idx + 1}
                    </td>
                    <td className="py-2.5 px-3">
                      <div className="font-semibold text-slate-900">{row.itemName}</div>
                      <div className="font-mono text-[11px] text-slate-500">{row.itemCode}</div>
                    </td>
                    <td className="py-2.5 px-3 text-center">
                      <Badge
                        variant="outline"
                        className="text-[10px] font-mono border-slate-200 bg-slate-50 text-slate-700"
                      >
                        {row.unitName || row.unitCode}
                      </Badge>
                    </td>
                    {/* Beginning */}
                    <td className="py-2.5 px-2.5 text-right font-mono text-slate-700">
                      {formatQuantity(row.beginningQty)}
                    </td>
                    <td className="py-2.5 px-2.5 text-right font-mono text-slate-700 border-r border-slate-200">
                      {formatVnd(row.beginningValue)}
                    </td>
                    {/* Purchases */}
                    <td className="py-2.5 px-2.5 text-right font-mono font-medium text-emerald-800">
                      {formatQuantity(row.purchasedQty)}
                    </td>
                    <td className="py-2.5 px-2.5 text-right font-mono font-medium text-emerald-800 border-r border-slate-200">
                      {formatVnd(row.purchasedValue)}
                    </td>
                    {/* COGS (Issues) */}
                    <td className="py-2.5 px-2.5 text-right font-mono font-medium text-rose-800">
                      {formatQuantity(row.issuedQty)}
                    </td>
                    <td className="py-2.5 px-2.5 text-right font-mono font-bold text-rose-800 border-r border-slate-200">
                      {formatVnd(row.cogsValue)}
                    </td>
                    {/* Ending */}
                    <td className="py-2.5 px-2.5 text-right font-mono font-bold text-slate-900">
                      {formatQuantity(row.endingQty)}
                    </td>
                    <td className="py-2.5 px-2.5 text-right font-mono text-slate-600">
                      {formatVnd(row.endingWac)}
                    </td>
                    <td className="py-2.5 px-2.5 text-right font-mono font-bold text-blue-900">
                      {formatVnd(row.endingValue)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
export default CogsReconciliationTab
