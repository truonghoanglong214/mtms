import React, { useState, useEffect, useCallback } from 'react'
import {
  Calendar,
  Plus,
  RefreshCw,
  Eye,
  CheckCircle2,
  Clock,
  Paperclip,
  Link2,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { revenueApi, formatVnd } from '../api/revenueApi'
import { CreateDailyRevenueModal } from './CreateDailyRevenueModal'
import { DailyRevenueDetailModal } from './DailyRevenueDetailModal'
import type {
  DailyRevenueReportDto,
  RevenueReportStatus,
} from '../types/revenue.types'

interface DailyRevenueListTabProps {
  branchId: string
  branchName?: string
}

export const DailyRevenueListTab: React.FC<DailyRevenueListTabProps> = ({
  branchId,
  branchName,
}) => {
  const [reports, setReports] = useState<DailyRevenueReportDto[]>([])
  const [isLoading, setIsLoading] = useState<boolean>(false)

  // Filters
  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')
  const [selectedStatus, setSelectedStatus] = useState<RevenueReportStatus | 'ALL'>('ALL')

  // Modals
  const [createModalOpen, setCreateModalOpen] = useState<boolean>(false)
  const [detailModalOpen, setDetailModalOpen] = useState<boolean>(false)
  const [selectedReport, setSelectedReport] = useState<DailyRevenueReportDto | null>(null)

  const loadReports = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    try {
      const res = await revenueApi.getDailyRevenueList(branchId, {
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        status: selectedStatus === 'ALL' ? undefined : selectedStatus,
        size: 50,
      })

      if (Array.isArray(res)) {
        setReports(res)
      } else if (res && Array.isArray(res.content)) {
        setReports(res.content)
      } else {
        setReports([])
      }
    } catch (err) {
      console.error('Failed to load daily revenue reports:', err)
    } finally {
      setIsLoading(false)
    }
  }, [branchId, startDate, endDate, selectedStatus])

  useEffect(() => {
    loadReports()
  }, [loadReports])

  const handleResetFilters = () => {
    setStartDate('')
    setEndDate('')
    setSelectedStatus('ALL')
  }

  const getStatusBadge = (status: DailyRevenueReportDto['status']) => {
    switch (status) {
      case 'CONFIRMED':
        return (
          <Badge className="bg-emerald-100 text-emerald-800 border-emerald-300 font-semibold px-2 py-0.5 text-[11px]">
            <CheckCircle2 className="w-3 h-3 mr-1 text-emerald-600" /> ĐÃ DUYỆT
          </Badge>
        )
      case 'SUBMITTED':
        return (
          <Badge className="bg-blue-100 text-blue-800 border-blue-300 font-semibold px-2 py-0.5 text-[11px]">
            <Clock className="w-3 h-3 mr-1 text-blue-600" /> CHỜ DUYỆT
          </Badge>
        )
      case 'DRAFT':
        return (
          <Badge className="bg-amber-100 text-amber-800 border-amber-300 font-semibold px-2 py-0.5 text-[11px]">
            <Clock className="w-3 h-3 mr-1 text-amber-600" /> BẢN NHÁP
          </Badge>
        )
      case 'SUPERSEDED':
        return (
          <Badge variant="outline" className="bg-zinc-100 text-zinc-600 border-zinc-300 text-[11px]">
            ĐÃ THAY THẾ
          </Badge>
        )
      case 'CANCELLED':
        return (
          <Badge variant="destructive" className="text-[11px]">
            ĐÃ HỦY
          </Badge>
        )
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  return (
    <div className="space-y-5">
      {/* Filter and Action Bar */}
      <div className="p-4 bg-white rounded-2xl border border-[#E5DED4] shadow-xs flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
        {/* Filters */}
        <div className="flex flex-wrap items-center gap-2.5 flex-1">
          <div className="relative">
            <span className="text-[11px] font-semibold text-[#786C60] block mb-1">Từ ngày</span>
            <Input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="h-9 text-xs border-[#E5DED4] bg-white w-36"
            />
          </div>

          <div className="relative">
            <span className="text-[11px] font-semibold text-[#786C60] block mb-1">Đến ngày</span>
            <Input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="h-9 text-xs border-[#E5DED4] bg-white w-36"
            />
          </div>

          <div>
            <span className="text-[11px] font-semibold text-[#786C60] block mb-1">Trạng thái</span>
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value as RevenueReportStatus | 'ALL')}
              className="h-9 text-xs border border-[#E5DED4] rounded-xl px-2.5 bg-white text-[#352D26] focus:ring-2 focus:ring-[#B96849]"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="CONFIRMED">Đã duyệt (CONFIRMED)</option>
              <option value="SUBMITTED">Chờ duyệt (SUBMITTED)</option>
              <option value="DRAFT">Bản nháp (DRAFT)</option>
              <option value="SUPERSEDED">Đã thay thế (SUPERSEDED)</option>
            </select>
          </div>

          <div className="flex items-end space-x-1.5 self-end pt-1">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleResetFilters}
              className="h-9 text-xs border-[#E5DED4] text-[#786C60]"
            >
              Đặt lại
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={loadReports}
              disabled={isLoading}
              className="h-9 text-xs border-[#E5DED4] text-[#352D26]"
            >
              <RefreshCw className={`w-3.5 h-3.5 mr-1.5 ${isLoading ? 'animate-spin' : ''}`} />
              Lọc
            </Button>
          </div>
        </div>

        {/* Action Button */}
        <div className="flex items-center space-x-2 pt-2 md:pt-0 self-end">
          <Button
            type="button"
            onClick={() => setCreateModalOpen(true)}
            className="text-xs h-9 bg-[#B96849] hover:bg-[#A0573B] text-white shadow-sm font-semibold"
          >
            <Plus className="w-3.5 h-3.5 mr-1" />
            Nhập doanh thu
          </Button>
        </div>
      </div>

      {/* Reports Table (Desktop) & Card List (Mobile) */}
      <div className="bg-white rounded-2xl border border-[#E5DED4] shadow-xs overflow-hidden">
        {/* Desktop Table View */}
        <div className="hidden lg:block overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#F7F3EC]/80 border-b border-[#E5DED4] text-[#786C60] font-semibold uppercase tracking-wider text-[11px]">
              <tr>
                <th className="py-3 px-4">Ngày kinh doanh</th>
                <th className="py-3 px-3 text-center">Rev</th>
                <th className="py-3 px-4 text-right">Tiền mặt tại quầy</th>
                <th className="py-3 px-4 text-right">Chuyển khoản QR</th>
                <th className="py-3 px-4 text-right">App NET</th>
                <th className="py-3 px-3 text-right">Điều chỉnh</th>
                <th className="py-3 px-4 text-right">Tổng doanh thu</th>
                <th className="py-3 px-3 text-center">Trạng thái</th>
                <th className="py-3 px-3 text-center">Chứng từ</th>
                <th className="py-3 px-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#E5DED4]">
              {reports.length === 0 ? (
                <tr>
                  <td colSpan={10} className="py-12 text-center text-zinc-400">
                    {isLoading ? 'Đang tải dữ liệu doanh thu...' : 'Chưa có báo cáo doanh thu nào trong khoảng thời gian này.'}
                  </td>
                </tr>
              ) : (
                reports.map((report) => (
                  <tr
                    key={report.id}
                    onClick={() => {
                      setSelectedReport(report)
                      setDetailModalOpen(true)
                    }}
                    className="hover:bg-[#F7F3EC]/50 cursor-pointer transition-colors"
                  >
                    <td className="py-3 px-4 font-semibold text-[#352D26]">
                      <div className="flex items-center space-x-1.5">
                        <Calendar className="w-3.5 h-3.5 text-[#786C60]" />
                        <span>{report.businessDate}</span>
                      </div>
                    </td>
                    <td className="py-3 px-3 text-center">
                      <span className="font-mono text-[10px] bg-zinc-100 text-zinc-700 px-1.5 py-0.5 rounded-sm">
                        r{report.revision}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right font-medium text-[#304638]">
                      {formatVnd(report.cashAmount)}
                    </td>
                    <td className="py-3 px-4 text-right font-medium text-[#B96849]">
                      {formatVnd(report.bankTransferAmount)}
                    </td>
                    <td className="py-3 px-4 text-right font-medium text-blue-700">
                      {formatVnd(report.deliveryAppNetAmount)}
                    </td>
                    <td className={`py-3 px-3 text-right font-medium ${report.otherAdjustmentAmount < 0 ? 'text-[#B7423E]' : 'text-[#786C60]'}`}>
                      {formatVnd(report.otherAdjustmentAmount)}
                    </td>
                    <td className="py-3 px-4 text-right font-bold text-[#304638] text-sm">
                      {formatVnd(report.totalRevenue)}
                    </td>
                    <td className="py-3 px-3 text-center">
                      <div className="flex flex-col items-center gap-1">
                        {getStatusBadge(report.status)}
                        {report.status === 'CONFIRMED' && (
                          <span
                            className="inline-flex items-center gap-1 text-[10px] text-emerald-700 bg-emerald-50 border border-emerald-200/80 px-1.5 py-0.5 rounded font-medium whitespace-nowrap"
                            title="Đã liên kết sổ quỹ tiền mặt & ngân hàng"
                          >
                            <Link2 className="w-2.5 h-2.5 text-emerald-600" />
                            Đã liên kết sổ quỹ
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="py-3 px-3 text-center">
                      {report.evidenceFileId ? (
                        <span className="inline-flex items-center p-1 bg-emerald-50 text-emerald-700 rounded-md" title="Có chứng từ Z-report">
                          <Paperclip className="w-3.5 h-3.5" />
                        </span>
                      ) : (
                        <span className="text-zinc-300">-</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation()
                          setSelectedReport(report)
                          setDetailModalOpen(true)
                        }}
                        className="h-8 text-xs border-[#E5DED4] text-[#352D26] hover:bg-[#F7F3EC]"
                      >
                        <Eye className="w-3.5 h-3.5 mr-1" />
                        Chi tiết
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Mobile / Tablet Card View */}
        <div className="block lg:hidden divide-y divide-[#E5DED4]">
          {reports.length === 0 ? (
            <div className="py-12 text-center text-zinc-400 text-xs">
              {isLoading ? 'Đang tải dữ liệu doanh thu...' : 'Chưa có báo cáo doanh thu nào trong khoảng thời gian này.'}
            </div>
          ) : (
            reports.map((report) => (
              <div
                key={report.id}
                onClick={() => {
                  setSelectedReport(report)
                  setDetailModalOpen(true)
                }}
                className="p-4 space-y-2.5 hover:bg-[#F7F3EC]/40 cursor-pointer transition-colors"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-sm text-[#352D26]">{report.businessDate}</span>
                    <span className="text-[10px] font-mono bg-zinc-100 text-zinc-700 px-1.5 py-0.2 rounded-sm">
                      r{report.revision}
                    </span>
                  </div>
                  <div className="flex items-center gap-1.5 flex-wrap justify-end">
                    {report.status === 'CONFIRMED' && (
                      <span
                        className="inline-flex items-center gap-1 text-[10px] text-emerald-700 bg-emerald-50 border border-emerald-200 px-1.5 py-0.5 rounded font-medium"
                        title="Đã liên kết sổ quỹ tiền mặt & ngân hàng"
                      >
                        <Link2 className="w-2.5 h-2.5 text-emerald-600" />
                        Đã liên kết sổ quỹ
                      </span>
                    )}
                    {getStatusBadge(report.status)}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 text-xs bg-[#F7F3EC]/50 p-2.5 rounded-xl border border-[#E5DED4]/60">
                  <div>
                    <span className="text-[10px] text-[#786C60] block">Tiền mặt:</span>
                    <span className="font-semibold text-[#304638]">{formatVnd(report.cashAmount)}</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-[#786C60] block">Chuyển khoản QR:</span>
                    <span className="font-semibold text-[#B96849]">{formatVnd(report.bankTransferAmount)}</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-[#786C60] block">App NET:</span>
                    <span className="font-semibold text-blue-700">{formatVnd(report.deliveryAppNetAmount)}</span>
                  </div>
                  <div>
                    <span className="text-[10px] text-[#786C60] block">Điều chỉnh:</span>
                    <span className="font-semibold text-[#786C60]">{formatVnd(report.otherAdjustmentAmount)}</span>
                  </div>
                </div>

                <div className="flex items-center justify-between pt-1">
                  <div>
                    <span className="text-[10px] uppercase tracking-wider text-[#786C60] block font-medium">
                      Tổng doanh thu
                    </span>
                    <span className="text-base font-extrabold text-[#304638]">
                      {formatVnd(report.totalRevenue)}
                    </span>
                  </div>
                  <div className="flex items-center space-x-2">
                    {report.evidenceFileId && (
                      <span className="p-1 bg-emerald-50 text-emerald-700 rounded-md text-[10px] flex items-center gap-1 font-medium">
                        <Paperclip className="w-3 h-3" /> Z-report
                      </span>
                    )}
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      className="h-8 text-xs border-[#E5DED4] text-[#352D26]"
                    >
                      Chi tiết
                    </Button>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Modals */}
      <CreateDailyRevenueModal
        isOpen={createModalOpen}
        branchId={branchId}
        branchName={branchName}
        onClose={() => setCreateModalOpen(false)}
        onSuccess={() => {
          loadReports()
        }}
      />

      <DailyRevenueDetailModal
        isOpen={detailModalOpen}
        report={selectedReport}
        branchId={branchId}
        branchName={branchName}
        onClose={() => setDetailModalOpen(false)}
        onConfirmed={() => {
          loadReports()
        }}
      />
    </div>
  )
}
