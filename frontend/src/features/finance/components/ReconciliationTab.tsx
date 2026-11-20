import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Scale,
  Plus,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  Calendar,
  Filter,
  RotateCcw,
  Building2,
  Wallet,
  Clock,
  TrendingUp,
  TrendingDown,
  Equal,
  FileText,
  ExternalLink,
  ChevronLeft,
  ChevronRight,
  Loader2,
  Lock,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import {
  financeApi,
  formatVnd,
  formatSignedVnd,
  getReconciliationItems,
  getReconciliationTotalElements,
} from '../api/financeApi'
import { CreateReconciliationModal } from './CreateReconciliationModal'
import { ExplainDiscrepancyModal } from './ExplainDiscrepancyModal'
import { AdjustVarianceModal } from './AdjustVarianceModal'
import {
  RECONCILIATION_DISPOSITION_BADGES,
} from '../types/finance.types'
import type {
  FinancialAccountDto,
  CashReconciliationDto,
  ReconciliationDisposition,
} from '../types/finance.types'

interface ReconciliationTabProps {
  branchId: string
  branchName?: string
  accounts: FinancialAccountDto[]
  onRefreshAccounts?: () => void
}

export const ReconciliationTab: React.FC<ReconciliationTabProps> = ({
  branchId,
  branchName,
  accounts,
  onRefreshAccounts,
}) => {
  const [reconciliations, setReconciliations] = useState<CashReconciliationDto[]>([])
  const [totalItems, setTotalItems] = useState<number>(0)
  const [totalPages, setTotalPages] = useState<number>(1)
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [pageSize] = useState<number>(15)

  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [closingId, setClosingId] = useState<string | null>(null)
  const [errorNotice, setErrorNotice] = useState<string | null>(null)
  const [successToast, setSuccessToast] = useState<string | null>(null)

  // Filters
  const [accountFilter, setAccountFilter] = useState<string>('ALL')
  const [dispositionFilter, setDispositionFilter] = useState<string>('ALL')
  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')

  // Modals state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false)
  const [explainingItem, setExplainingItem] = useState<CashReconciliationDto | null>(null)
  const [adjustingItem, setAdjustingItem] = useState<CashReconciliationDto | null>(null)

  // Fetch reconciliations list
  const fetchReconciliations = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    setErrorNotice(null)

    try {
      const res = await financeApi.getReconciliations(branchId, {
        accountId: accountFilter !== 'ALL' ? accountFilter : undefined,
        disposition: dispositionFilter !== 'ALL' ? dispositionFilter : undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        page: currentPage,
        size: pageSize,
      })

      const items = getReconciliationItems(res)
      setReconciliations(items)
      const count = getReconciliationTotalElements(res)
      setTotalItems(count)
      setTotalPages(res.totalPages || Math.ceil(count / pageSize) || 1)
    } catch (err: unknown) {
      console.error('Failed to load reconciliations:', err)
      setErrorNotice('Không thể tải danh sách đối soát kiểm kê quỹ. Vui lòng thử lại.')
    } finally {
      setIsLoading(false)
    }
  }, [branchId, accountFilter, dispositionFilter, startDate, endDate, currentPage, pageSize])

  useEffect(() => {
    fetchReconciliations()
  }, [fetchReconciliations])

  // KPI Calculations
  const kpiData = useMemo(() => {
    let openCount = 0
    let netVariance = 0
    let totalAbsDiscrepancy = 0

    reconciliations.forEach((r) => {
      const v = typeof r.varianceAmount === 'string' ? parseFloat(r.varianceAmount) || 0 : r.varianceAmount
      if (r.disposition === 'OPEN') {
        openCount += 1
      }
      netVariance += v
      totalAbsDiscrepancy += Math.abs(v)
    })

    return {
      totalCount: totalItems,
      openCount,
      netVariance,
      totalAbsDiscrepancy,
    }
  }, [reconciliations, totalItems])

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
    setAccountFilter('ALL')
    setDispositionFilter('ALL')
    setStartDate('')
    setEndDate('')
    setCurrentPage(0)
  }

  // Handle successful creation
  const handleCreateSuccess = (newRec: CashReconciliationDto) => {
    setSuccessToast(
      `Đã ghi nhận đối soát thành công ngày ${newRec.reconciliationDate} cho tài khoản ${newRec.accountName}.`
    )
    setTimeout(() => setSuccessToast(null), 5000)
    fetchReconciliations()
    if (onRefreshAccounts) onRefreshAccounts()
  }

  // Handle successful explanation
  const handleExplainSuccess = (updated: CashReconciliationDto) => {
    setSuccessToast(
      `Đã cập nhật giải trình đối soát ngày ${updated.reconciliationDate} (Trạng thái: ĐÃ GIẢI TRÌNH).`
    )
    setTimeout(() => setSuccessToast(null), 5000)
    fetchReconciliations()
  }

  // Handle successful adjustment
  const handleAdjustSuccess = (updated: CashReconciliationDto) => {
    setSuccessToast(
      `Đã duyệt điều chỉnh số dư ngày ${updated.reconciliationDate} thành công. Bút toán đã hạch toán vào sổ quỹ chi nhánh (TC-012).`
    )
    setTimeout(() => setSuccessToast(null), 5000)
    fetchReconciliations()
    if (onRefreshAccounts) onRefreshAccounts()
  }

  // Handle closing reconciliation
  const handleCloseReconciliation = async (item: CashReconciliationDto) => {
    if (!branchId) return
    const confirmed = window.confirm(
      `Bạn có chắc chắn muốn đóng đối soát ngày ${item.reconciliationDate} của tài khoản ${item.accountName}? Trạng thái sẽ chuyển sang ĐÃ ĐÓNG (CLOSED).`
    )
    if (!confirmed) return

    try {
      setClosingId(item.id)
      setErrorNotice(null)
      await financeApi.closeReconciliation(branchId, item.id)
      setSuccessToast(`Đã đóng đối soát ngày ${item.reconciliationDate} thành công.`)
      setTimeout(() => setSuccessToast(null), 5000)
      fetchReconciliations()
    } catch (err: unknown) {
      console.error('Failed to close reconciliation:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string } }
      }
      setErrorNotice(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể đóng bản ghi đối soát. Vui lòng kiểm tra quyền hạn Owner.'
      )
    } finally {
      setClosingId(null)
    }
  }

  const getDispositionBadge = (disposition: ReconciliationDisposition) => {
    const config = RECONCILIATION_DISPOSITION_BADGES[disposition] || {
      label: disposition,
      badgeClass: 'bg-slate-100 text-slate-700 border-slate-300',
      dotClass: 'bg-slate-400',
    }

    return (
      <Badge
        variant="outline"
        className={`font-semibold text-[11px] flex items-center gap-1.5 py-0.5 px-2 ${config.badgeClass}`}
      >
        <span className={`w-1.5 h-1.5 rounded-full ${config.dotClass}`} />
        <span>{config.label}</span>
      </Badge>
    )
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

      {/* 1. KPI Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* KPI 1: Tổng số đợt đối soát */}
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs hover:border-slate-300 transition-all">
          <div className="flex items-center justify-between text-slate-500 mb-1.5">
            <span className="text-xs font-semibold">Tổng số đợt đối soát</span>
            <div className="w-7 h-7 rounded-lg bg-[#304638]/10 text-[#304638] flex items-center justify-center">
              <Scale className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-extrabold text-slate-900 font-mono">
            {kpiData.totalCount}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">
            Số lần kiểm kê tiền mặt & ngân hàng chi nhánh
          </p>
        </div>

        {/* KPI 2: Chênh lệch chờ xử lý (OPEN) */}
        <div
          onClick={() => {
            setDispositionFilter((prev) => (prev === 'OPEN' ? 'ALL' : 'OPEN'))
            setCurrentPage(0)
          }}
          className={`p-4 rounded-2xl border shadow-xs transition-all cursor-pointer ${
            dispositionFilter === 'OPEN'
              ? 'bg-amber-100/70 border-amber-400 ring-2 ring-amber-400/50'
              : 'bg-white border-slate-200 hover:border-amber-300'
          }`}
          title="Nhấp để lọc nhanh các đợt đối soát chờ xử lý (TC-012)"
        >
          <div className="flex items-center justify-between text-slate-500 mb-1.5">
            <div className="flex items-center gap-1.5">
              <span className="text-xs font-bold text-amber-900">Chênh lệch chờ xử lý</span>
              <Badge className="bg-amber-500 text-white text-[10px] py-0 px-1 font-mono">
                {kpiData.openCount}
              </Badge>
            </div>
            <div className="w-7 h-7 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center">
              <Clock className="w-4 h-4 text-amber-600" />
            </div>
          </div>
          <div className="text-2xl font-extrabold text-amber-900 font-mono">
            {kpiData.openCount} đợt
          </div>
          <p className="text-[11px] text-amber-700 mt-1">
            Trạng thái OPEN cần giải trình hoặc điều chỉnh sổ
          </p>
        </div>

        {/* KPI 3: Giá trị chênh lệch ròng (Net Variance) */}
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs hover:border-slate-300 transition-all">
          <div className="flex items-center justify-between text-slate-500 mb-1.5">
            <span className="text-xs font-semibold">Chênh lệch ròng trang này</span>
            <div
              className={`w-7 h-7 rounded-lg flex items-center justify-center ${
                kpiData.netVariance < 0
                  ? 'bg-rose-100 text-rose-700'
                  : kpiData.netVariance > 0
                  ? 'bg-emerald-100 text-emerald-700'
                  : 'bg-slate-100 text-slate-600'
              }`}
            >
              {kpiData.netVariance < 0 ? (
                <TrendingDown className="w-4 h-4" />
              ) : kpiData.netVariance > 0 ? (
                <TrendingUp className="w-4 h-4" />
              ) : (
                <Equal className="w-4 h-4" />
              )}
            </div>
          </div>
          <div
            className={`text-2xl font-extrabold font-mono ${
              kpiData.netVariance < 0
                ? 'text-rose-600'
                : kpiData.netVariance > 0
                ? 'text-emerald-700'
                : 'text-slate-700'
            }`}
          >
            {formatSignedVnd(kpiData.netVariance)}
          </div>
          <p className="text-[11px] text-slate-400 mt-1">
            Tổng giá trị lệch tuyệt đối: {formatVnd(kpiData.totalAbsDiscrepancy)}
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
            <span>Thực hiện đối soát quỹ</span>
          </Button>
        </div>

        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => fetchReconciliations()}
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
            <span>Bộ lọc lịch sử đối soát</span>
          </div>
          {(accountFilter !== 'ALL' ||
            dispositionFilter !== 'ALL' ||
            startDate ||
            endDate) && (
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

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 text-xs">
          {/* Lọc tài khoản */}
          <div className="space-y-1">
            <label className="text-slate-500 font-medium">Tài khoản quỹ</label>
            <select
              value={accountFilter}
              onChange={(e) => {
                setAccountFilter(e.target.value)
                setCurrentPage(0)
              }}
              className="w-full rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs text-slate-700 font-medium focus:outline-none focus:ring-1 focus:ring-[#304638]"
            >
              <option value="ALL">Tất cả tài khoản ({accounts.length})</option>
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.accountName} ({a.accountKind === 'CASH' ? 'Tiền mặt' : 'Ngân hàng'})
                </option>
              ))}
            </select>
          </div>

          {/* Lọc trạng thái xử lý */}
          <div className="space-y-1">
            <label className="text-slate-500 font-medium">Trạng thái xử lý (Disposition)</label>
            <select
              value={dispositionFilter}
              onChange={(e) => {
                setDispositionFilter(e.target.value)
                setCurrentPage(0)
              }}
              className="w-full rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-xs text-slate-700 font-medium focus:outline-none focus:ring-1 focus:ring-[#304638]"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="OPEN">Chờ xử lý (OPEN)</option>
              <option value="EXPLAINED">Đã giải trình (EXPLAINED)</option>
              <option value="ADJUSTED">Đã điều chỉnh sổ (ADJUSTED)</option>
              <option value="CLOSED">Đã đóng (CLOSED)</option>
            </select>
          </div>

          {/* Date Shortcuts inline */}
          <div className="space-y-1 sm:col-span-2 lg:col-span-1">
            <label className="text-slate-500 font-medium">Chọn nhanh khoảng ngày</label>
            <div className="flex items-center gap-1.5 pt-0.5">
              <button
                type="button"
                onClick={() => handleQuickDateFilter('today')}
                className="px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-[11px] font-medium text-slate-700 transition-colors cursor-pointer"
              >
                Hôm nay
              </button>
              <button
                type="button"
                onClick={() => handleQuickDateFilter('7days')}
                className="px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-[11px] font-medium text-slate-700 transition-colors cursor-pointer"
              >
                7 ngày qua
              </button>
              <button
                type="button"
                onClick={() => handleQuickDateFilter('thisMonth')}
                className="px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-[11px] font-medium text-slate-700 transition-colors cursor-pointer"
              >
                Tháng này
              </button>
            </div>
          </div>
        </div>

        {/* Date Range Picker */}
        <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-slate-100 text-xs">
          <span className="text-slate-500 text-[11px] font-medium flex items-center gap-1">
            <Calendar className="w-3.5 h-3.5 text-slate-400" />
            Khoảng ngày đối soát:
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
      </div>

      {/* 4. Reconciliations Table */}
      <div className="rounded-2xl border border-slate-200 bg-white shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-slate-50/80 border-b border-slate-200 text-slate-600 font-semibold uppercase tracking-wider text-[11px]">
                <th className="py-3 px-4">Ngày đối soát</th>
                <th className="py-3 px-4">Tài khoản quỹ</th>
                <th className="py-3 px-3 text-right">Số dư sổ sách</th>
                <th className="py-3 px-3 text-right">Số kiểm kê thực tế</th>
                <th className="py-3 px-4 text-right">Chênh lệch</th>
                <th className="py-3 px-3">Trạng thái</th>
                <th className="py-3 px-4">Người lập / Người duyệt</th>
                <th className="py-3 px-4 text-right min-w-[200px]">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-400">
                    <Loader2 className="w-6 h-6 animate-spin mx-auto text-[#304638] mb-2" />
                    <span>Đang tải danh sách đối soát kiểm kê...</span>
                  </td>
                </tr>
              ) : reconciliations.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-slate-400">
                    <Scale className="w-8 h-8 mx-auto text-slate-300 mb-2" />
                    <p className="font-medium text-slate-600 text-sm">
                      Chưa có bản ghi đối soát kiểm kê nào
                    </p>
                    <p className="text-xs text-slate-400 mt-1">
                      Nhấn &quot;Thực hiện đối soát quỹ&quot; để ghi nhận đợt kiểm kê tiền mặt hoặc ngân hàng.
                    </p>
                  </td>
                </tr>
              ) : (
                reconciliations.map((item) => {
                  const varNum =
                    typeof item.varianceAmount === 'string'
                      ? parseFloat(item.varianceAmount) || 0
                      : item.varianceAmount
                  const isShortage = varNum < 0
                  const isSurplus = varNum > 0
                  const isMatched = varNum === 0

                  return (
                    <tr key={item.id} className="hover:bg-slate-50/80 transition-colors">
                      {/* Ngày đối soát */}
                      <td className="py-3.5 px-4 font-mono font-semibold text-slate-800 whitespace-nowrap">
                        <div className="flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5 text-slate-400" />
                          <span>{item.reconciliationDate}</span>
                        </div>
                      </td>

                      {/* Tài khoản quỹ */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <div className="flex items-center gap-2">
                          {item.accountKind === 'CASH' ? (
                            <Wallet className="w-4 h-4 text-emerald-600 shrink-0" />
                          ) : (
                            <Building2 className="w-4 h-4 text-blue-600 shrink-0" />
                          )}
                          <div>
                            <span className="font-semibold text-slate-800 block">
                              {item.accountName}
                            </span>
                            <span className="text-[10px] text-slate-400 font-mono">
                              {item.accountKind === 'CASH' ? 'Quỹ tiền mặt' : 'Tài khoản ngân hàng'}
                            </span>
                          </div>
                        </div>
                      </td>

                      {/* Số dư sổ sách */}
                      <td className="py-3.5 px-3 text-right font-mono font-medium text-slate-700 whitespace-nowrap">
                        {formatVnd(item.recordedBalance)}
                      </td>

                      {/* Số kiểm kê thực tế */}
                      <td className="py-3.5 px-3 text-right font-mono font-bold text-slate-900 whitespace-nowrap">
                        {formatVnd(item.observedBalance)}
                      </td>

                      {/* Chênh lệch */}
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <span
                          className={`font-mono font-bold text-xs ${
                            isShortage
                              ? 'text-rose-600'
                              : isSurplus
                              ? 'text-emerald-700'
                              : 'text-slate-500'
                          }`}
                        >
                          {formatSignedVnd(varNum)}
                        </span>
                        {isShortage && (
                          <span className="block text-[10px] text-rose-500 font-medium">Thiếu quỹ</span>
                        )}
                        {isSurplus && (
                          <span className="block text-[10px] text-emerald-600 font-medium">Thừa quỹ</span>
                        )}
                        {isMatched && (
                          <span className="block text-[10px] text-slate-400">Khớp số dư</span>
                        )}
                      </td>

                      {/* Trạng thái */}
                      <td className="py-3.5 px-3 whitespace-nowrap">
                        {getDispositionBadge(item.disposition)}
                        {item.adjustmentLedgerEntryId && (
                          <span className="block text-[10px] font-mono text-emerald-700 mt-0.5">
                            Sổ quỹ: #{item.adjustmentLedgerEntryId.slice(0, 8)}
                          </span>
                        )}
                      </td>

                      {/* Người lập / Người duyệt */}
                      <td className="py-3.5 px-4 text-slate-700 max-w-xs">
                        <div className="space-y-0.5">
                          <div className="text-[11px] text-slate-600 truncate" title={item.createdByName || ''}>
                            <span className="text-slate-400">Lập:</span> {item.createdByName || 'Hệ thống'}
                          </div>
                          {item.confirmedByName && (
                            <div className="text-[11px] text-emerald-800 font-medium truncate" title={item.confirmedByName}>
                              <span className="text-slate-400 font-normal">Duyệt:</span> {item.confirmedByName}
                            </div>
                          )}
                          {item.note && (
                            <div className="text-[10px] text-slate-500 italic line-clamp-1" title={item.note}>
                              &quot;{item.note}&quot;
                            </div>
                          )}
                        </div>
                      </td>

                      {/* Hành động */}
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <div className="flex items-center justify-end gap-1.5 flex-wrap">
                          {/* 1. Giải trình (Nút cho trạng thái OPEN) */}
                          {item.disposition === 'OPEN' && (
                            <button
                              type="button"
                              onClick={() => setExplainingItem(item)}
                              className="px-2.5 py-1 rounded-lg bg-blue-50 text-blue-700 hover:bg-blue-100 border border-blue-200 text-xs font-semibold transition-colors cursor-pointer flex items-center gap-1"
                              title="Cập nhật giải trình và đính kèm chứng từ"
                            >
                              <FileText className="w-3 h-3" />
                              <span>Giải trình</span>
                            </button>
                          )}

                          {/* 2. Duyệt điều chỉnh (Nút cho OPEN hoặc EXPLAINED khi có chênh lệch != 0) */}
                          {(item.disposition === 'OPEN' || item.disposition === 'EXPLAINED') &&
                            varNum !== 0 && (
                              <button
                                type="button"
                                onClick={() => setAdjustingItem(item)}
                                className="px-2.5 py-1 rounded-lg bg-amber-50 text-amber-800 hover:bg-amber-100 border border-amber-300 text-xs font-semibold transition-colors cursor-pointer flex items-center gap-1"
                                title="Hạch toán điều chỉnh vào sổ quỹ chi nhánh (TC-012)"
                              >
                                <Scale className="w-3 h-3" />
                                <span>Duyệt điều chỉnh</span>
                              </button>
                            )}

                          {/* 3. Đóng đối soát (Cho EXPLAINED hoặc khi khớp số dư variance == 0) */}
                          {item.disposition !== 'CLOSED' &&
                            (item.disposition === 'EXPLAINED' || item.disposition === 'ADJUSTED' || varNum === 0) && (
                              <button
                                type="button"
                                onClick={() => handleCloseReconciliation(item)}
                                disabled={closingId === item.id}
                                className="px-2.5 py-1 rounded-lg bg-slate-100 text-slate-700 hover:bg-slate-200 border border-slate-300 text-xs font-semibold transition-colors cursor-pointer flex items-center gap-1"
                                title="Đóng và hoàn tất kỳ đối soát"
                              >
                                {closingId === item.id ? (
                                  <Loader2 className="w-3 h-3 animate-spin" />
                                ) : (
                                  <Lock className="w-3 h-3" />
                                )}
                                <span>Đóng đối soát</span>
                              </button>
                            )}

                          {/* 4. Xem chứng từ nếu có */}
                          {(item.evidenceDownloadUrl || item.evidenceFileId) && (
                            <a
                              href={item.evidenceDownloadUrl || `/api/v1/evidence/${item.evidenceFileId}/download`}
                              target="_blank"
                              rel="noreferrer"
                              className="p-1 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100 transition-colors"
                              title="Tải / Xem chứng từ đính kèm"
                            >
                              <ExternalLink className="w-3.5 h-3.5" />
                            </a>
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

        {/* Pagination Controls */}
        {totalPages > 1 && (
          <div className="p-3 border-t border-slate-200 bg-slate-50/50 flex items-center justify-between text-xs text-slate-600">
            <span className="font-mono text-slate-500">
              Tổng cộng: <strong>{totalItems}</strong> đợt đối soát
            </span>
            <div className="flex items-center gap-2">
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
      <CreateReconciliationModal
        isOpen={isCreateModalOpen}
        branchId={branchId}
        branchName={branchName}
        accounts={accounts}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={handleCreateSuccess}
      />

      <ExplainDiscrepancyModal
        isOpen={!!explainingItem}
        branchId={branchId}
        reconciliation={explainingItem}
        onClose={() => setExplainingItem(null)}
        onSuccess={handleExplainSuccess}
      />

      <AdjustVarianceModal
        isOpen={!!adjustingItem}
        branchId={branchId}
        reconciliation={adjustingItem}
        onClose={() => setAdjustingItem(null)}
        onSuccess={handleAdjustSuccess}
      />
    </div>
  )
}

export default ReconciliationTab
