import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Clock,
  CalendarCheck,
  AlertTriangle,
  Building2,
  Users,
  Plus,
  RefreshCw,
  ShieldAlert,
  FileCheck2,
  XCircle,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { useAuthStore } from '@/features/auth/authStore'
import { apiClient } from '@/shared/api/apiClient'
import type { ApiResponse } from '@/shared/types'
import { hrApi } from '../api/hrApi'
import { timesheetApi } from '../api/timesheetApi'
import { TimesheetReviewQueueTab } from '../components/TimesheetReviewQueueTab'
import { TimesheetGridTab } from '../components/TimesheetGridTab'
import { CreateTimesheetModal } from '../components/CreateTimesheetModal'
import { BulkTimesheetModal } from '../components/BulkTimesheetModal'
import type { EmployeeSummary } from '../types/hr.types'
import type { MonthlyTimesheetSummary } from '../types/timesheet.types'

interface BranchSummary {
  id: string
  code?: string
  name: string
  status?: string
}

export const TimesheetManagementPage: React.FC = () => {
  const activeBranchId = useAuthStore((state) => state.activeBranchId)
  const setActiveBranch = useAuthStore((state) => state.setActiveBranch)

  // Branch Selection
  const [branches, setBranches] = useState<BranchSummary[]>([])
  const [selectedBranchId, setSelectedBranchId] = useState<string>(activeBranchId || '')

  // Active Tab
  const [activeTab, setActiveTab] = useState<'queue' | 'grid'>('queue')

  // Employees list for selectors and modals
  const [employees, setEmployees] = useState<EmployeeSummary[]>([])

  // Monthly KPIs
  const [monthlySummaries, setMonthlySummaries] = useState<MonthlyTimesheetSummary[]>([])
  const [pendingQueueCount, setPendingQueueCount] = useState<number>(0)
  const [isLoadingKPIs, setIsLoadingKPIs] = useState<boolean>(false)

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false)
  const [isBulkModalOpen, setIsBulkModalOpen] = useState<boolean>(false)

  // Current year & month for KPIs
  const now = new Date()
  const currentYear = now.getFullYear()
  const currentMonth = now.getMonth() + 1

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

  // 2. Fetch Employees for the selected branch
  const fetchEmployees = useCallback(async () => {
    if (!selectedBranchId) return
    try {
      const res = await hrApi.getEmployees(selectedBranchId, {
        size: 100,
        status: 'ALL',
      })
      setEmployees(res.content || [])
    } catch (err) {
      console.error('Failed to load employees:', err)
      setEmployees([])
    }
  }, [selectedBranchId])

  // 3. Fetch Monthly KPIs & Pending queue count
  const fetchKPIs = useCallback(async () => {
    if (!selectedBranchId) return
    setIsLoadingKPIs(true)
    try {
      const [summaries, queueRes] = await Promise.all([
        timesheetApi.getMonthlySummary(selectedBranchId, currentYear, currentMonth),
        timesheetApi.getTimesheets(selectedBranchId, {
          status: 'SUBMITTED',
          size: 1,
        }),
      ])
      setMonthlySummaries(summaries || [])
      setPendingQueueCount(queueRes.totalElements || 0)
    } catch (err) {
      console.error('Failed to load timesheet KPIs:', err)
    } finally {
      setIsLoadingKPIs(false)
    }
  }, [selectedBranchId, currentYear, currentMonth])

  useEffect(() => {
    if (selectedBranchId) {
      fetchEmployees()
      fetchKPIs()
    }
  }, [selectedBranchId, fetchEmployees, fetchKPIs])

  // KPI Calculations
  const totalApprovedHours = useMemo(() => {
    return monthlySummaries.reduce(
      (sum, s) => sum + (s.totalApprovedHours || 0),
      0
    )
  }, [monthlySummaries])

  const totalApprovedWorkdays = useMemo(() => {
    return monthlySummaries.reduce(
      (sum, s) => sum + (s.totalPayableWorkdays || 0),
      0
    )
  }, [monthlySummaries])

  const totalPendingInMonth = useMemo(() => {
    return monthlySummaries.reduce(
      (sum, s) => sum + (s.pendingDaysCount || 0),
      0
    )
  }, [monthlySummaries])

  const totalRejectedInMonth = useMemo(() => {
    return monthlySummaries.reduce(
      (sum, s) => sum + (s.rejectedDaysCount || 0),
      0
    )
  }, [monthlySummaries])

  // Format employees list for options
  const simpleEmployees = useMemo(() => {
    return employees.map((e) => ({
      id: e.id,
      staffCode: e.staffCode,
      fullName: e.fullName,
      positionName: e.positionName,
    }))
  }, [employees])

  const handleBranchChange = (branchId: string) => {
    setSelectedBranchId(branchId)
    const b = branches.find((item) => item.id === branchId)
    setActiveBranch(branchId, b?.name || null)
  }

  return (
    <div className="space-y-6 pb-12">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="p-2 rounded-xl bg-[#304638] text-white">
              <CalendarCheck className="h-5 w-5" />
            </span>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-stone-900">
                Chấm công & Phê duyệt Giờ làm
              </h1>
              <p className="text-xs text-stone-500">
                Hạch toán giờ công độc lập GPS · Kiểm soát bất biến cấm tự duyệt
                (P0-HR-03, P0-HR-04 / TC-017 / FE-P0-19)
              </p>
            </div>
          </div>
        </div>

        {/* Branch Selector & Actions */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Branch Picker */}
          <div className="flex items-center gap-2 bg-white px-3 py-1.5 rounded-xl border border-stone-200 shadow-sm">
            <Building2 className="h-4 w-4 text-[#304638]" />
            <select
              value={selectedBranchId}
              onChange={(e) => handleBranchChange(e.target.value)}
              className="text-xs font-semibold text-stone-800 bg-transparent focus:outline-none cursor-pointer"
            >
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name}
                </option>
              ))}
            </select>
          </div>

          <Button
            onClick={() => setIsBulkModalOpen(true)}
            className="bg-[#304638] hover:bg-[#25372C] text-white text-xs gap-1.5 shadow-sm"
          >
            <Users className="h-4 w-4" />
            Chấm công theo ca
          </Button>

          <Button
            onClick={() => setIsCreateModalOpen(true)}
            variant="outline"
            className="text-xs border-[#304638] text-[#304638] hover:bg-[#304638]/10 gap-1.5"
          >
            <Plus className="h-4 w-4" />
            Ghi công nhanh
          </Button>

          <Button
            onClick={() => {
              fetchEmployees()
              fetchKPIs()
            }}
            variant="outline"
            size="sm"
            disabled={isLoadingKPIs}
            className="text-xs border-stone-200 text-stone-600 hover:bg-stone-100 gap-1"
          >
            <RefreshCw
              className={`h-3.5 w-3.5 ${isLoadingKPIs ? 'animate-spin' : ''}`}
            />
            Làm mới
          </Button>
        </div>
      </div>

      {/* TC-017 Policy Notice Banner */}
      <div className="rounded-2xl border border-amber-200/90 bg-gradient-to-r from-amber-50 to-orange-50/60 p-4 shadow-sm flex items-start gap-3.5 text-xs text-amber-950">
        <ShieldAlert className="h-5 w-5 text-amber-600 shrink-0 mt-0.5" />
        <div className="space-y-1">
          <div className="font-bold flex items-center gap-2">
            <span>Ràng buộc Kiểm soát Bất biến & Quy tắc An toàn TC-017</span>
            <span className="px-2 py-0.5 text-[10px] rounded-full bg-amber-200/80 font-mono text-amber-900 uppercase">
              Immutability & Segregation
            </span>
          </div>
          <p className="text-amber-900 leading-relaxed">
            Hệ thống quản trị MTMS áp dụng nguyên tắc kiểm soát phân định thẩm quyền:
            <strong> Quản lý cửa hàng (Manager) chỉ được duyệt công cho nhân viên cấp dưới</strong>.
            Bản ghi của Quản lý cửa hàng bắt buộc phải do Chủ quán (Branch Owner) hoặc Admin phê duyệt.
            Mọi hành vi tự phê duyệt công của chính mình đều bị từ chối tự động.
          </p>
        </div>
      </div>

      {/* 4 Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1: Tổng giờ làm tháng */}
        <div className="bg-white p-5 rounded-2xl border border-stone-200 shadow-sm relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-stone-500 uppercase tracking-wider">
              Tổng giờ tháng {currentMonth}
            </span>
            <div className="p-2 rounded-xl bg-emerald-50 text-emerald-800">
              <Clock className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-black text-stone-900">
              {totalApprovedHours.toFixed(1)}h
            </div>
            <p className="text-[11px] text-stone-500 mt-0.5">
              Tương đương{' '}
              <strong className="text-emerald-700">
                {totalApprovedWorkdays.toFixed(2)} công
              </strong>
            </p>
          </div>
        </div>

        {/* KPI 2: Giờ đã duyệt */}
        <div className="bg-white p-5 rounded-2xl border border-stone-200 shadow-sm relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-stone-500 uppercase tracking-wider">
              Giờ đã duyệt
            </span>
            <div className="p-2 rounded-xl bg-[#304638]/10 text-[#304638]">
              <FileCheck2 className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3">
            <div className="text-2xl font-black text-[#304638]">
              {totalApprovedHours.toFixed(1)}h
            </div>
            <p className="text-[11px] text-stone-500 mt-0.5">
              Đủ điều kiện tính lương P0-HR-05
            </p>
          </div>
        </div>

        {/* KPI 3: Chờ phê duyệt (Queue) */}
        <div className="bg-white p-5 rounded-2xl border border-stone-200 shadow-sm relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-stone-500 uppercase tracking-wider">
              Chờ phê duyệt
            </span>
            <div className="p-2 rounded-xl bg-amber-50 text-amber-700">
              <AlertTriangle className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <div className="text-2xl font-black text-amber-600">
              {pendingQueueCount}
            </div>
            <span className="text-xs text-amber-700 font-medium">dòng công</span>
          </div>
          <p className="text-[11px] text-stone-500 mt-0.5">
            {totalPendingInMonth} ca chờ xử lý trong tháng
          </p>
        </div>

        {/* KPI 4: Bản ghi từ chối */}
        <div className="bg-white p-5 rounded-2xl border border-stone-200 shadow-sm relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-stone-500 uppercase tracking-wider">
              Bị từ chối
            </span>
            <div className="p-2 rounded-xl bg-rose-50 text-rose-700">
              <XCircle className="h-4 w-4" />
            </div>
          </div>
          <div className="mt-3 flex items-baseline gap-2">
            <div className="text-2xl font-black text-rose-600">
              {totalRejectedInMonth}
            </div>
            <span className="text-xs text-rose-700 font-medium">dòng</span>
          </div>
          <p className="text-[11px] text-stone-500 mt-0.5">
            Cần rà soát hoặc nhập lại giờ công
          </p>
        </div>
      </div>

      {/* Main Tabs Navigation */}
      <div className="flex items-center gap-2 border-b border-stone-200">
        <button
          type="button"
          onClick={() => setActiveTab('queue')}
          className={`flex items-center gap-2 px-5 py-3 text-xs font-bold border-b-2 transition-all ${
            activeTab === 'queue'
              ? 'border-[#304638] text-[#304638]'
              : 'border-transparent text-stone-500 hover:text-stone-800'
          }`}
        >
          <Clock className="h-4 w-4" />
          <span>Hàng đợi Phê duyệt (Review Queue)</span>
          {pendingQueueCount > 0 && (
            <span className="ml-1 px-2 py-0.5 rounded-full text-[10px] bg-amber-100 text-amber-800 font-bold">
              {pendingQueueCount}
            </span>
          )}
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('grid')}
          className={`flex items-center gap-2 px-5 py-3 text-xs font-bold border-b-2 transition-all ${
            activeTab === 'grid'
              ? 'border-[#304638] text-[#304638]'
              : 'border-transparent text-stone-500 hover:text-stone-800'
          }`}
        >
          <CalendarCheck className="h-4 w-4" />
          <span>Lưới Chấm công Tháng (Timesheet Matrix)</span>
        </button>
      </div>

      {/* Tab Contents */}
      {activeTab === 'queue' ? (
        <TimesheetReviewQueueTab
          branchId={selectedBranchId}
          employees={simpleEmployees}
          onRefreshKPIs={fetchKPIs}
        />
      ) : (
        <TimesheetGridTab
          branchId={selectedBranchId}
          employees={simpleEmployees}
          onRefreshKPIs={fetchKPIs}
        />
      )}

      {/* Create Timesheet Modal */}
      <CreateTimesheetModal
        isOpen={isCreateModalOpen}
        branchId={selectedBranchId}
        employees={simpleEmployees}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={() => {
          fetchKPIs()
        }}
      />

      {/* Bulk Timesheet Modal */}
      <BulkTimesheetModal
        isOpen={isBulkModalOpen}
        branchId={selectedBranchId}
        employees={simpleEmployees}
        onClose={() => setIsBulkModalOpen(false)}
        onSuccess={() => {
          fetchKPIs()
        }}
      />
    </div>
  )
}
