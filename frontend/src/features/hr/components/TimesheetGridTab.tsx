import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  ChevronLeft,
  ChevronRight,
  Calendar,
  RefreshCw,
  Search,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import {
  timesheetApi,
  formatMinutesToHours,
  convertMinutesToHours,
} from '../api/timesheetApi'
import type {
  TimesheetEntry,
  MonthlyTimesheetSummary,
} from '../types/timesheet.types'
import { CreateTimesheetModal } from './CreateTimesheetModal'

interface SimpleEmployeeOption {
  id: string
  staffCode: string
  fullName: string
  positionName: string
}

interface TimesheetGridTabProps {
  branchId: string
  employees: SimpleEmployeeOption[]
  onRefreshKPIs?: () => void
}

function getDaysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate()
}

function getDayInfo(year: number, month: number, day: number) {
  const date = new Date(year, month - 1, day)
  const dayIndex = date.getDay()
  const labels = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']
  return {
    dayOfWeek: labels[dayIndex],
    isWeekend: dayIndex === 0 || dayIndex === 6,
    isSunday: dayIndex === 0,
  }
}

function formatIsoDate(year: number, month: number, day: number): string {
  return `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`
}

export const TimesheetGridTab: React.FC<TimesheetGridTabProps> = ({
  branchId,
  employees,
  onRefreshKPIs,
}) => {
  const currentDate = new Date()
  const [selectedYear, setSelectedYear] = useState<number>(currentDate.getFullYear())
  const [selectedMonth, setSelectedMonth] = useState<number>(currentDate.getMonth() + 1)
  const [searchQuery, setSearchQuery] = useState<string>('')

  // Data
  const [monthlySummaries, setMonthlySummaries] = useState<MonthlyTimesheetSummary[]>([])
  const [entries, setEntries] = useState<TimesheetEntry[]>([])
  const [isLoading, setIsLoading] = useState<boolean>(false)

  // Cell Click -> Edit or Create
  const [activeCellEntry, setActiveCellEntry] = useState<TimesheetEntry | null>(null)
  const [activeCellDate, setActiveCellDate] = useState<string | undefined>(undefined)
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false)

  const daysCount = useMemo(
    () => getDaysInMonth(selectedYear, selectedMonth),
    [selectedYear, selectedMonth]
  )

  const daysArray = useMemo(() => {
    return Array.from({ length: daysCount }, (_, i) => i + 1)
  }, [daysCount])

  // Fetch monthly data
  const fetchData = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    try {
      const startDate = formatIsoDate(selectedYear, selectedMonth, 1)
      const endDate = formatIsoDate(selectedYear, selectedMonth, daysCount)

      const [summaryRes, entriesRes] = await Promise.all([
        timesheetApi.getMonthlySummary(branchId, selectedYear, selectedMonth),
        timesheetApi.getTimesheets(branchId, {
          startDate,
          endDate,
          size: 1000,
        }),
      ])

      setMonthlySummaries(summaryRes || [])
      setEntries(entriesRes.content || [])
    } catch (err) {
      console.error('Failed to load grid data:', err)
      setMonthlySummaries([])
      setEntries([])
    } finally {
      setIsLoading(false)
    }
  }, [branchId, selectedYear, selectedMonth, daysCount])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  // Month navigation
  const handlePrevMonth = () => {
    if (selectedMonth === 1) {
      setSelectedMonth(12)
      setSelectedYear((y) => y - 1)
    } else {
      setSelectedMonth((m) => m - 1)
    }
  }

  const handleNextMonth = () => {
    if (selectedMonth === 12) {
      setSelectedMonth(1)
      setSelectedYear((y) => y + 1)
    } else {
      setSelectedMonth((m) => m + 1)
    }
  }

  // Create entry mapping: key `${employeeId}_${workDate}` -> TimesheetEntry
  const entryMap = useMemo(() => {
    const map = new Map<string, TimesheetEntry>()
    for (const e of entries) {
      map.set(`${e.employeeId}_${e.workDate}`, e)
    }
    return map
  }, [entries])

  // Create summary mapping: key `employeeId` -> MonthlyTimesheetSummary
  const summaryMap = useMemo(() => {
    const map = new Map<string, MonthlyTimesheetSummary>()
    for (const s of monthlySummaries) {
      map.set(s.employeeId, s)
    }
    return map
  }, [monthlySummaries])

  // Filtered employees
  const filteredEmployees = useMemo(() => {
    if (!searchQuery.trim()) return employees
    const q = searchQuery.toLowerCase()
    return employees.filter(
      (e) =>
        e.fullName.toLowerCase().includes(q) ||
        e.staffCode.toLowerCase().includes(q) ||
        e.positionName.toLowerCase().includes(q)
    )
  }, [employees, searchQuery])

  // Open modal on cell click
  const handleCellClick = (employeeId: string, day: number) => {
    const dateStr = formatIsoDate(selectedYear, selectedMonth, day)
    const existing = entryMap.get(`${employeeId}_${dateStr}`)
    if (existing) {
      setActiveCellEntry(existing)
      setActiveCellDate(undefined)
    } else {
      setActiveCellEntry(null)
      setActiveCellDate(dateStr)
    }
    setIsModalOpen(true)
  }

  const handleSuccess = () => {
    fetchData()
    if (onRefreshKPIs) onRefreshKPIs()
  }

  return (
    <div className="space-y-4">
      {/* Month Navigation & Controls */}
      <div className="p-4 bg-white rounded-2xl border border-stone-200 shadow-sm flex flex-wrap items-center justify-between gap-3">
        {/* Month Selector */}
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={handlePrevMonth}
            className="h-8 w-8 p-0 rounded-xl border-stone-200"
            aria-label="Tháng trước"
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>

          <div className="flex items-center gap-2 px-3 py-1 bg-stone-50 rounded-xl border border-stone-200 font-bold text-stone-800 text-sm">
            <Calendar className="h-4 w-4 text-[#304638]" />
            <span>
              Tháng {selectedMonth.toString().padStart(2, '0')} / {selectedYear}
            </span>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={handleNextMonth}
            className="h-8 w-8 p-0 rounded-xl border-stone-200"
            aria-label="Tháng sau"
          >
            <ChevronRight className="h-4 w-4" />
          </Button>

          <button
            type="button"
            onClick={() => {
              const now = new Date()
              setSelectedYear(now.getFullYear())
              setSelectedMonth(now.getMonth() + 1)
            }}
            className="text-[11px] font-semibold text-[#304638] hover:underline ml-1"
          >
            Tháng này
          </button>
        </div>

        {/* Legend */}
        <div className="flex flex-wrap items-center gap-3 text-[11px]">
          <span className="text-stone-400 font-medium">Chú giải màu:</span>
          <div className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded bg-emerald-500" />
            <span className="text-stone-700">Đã duyệt (Approved)</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded bg-amber-500" />
            <span className="text-stone-700">Chờ duyệt (Submitted)</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded bg-stone-300" />
            <span className="text-stone-700">Nháp (Draft)</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded bg-rose-500" />
            <span className="text-stone-700">Từ chối (Rejected)</span>
          </div>
        </div>

        {/* Search & Refresh */}
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="h-3.5 w-3.5 text-stone-400 absolute left-2.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo tên/mã..."
              className="pl-8 pr-3 py-1.5 text-xs rounded-xl border border-stone-200 bg-stone-50 focus:outline-none focus:ring-1 focus:ring-[#304638] w-40"
            />
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => fetchData()}
            disabled={isLoading}
            className="h-8 text-xs gap-1 border-stone-200"
          >
            <RefreshCw
              className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`}
            />
            Làm mới
          </Button>
        </div>
      </div>

      {/* Grid Table Container */}
      <div className="bg-white rounded-2xl border border-stone-200 shadow-sm overflow-hidden flex flex-col">
        <div className="overflow-x-auto max-h-[680px]">
          <table className="w-full text-center text-xs border-collapse min-w-max">
            {/* Header */}
            <thead className="sticky top-0 bg-stone-100 border-b border-stone-300 z-20 text-stone-700">
              <tr>
                {/* Fixed Left Header */}
                <th className="sticky left-0 bg-stone-100 p-2.5 text-left font-bold w-48 z-30 shadow-[1px_0_0_0_#d6d3d1]">
                  Nhân viên ({filteredEmployees.length})
                </th>
                <th className="sticky left-48 bg-stone-100 p-2.5 text-left font-bold w-24 z-30 shadow-[1px_0_0_0_#d6d3d1]">
                  Chức danh
                </th>

                {/* Days of Month */}
                {daysArray.map((day) => {
                  const info = getDayInfo(selectedYear, selectedMonth, day)
                  return (
                    <th
                      key={day}
                      className={`p-1.5 font-semibold border-r border-stone-200 min-w-[34px] ${
                        info.isWeekend ? 'bg-amber-50/60' : ''
                      }`}
                    >
                      <div className="text-[11px] font-bold text-stone-800">
                        {day}
                      </div>
                      <div
                        className={`text-[9px] uppercase ${
                          info.isSunday
                            ? 'text-rose-600 font-bold'
                            : info.isWeekend
                            ? 'text-amber-700 font-medium'
                            : 'text-stone-400'
                        }`}
                      >
                        {info.dayOfWeek}
                      </div>
                    </th>
                  )
                })}

                {/* Summary Headers */}
                <th className="p-2.5 font-bold bg-emerald-50 text-emerald-900 border-l border-stone-300 min-w-[70px]">
                  Tổng giờ
                </th>
                <th className="p-2.5 font-bold bg-emerald-50 text-emerald-900 min-w-[70px]">
                  Tổng công
                </th>
                <th className="p-2.5 font-bold bg-stone-100 text-stone-700 min-w-[60px]">
                  Số ca
                </th>
              </tr>
            </thead>

            {/* Body */}
            <tbody className="divide-y divide-stone-200">
              {isLoading ? (
                <tr>
                  <td
                    colSpan={daysCount + 5}
                    className="p-16 text-center text-stone-500"
                  >
                    <RefreshCw className="h-6 w-6 animate-spin mx-auto mb-2 text-[#304638]" />
                    Đang nạp dữ liệu ma trận chấm công tháng {selectedMonth}/{selectedYear}...
                  </td>
                </tr>
              ) : filteredEmployees.length === 0 ? (
                <tr>
                  <td
                    colSpan={daysCount + 5}
                    className="p-16 text-center text-stone-500 italic"
                  >
                    Không có nhân viên nào phù hợp.
                  </td>
                </tr>
              ) : (
                filteredEmployees.map((emp) => {
                  const summary = summaryMap.get(emp.id)

                  return (
                    <tr
                      key={emp.id}
                      className="hover:bg-stone-50/70 transition-colors"
                    >
                      {/* Sticky Employee Name */}
                      <td className="sticky left-0 bg-white p-2 text-left font-semibold text-stone-800 z-10 shadow-[1px_0_0_0_#e7e5e4] truncate max-w-[192px]">
                        <div className="truncate" title={emp.fullName}>
                          {emp.fullName}
                        </div>
                        <div className="text-[10px] font-mono text-stone-400 font-normal">
                          {emp.staffCode}
                        </div>
                      </td>

                      {/* Sticky Position */}
                      <td className="sticky left-48 bg-white p-2 text-left text-[11px] text-stone-600 z-10 shadow-[1px_0_0_0_#e7e5e4] truncate max-w-[96px]">
                        <span className="truncate" title={emp.positionName}>
                          {emp.positionName}
                        </span>
                      </td>

                      {/* Day Cells */}
                      {daysArray.map((day) => {
                        const dateStr = formatIsoDate(
                          selectedYear,
                          selectedMonth,
                          day
                        )
                        const entry = entryMap.get(`${emp.id}_${dateStr}`)
                        const dayInfo = getDayInfo(
                          selectedYear,
                          selectedMonth,
                          day
                        )

                        let cellClass =
                          'hover:ring-1 hover:ring-[#304638] cursor-pointer transition-all '
                        let textContent = '-'
                        let hoursShort = ''

                        if (entry) {
                          const hrs = convertMinutesToHours(entry.approvedMinutes)
                          hoursShort = `${hrs}h`
                          if (entry.status === 'APPROVED') {
                            cellClass +=
                              'bg-emerald-100 text-emerald-900 font-bold'
                            textContent = hoursShort
                          } else if (entry.status === 'SUBMITTED') {
                            cellClass +=
                              'bg-amber-100 text-amber-900 font-bold'
                            textContent = hoursShort
                          } else if (entry.status === 'REJECTED') {
                            cellClass += 'bg-rose-100 text-rose-900 font-bold'
                            textContent = 'Từ chối'
                          } else {
                            // DRAFT
                            cellClass +=
                              'bg-stone-100 text-stone-700 font-medium'
                            textContent = hoursShort
                          }
                        } else {
                          cellClass += dayInfo.isWeekend
                            ? 'bg-amber-50/20 text-stone-300'
                            : 'text-stone-300'
                        }

                        return (
                          <td
                            key={day}
                            onClick={() => handleCellClick(emp.id, day)}
                            title={
                              entry
                                ? `${entry.employeeName} (${dateStr}): ${formatMinutesToHours(
                                    entry.approvedMinutes
                                  )} - ${entry.status} ${
                                    entry.note ? `[${entry.note}]` : ''
                                  }`
                                : `Chưa có công ngày ${dateStr}. Bấm để nhập công.`
                            }
                            className={`p-1 border-r border-stone-200 text-[10px] select-none ${cellClass}`}
                          >
                            <span className="block truncate">{textContent}</span>
                          </td>
                        )
                      })}

                      {/* Summary Columns */}
                      <td className="p-2 border-l border-stone-300 font-bold text-[#304638] bg-emerald-50/30">
                        {summary
                          ? `${summary.totalApprovedHours.toFixed(1)}h`
                          : '0.0h'}
                      </td>
                      <td className="p-2 font-bold text-emerald-700 bg-emerald-50/30">
                        {summary
                          ? `${summary.totalPayableWorkdays.toFixed(2)}`
                          : '0.00'}
                      </td>
                      <td className="p-2 font-medium text-stone-600">
                        {summary ? summary.approvedDaysCount : 0} ca
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Edit / Create Timesheet Modal */}
      <CreateTimesheetModal
        isOpen={isModalOpen}
        branchId={branchId}
        employees={employees}
        initialEntry={activeCellEntry}
        defaultDate={activeCellDate}
        onClose={() => {
          setIsModalOpen(false)
          setActiveCellEntry(null)
          setActiveCellDate(undefined)
        }}
        onSuccess={handleSuccess}
      />
    </div>
  )
}
