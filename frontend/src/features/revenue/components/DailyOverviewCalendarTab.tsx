import React, { useState, useEffect, useMemo, useCallback } from 'react'
import {
  ChevronLeft,
  ChevronRight,
  AlertTriangle,
  DoorClosed,
  Plus,
  Settings2,
  RefreshCw,
  TrendingUp,
  DollarSign,
  QrCode,
  Smartphone,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { revenueApi, formatVnd } from '../api/revenueApi'
import { CreateDailyRevenueModal } from './CreateDailyRevenueModal'
import { DailyRevenueDetailModal } from './DailyRevenueDetailModal'
import { BusinessDayStatusModal } from './BusinessDayStatusModal'
import type {
  BusinessDayDto,
  DailyRevenueReportDto,
  MissingDateDto,
} from '../types/revenue.types'

interface DailyOverviewCalendarTabProps {
  branchId: string
  branchName?: string
}

export const DailyOverviewCalendarTab: React.FC<DailyOverviewCalendarTabProps> = ({
  branchId,
  branchName,
}) => {
  const [selectedDate, setSelectedDate] = useState<Date>(new Date())
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [businessDays, setBusinessDays] = useState<BusinessDayDto[]>([])
  const [reports, setReports] = useState<DailyRevenueReportDto[]>([])
  const [missingDates, setMissingDates] = useState<MissingDateDto[]>([])

  // Modal states
  const [createModalOpen, setCreateModalOpen] = useState<boolean>(false)
  const [targetDateForCreate, setTargetDateForCreate] = useState<string>('')

  const [detailModalOpen, setDetailModalOpen] = useState<boolean>(false)
  const [selectedReport, setSelectedReport] = useState<DailyRevenueReportDto | null>(null)

  const [statusModalOpen, setStatusModalOpen] = useState<boolean>(false)
  const [targetDateForStatus, setTargetDateForStatus] = useState<string>('')
  const [targetStatus, setTargetStatus] = useState<'OPEN' | 'CLOSED'>('OPEN')
  const [targetCloseReason, setTargetCloseReason] = useState<string | null>(null)

  const year = selectedDate.getFullYear()
  const month = selectedDate.getMonth() // 0-indexed

  // Format month query string YYYY-MM
  const monthStr = useMemo(() => {
    return `${year}-${String(month + 1).padStart(2, '0')}`
  }, [year, month])

  const startDateStr = useMemo(() => `${monthStr}-01`, [monthStr])
  const daysInMonth = useMemo(() => new Date(year, month + 1, 0).getDate(), [year, month])
  const endDateStr = useMemo(() => `${monthStr}-${String(daysInMonth).padStart(2, '0')}`, [monthStr, daysInMonth])

  const todayStr = useMemo(() => new Date().toISOString().split('T')[0], [])

  // Load calendar data
  const loadCalendarData = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    try {
      const [daysData, reportsData, missingData] = await Promise.all([
        revenueApi.getBusinessDays(branchId, { startDate: startDateStr, endDate: endDateStr }),
        revenueApi.getDailyRevenueList(branchId, { startDate: startDateStr, endDate: endDateStr, size: 100 }),
        revenueApi.getMissingDates(branchId, { startDate: startDateStr, endDate: endDateStr }),
      ])

      setBusinessDays(Array.isArray(daysData) ? daysData : [])

      if (Array.isArray(reportsData)) {
        setReports(reportsData)
      } else if (reportsData && Array.isArray(reportsData.content)) {
        setReports(reportsData.content)
      } else {
        setReports([])
      }

      setMissingDates(Array.isArray(missingData) ? missingData : [])
    } catch (err) {
      console.error('Failed to load revenue calendar data:', err)
    } finally {
      setIsLoading(false)
    }
  }, [branchId, startDateStr, endDateStr])

  useEffect(() => {
    loadCalendarData()
  }, [loadCalendarData])

  // Month navigation
  const handlePrevMonth = () => {
    setSelectedDate(new Date(year, month - 1, 1))
  }

  const handleNextMonth = () => {
    setSelectedDate(new Date(year, month + 1, 1))
  }

  const handleToday = () => {
    setSelectedDate(new Date())
  }

  // Monthly aggregated totals
  const monthlyTotals = useMemo(() => {
    let totalRev = 0
    let totalCash = 0
    let totalBank = 0
    let totalApp = 0
    let confirmedCount = 0
    let pendingCount = 0

    reports.forEach((r) => {
      if (r.isCurrent && (r.status === 'CONFIRMED' || r.status === 'SUBMITTED')) {
        totalRev += Number(r.totalRevenue || 0)
        totalCash += Number(r.cashAmount || 0)
        totalBank += Number(r.bankTransferAmount || 0)
        totalApp += Number(r.deliveryAppNetAmount || 0)

        if (r.status === 'CONFIRMED') confirmedCount++
        if (r.status === 'SUBMITTED') pendingCount++
      }
    })

    return { totalRev, totalCash, totalBank, totalApp, confirmedCount, pendingCount }
  }, [reports])

  // Calendar cells generation
  // Monday is index 0 in VN (Sunday is 6)
  const firstDayOfWeek = (new Date(year, month, 1).getDay() + 6) % 7 // 0 = Mon, 6 = Sun

  const calendarDays = useMemo(() => {
    const list = []
    // Days in current month
    for (let day = 1; day <= daysInMonth; day++) {
      const dateStr = `${monthStr}-${String(day).padStart(2, '0')}`
      const dayRecord = businessDays.find((b) => b.businessDate === dateStr)
      const reportRecord = reports.find((r) => r.businessDate === dateStr && r.isCurrent)
      const isMissing = missingDates.some((m) => m.businessDate === dateStr)

      const isPast = dateStr < todayStr
      const isToday = dateStr === todayStr
      const isFuture = dateStr > todayStr

      list.push({
        day,
        dateStr,
        isPast,
        isToday,
        isFuture,
        dayRecord,
        reportRecord,
        isClosed: dayRecord?.operationStatus === 'CLOSED',
        closeReason: dayRecord?.closeReason,
        isMissing,
      })
    }
    return list
  }, [daysInMonth, monthStr, businessDays, reports, missingDates, todayStr])

  const handleDayClick = (cell: (typeof calendarDays)[0]) => {
    if (cell.reportRecord) {
      setSelectedReport(cell.reportRecord)
      setDetailModalOpen(true)
    } else if (cell.isClosed) {
      setTargetDateForStatus(cell.dateStr)
      setTargetStatus('CLOSED')
      setTargetCloseReason(cell.closeReason || '')
      setStatusModalOpen(true)
    } else {
      setTargetDateForCreate(cell.dateStr)
      setCreateModalOpen(true)
    }
  }

  const handleOpenStatusModal = (e: React.MouseEvent, cell: (typeof calendarDays)[0]) => {
    e.stopPropagation()
    setTargetDateForStatus(cell.dateStr)
    setTargetStatus(cell.isClosed ? 'CLOSED' : 'OPEN')
    setTargetCloseReason(cell.closeReason || '')
    setStatusModalOpen(true)
  }

  return (
    <div className="space-y-6">
      {/* Monthly Summary Statistics Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
        {/* Tổng doanh thu */}
        <div className="p-4 bg-white rounded-2xl border border-[#E5DED4] shadow-xs">
          <div className="flex items-center justify-between text-xs text-[#786C60] mb-1.5">
            <span className="font-semibold uppercase tracking-wider text-[11px]">Tổng doanh thu tháng</span>
            <div className="p-1.5 bg-[#819B78]/15 rounded-lg text-[#304638]">
              <TrendingUp className="w-4 h-4" />
            </div>
          </div>
          <div className="text-xl sm:text-2xl font-extrabold text-[#304638] tracking-tight">
            {formatVnd(monthlyTotals.totalRev)}
          </div>
          <div className="mt-1 text-[11px] text-[#786C60]">
            {monthlyTotals.confirmedCount} ngày đã duyệt, {monthlyTotals.pendingCount} chờ duyệt
          </div>
        </div>

        {/* Tiền mặt */}
        <div className="p-4 bg-white rounded-2xl border border-[#E5DED4] shadow-xs">
          <div className="flex items-center justify-between text-xs text-[#786C60] mb-1.5">
            <span className="font-semibold uppercase tracking-wider text-[11px]">Tiền mặt tại quầy</span>
            <div className="p-1.5 bg-emerald-50 rounded-lg text-emerald-700">
              <DollarSign className="w-4 h-4" />
            </div>
          </div>
          <div className="text-xl sm:text-2xl font-extrabold text-[#352D26] tracking-tight">
            {formatVnd(monthlyTotals.totalCash)}
          </div>
          <div className="mt-1 text-[11px] text-emerald-700 font-medium">
            Thu trực tiếp tại quầy thu ngân
          </div>
        </div>

        {/* Chuyển khoản */}
        <div className="p-4 bg-white rounded-2xl border border-[#E5DED4] shadow-xs">
          <div className="flex items-center justify-between text-xs text-[#786C60] mb-1.5">
            <span className="font-semibold uppercase tracking-wider text-[11px]">Chuyển khoản VietQR</span>
            <div className="p-1.5 bg-[#B96849]/15 rounded-lg text-[#B96849]">
              <QrCode className="w-4 h-4" />
            </div>
          </div>
          <div className="text-xl sm:text-2xl font-extrabold text-[#B96849] tracking-tight">
            {formatVnd(monthlyTotals.totalBank)}
          </div>
          <div className="mt-1 text-[11px] text-[#B96849] font-medium">
            Tiền chuyển thẳng tài khoản NH
          </div>
        </div>

        {/* App giao hàng NET */}
        <div className="p-4 bg-white rounded-2xl border border-[#E5DED4] shadow-xs">
          <div className="flex items-center justify-between text-xs text-[#786C60] mb-1.5">
            <span className="font-semibold uppercase tracking-wider text-[11px]">Doanh thu App NET</span>
            <div className="p-1.5 bg-blue-50 rounded-lg text-blue-700">
              <Smartphone className="w-4 h-4" />
            </div>
          </div>
          <div className="text-xl sm:text-2xl font-extrabold text-blue-700 tracking-tight">
            {formatVnd(monthlyTotals.totalApp)}
          </div>
          <div className="mt-1 text-[11px] text-blue-700 font-medium truncate" title="Đối soát chuyển khoản riêng">
            Đối soát NH sau
          </div>
        </div>
      </div>

      {/* Missing Dates Warning Box */}
      {missingDates.length > 0 && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-900 shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-start space-x-3">
            <div className="p-2 bg-red-100 text-red-700 rounded-xl shrink-0 mt-0.5">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-red-950 flex items-center gap-1.5">
                Cảnh báo thiếu báo cáo doanh thu
              </h4>
              <p className="text-xs text-red-800 mt-0.5">
                Có <strong>{missingDates.length} ngày</strong> mở cửa trong quá khứ chưa nộp báo cáo doanh thu. Kỳ kế toán sẽ bị chặn chốt sổ tháng nếu còn ngày thiếu!
              </p>
              <div className="flex flex-wrap gap-1.5 mt-2">
                {missingDates.map((m) => (
                  <button
                    key={m.businessDate}
                    type="button"
                    onClick={() => {
                      setTargetDateForCreate(m.businessDate)
                      setCreateModalOpen(true)
                    }}
                    className="text-[11px] font-semibold bg-white border border-red-300 text-red-800 px-2.5 py-0.5 rounded-lg hover:bg-red-100 transition-colors flex items-center gap-1"
                  >
                    <Plus className="w-3 h-3" /> Ngày {m.businessDate}
                  </button>
                ))}
              </div>
            </div>
          </div>
          <Button
            type="button"
            size="sm"
            onClick={() => {
              if (missingDates[0]) {
                setTargetDateForCreate(missingDates[0].businessDate)
                setCreateModalOpen(true)
              }
            }}
            className="bg-red-700 hover:bg-red-800 text-white text-xs h-9 shrink-0 shadow-sm"
          >
            Bổ sung ngay
          </Button>
        </div>
      )}

      {/* Calendar Controls Bar */}
      <div className="bg-white p-4 rounded-2xl border border-[#E5DED4] shadow-xs flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="flex items-center space-x-3">
          <div className="flex items-center space-x-1 bg-[#F7F3EC] p-1 rounded-xl border border-[#E5DED4]">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handlePrevMonth}
              className="h-8 w-8 p-0 border-none bg-transparent hover:bg-white text-[#352D26]"
              title="Tháng trước"
            >
              <ChevronLeft className="w-4 h-4" />
            </Button>
            <div className="px-3 text-sm font-bold text-[#352D26] min-w-[140px] text-center">
              Tháng {month + 1} / {year}
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleNextMonth}
              className="h-8 w-8 p-0 border-none bg-transparent hover:bg-white text-[#352D26]"
              title="Tháng sau"
            >
              <ChevronRight className="w-4 h-4" />
            </Button>
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleToday}
            className="h-9 text-xs border-[#E5DED4] text-[#352D26]"
          >
            Hôm nay
          </Button>

          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={loadCalendarData}
            disabled={isLoading}
            className="h-9 w-9 p-0 border-[#E5DED4] text-[#786C60]"
            title="Tải lại dữ liệu"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          </Button>
        </div>

        {/* Legend */}
        <div className="flex flex-wrap items-center gap-3 text-xs text-[#786C60]">
          <div className="flex items-center space-x-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
            <span>Đã duyệt (CONFIRMED)</span>
          </div>
          <div className="flex items-center space-x-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-amber-500" />
            <span>Chờ duyệt / Nháp</span>
          </div>
          <div className="flex items-center space-x-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-red-500" />
            <span>Chưa nộp</span>
          </div>
          <div className="flex items-center space-x-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-zinc-400" />
            <span>Đóng cửa</span>
          </div>
        </div>
      </div>

      {/* Calendar Grid */}
      <div className="bg-white rounded-2xl border border-[#E5DED4] shadow-xs overflow-hidden">
        {/* Weekday Headers */}
        <div className="grid grid-cols-7 border-b border-[#E5DED4] bg-[#F7F3EC]/70 text-center text-xs font-semibold text-[#786C60] py-2.5">
          <span>Thứ 2</span>
          <span>Thứ 3</span>
          <span>Thứ 4</span>
          <span>Thứ 5</span>
          <span>Thứ 6</span>
          <span className="text-[#B96849]">Thứ 7</span>
          <span className="text-[#B7423E]">Chủ Nhật</span>
        </div>

        {/* Days Grid */}
        <div className="grid grid-cols-7 auto-rows-fr divide-x divide-y divide-[#E5DED4] bg-[#F7F3EC]/20 min-h-[520px]">
          {/* Empty cells before month start */}
          {Array.from({ length: firstDayOfWeek }).map((_, idx) => (
            <div key={`empty-${idx}`} className="bg-zinc-50/50 p-2 min-h-[95px] select-none opacity-40" />
          ))}

          {/* Month Day Cells */}
          {calendarDays.map((cell) => {
            const hasReport = !!cell.reportRecord
            const status = cell.reportRecord?.status
            const isConfirmed = status === 'CONFIRMED'

            return (
              <div
                key={cell.dateStr}
                onClick={() => handleDayClick(cell)}
                className={`relative group p-2 min-h-[95px] transition-all cursor-pointer flex flex-col justify-between ${
                  cell.isToday
                    ? 'ring-2 ring-inset ring-[#B96849] bg-orange-50/40'
                    : 'hover:bg-white/90 bg-white'
                } ${
                  cell.isClosed
                    ? 'bg-zinc-100/70 hover:bg-zinc-100'
                    : cell.isMissing
                    ? 'bg-red-50/50 hover:bg-red-50'
                    : ''
                }`}
              >
                {/* Top cell row: Day number + Quick Status Button */}
                <div className="flex items-center justify-between">
                  <span
                    className={`text-xs font-bold w-6 h-6 rounded-full flex items-center justify-center ${
                      cell.isToday
                        ? 'bg-[#B96849] text-white shadow-xs'
                        : cell.isMissing
                        ? 'text-red-700 bg-red-100'
                        : 'text-[#352D26]'
                    }`}
                  >
                    {cell.day}
                  </span>

                  <button
                    type="button"
                    onClick={(e) => handleOpenStatusModal(e, cell)}
                    className="opacity-0 group-hover:opacity-100 p-1 text-[#786C60] hover:text-[#352D26] hover:bg-zinc-200/60 rounded-md transition-opacity"
                    title="Đổi trạng thái ngày (Mở/Đóng cửa)"
                  >
                    <Settings2 className="w-3.5 h-3.5" />
                  </button>
                </div>

                {/* Middle cell row: Content Status */}
                <div className="my-1">
                  {cell.isClosed ? (
                    <div className="text-center py-1">
                      <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-zinc-700 bg-zinc-200/80 px-2 py-0.5 rounded-md">
                        <DoorClosed className="w-3 h-3 text-zinc-600" /> NGHỈ
                      </span>
                      {cell.closeReason && (
                        <p className="text-[10px] text-zinc-500 truncate mt-0.5 max-w-[90px] mx-auto" title={cell.closeReason}>
                          {cell.closeReason}
                        </p>
                      )}
                    </div>
                  ) : hasReport ? (
                    <div className="space-y-0.5">
                      <div className="flex items-center justify-between">
                        <span
                          className={`text-[9px] font-bold px-1.5 py-0.2 rounded-sm ${
                            isConfirmed
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-amber-100 text-amber-800'
                          }`}
                        >
                          {isConfirmed ? 'DUYỆT' : 'CHỜ'}
                        </span>
                        <span className="text-[9px] font-mono text-[#786C60]">
                          r{cell.reportRecord?.revision}
                        </span>
                      </div>
                      <p className="text-[11px] font-bold text-[#304638] tracking-tight truncate">
                        {formatVnd(cell.reportRecord?.totalRevenue)}
                      </p>
                    </div>
                  ) : cell.isMissing ? (
                    <div className="text-center py-1">
                      <span className="inline-flex items-center gap-0.5 text-[10px] font-bold text-red-700 bg-red-100 px-1.5 py-0.5 rounded-md">
                        <AlertTriangle className="w-3 h-3 text-red-600" /> THIẾU
                      </span>
                    </div>
                  ) : cell.isFuture ? (
                    <div className="text-center py-1 text-[10px] text-zinc-400">
                      Chưa tới
                    </div>
                  ) : (
                    <div className="text-center py-1">
                      <span className="text-[10px] text-[#786C60] hover:text-[#B96849] font-medium flex items-center justify-center gap-1">
                        <Plus className="w-3 h-3" /> Nhập
                      </span>
                    </div>
                  )}
                </div>

                {/* Bottom cell indicator */}
                <div className="text-[9px] text-[#786C60] flex items-center justify-between">
                  {cell.reportRecord && cell.reportRecord.cashAmount > 0 && (
                    <span className="text-emerald-700 font-medium">Tiền mặt</span>
                  )}
                  {cell.reportRecord && cell.reportRecord.bankTransferAmount > 0 && (
                    <span className="text-[#B96849] font-medium">QR</span>
                  )}
                  {cell.reportRecord && cell.reportRecord.deliveryAppNetAmount > 0 && (
                    <span className="text-blue-600 font-medium">App</span>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      </div>

      {/* Modals */}
      <CreateDailyRevenueModal
        isOpen={createModalOpen}
        branchId={branchId}
        branchName={branchName}
        initialDate={targetDateForCreate}
        onClose={() => setCreateModalOpen(false)}
        onSuccess={() => {
          loadCalendarData()
        }}
      />

      <DailyRevenueDetailModal
        isOpen={detailModalOpen}
        report={selectedReport}
        branchId={branchId}
        branchName={branchName}
        onClose={() => setDetailModalOpen(false)}
        onConfirmed={() => {
          loadCalendarData()
        }}
      />

      <BusinessDayStatusModal
        isOpen={statusModalOpen}
        branchId={branchId}
        branchName={branchName}
        businessDate={targetDateForStatus}
        currentStatus={targetStatus}
        currentCloseReason={targetCloseReason}
        onClose={() => setStatusModalOpen(false)}
        onSuccess={() => {
          loadCalendarData()
        }}
      />
    </div>
  )
}
