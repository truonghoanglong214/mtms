import React, { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'motion/react'
import {
  X,
  Users,
  Calendar,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  CheckSquare,
  Square,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import { timesheetApi, convertHoursToMinutes } from '../api/timesheetApi'
import type {
  BulkTimesheetItem,
  BulkTimesheetPayload,
} from '../types/timesheet.types'

interface SimpleEmployeeOption {
  id: string
  staffCode: string
  fullName: string
  positionName: string
}

interface BulkRowState {
  employeeId: string
  staffCode: string
  fullName: string
  positionName: string
  isSelected: boolean
  hours: number
  workdays: number
  note: string
}

interface BulkTimesheetModalProps {
  isOpen: boolean
  branchId: string
  employees: SimpleEmployeeOption[]
  defaultDate?: string
  onClose: () => void
  onSuccess: () => void
}

export const BulkTimesheetModal: React.FC<BulkTimesheetModalProps> = ({
  isOpen,
  branchId,
  employees,
  defaultDate,
  onClose,
  onSuccess,
}) => {
  const [workDate, setWorkDate] = useState<string>(
    defaultDate || new Date().toISOString().split('T')[0]
  )
  const [rows, setRows] = useState<BulkRowState[]>([])
  const [commonNote, setCommonNote] = useState<string>('Ca làm việc theo lịch')
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Initialize rows when modal opens or employees change
  useEffect(() => {
    if (isOpen) {
      if (defaultDate) {
        setWorkDate(defaultDate)
      }
      const initialRows: BulkRowState[] = employees.map((emp) => ({
        employeeId: emp.id,
        staffCode: emp.staffCode,
        fullName: emp.fullName,
        positionName: emp.positionName,
        isSelected: true,
        hours: 8,
        workdays: 1.0,
        note: 'Ca làm việc chính',
      }))
      setRows(initialRows)
      setErrorMessage(null)
    }
  }, [isOpen, employees, defaultDate])

  const handleSelectAll = (select: boolean) => {
    setRows((prev) => prev.map((r) => ({ ...r, isSelected: select })))
  }

  const handleApplyPresetAll = (hours: number, workdays: number) => {
    setRows((prev) =>
      prev.map((r) => (r.isSelected ? { ...r, hours, workdays } : r))
    )
  }

  const handleApplyCommonNote = () => {
    setRows((prev) =>
      prev.map((r) => (r.isSelected ? { ...r, note: commonNote } : r))
    )
  }

  const updateRow = (
    employeeId: string,
    field: 'hours' | 'workdays' | 'note' | 'isSelected',
    val: unknown
  ) => {
    setRows((prev) =>
      prev.map((r) => {
        if (r.employeeId !== employeeId) return r
        if (field === 'hours') {
          const num = typeof val === 'number' ? val : parseFloat(val as string) || 0
          const calcWorkdays = num > 0 ? Number((num / 8).toFixed(2)) : 0
          return { ...r, hours: num, workdays: calcWorkdays }
        }
        return { ...r, [field]: val }
      })
    )
  }

  const selectedRows = rows.filter((r) => r.isSelected)
  const totalSelectedHours = selectedRows.reduce((sum, r) => sum + r.hours, 0)
  const totalSelectedWorkdays = selectedRows.reduce(
    (sum, r) => sum + r.workdays,
    0
  )

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)

    if (!workDate) {
      setErrorMessage('Vui lòng chọn ngày làm việc.')
      return
    }

    if (selectedRows.length === 0) {
      setErrorMessage('Vui lòng chọn ít nhất 1 nhân viên để chấm công.')
      return
    }

    const invalidRow = selectedRows.find(
      (r) => r.hours <= 0 || isNaN(r.hours) || r.workdays <= 0 || isNaN(r.workdays)
    )
    if (invalidRow) {
      setErrorMessage(
        `Nhân viên ${invalidRow.fullName} có số giờ hoặc ngày công không hợp lệ (> 0).`
      )
      return
    }

    const entries: BulkTimesheetItem[] = selectedRows.map((r) => ({
      employeeId: r.employeeId,
      approvedMinutes: convertHoursToMinutes(r.hours),
      payableWorkdays: r.workdays,
      status: 'SUBMITTED',
      note: r.note.trim() || undefined,
    }))

    const payload: BulkTimesheetPayload = {
      workDate,
      entries,
    }

    setIsSubmitting(true)
    try {
      await timesheetApi.bulkCreateTimesheets(branchId, payload)
      onSuccess()
      onClose()
    } catch (err: unknown) {
      console.error('Error saving bulk timesheets:', err)
      const errorObj = err as {
        response?: { data?: { detail?: string; message?: string } }
      }
      setErrorMessage(
        errorObj.response?.data?.detail ||
          errorObj.response?.data?.message ||
          'Không thể lưu chấm công hàng loạt. Vui lòng thử lại.'
      )
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
          className="relative w-full max-w-4xl bg-white rounded-2xl shadow-2xl border border-stone-200 overflow-hidden flex flex-col my-6 max-h-[90vh]"
        >
          {/* Header */}
          <div className="bg-[#304638] p-5 border-b border-[#25372C] flex items-center justify-between text-white shrink-0">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-white/10 backdrop-blur-sm">
                <Users className="h-6 w-6 text-emerald-300" />
              </div>
              <div>
                <h2 className="text-lg font-bold tracking-tight">
                  Chấm công Nhanh theo Ca (Hàng loạt)
                </h2>
                <p className="text-xs text-white/80">
                  Ghi nhận bảng công cho nhiều nhân viên trong cùng một ngày
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

          <form onSubmit={handleSubmit} className="flex flex-col flex-1 overflow-hidden">
            {/* Top Toolbar */}
            <div className="p-4 bg-stone-50 border-b border-stone-200 space-y-3 shrink-0">
              <div className="flex flex-wrap items-center justify-between gap-3">
                {/* Date Picker */}
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-stone-700 flex items-center gap-1.5">
                    <Calendar className="h-4 w-4 text-stone-500" />
                    Ngày làm việc:
                  </span>
                  <input
                    type="date"
                    value={workDate}
                    onChange={(e) => setWorkDate(e.target.value)}
                    className="text-xs p-2 rounded-lg border border-stone-300 bg-white focus:outline-none focus:ring-2 focus:ring-[#304638] font-medium"
                  />
                  <button
                    type="button"
                    onClick={() =>
                      setWorkDate(new Date().toISOString().split('T')[0])
                    }
                    className="text-[11px] px-2 py-1 rounded-md bg-stone-200 hover:bg-stone-300 text-stone-700 font-medium transition-colors"
                  >
                    Hôm nay
                  </button>
                </div>

                {/* Batch Action Presets */}
                <div className="flex items-center gap-1.5 text-xs">
                  <span className="text-stone-500 text-[11px] font-medium mr-1">
                    Điền nhanh:
                  </span>
                  <button
                    type="button"
                    onClick={() => handleApplyPresetAll(8, 1.0)}
                    className="px-2.5 py-1 rounded-lg bg-emerald-50 border border-emerald-200 hover:bg-emerald-100 text-emerald-800 text-[11px] font-medium transition-colors"
                  >
                    Ca 8h (1 công)
                  </button>
                  <button
                    type="button"
                    onClick={() => handleApplyPresetAll(4, 0.5)}
                    className="px-2.5 py-1 rounded-lg bg-blue-50 border border-blue-200 hover:bg-blue-100 text-blue-800 text-[11px] font-medium transition-colors"
                  >
                    Ca 4h (0.5 công)
                  </button>
                  <button
                    type="button"
                    onClick={() => handleApplyPresetAll(6, 0.75)}
                    className="px-2.5 py-1 rounded-lg bg-amber-50 border border-amber-200 hover:bg-amber-100 text-amber-800 text-[11px] font-medium transition-colors"
                  >
                    Ca 6h (0.75 công)
                  </button>
                </div>
              </div>

              {/* Note bar & Selection controls */}
              <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-stone-200 text-xs">
                <div className="flex items-center gap-2 flex-1 max-w-md">
                  <input
                    type="text"
                    value={commonNote}
                    onChange={(e) => setCommonNote(e.target.value)}
                    placeholder="Ghi chú chung cho ca..."
                    className="flex-1 text-xs px-2.5 py-1.5 rounded-lg border border-stone-300 bg-white focus:outline-none focus:ring-2 focus:ring-[#304638]"
                  />
                  <button
                    type="button"
                    onClick={handleApplyCommonNote}
                    className="px-2.5 py-1.5 rounded-lg bg-stone-200 hover:bg-stone-300 text-stone-800 text-[11px] font-medium whitespace-nowrap transition-colors"
                  >
                    Áp dụng ghi chú
                  </button>
                </div>

                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => handleSelectAll(true)}
                    className="text-[11px] text-[#304638] hover:underline font-semibold"
                  >
                    Chọn tất cả
                  </button>
                  <span className="text-stone-300">|</span>
                  <button
                    type="button"
                    onClick={() => handleSelectAll(false)}
                    className="text-[11px] text-stone-500 hover:underline"
                  >
                    Bỏ chọn tất cả
                  </button>
                  <Badge variant="outline" className="border-stone-300 ml-1">
                    Đã chọn {selectedRows.length}/{rows.length} người
                  </Badge>
                </div>
              </div>
            </div>

            {/* Error Message */}
            {errorMessage && (
              <div className="mx-4 mt-3 p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex gap-2.5 items-start shrink-0">
                <AlertTriangle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
                <span>{errorMessage}</span>
              </div>
            )}

            {/* Table Area */}
            <div className="flex-1 overflow-y-auto p-4">
              <table className="w-full text-left text-xs border-collapse">
                <thead className="sticky top-0 bg-stone-100 border-b border-stone-200 text-stone-600 uppercase text-[10px] tracking-wider z-10">
                  <tr>
                    <th className="p-2.5 w-10 text-center">
                      <span className="sr-only">Chọn</span>
                    </th>
                    <th className="p-2.5 font-semibold">Nhân viên</th>
                    <th className="p-2.5 font-semibold w-28">Vị trí</th>
                    <th className="p-2.5 font-semibold w-28">Số giờ làm</th>
                    <th className="p-2.5 font-semibold w-28">Công quy đổi</th>
                    <th className="p-2.5 font-semibold">Ghi chú</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-stone-200">
                  {rows.map((row) => (
                    <tr
                      key={row.employeeId}
                      className={`hover:bg-stone-50 transition-colors ${
                        row.isSelected ? 'bg-white' : 'bg-stone-50/60 opacity-60'
                      }`}
                    >
                      <td className="p-2.5 text-center">
                        <button
                          type="button"
                          onClick={() =>
                            updateRow(
                              row.employeeId,
                              'isSelected',
                              !row.isSelected
                            )
                          }
                          className="text-stone-500 hover:text-[#304638]"
                        >
                          {row.isSelected ? (
                            <CheckSquare className="h-4 w-4 text-[#304638]" />
                          ) : (
                            <Square className="h-4 w-4" />
                          )}
                        </button>
                      </td>
                      <td className="p-2.5">
                        <div className="font-semibold text-stone-800">
                          {row.fullName}
                        </div>
                        <div className="text-[10px] font-mono text-stone-500">
                          {row.staffCode}
                        </div>
                      </td>
                      <td className="p-2.5 text-stone-600">
                        <Badge
                          variant="secondary"
                          className="text-[10px] bg-stone-100 text-stone-700"
                        >
                          {row.positionName}
                        </Badge>
                      </td>
                      <td className="p-2.5">
                        <div className="flex items-center gap-1.5">
                          <input
                            type="number"
                            step="0.5"
                            min="0"
                            max="24"
                            value={row.hours}
                            onChange={(e) =>
                              updateRow(
                                row.employeeId,
                                'hours',
                                e.target.value
                              )
                            }
                            disabled={!row.isSelected}
                            className="w-16 text-xs p-1.5 rounded-lg border border-stone-300 focus:outline-none focus:ring-1 focus:ring-[#304638] text-right font-medium"
                          />
                          <span className="text-[11px] text-stone-500">h</span>
                        </div>
                      </td>
                      <td className="p-2.5">
                        <div className="flex items-center gap-1.5">
                          <input
                            type="number"
                            step="0.05"
                            min="0"
                            max="3"
                            value={row.workdays}
                            onChange={(e) =>
                              updateRow(
                                row.employeeId,
                                'workdays',
                                parseFloat(e.target.value) || 0
                              )
                            }
                            disabled={!row.isSelected}
                            className="w-16 text-xs p-1.5 rounded-lg border border-stone-300 focus:outline-none focus:ring-1 focus:ring-[#304638] text-right font-bold text-emerald-800"
                          />
                          <span className="text-[10px] text-stone-500">công</span>
                        </div>
                      </td>
                      <td className="p-2.5">
                        <input
                          type="text"
                          value={row.note}
                          onChange={(e) =>
                            updateRow(row.employeeId, 'note', e.target.value)
                          }
                          disabled={!row.isSelected}
                          placeholder="Ghi chú ca..."
                          className="w-full text-xs p-1.5 rounded-lg border border-stone-300 focus:outline-none focus:ring-1 focus:ring-[#304638]"
                        />
                      </td>
                    </tr>
                  ))}
                  {rows.length === 0 && (
                    <tr>
                      <td
                        colSpan={6}
                        className="p-8 text-center text-stone-500 italic"
                      >
                        Không có nhân viên nào tại chi nhánh này để chấm công.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            {/* Footer Summary & Buttons */}
            <div className="p-4 bg-stone-50 border-t border-stone-200 flex flex-wrap items-center justify-between gap-3 shrink-0">
              <div className="flex items-center gap-4 text-xs">
                <div>
                  <span className="text-stone-500">Số người: </span>
                  <strong className="text-stone-800 font-bold">
                    {selectedRows.length}
                  </strong>
                </div>
                <div>
                  <span className="text-stone-500">Tổng giờ làm: </span>
                  <strong className="text-[#304638] font-bold">
                    {totalSelectedHours.toFixed(1)}h
                  </strong>
                </div>
                <div>
                  <span className="text-stone-500">Tổng công quy đổi: </span>
                  <strong className="text-emerald-700 font-bold">
                    {totalSelectedWorkdays.toFixed(2)} công
                  </strong>
                </div>
              </div>

              <div className="flex items-center gap-3">
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
                  disabled={isSubmitting || selectedRows.length === 0}
                  className="bg-[#304638] hover:bg-[#25372C] text-white text-xs gap-1.5"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin" />
                      Đang lưu đồng loạt...
                    </>
                  ) : (
                    <>
                      <CheckCircle2 className="h-4 w-4" />
                      Lưu chấm công ({selectedRows.length} nhân viên)
                    </>
                  )}
                </Button>
              </div>
            </div>
          </form>
        </motion.div>
      </div>
    </AnimatePresence>
  )
}
