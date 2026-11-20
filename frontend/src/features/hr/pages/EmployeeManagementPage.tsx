import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Users,
  UserCheck,
  UserPlus,
  RefreshCw,
  Building2,
  Clock,
  Coins,
  AlertCircle,
  CheckCircle2,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { useAuthStore } from '@/features/auth/authStore'
import { apiClient } from '@/shared/api/apiClient'
import type { ApiResponse } from '@/shared/types'
import { hrApi } from '../api/hrApi'
import { EmployeeListTab } from '../components/EmployeeListTab'
import { CreateEmployeeModal } from '../components/CreateEmployeeModal'
import { EditEmployeeModal } from '../components/EditEmployeeModal'
import { AssignBranchModal } from '../components/AssignBranchModal'
import { SalaryPolicyModal } from '../components/SalaryPolicyModal'
import { EmployeeDetailDrawer } from '../components/EmployeeDetailDrawer'
import type {
  EmployeeSummary,
  EmployeeDetail,
} from '../types/hr.types'

interface BranchSummary {
  id: string
  code?: string
  name: string
  status?: string
}

export const EmployeeManagementPage: React.FC = () => {
  const activeBranchId = useAuthStore((state) => state.activeBranchId)
  const setActiveBranch = useAuthStore((state) => state.setActiveBranch)

  // Branch Selector State
  const [branches, setBranches] = useState<BranchSummary[]>([])
  const [selectedBranchId, setSelectedBranchId] = useState<string>(activeBranchId || '')

  // Data & Filter State
  const [employees, setEmployees] = useState<EmployeeSummary[]>([])
  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [searchQuery, setSearchQuery] = useState<string>('')
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL')
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [pageSize] = useState<number>(20)
  const [totalPages, setTotalPages] = useState<number>(1)
  const [totalElements, setTotalElements] = useState<number>(0)

  // Modals & Drawers State
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false)
  const [isEditModalOpen, setIsEditModalOpen] = useState<boolean>(false)
  const [isAssignModalOpen, setIsAssignModalOpen] = useState<boolean>(false)
  const [isSalaryModalOpen, setIsSalaryModalOpen] = useState<boolean>(false)
  const [isDrawerOpen, setIsDrawerOpen] = useState<boolean>(false)

  const [activeEmployee, setActiveEmployee] = useState<EmployeeSummary | EmployeeDetail | null>(null)
  const [drawerEmployeeId, setDrawerEmployeeId] = useState<string | null>(null)

  // Toasts
  const [successToast, setSuccessToast] = useState<string | null>(null)
  const [errorNotice, setErrorNotice] = useState<string | null>(null)

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

  // Sync selected branch if changed from outer auth state
  useEffect(() => {
    if (activeBranchId && activeBranchId !== selectedBranchId) {
      setSelectedBranchId(activeBranchId)
    }
  }, [activeBranchId, selectedBranchId])

  // 2. Fetch Employees
  const fetchEmployees = useCallback(async () => {
    if (!selectedBranchId) return
    try {
      setIsLoading(true)
      setErrorNotice(null)
      const res = await hrApi.getEmployees(selectedBranchId, {
        search: searchQuery.trim() || undefined,
        status: selectedStatus,
        page: currentPage,
        size: pageSize,
      })

      // Backend may return EmployeePageResponse or array
      if (Array.isArray(res)) {
        setEmployees(res)
        setTotalElements(res.length)
        setTotalPages(1)
      } else if (res && Array.isArray(res.content)) {
        setEmployees(res.content)
        setTotalElements(res.totalElements ?? res.content.length)
        setTotalPages(res.totalPages ?? 1)
      } else {
        setEmployees([])
        setTotalElements(0)
        setTotalPages(1)
      }
    } catch (err: unknown) {
      console.error('Failed to fetch employees:', err)
      setErrorNotice('Không thể tải danh sách nhân sự của chi nhánh. Vui lòng kiểm tra quyền truy cập.')
    } finally {
      setIsLoading(false)
    }
  }, [selectedBranchId, searchQuery, selectedStatus, currentPage, pageSize])

  useEffect(() => {
    fetchEmployees()
  }, [fetchEmployees])

  // Reset page when search or status filter changes
  const handleSearchChange = (q: string) => {
    setSearchQuery(q)
    setCurrentPage(0)
  }

  const handleStatusChange = (st: string) => {
    setSelectedStatus(st)
    setCurrentPage(0)
  }

  // Branch change handler
  const handleBranchChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newId = e.target.value
    setSelectedBranchId(newId)
    const b = branches.find((item) => item.id === newId)
    setActiveBranch(newId, b ? b.name : null)
    setCurrentPage(0)
  }

  // Selected branch object
  const currentBranch = useMemo(
    () => branches.find((b) => b.id === selectedBranchId),
    [branches, selectedBranchId]
  )

  // KPI Calculations
  const kpiStats = useMemo(() => {
    const total = employees.length
    const activeCount = employees.filter((e) => e.status === 'ACTIVE').length
    const hourlyCount = employees.filter((e) => e.activeRateType === 'HOURLY').length
    const monthlyCount = employees.filter((e) => e.activeRateType === 'MONTHLY').length
    return { total, activeCount, hourlyCount, monthlyCount }
  }, [employees])

  // Handlers for modal triggers
  const handleOpenDetail = (emp: EmployeeSummary | EmployeeDetail) => {
    setDrawerEmployeeId(emp.id)
    setIsDrawerOpen(true)
  }

  const handleOpenEdit = (emp: EmployeeSummary | EmployeeDetail) => {
    setActiveEmployee(emp)
    setIsEditModalOpen(true)
  }

  const handleOpenAssign = (emp: EmployeeSummary | EmployeeDetail) => {
    setActiveEmployee(emp)
    setIsAssignModalOpen(true)
  }

  const handleOpenSalary = (emp: EmployeeSummary | EmployeeDetail) => {
    setActiveEmployee(emp)
    setIsSalaryModalOpen(true)
  }

  return (
    <div className="space-y-6 pb-12">
      {/* Toast Messages */}
      {successToast && (
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-800 flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2.5">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span className="font-semibold">{successToast}</span>
          </div>
          <button
            type="button"
            onClick={() => setSuccessToast(null)}
            className="text-emerald-500 hover:text-emerald-700"
          >
            &times;
          </button>
        </div>
      )}

      {errorNotice && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-xs text-rose-800 flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2.5">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span className="font-semibold">{errorNotice}</span>
          </div>
          <button
            type="button"
            onClick={() => setErrorNotice(null)}
            className="text-rose-500 hover:text-rose-700"
          >
            &times;
          </button>
        </div>
      )}

      {/* Top Banner / Header & Branch Switcher */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
        <div className="space-y-1">
          <div className="flex items-center gap-2.5">
            <div className="p-2.5 rounded-2xl bg-[#304638] text-white">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-slate-900">
                Hồ sơ Nhân sự & Chính sách Lương
              </h1>
              <p className="text-xs text-slate-500 flex items-center gap-1.5 mt-0.5">
                <span>P0-HR-01 & P0-HR-02 • Phân bổ chi nhánh (TC-015) • Mốc lương hiệu lực (TC-013)</span>
              </p>
            </div>
          </div>
        </div>

        {/* Branch Selector and Action Buttons */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Branch Switcher */}
          <div className="flex items-center gap-2 bg-slate-50 border border-slate-200 rounded-2xl px-3 py-1.5 shadow-2xs">
            <Building2 className="w-4 h-4 text-[#304638] shrink-0" />
            <select
              value={selectedBranchId}
              onChange={handleBranchChange}
              className="bg-transparent text-xs font-semibold text-slate-800 focus:outline-none cursor-pointer pr-2"
            >
              {branches.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.name} {b.code ? `(${b.code})` : ''}
                </option>
              ))}
            </select>
          </div>

          {/* Refresh Button */}
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={fetchEmployees}
            disabled={isLoading}
            className="h-9 px-3 rounded-xl border-slate-200 text-xs"
          >
            <RefreshCw className={`w-3.5 h-3.5 mr-1.5 ${isLoading ? 'animate-spin' : ''}`} />
            <span>Làm mới</span>
          </Button>

          {/* Create Employee Button */}
          <Button
            type="button"
            size="sm"
            onClick={() => setIsCreateModalOpen(true)}
            className="h-9 px-4 rounded-xl bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold shadow-xs"
          >
            <UserPlus className="w-3.5 h-3.5 mr-1.5" />
            <span>Thêm nhân viên mới</span>
          </Button>
        </div>
      </div>

      {/* KPI Quick Overview Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Tổng nhân sự */}
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-2xs flex items-center justify-between">
          <div className="space-y-1">
            <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block">
              Tổng nhân sự chi nhánh
            </span>
            <span className="text-2xl font-bold font-mono text-slate-900 block">
              {kpiStats.total}
            </span>
            <span className="text-[10px] text-slate-400">Đã đăng ký công tác</span>
          </div>
          <div className="w-10 h-10 rounded-2xl bg-slate-100 flex items-center justify-center text-slate-600">
            <Users className="w-5 h-5" />
          </div>
        </div>

        {/* Card 2: Đang làm việc */}
        <div className="p-4 rounded-2xl bg-white border border-emerald-200 shadow-2xs flex items-center justify-between bg-emerald-50/20">
          <div className="space-y-1">
            <span className="text-[11px] font-semibold text-emerald-800 uppercase tracking-wider block">
              Đang làm việc (ACTIVE)
            </span>
            <span className="text-2xl font-bold font-mono text-emerald-700 block">
              {kpiStats.activeCount}
            </span>
            <span className="text-[10px] text-emerald-600">Sẵn sàng bố trí ca</span>
          </div>
          <div className="w-10 h-10 rounded-2xl bg-emerald-100 flex items-center justify-center text-emerald-700">
            <UserCheck className="w-5 h-5" />
          </div>
        </div>

        {/* Card 3: Lương theo giờ */}
        <div className="p-4 rounded-2xl bg-white border border-blue-200 shadow-2xs flex items-center justify-between bg-blue-50/20">
          <div className="space-y-1">
            <span className="text-[11px] font-semibold text-blue-800 uppercase tracking-wider block">
              Lương theo giờ (Hourly)
            </span>
            <span className="text-2xl font-bold font-mono text-blue-700 block">
              {kpiStats.hourlyCount}
            </span>
            <span className="text-[10px] text-blue-600">Pha chế & Part-time</span>
          </div>
          <div className="w-10 h-10 rounded-2xl bg-blue-100 flex items-center justify-center text-blue-700">
            <Clock className="w-5 h-5" />
          </div>
        </div>

        {/* Card 4: Lương theo tháng */}
        <div className="p-4 rounded-2xl bg-white border border-purple-200 shadow-2xs flex items-center justify-between bg-purple-50/20">
          <div className="space-y-1">
            <span className="text-[11px] font-semibold text-purple-800 uppercase tracking-wider block">
              Lương theo tháng (Monthly)
            </span>
            <span className="text-2xl font-bold font-mono text-purple-700 block">
              {kpiStats.monthlyCount}
            </span>
            <span className="text-[10px] text-purple-600">Quản lý & Full-time</span>
          </div>
          <div className="w-10 h-10 rounded-2xl bg-purple-100 flex items-center justify-center text-purple-700">
            <Coins className="w-5 h-5" />
          </div>
        </div>
      </div>

      {/* Main Employee Directory Tab Content */}
      <EmployeeListTab
        employees={employees}
        isLoading={isLoading}
        searchQuery={searchQuery}
        onSearchChange={handleSearchChange}
        selectedStatus={selectedStatus}
        onStatusChange={handleStatusChange}
        currentPage={currentPage}
        totalPages={totalPages}
        totalElements={totalElements}
        pageSize={pageSize}
        onPageChange={setCurrentPage}
        onViewDetail={handleOpenDetail}
        onEdit={handleOpenEdit}
        onManageSalary={handleOpenSalary}
        onAssignBranch={handleOpenAssign}
        onOpenCreateModal={() => setIsCreateModalOpen(true)}
      />

      {/* Modal 1: Create Employee */}
      <CreateEmployeeModal
        isOpen={isCreateModalOpen}
        branchId={selectedBranchId}
        branchName={currentBranch?.name}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={() => {
          setSuccessToast('Đã thêm nhân viên mới và thiết lập phân bổ chi nhánh thành công!')
          fetchEmployees()
        }}
      />

      {/* Modal 2: Edit Employee */}
      <EditEmployeeModal
        isOpen={isEditModalOpen}
        branchId={selectedBranchId}
        employee={activeEmployee}
        onClose={() => {
          setIsEditModalOpen(false)
          setActiveEmployee(null)
        }}
        onSuccess={() => {
          setSuccessToast('Cập nhật hồ sơ nhân sự thành công!')
          fetchEmployees()
        }}
      />

      {/* Modal 3: Assign Branch (TC-015) */}
      <AssignBranchModal
        isOpen={isAssignModalOpen}
        branchId={selectedBranchId}
        employee={activeEmployee}
        branches={branches}
        onClose={() => {
          setIsAssignModalOpen(false)
          setActiveEmployee(null)
        }}
        onSuccess={() => {
          setSuccessToast('Đã phân bổ / điều chuyển chi nhánh (TC-015) thành công!')
          fetchEmployees()
        }}
      />

      {/* Modal 4: Salary Policy (TC-013) */}
      <SalaryPolicyModal
        isOpen={isSalaryModalOpen}
        branchId={selectedBranchId}
        employee={activeEmployee}
        onClose={() => {
          setIsSalaryModalOpen(false)
          setActiveEmployee(null)
        }}
        onSuccess={() => {
          setSuccessToast('Đã thiết lập chính sách lương mới theo mốc hiệu lực (TC-013) thành công!')
          fetchEmployees()
        }}
      />

      {/* Slide-over Drawer: Comprehensive Detail */}
      <EmployeeDetailDrawer
        isOpen={isDrawerOpen}
        branchId={selectedBranchId}
        employeeId={drawerEmployeeId}
        onClose={() => {
          setIsDrawerOpen(false)
          setDrawerEmployeeId(null)
        }}
        onEdit={(detail) => {
          setIsDrawerOpen(false)
          setActiveEmployee(detail)
          setIsEditModalOpen(true)
        }}
        onAssignBranch={(detail) => {
          setIsDrawerOpen(false)
          setActiveEmployee(detail)
          setIsAssignModalOpen(true)
        }}
        onManageSalary={(detail) => {
          setIsDrawerOpen(false)
          setActiveEmployee(detail)
          setIsSalaryModalOpen(true)
        }}
      />
    </div>
  )
}
export default EmployeeManagementPage
