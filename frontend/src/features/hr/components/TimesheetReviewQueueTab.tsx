import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  CheckCircle2,
  XCircle,
  Clock,
  Calendar,
  RefreshCw,
  Edit2,
  CheckSquare,
  ShieldCheck,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { useAuthStore } from '@/features/auth/authStore'
import {
  timesheetApi,
  formatMinutesToHours,
  formatWorkdays,
} from '../api/timesheetApi'
import {
  TIMESHEET_STATUS_CONFIG,
  TIMESHEET_SOURCE_LABELS,
  type TimesheetEntry,
  type TimesheetStatus,
} from '../types/timesheet.types'
import {
  TimesheetApprovalModal,
  type ApprovalModalMode,
} from './TimesheetApprovalModal'
import { CreateTimesheetModal } from './CreateTimesheetModal'

interface SimpleEmployeeOption {
  id: string
  staffCode: string
  fullName: string
  positionName: string
}

interface TimesheetReviewQueueTabProps {
  branchId: string
  employees: SimpleEmployeeOption[]
  onRefreshKPIs?: () => void
}

export const TimesheetReviewQueueTab: React.FC<TimesheetReviewQueueTabProps> = ({
  branchId,
  employees,
  onRefreshKPIs,
}) => {
  const user = useAuthStore((state) => state.user)

  // Filters
  const [selectedStatus, setSelectedStatus] = useState<TimesheetStatus | 'ALL'>('SUBMITTED')
  const [selectedEmployeeId, setSelectedEmployeeId] = useState<string>('')
  const [startDate, setStartDate] = useState<string>(() => {
    const d = new Date()
    d.setDate(d.getDate() - 7) // past 7 days default
    return d.toISOString().split('T')[0]
  })
  const [endDate, setEndDate] = useState<string>(() => {
    return new Date().toISOString().split('T')[0]
  })

  // Data & Pagination
  const [entries, setEntries] = useState<TimesheetEntry[]>([])
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [page, setPage] = useState<number>(0)
  const [pageSize] = useState<number>(50)
  const [totalElements, setTotalElements] = useState<number>(0)
  const [totalPages, setTotalPages] = useState<number>(1)

  // Selection for bulk action
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())

  // Modals state
  const [approvalModalState, setApprovalModalState] = useState<{
    isOpen: boolean
    mode: ApprovalModalMode
    entries: TimesheetEntry[]
  }>({
    isOpen: false,
    mode: 'APPROVE',
    entries: [],
  })

  const [editModalEntry, setEditModalEntry] = useState<TimesheetEntry | null>(null)
  const [isEditModalOpen, setIsEditModalOpen] = useState<boolean>(false)

  // Roles & Security check
  const isOwnerOrAdmin =
    user?.grants?.some(
      (g) => g.roleKey === 'BRANCH_OWNER' || g.roleKey === 'SYSTEM_ADMIN'
    ) || false

  const isManagerOnly =
    !isOwnerOrAdmin &&
    (user?.grants?.some((g) => g.roleKey === 'MANAGER') ?? false)

  // Fetch entries
  const fetchEntries = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    try {
      const res = await timesheetApi.getTimesheets(branchId, {
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        employeeId: selectedEmployeeId || undefined,
        status: selectedStatus,
        page,
        size: pageSize,
      })
      setEntries(res.content || [])
      setTotalElements(res.totalElements || 0)
      setTotalPages(res.totalPages || 1)
      setSelectedIds(new Set())
    } catch (err) {
      console.error('Failed to load timesheets:', err)
      setEntries([])
    } finally {
      setIsLoading(false)
    }
  }, [branchId, startDate, endDate, selectedEmployeeId, selectedStatus, page, pageSize])

  useEffect(() => {
    fetchEntries()
  }, [fetchEntries])

  // Bulk selection handlers
  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      // Select all SUBMITTED or DRAFT entries in current page
      const eligible = entries
        .filter((e) => e.status === 'SUBMITTED' || e.status === 'DRAFT')
        .map((e) => e.id)
      setSelectedIds(new Set(eligible))
    } else {
      setSelectedIds(new Set())
    }
  }

  const toggleSelectOne = (id: string) => {
    const next = new Set(selectedIds)
    if (next.has(id)) {
      next.delete(id)
    } else {
      next.add(id)
    }
    setSelectedIds(next)
  }

  const selectedEntries = useMemo(() => {
    return entries.filter((e) => selectedIds.has(e.id))
  }, [entries, selectedIds])

  // Single Approve / Reject trigger
  const handleOpenApproveSingle = (entry: TimesheetEntry) => {
    setApprovalModalState({
      isOpen: true,
      mode: 'APPROVE',
      entries: [entry],
    })
  }

  const handleOpenRejectSingle = (entry: TimesheetEntry) => {
    setApprovalModalState({
      isOpen: true,
      mode: 'REJECT',
      entries: [entry],
    })
  }

  const handleOpenBulkApprove = () => {
    if (selectedEntries.length === 0) return
    setApprovalModalState({
      isOpen: true,
      mode: 'BULK_APPROVE',
      entries: selectedEntries,
    })
  }

  const handleEditEntry = (entry: TimesheetEntry) => {
    setEditModalEntry(entry)
    setIsEditModalOpen(true)
  }

  const handleModalSuccess = () => {
    fetchEntries()
    if (onRefreshKPIs) onRefreshKPIs()
  }

  return (
    <div className="space-y-4">
      {/* Filters Bar */}
      <div className="p-4 bg-white rounded-2xl border border-stone-200 shadow-sm space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-3">
            {/* Status Tabs */}
            <div className="flex items-center bg-stone-100 p-1 rounded-xl text-xs">
              <button
                type="button"
                onClick={() => {
                  setSelectedStatus('SUBMITTED')
                  setPage(0)
                }}
                className={`px-3 py-1.5 rounded-lg font-medium transition-all ${
                  selectedStatus === 'SUBMITTED'
                    ? 'bg-white text-amber-900 font-bold shadow-sm'
                    : 'text-stone-600 hover:text-stone-900'
                }`}
              >
                Chờ duyệt
              </button>
              <button
                type="button"
                onClick={() => {
                  setSelectedStatus('APPROVED')
                  setPage(0)
                }}
                className={`px-3 py-1.5 rounded-lg font-medium transition-all ${
                  selectedStatus === 'APPROVED'
                    ? 'bg-white text-emerald-900 font-bold shadow-sm'
                    : 'text-stone-600 hover:text-stone-900'
                }`}
              >
                Đã duyệt
              </button>
              <button
                type="button"
                onClick={() => {
                  setSelectedStatus('DRAFT')
                  setPage(0)
                }}
                className={`px-3 py-1.5 rounded-lg font-medium transition-all ${
                  selectedStatus === 'DRAFT'
                    ? 'bg-white text-stone-900 font-bold shadow-sm'
                    : 'text-stone-600 hover:text-stone-900'
                }`}
              >
                Nháp
              </button>
              <button
                type="button"
                onClick={() => {
                  setSelectedStatus('REJECTED')
                  setPage(0)
                }}
                className={`px-3 py-1.5 rounded-lg font-medium transition-all ${
                  selectedStatus === 'REJECTED'
                    ? 'bg-white text-rose-900 font-bold shadow-sm'
                    : 'text-stone-600 hover:text-stone-900'
                }`}
              >
                Từ chối
              </button>
              <button
                type="button"
                onClick={() => {
                  setSelectedStatus('ALL')
                  setPage(0)
                }}
                className={`px-3 py-1.5 rounded-lg font-medium transition-all ${
                  selectedStatus === 'ALL'
                    ? 'bg-white text-stone-900 font-bold shadow-sm'
                    : 'text-stone-600 hover:text-stone-900'
                }`}
              >
                Tất cả
              </button>
            </div>

            {/* Employee Filter */}
            <select
              value={selectedEmployeeId}
              onChange={(e) => {
                setSelectedEmployeeId(e.target.value)
                setPage(0)
              }}
              className="text-xs p-2 rounded-xl border border-stone-200 bg-stone-50 font-medium text-stone-700 focus:outline-none focus:ring-2 focus:ring-[#304638]"
            >
              <option value="">Tất cả nhân viên</option>
              {employees.map((emp) => (
                <option key={emp.id} value={emp.id}>
                  {emp.fullName} ({emp.staffCode})
                </option>
              ))}
            </select>
          </div>

          {/* Date Range & Refresh */}
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1.5 text-xs text-stone-600 bg-stone-50 px-2.5 py-1.5 rounded-xl border border-stone-200">
              <Calendar className="h-3.5 w-3.5 text-stone-400" />
              <input
                type="date"
                value={startDate}
                onChange={(e) => {
                  setStartDate(e.target.value)
                  setPage(0)
                }}
                className="bg-transparent focus:outline-none text-xs"
              />
              <span className="text-stone-400">→</span>
              <input
                type="date"
                value={endDate}
                onChange={(e) => {
                  setEndDate(e.target.value)
                  setPage(0)
                }}
                className="bg-transparent focus:outline-none text-xs"
              />
            </div>

            <Button
              variant="outline"
              size="sm"
              onClick={() => fetchEntries()}
              disabled={isLoading}
              className="h-8 text-xs gap-1 border-stone-200 text-stone-600"
            >
              <RefreshCw
                className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`}
              />
              Làm mới
            </Button>
          </div>
        </div>

        {/* Bulk Action Bar (when records are selected) */}
        {selectedIds.size > 0 && (
          <div className="p-3 bg-amber-50/80 rounded-xl border border-amber-200 flex flex-wrap items-center justify-between gap-3 text-xs">
            <div className="flex items-center gap-2 text-amber-900 font-medium">
              <CheckSquare className="h-4 w-4 text-amber-700" />
              <span>
                Đang chọn <strong>{selectedIds.size}</strong> dòng chấm công
                chờ duyệt
              </span>
            </div>
            <div className="flex items-center gap-2">
              <Button
                size="sm"
                variant="outline"
                onClick={() => setSelectedIds(new Set())}
                className="h-7 text-xs border-amber-300 text-amber-800 hover:bg-amber-100"
              >
                Bỏ chọn
              </Button>
              <Button
                size="sm"
                onClick={handleOpenBulkApprove}
                className="h-7 text-xs bg-[#304638] hover:bg-[#25372C] text-white gap-1"
              >
                <CheckCircle2 className="h-3.5 w-3.5" />
                Duyệt hàng loạt ({selectedIds.size})
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Main Review Table */}
      <div className="bg-white rounded-2xl border border-stone-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead className="bg-stone-50 border-b border-stone-200 text-stone-600 uppercase text-[10px] tracking-wider">
              <tr>
                <th className="p-3 w-10 text-center">
                  <input
                    type="checkbox"
                    checked={
                      entries.length > 0 &&
                      entries
                        .filter(
                          (e) => e.status === 'SUBMITTED' || e.status === 'DRAFT'
                        )
                        .every((e) => selectedIds.has(e.id))
                    }
                    onChange={(e) => handleSelectAll(e.target.checked)}
                    className="rounded border-stone-300 text-[#304638] focus:ring-[#304638]"
                    aria-label="Chọn tất cả dòng"
                  />
                </th>
                <th className="p-3 font-semibold">Nhân viên</th>
                <th className="p-3 font-semibold">Ngày làm việc</th>
                <th className="p-3 font-semibold">Giờ & Công</th>
                <th className="p-3 font-semibold">Nguồn & Ghi chú</th>
                <th className="p-3 font-semibold">Trạng thái</th>
                <th className="p-3 font-semibold">Thông tin Duyệt</th>
                <th className="p-3 font-semibold text-right">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="p-12 text-center text-stone-500">
                    <RefreshCw className="h-6 w-6 animate-spin mx-auto mb-2 text-[#304638]" />
                    Đang tải danh sách bảng công...
                  </td>
                </tr>
              ) : entries.length === 0 ? (
                <tr>
                  <td colSpan={8} className="p-12 text-center text-stone-500">
                    <Clock className="h-8 w-8 mx-auto mb-2 text-stone-300" />
                    Không tìm thấy bản ghi chấm công nào phù hợp với bộ lọc.
                  </td>
                </tr>
              ) : (
                entries.map((entry) => {
                  const statusConf = TIMESHEET_STATUS_CONFIG[entry.status]
                  const isSelected = selectedIds.has(entry.id)
                  const isSelfRecord = Boolean(
                    isManagerOnly &&
                    ((user?.displayName &&
                      entry.employeeName.trim().toLowerCase() ===
                        user.displayName.trim().toLowerCase()) ||
                      (user?.id && entry.employeeId === user.id))
                  )

                  return (
                    <tr
                      key={entry.id}
                      className={`hover:bg-stone-50/80 transition-colors ${
                        isSelected ? 'bg-amber-50/40' : ''
                      }`}
                    >
                      {/* Checkbox */}
                      <td className="p-3 text-center">
                        {entry.status === 'SUBMITTED' || entry.status === 'DRAFT' ? (
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => toggleSelectOne(entry.id)}
                            className="rounded border-stone-300 text-[#304638] focus:ring-[#304638]"
                            aria-label={`Chọn bản ghi ${entry.employeeName}`}
                          />
                        ) : (
                          <span className="text-stone-300 text-[10px]">—</span>
                        )}
                      </td>

                      {/* Employee */}
                      <td className="p-3">
                        <div className="font-semibold text-stone-800">
                          {entry.employeeName}
                        </div>
                        <div className="text-[10px] font-mono text-stone-500 flex items-center gap-1.5 mt-0.5">
                          <span>{entry.staffCode}</span>
                          <span>·</span>
                          <span className="text-stone-600">
                            {entry.positionName}
                          </span>
                        </div>
                        {isSelfRecord && (
                          <span className="inline-block mt-1 text-[9px] font-bold px-1.5 py-0.5 rounded bg-rose-50 border border-rose-200 text-rose-700">
                            Bản ghi của bạn (Cấm tự duyệt)
                          </span>
                        )}
                      </td>

                      {/* Work Date */}
                      <td className="p-3 text-stone-700 whitespace-nowrap">
                        <span className="font-medium">{entry.workDate}</span>
                      </td>

                      {/* Hours & Workdays */}
                      <td className="p-3 whitespace-nowrap">
                        <div className="font-bold text-[#304638]">
                          {formatMinutesToHours(entry.approvedMinutes)}
                        </div>
                        <div className="text-[10px] text-stone-500 font-medium">
                          {formatWorkdays(entry.payableWorkdays)}
                        </div>
                      </td>

                      {/* Source & Note */}
                      <td className="p-3 text-stone-600 max-w-xs">
                        <div className="text-[10px] font-medium text-stone-500">
                          {TIMESHEET_SOURCE_LABELS[entry.sourceType] || entry.sourceType}
                        </div>
                        {entry.note ? (
                          <div className="text-stone-700 truncate" title={entry.note}>
                            {entry.note}
                          </div>
                        ) : (
                          <span className="text-stone-400 italic text-[11px]">
                            Không có ghi chú
                          </span>
                        )}
                      </td>

                      {/* Status */}
                      <td className="p-3 whitespace-nowrap">
                        <span
                          className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-semibold border ${statusConf.badgeClass}`}
                        >
                          <span
                            className={`h-1.5 w-1.5 rounded-full ${statusConf.dotClass}`}
                          />
                          {statusConf.label}
                        </span>
                      </td>

                      {/* Approval Audit Trail */}
                      <td className="p-3 text-[11px] text-stone-600">
                        {entry.status === 'APPROVED' ? (
                          <div>
                            <div className="font-medium text-stone-800 flex items-center gap-1">
                              <ShieldCheck className="h-3 w-3 text-emerald-600" />
                              {entry.approverName || 'Hệ thống / Quản lý'}
                            </div>
                            <div className="text-[10px] text-stone-400">
                              {entry.approvedAt
                                ? new Date(entry.approvedAt).toLocaleString('vi-VN')
                                : '—'}
                            </div>
                            {entry.approvalNote && (
                              <div
                                className="text-[10px] text-stone-500 italic mt-0.5 truncate max-w-[160px]"
                                title={entry.approvalNote}
                              >
                                {entry.approvalNote}
                              </div>
                            )}
                          </div>
                        ) : entry.status === 'REJECTED' ? (
                          <div className="text-rose-700">
                            <span className="font-medium">Đã từ chối</span>
                            {entry.approvalNote && (
                              <div
                                className="text-[10px] text-rose-600 italic mt-0.5 truncate max-w-[160px]"
                                title={entry.approvalNote}
                              >
                                Lý do: {entry.approvalNote}
                              </div>
                            )}
                          </div>
                        ) : (
                          <span className="text-stone-400 italic">Chưa duyệt</span>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="p-3 text-right whitespace-nowrap">
                        <div className="flex items-center justify-end gap-1.5">
                          {entry.status !== 'APPROVED' && (
                            <button
                              type="button"
                              onClick={() => handleEditEntry(entry)}
                              title="Chỉnh sửa giờ"
                              className="p-1.5 rounded-lg text-stone-500 hover:text-stone-800 hover:bg-stone-100 transition-colors"
                            >
                              <Edit2 className="h-3.5 w-3.5" />
                            </button>
                          )}

                          {entry.status !== 'APPROVED' && (
                            <>
                              <button
                                type="button"
                                onClick={() => handleOpenApproveSingle(entry)}
                                disabled={isSelfRecord}
                                title={
                                  isSelfRecord
                                    ? 'Cấm tự duyệt giờ làm của chính mình'
                                    : 'Phê duyệt giờ công'
                                }
                                className={`p-1.5 rounded-lg transition-colors ${
                                  isSelfRecord
                                    ? 'text-stone-300 cursor-not-allowed'
                                    : 'text-emerald-700 hover:bg-emerald-50 hover:text-emerald-800'
                                }`}
                              >
                                <CheckCircle2 className="h-4 w-4" />
                              </button>
                              <button
                                type="button"
                                onClick={() => handleOpenRejectSingle(entry)}
                                title="Từ chối chấm công"
                                className="p-1.5 rounded-lg text-rose-600 hover:bg-rose-50 hover:text-rose-700 transition-colors"
                              >
                                <XCircle className="h-4 w-4" />
                              </button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        {totalPages > 1 && (
          <div className="p-3 border-t border-stone-200 flex items-center justify-between text-xs text-stone-600">
            <div>
              Hiển thị {entries.length} / {totalElements} bản ghi
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="h-7 px-2 text-xs"
              >
                <ChevronLeft className="h-3.5 w-3.5 mr-1" />
                Trang trước
              </Button>
              <span className="font-semibold text-stone-800">
                {page + 1} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
                className="h-7 px-2 text-xs"
              >
                Trang sau
                <ChevronRight className="h-3.5 w-3.5 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Approval Modal */}
      <TimesheetApprovalModal
        isOpen={approvalModalState.isOpen}
        branchId={branchId}
        mode={approvalModalState.mode}
        entries={approvalModalState.entries}
        onClose={() =>
          setApprovalModalState((prev) => ({ ...prev, isOpen: false }))
        }
        onSuccess={handleModalSuccess}
      />

      {/* Edit Timesheet Modal */}
      <CreateTimesheetModal
        isOpen={isEditModalOpen}
        branchId={branchId}
        employees={employees}
        initialEntry={editModalEntry}
        onClose={() => {
          setIsEditModalOpen(false)
          setEditModalEntry(null)
        }}
        onSuccess={handleModalSuccess}
      />
    </div>
  )
}
