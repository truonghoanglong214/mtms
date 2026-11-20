import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Search,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  Receipt,
  Plus,
  Eye,
  Calendar,
  Filter,
  RotateCcw,
  ShieldAlert,
  Wallet,
  Building2,
  CreditCard,
  Ban,
  Clock,
  ChevronLeft,
  ChevronRight,
  Loader2,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { financeApi, formatVnd, getExpenseItems, getExpenseTotalElements } from '../api/financeApi'
import { CreateExpenseModal } from './CreateExpenseModal'
import { ExpenseDetailModal } from './ExpenseDetailModal'
import type {
  ExpenseDto,
  FinancialAccountDto,
  FinancialCategoryDto,
  ExpenseStatus,
} from '../types/finance.types'

interface OperatingExpensesTabProps {
  branchId: string
  branchName?: string
  accounts: FinancialAccountDto[]
  categories: FinancialCategoryDto[]
  onRefreshAccounts?: () => void
}

export const OperatingExpensesTab: React.FC<OperatingExpensesTabProps> = ({
  branchId,
  branchName,
  accounts,
  categories,
  onRefreshAccounts,
}) => {
  const [expenses, setExpenses] = useState<ExpenseDto[]>([])
  const [totalItems, setTotalItems] = useState<number>(0)
  const [totalPages, setTotalPages] = useState<number>(1)
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [pageSize, setPageSize] = useState<number>(15)

  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [errorNotice, setErrorNotice] = useState<string | null>(null)
  const [successToast, setSuccessToast] = useState<string | null>(null)

  // Filters
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL')
  const [reviewFlagOnly, setReviewFlagOnly] = useState<boolean>(false)
  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')
  const [searchQuery, setSearchQuery] = useState<string>('')

  // Modals state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false)
  const [selectedExpenseForDetail, setSelectedExpenseForDetail] = useState<ExpenseDto | null>(null)

  // Filter OPEX categories
  const opexCategories = useMemo(() => {
    return categories.filter(
      (c) => c.categoryGroup === 'OPERATING_EXPENSE' && (c.isActive ?? true)
    )
  }, [categories])

  // Load Expenses with Server Filters
  const fetchExpenses = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    setErrorNotice(null)

    try {
      const res = await financeApi.getExpenses(branchId, {
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        categoryId: categoryFilter !== 'ALL' ? categoryFilter : undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        reviewFlagOnly: reviewFlagOnly ? true : undefined,
        search: searchQuery.trim() || undefined,
        page: currentPage,
        size: pageSize,
      })

      const items = getExpenseItems(res)
      setExpenses(items)
      const count = getExpenseTotalElements(res)
      setTotalItems(count)
      setTotalPages(res.totalPages || Math.ceil(count / pageSize) || 1)
    } catch (err: unknown) {
      console.error('Failed to load expenses:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string } }
      }
      setErrorNotice(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải danh sách chi phí vận hành. Vui lòng thử lại.'
      )
    } finally {
      setIsLoading(false)
    }
  }, [
    branchId,
    statusFilter,
    categoryFilter,
    reviewFlagOnly,
    startDate,
    endDate,
    searchQuery,
    currentPage,
    pageSize,
  ])

  useEffect(() => {
    fetchExpenses()
  }, [fetchExpenses])

  // Compute 4 KPI summary cards from active view
  const kpiData = useMemo(() => {
    let totalIncurred = 0
    let totalCash = 0
    let totalBank = 0
    let totalReviewFlagAmount = 0
    let countReviewFlag = 0

    expenses.forEach((item) => {
      // Exclude voided expenses from active KPI calculations
      if (item.status === 'VOIDED') return

      const amt =
        typeof item.amount === 'string' ? parseFloat(item.amount) || 0 : item.amount

      totalIncurred += amt

      if (item.reviewFlag) {
        countReviewFlag += 1
        totalReviewFlagAmount += amt
      }

      if (item.isPaid) {
        // Find linked account kind
        const acc = accounts.find((a) => a.id === item.paymentAccountId)
        if (acc?.accountKind === 'BANK') {
          totalBank += amt
        } else {
          totalCash += amt
        }
      }
    })

    return {
      totalIncurred,
      totalCash,
      totalBank,
      totalReviewFlagAmount,
      countReviewFlag,
    }
  }, [expenses, accounts])

  // Quick Date Shortcut Handlers
  const handleQuickDateFilter = (type: 'today' | '7days' | 'thisMonth' | 'all') => {
    const today = new Date()
    const formatDate = (d: Date) => d.toISOString().split('T')[0]

    setCurrentPage(0)
    if (type === 'today') {
      const str = formatDate(today)
      setStartDate(str)
      setEndDate(str)
    } else if (type === '7days') {
      const d = new Date()
      d.setDate(d.getDate() - 6)
      setStartDate(formatDate(d))
      setEndDate(formatDate(today))
    } else if (type === 'thisMonth') {
      const firstDay = new Date(today.getFullYear(), today.getMonth(), 1)
      setStartDate(formatDate(firstDay))
      setEndDate(formatDate(today))
    } else {
      setStartDate('')
      setEndDate('')
    }
  }

  const handleResetFilters = () => {
    setStatusFilter('ALL')
    setCategoryFilter('ALL')
    setReviewFlagOnly(false)
    setStartDate('')
    setEndDate('')
    setSearchQuery('')
    setCurrentPage(0)
  }

  const handleCreateSuccess = (newExp: ExpenseDto) => {
    setSuccessToast(`Đã tạo phiếu chi thành công: ${newExp.expenseCode}`)
    setTimeout(() => setSuccessToast(null), 5000)
    fetchExpenses()
    if (onRefreshAccounts) {
      onRefreshAccounts()
    }
  }

  const handleExpenseUpdated = (updated: ExpenseDto) => {
    setExpenses((prev) =>
      prev.map((e) => (e.id === updated.id ? { ...e, ...updated } : e))
    )
    fetchExpenses()
    if (onRefreshAccounts) {
      onRefreshAccounts()
    }
  }

  const getStatusBadge = (status: ExpenseStatus | string) => {
    switch (status) {
      case 'CONFIRMED':
        return (
          <Badge className="bg-emerald-100 text-emerald-800 border-emerald-300 font-semibold text-[11px] flex items-center gap-1">
            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
            <span>Đã duyệt</span>
          </Badge>
        )
      case 'DRAFT':
        return (
          <Badge className="bg-amber-100 text-amber-800 border-amber-300 font-semibold text-[11px] flex items-center gap-1">
            <Clock className="w-3 h-3 text-amber-600" />
            <span>Chờ duyệt</span>
          </Badge>
        )
      case 'VOIDED':
        return (
          <Badge variant="outline" className="bg-rose-50 text-rose-700 border-rose-300 font-semibold text-[11px] flex items-center gap-1">
            <Ban className="w-3 h-3 text-rose-600" />
            <span>Đã hủy</span>
          </Badge>
        )
      default:
        return (
          <Badge variant="secondary" className="text-[11px]">
            {status}
          </Badge>
        )
    }
  }

  return (
    <div className="space-y-5 animate-in fade-in duration-200">
      {/* Toast Notification */}
      {successToast && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs text-emerald-800 shadow-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span className="font-medium">{successToast}</span>
          </div>
          <button
            type="button"
            onClick={() => setSuccessToast(null)}
            className="text-emerald-700 hover:text-emerald-900 cursor-pointer"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Error Notice */}
      {errorNotice && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-xs text-rose-800 shadow-xs">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
          <span>{errorNotice}</span>
        </div>
      )}

      {/* 1. 4 KPI Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        {/* KPI 1: Tổng chi phí phát sinh */}
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs hover:border-slate-300 transition-all">
          <div className="flex items-center justify-between text-slate-500 mb-1.5">
            <span className="text-xs font-semibold">Tổng chi phí phát sinh</span>
            <div className="w-7 h-7 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center">
              <Receipt className="w-4 h-4" />
            </div>
          </div>
          <div className="text-xl sm:text-2xl font-extrabold text-[#304638] font-mono">
            {formatVnd(kpiData.totalIncurred)}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">
            Gồm chi phí đã duyệt và chờ duyệt trong kỳ
          </p>
        </div>

        {/* KPI 2: Đã chi tiền mặt */}
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs hover:border-slate-300 transition-all">
          <div className="flex items-center justify-between text-slate-500 mb-1.5">
            <span className="text-xs font-semibold">Đã chi tiền mặt</span>
            <div className="w-7 h-7 rounded-lg bg-amber-50 text-amber-700 flex items-center justify-center">
              <Wallet className="w-4 h-4" />
            </div>
          </div>
          <div className="text-xl sm:text-2xl font-extrabold text-amber-900 font-mono">
            {formatVnd(kpiData.totalCash)}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">
            Chi trực tiếp qua Quỹ tiền mặt chi nhánh
          </p>
        </div>

        {/* KPI 3: Đã chi ngân hàng */}
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs hover:border-slate-300 transition-all">
          <div className="flex items-center justify-between text-slate-500 mb-1.5">
            <span className="text-xs font-semibold">Đã chi ngân hàng</span>
            <div className="w-7 h-7 rounded-lg bg-blue-50 text-blue-700 flex items-center justify-center">
              <Building2 className="w-4 h-4" />
            </div>
          </div>
          <div className="text-xl sm:text-2xl font-extrabold text-blue-900 font-mono">
            {formatVnd(kpiData.totalBank)}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">
            Chuyển khoản thanh toán qua ngân hàng
          </p>
        </div>

        {/* KPI 4: Cần rà soát (review_flag) */}
        <div
          onClick={() => {
            setReviewFlagOnly((prev) => !prev)
            setCurrentPage(0)
          }}
          className={`p-4 rounded-2xl border shadow-xs transition-all cursor-pointer ${
            reviewFlagOnly
              ? 'bg-amber-100/70 border-amber-400 ring-2 ring-amber-400/50'
              : 'bg-white border-slate-200 hover:border-amber-300'
          }`}
          title="Nhấp để lọc nhanh các khoản chi cần rà soát"
        >
          <div className="flex items-center justify-between text-slate-500 mb-1.5">
            <div className="flex items-center gap-1.5">
              <span className="text-xs font-bold text-amber-900">Chi phí cần rà soát</span>
              <Badge className="bg-amber-500 text-white text-[10px] py-0 px-1 font-mono">
                {kpiData.countReviewFlag}
              </Badge>
            </div>
            <div className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center">
              <ShieldAlert className="w-4 h-4 text-amber-600" />
            </div>
          </div>
          <div className="text-xl sm:text-2xl font-extrabold text-amber-900 font-mono">
            {formatVnd(kpiData.totalReviewFlagAmount)}
          </div>
          <p className="text-[11px] text-amber-700 mt-1">
            Khoản chi ≥ 2.000.000 ₫ (BR-FIN-07)
          </p>
        </div>
      </div>

      {/* 2. Action Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
        <div className="flex items-center gap-2 flex-wrap">
          <Button
            type="button"
            onClick={() => setIsCreateModalOpen(true)}
            className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold px-4 py-2 rounded-xl flex items-center gap-2 shadow-xs cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Lập phiếu chi mới</span>
          </Button>

          {/* Toggle button for review_flag */}
          <Button
            type="button"
            variant="outline"
            onClick={() => {
              setReviewFlagOnly((prev) => !prev)
              setCurrentPage(0)
            }}
            className={`text-xs font-semibold px-3.5 py-2 rounded-xl flex items-center gap-2 cursor-pointer transition-all ${
              reviewFlagOnly
                ? 'border-amber-500 bg-amber-50 text-amber-900 font-bold'
                : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-100'
            }`}
          >
            <ShieldAlert className={`w-4 h-4 ${reviewFlagOnly ? 'text-amber-600' : 'text-slate-400'}`} />
            <span>Chỉ xem cần rà soát (≥ 2M)</span>
          </Button>
        </div>

        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => fetchExpenses()}
          disabled={isLoading}
          className="text-xs text-slate-600 flex items-center gap-1.5 cursor-pointer rounded-xl h-8"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin text-[#B96849]' : ''}`} />
          <span>Tải lại</span>
        </Button>
      </div>

      {/* 3. Filter Toolbar */}
      <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-3">
        <div className="flex items-center justify-between text-xs font-semibold text-slate-700">
          <div className="flex items-center gap-1.5">
            <Filter className="w-3.5 h-3.5 text-[#304638]" />
            <span>Bộ lọc danh sách phiếu chi</span>
          </div>
          {(statusFilter !== 'ALL' ||
            categoryFilter !== 'ALL' ||
            reviewFlagOnly ||
            startDate ||
            endDate ||
            searchQuery) && (
            <button
              type="button"
              onClick={handleResetFilters}
              className="text-[11px] text-rose-600 hover:text-rose-800 font-medium flex items-center gap-1 cursor-pointer"
            >
              <RotateCcw className="w-3 h-3" />
              <span>Xóa bộ lọc</span>
            </button>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-xs">
          {/* Lọc danh mục */}
          <div className="space-y-1">
            <label className="text-slate-500 font-medium">Danh mục OPEX</label>
            <select
              value={categoryFilter}
              onChange={(e) => {
                setCategoryFilter(e.target.value)
                setCurrentPage(0)
              }}
              className="w-full rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs text-slate-700 font-medium focus:outline-none focus:ring-1 focus:ring-[#304638]"
            >
              <option value="ALL">Tất cả danh mục ({opexCategories.length})</option>
              {opexCategories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.categoryName} ({c.categoryCode})
                </option>
              ))}
            </select>
          </div>

          {/* Lọc trạng thái */}
          <div className="space-y-1">
            <label className="text-slate-500 font-medium">Trạng thái phê duyệt</label>
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value)
                setCurrentPage(0)
              }}
              className="w-full rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs text-slate-700 font-medium focus:outline-none focus:ring-1 focus:ring-[#304638]"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="CONFIRMED">Đã duyệt chi (CONFIRMED)</option>
              <option value="DRAFT">Chờ duyệt / Bản nháp (DRAFT)</option>
              <option value="VOIDED">Đã hủy phiếu (VOIDED)</option>
            </select>
          </div>

          {/* Tìm kiếm */}
          <div className="space-y-1 sm:col-span-2">
            <label className="text-slate-500 font-medium">Tìm kiếm diễn giải, mã phiếu</label>
            <div className="relative">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
              <Input
                type="text"
                placeholder="Nhập mã phiếu chi, lý do, người tạo..."
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value)
                  setCurrentPage(0)
                }}
                className="pl-8 text-xs h-8"
              />
            </div>
          </div>
        </div>

        {/* Date Filter & Shortcuts */}
        <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-100 text-xs">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-slate-500 text-[11px] font-medium flex items-center gap-1">
              <Calendar className="w-3.5 h-3.5 text-slate-400" />
              Khoảng thời gian:
            </span>
            <input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value)
                setCurrentPage(0)
              }}
              className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-700 font-mono"
            />
            <span className="text-slate-400">đến</span>
            <input
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value)
                setCurrentPage(0)
              }}
              className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-700 font-mono"
            />
          </div>

          {/* Quick Date Pills */}
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-slate-400 text-[11px]">Nhanh:</span>
            <button
              type="button"
              onClick={() => handleQuickDateFilter('today')}
              className="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-[11px] font-medium text-slate-700 transition-colors cursor-pointer"
            >
              Hôm nay
            </button>
            <button
              type="button"
              onClick={() => handleQuickDateFilter('7days')}
              className="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-[11px] font-medium text-slate-700 transition-colors cursor-pointer"
            >
              7 ngày qua
            </button>
            <button
              type="button"
              onClick={() => handleQuickDateFilter('thisMonth')}
              className="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-[11px] font-medium text-slate-700 transition-colors cursor-pointer"
            >
              Tháng này
            </button>
            <button
              type="button"
              onClick={() => handleQuickDateFilter('all')}
              className="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-[11px] font-medium text-slate-700 transition-colors cursor-pointer"
            >
              Tất cả
            </button>
          </div>
        </div>
      </div>

      {/* 4. Expenses Table */}
      <div className="rounded-2xl border border-slate-200 bg-white shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-600 font-semibold uppercase tracking-wider text-[11px]">
                <th className="py-3 px-4">Mã phiếu chi</th>
                <th className="py-3 px-3">Ngày chi</th>
                <th className="py-3 px-3">Danh mục OPEX</th>
                <th className="py-3 px-4 min-w-[200px]">Diễn giải lý do</th>
                <th className="py-3 px-4 text-right">Số tiền</th>
                <th className="py-3 px-3">Hình thức</th>
                <th className="py-3 px-3">Trạng thái</th>
                <th className="py-3 px-3 text-center">Cảnh báo</th>
                <th className="py-3 px-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-slate-400">
                    <Loader2 className="w-6 h-6 animate-spin mx-auto text-[#304638] mb-2" />
                    <span>Đang tải danh sách phiếu chi...</span>
                  </td>
                </tr>
              ) : expenses.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-slate-400">
                    <Receipt className="w-8 h-8 mx-auto text-slate-300 mb-2" />
                    <p className="font-medium text-slate-600 text-sm">
                      Không tìm thấy phiếu chi nào phù hợp
                    </p>
                    <p className="text-xs text-slate-400 mt-1">
                      Thử điều chỉnh bộ lọc hoặc tạo phiếu chi vận hành mới.
                    </p>
                  </td>
                </tr>
              ) : (
                expenses.map((item) => {
                  const amt =
                    typeof item.amount === 'string'
                      ? parseFloat(item.amount) || 0
                      : item.amount

                  return (
                    <tr
                      key={item.id}
                      className="hover:bg-slate-50/80 transition-colors"
                    >
                      {/* Mã phiếu chi */}
                      <td className="py-3.5 px-4 font-mono font-semibold text-slate-800">
                        {item.expenseCode}
                      </td>

                      {/* Ngày chi */}
                      <td className="py-3.5 px-3 font-mono text-slate-600 whitespace-nowrap">
                        {item.expenseDate}
                      </td>

                      {/* Danh mục */}
                      <td className="py-3.5 px-3 whitespace-nowrap">
                        <span className="font-semibold text-slate-700 block">
                          {item.categoryName || item.categoryCode}
                        </span>
                        <span className="text-[10px] text-slate-400 font-mono">
                          {item.categoryCode}
                        </span>
                      </td>

                      {/* Diễn giải lý do */}
                      <td className="py-3.5 px-4 text-slate-700 max-w-xs">
                        <span className="line-clamp-2 leading-relaxed">
                          {item.reason}
                        </span>
                      </td>

                      {/* Số tiền */}
                      <td className="py-3.5 px-4 text-right font-mono font-bold text-slate-900 whitespace-nowrap">
                        {formatVnd(amt)}
                      </td>

                      {/* Hình thức */}
                      <td className="py-3.5 px-3 whitespace-nowrap">
                        {item.isPaid ? (
                          <div className="flex items-center gap-1.5 text-slate-700 font-medium">
                            {item.paymentAccountName?.toLowerCase().includes('ngân hàng') ? (
                              <Building2 className="w-3.5 h-3.5 text-blue-600 shrink-0" />
                            ) : (
                              <Wallet className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                            )}
                            <span className="truncate max-w-[120px]" title={item.paymentAccountName || ''}>
                              {item.paymentAccountName || 'Đã thanh toán'}
                            </span>
                          </div>
                        ) : (
                          <div className="flex items-center gap-1.5 text-amber-700 font-medium">
                            <CreditCard className="w-3.5 h-3.5 text-amber-600 shrink-0" />
                            <span>Ghi nợ NCC</span>
                          </div>
                        )}
                      </td>

                      {/* Trạng thái */}
                      <td className="py-3.5 px-3 whitespace-nowrap">
                        {getStatusBadge(item.status)}
                      </td>

                      {/* Cảnh báo chi lớn */}
                      <td className="py-3.5 px-3 text-center whitespace-nowrap">
                        {item.reviewFlag ? (
                          <Badge className="bg-amber-100 text-amber-800 border-amber-300 font-mono text-[10px]">
                            ≥ 2M
                          </Badge>
                        ) : (
                          <span className="text-slate-300">—</span>
                        )}
                      </td>

                      {/* Thao tác */}
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => setSelectedExpenseForDetail(item)}
                          className="h-7 px-2.5 text-xs text-[#304638] hover:text-[#25372C] hover:bg-slate-100 rounded-lg cursor-pointer"
                        >
                          <Eye className="w-3.5 h-3.5 mr-1" />
                          <span>Chi tiết</span>
                        </Button>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {totalItems > 0 && (
          <div className="px-4 py-3 bg-slate-50/70 border-t border-slate-200 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-600">
            <div className="flex items-center gap-2">
              <span>Hiển thị</span>
              <select
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value))
                  setCurrentPage(0)
                }}
                className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-700 font-medium focus:outline-none"
              >
                <option value={10}>10</option>
                <option value={15}>15</option>
                <option value={25}>25</option>
                <option value={50}>50</option>
              </select>
              <span>trên tổng số <strong>{totalItems}</strong> phiếu chi</span>
            </div>

            <div className="flex items-center gap-1.5">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                disabled={currentPage === 0 || isLoading}
                className="h-7 w-7 p-0 rounded-lg cursor-pointer"
              >
                <ChevronLeft className="w-4 h-4" />
              </Button>
              <span className="font-mono text-xs px-2">
                Trang {currentPage + 1} / {totalPages}
              </span>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={currentPage >= totalPages - 1 || isLoading}
                className="h-7 w-7 p-0 rounded-lg cursor-pointer"
              >
                <ChevronRight className="w-4 h-4" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Modals */}
      <CreateExpenseModal
        isOpen={isCreateModalOpen}
        branchId={branchId}
        branchName={branchName}
        accounts={accounts}
        categories={categories}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={handleCreateSuccess}
      />

      <ExpenseDetailModal
        isOpen={!!selectedExpenseForDetail}
        branchId={branchId}
        expense={selectedExpenseForDetail}
        onClose={() => setSelectedExpenseForDetail(null)}
        onExpenseUpdated={handleExpenseUpdated}
      />
    </div>
  )
}
export default OperatingExpensesTab
