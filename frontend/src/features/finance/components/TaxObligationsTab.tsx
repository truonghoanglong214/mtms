import React, { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Landmark,
  Plus,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  Calendar,
  Filter,
  RotateCcw,
  Building2,
  Clock,
  Receipt,
  ChevronLeft,
  ChevronRight,
  Loader2,
  Trash2,
  Eye,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Badge } from '@/shared/components/ui/badge'
import {
  financeApi,
  formatVnd,
  getTaxObligationItems,
  getTaxObligationTotalElements,
} from '../api/financeApi'
import { CreateTaxObligationModal } from './CreateTaxObligationModal'
import { PayTaxModal } from './PayTaxModal'
import { TaxObligationDetailModal } from './TaxObligationDetailModal'
import {
  TAX_OBLIGATION_TYPE_LABELS,
  TAX_OBLIGATION_STATUS_BADGES,
  type TaxObligationDto,
  type TaxObligationType,
  type FinancialAccountDto,
} from '../types/finance.types'

interface TaxObligationsTabProps {
  branchId: string
  branchName?: string
  accounts: FinancialAccountDto[]
  onRefreshAccounts?: () => void
}

export const TaxObligationsTab: React.FC<TaxObligationsTabProps> = ({
  branchId,
  branchName,
  accounts,
  onRefreshAccounts,
}) => {
  const [obligations, setObligations] = useState<TaxObligationDto[]>([])
  const [totalItems, setTotalItems] = useState<number>(0)
  const [totalPages, setTotalPages] = useState<number>(1)
  const [currentPage, setCurrentPage] = useState<number>(0)
  const [pageSize] = useState<number>(15)

  const [isLoading, setIsLoading] = useState<boolean>(false)
  const [errorNotice, setErrorNotice] = useState<string | null>(null)
  const [successToast, setSuccessToast] = useState<string | null>(null)

  // Filters
  const [typeFilter, setTypeFilter] = useState<string>('ALL')
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')

  // Modals state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false)
  const [payingObligation, setPayingObligation] = useState<TaxObligationDto | null>(null)
  const [selectedDetailId, setSelectedDetailId] = useState<string | null>(null)
  const [cancellingId, setCancellingId] = useState<string | null>(null)

  // Fetch obligations list
  const fetchObligations = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    setErrorNotice(null)

    try {
      const res = await financeApi.getTaxObligations(branchId, {
        obligationType: typeFilter !== 'ALL' ? typeFilter : undefined,
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        page: currentPage,
        size: pageSize,
      })

      const items = getTaxObligationItems(res)
      setObligations(items)
      const count = getTaxObligationTotalElements(res)
      setTotalItems(count)
      setTotalPages(res.totalPages || Math.ceil(count / pageSize) || 1)
    } catch (err: unknown) {
      console.error('Failed to load tax obligations:', err)
      setErrorNotice('Không thể tải danh sách nghĩa vụ thuế. Vui lòng thử lại.')
    } finally {
      setIsLoading(false)
    }
  }, [branchId, typeFilter, statusFilter, startDate, endDate, currentPage, pageSize])

  useEffect(() => {
    fetchObligations()
  }, [fetchObligations])

  // KPI Metrics calculated from current records or branch aggregates
  const kpis = useMemo(() => {
    let totalDue = 0
    let totalPaid = 0
    let totalRemaining = 0
    let unsettledCount = 0

    obligations.forEach((item) => {
      if (item.status !== 'CANCELLED') {
        const due = typeof item.amountDue === 'number' ? item.amountDue : parseFloat(item.amountDue) || 0
        const paid = typeof item.amountPaid === 'number' ? item.amountPaid : parseFloat(item.amountPaid) || 0
        const rem = typeof item.remainingLiability === 'number' ? item.remainingLiability : Math.max(0, due - paid)

        totalDue += due
        totalPaid += paid
        totalRemaining += rem

        if (item.status === 'OPEN' || item.status === 'PARTIALLY_PAID') {
          unsettledCount++
        }
      }
    })

    return { totalDue, totalPaid, totalRemaining, unsettledCount }
  }, [obligations])

  const handleResetFilters = () => {
    setTypeFilter('ALL')
    setStatusFilter('ALL')
    setStartDate('')
    setEndDate('')
    setCurrentPage(0)
  }

  const handleCreateSuccess = (newObligation: TaxObligationDto) => {
    setSuccessToast(`Đã khai báo nghĩa vụ thuế ${newObligation.obligationCode} thành công.`)
    setTimeout(() => setSuccessToast(null), 5000)
    fetchObligations()
  }

  const handlePaySuccess = () => {
    setSuccessToast('Đã ghi nhận nộp tiền thuế vào Ngân sách Nhà nước thành công.')
    setTimeout(() => setSuccessToast(null), 5000)
    fetchObligations()
    if (onRefreshAccounts) {
      onRefreshAccounts()
    }
  }

  const handleCancelObligation = async (id: string, code: string) => {
    if (!window.confirm(`Bạn có chắc chắn muốn hủy hồ sơ thuế "${code}"?`)) {
      return
    }

    setCancellingId(id)
    try {
      await financeApi.cancelTaxObligation(branchId, id)
      setSuccessToast(`Đã hủy hồ sơ thuế ${code}.`)
      setTimeout(() => setSuccessToast(null), 5000)
      fetchObligations()
    } catch (err: unknown) {
      console.error('Failed to cancel tax obligation:', err)
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string } }
      }
      alert(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể hủy nghĩa vụ thuế.'
      )
    } finally {
      setCancellingId(null)
    }
  }

  return (
    <div className="space-y-5 animate-in fade-in duration-200">
      {/* Toast Notification */}
      {successToast && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs text-emerald-800 shadow-sm">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span className="font-medium">{successToast}</span>
          </div>
          <button
            type="button"
            onClick={() => setSuccessToast(null)}
            className="text-emerald-700 hover:text-emerald-900 cursor-pointer font-medium"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Error Notice */}
      {errorNotice && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 flex items-center gap-2 text-xs text-rose-800 shadow-sm">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
          <span>{errorNotice}</span>
        </div>
      )}

      {/* 1. KPI Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Tổng nghĩa vụ thuế */}
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-xs font-medium text-slate-500 block">Tổng nghĩa vụ thuế</span>
            <div className="text-xl font-bold font-mono text-slate-900 mt-1">
              {formatVnd(kpis.totalDue)}
            </div>
            <span className="text-[10px] text-slate-400 mt-0.5 block">
              Các khoản thuế đã phát sinh
            </span>
          </div>
          <div className="w-10 h-10 rounded-xl bg-slate-100 text-slate-700 flex items-center justify-center shrink-0">
            <Landmark className="w-5 h-5 text-slate-600" />
          </div>
        </div>

        {/* Card 2: Đã nộp vào NSNN */}
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-xs font-medium text-slate-500 block">Đã nộp vào NSNN</span>
            <div className="text-xl font-bold font-mono text-emerald-700 mt-1">
              {formatVnd(kpis.totalPaid)}
            </div>
            <span className="text-[10px] text-emerald-600 mt-0.5 block">
              Đã trích quỹ chuyển ngân sách
            </span>
          </div>
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-700 flex items-center justify-center shrink-0 border border-emerald-100">
            <CheckCircle2 className="w-5 h-5 text-emerald-600" />
          </div>
        </div>

        {/* Card 3: Còn phải nộp */}
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-xs font-medium text-slate-500 block">Còn phải nộp</span>
            <div
              className={`text-xl font-bold font-mono mt-1 ${
                kpis.totalRemaining > 0 ? 'text-rose-600' : 'text-slate-900'
              }`}
            >
              {formatVnd(kpis.totalRemaining)}
            </div>
            <span className="text-[10px] text-slate-400 mt-0.5 block">
              Nghĩa vụ chưa hoàn thành
            </span>
          </div>
          <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-700 flex items-center justify-center shrink-0 border border-amber-100">
            <Clock className="w-5 h-5 text-amber-600" />
          </div>
        </div>

        {/* Card 4: Nghĩa vụ chưa tất toán */}
        <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs flex items-center justify-between">
          <div>
            <span className="text-xs font-medium text-slate-500 block">Chưa tất toán</span>
            <div className="text-xl font-bold font-mono text-slate-900 mt-1">
              {kpis.unsettledCount} <span className="text-xs font-normal text-slate-500">hồ sơ</span>
            </div>
            <span className="text-[10px] text-slate-400 mt-0.5 block">
              Chưa nộp hoặc nộp một phần
            </span>
          </div>
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-700 flex items-center justify-center shrink-0 border border-blue-100">
            <Receipt className="w-5 h-5 text-blue-600" />
          </div>
        </div>
      </div>

      {/* 2. Filter Toolbar & Action Buttons */}
      <div className="p-4 rounded-2xl bg-white border border-slate-200 shadow-xs space-y-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Filter className="w-4 h-4 text-slate-400" />
            <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
              Bộ Lọc Hồ Sơ Thuế
            </h3>
          </div>

          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleResetFilters}
              className="text-xs text-slate-600 h-8 flex items-center gap-1.5"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Đặt lại</span>
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => fetchObligations()}
              disabled={isLoading}
              className="text-xs text-slate-600 h-8 flex items-center gap-1.5"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
              <span>Tải lại</span>
            </Button>
            <Button
              type="button"
              size="sm"
              onClick={() => setIsCreateModalOpen(true)}
              className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold h-8 flex items-center gap-1.5 shadow-xs"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Khai Báo Nghĩa Vụ Thuế</span>
            </Button>
          </div>
        </div>

        {/* Filter controls row */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 pt-2 border-t border-slate-100">
          {/* Loại thuế */}
          <div className="space-y-1">
            <label className="text-[11px] font-semibold text-slate-600">Loại thuế / Phí</label>
            <select
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value)
                setCurrentPage(0)
              }}
              className="w-full rounded-xl border border-slate-200 bg-white px-2.5 py-1.5 text-xs text-slate-800 font-medium focus:outline-none focus:ring-1 focus:ring-[#304638]"
            >
              <option value="ALL">Tất cả loại thuế</option>
              {(Object.keys(TAX_OBLIGATION_TYPE_LABELS) as TaxObligationType[]).map((key) => (
                <option key={key} value={key}>
                  {TAX_OBLIGATION_TYPE_LABELS[key]}
                </option>
              ))}
            </select>
          </div>

          {/* Trạng thái */}
          <div className="space-y-1">
            <label className="text-[11px] font-semibold text-slate-600">Trạng thái nộp</label>
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value)
                setCurrentPage(0)
              }}
              className="w-full rounded-xl border border-slate-200 bg-white px-2.5 py-1.5 text-xs text-slate-800 font-medium focus:outline-none focus:ring-1 focus:ring-[#304638]"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="OPEN">Chưa nộp (OPEN)</option>
              <option value="PARTIALLY_PAID">Đã nộp một phần (PARTIALLY_PAID)</option>
              <option value="PAID">Đã hoàn thành (PAID)</option>
              <option value="CANCELLED">Đã hủy (CANCELLED)</option>
            </select>
          </div>

          {/* Từ ngày */}
          <div className="space-y-1">
            <label className="text-[11px] font-semibold text-slate-600">Kỳ thuế từ ngày</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value)
                setCurrentPage(0)
              }}
              className="w-full rounded-xl border border-slate-200 bg-white px-2.5 py-1.5 text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-[#304638]"
            />
          </div>

          {/* Đến ngày */}
          <div className="space-y-1">
            <label className="text-[11px] font-semibold text-slate-600">Kỳ thuế đến ngày</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value)
                setCurrentPage(0)
              }}
              className="w-full rounded-xl border border-slate-200 bg-white px-2.5 py-1.5 text-xs text-slate-800 focus:outline-none focus:ring-1 focus:ring-[#304638]"
            />
          </div>
        </div>
      </div>

      {/* 3. Data Table */}
      <div className="rounded-2xl border border-slate-200 bg-white shadow-xs overflow-hidden">
        {isLoading && (
          <div className="p-8 flex items-center justify-center text-slate-400 gap-2">
            <Loader2 className="w-5 h-5 animate-spin text-[#304638]" />
            <span className="text-xs">Đang tải danh sách nghĩa vụ thuế...</span>
          </div>
        )}

        {!isLoading && obligations.length === 0 && (
          <div className="p-12 text-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-slate-100 text-slate-400 flex items-center justify-center mx-auto">
              <Landmark className="w-6 h-6" />
            </div>
            <div className="space-y-1">
              <h4 className="text-sm font-bold text-slate-800">Chưa có hồ sơ nghĩa vụ thuế nào</h4>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                Bấm vào nút "Khai Báo Nghĩa Vụ Thuế" ở trên để ghi nhận nghĩa vụ thuế môn bài, thuế GTGT, thuế TNDN hoặc lệ phí phát sinh.
              </p>
            </div>
            <Button
              type="button"
              onClick={() => setIsCreateModalOpen(true)}
              className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold"
            >
              <Plus className="w-3.5 h-3.5 mr-1" />
              Khai Báo Nghĩa Vụ Thuế Mới
            </Button>
          </div>
        )}

        {!isLoading && obligations.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 font-semibold uppercase text-[10px] tracking-wider border-b border-slate-200">
                <tr>
                  <th className="px-4 py-3">Mã hồ sơ / Loại thuế</th>
                  <th className="px-4 py-3">Phạm vi / Cơ quan quản lý</th>
                  <th className="px-4 py-3">Kỳ tính thuế & Hạn nộp</th>
                  <th className="px-4 py-3 text-right">Số phải nộp</th>
                  <th className="px-4 py-3 text-right">Đã nộp NSNN</th>
                  <th className="px-4 py-3 text-right">Còn phải nộp</th>
                  <th className="px-4 py-3 text-center">Trạng thái</th>
                  <th className="px-4 py-3 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {obligations.map((item) => {
                  const statusBadge = TAX_OBLIGATION_STATUS_BADGES[item.status]
                  const isCancelable = item.amountPaid === 0 && item.status === 'OPEN'
                  const isPayable = item.remainingLiability > 0 && item.status !== 'CANCELLED'

                  return (
                    <tr
                      key={item.id}
                      className="hover:bg-slate-50/70 transition-colors cursor-pointer"
                      onClick={() => setSelectedDetailId(item.id)}
                    >
                      {/* Mã hồ sơ / Loại thuế */}
                      <td className="px-4 py-3.5">
                        <div className="font-bold text-slate-900 font-mono flex items-center gap-1.5">
                          <span>{item.obligationCode}</span>
                        </div>
                        <div className="text-[11px] text-slate-500 font-medium mt-0.5">
                          {TAX_OBLIGATION_TYPE_LABELS[item.obligationType]}
                        </div>
                      </td>

                      {/* Phạm vi & Cơ quan thuế */}
                      <td className="px-4 py-3.5">
                        <div className="flex items-center gap-1 text-slate-800 font-medium">
                          <Building2 className="w-3 h-3 text-slate-400 shrink-0" />
                          <span className="truncate max-w-[140px]">
                            {item.branchName || (item.branchId ? 'Chi nhánh riêng' : 'Toàn pháp nhân')}
                          </span>
                        </div>
                        <div className="text-[11px] text-slate-500 truncate max-w-[140px] mt-0.5">
                          {item.taxAuthorityName || '—'}
                        </div>
                      </td>

                      {/* Kỳ tính thuế & Hạn nộp */}
                      <td className="px-4 py-3.5 font-mono text-[11px]">
                        <div className="text-slate-800 flex items-center gap-1">
                          <Calendar className="w-3 h-3 text-slate-400 shrink-0" />
                          <span>{item.taxPeriodStart} → {item.taxPeriodEnd}</span>
                        </div>
                        <div className="text-slate-500 flex items-center gap-1 mt-0.5">
                          <Clock className="w-3 h-3 text-slate-400 shrink-0" />
                          <span>Hạn: {item.dueDate || 'Chưa định'}</span>
                        </div>
                      </td>

                      {/* Số phải nộp */}
                      <td className="px-4 py-3.5 text-right font-bold font-mono text-slate-900">
                        {formatVnd(item.amountDue)}
                      </td>

                      {/* Đã nộp */}
                      <td className="px-4 py-3.5 text-right font-bold font-mono text-emerald-700">
                        {formatVnd(item.amountPaid)}
                      </td>

                      {/* Còn phải nộp */}
                      <td
                        className={`px-4 py-3.5 text-right font-bold font-mono ${
                          item.remainingLiability > 0 ? 'text-rose-600' : 'text-slate-700'
                        }`}
                      >
                        {formatVnd(item.remainingLiability)}
                      </td>

                      {/* Trạng thái */}
                      <td className="px-4 py-3.5 text-center">
                        {statusBadge && (
                          <Badge
                            variant="outline"
                            className={`${statusBadge.badgeClass} text-[10px] font-medium border whitespace-nowrap`}
                          >
                            {statusBadge.label}
                          </Badge>
                        )}
                      </td>

                      {/* Thao tác */}
                      <td
                        className="px-4 py-3.5 text-right whitespace-nowrap space-x-1"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => setSelectedDetailId(item.id)}
                          className="h-7 px-2 text-slate-600 hover:text-slate-900 text-xs"
                          title="Xem chi tiết"
                        >
                          <Eye className="w-3.5 h-3.5" />
                        </Button>

                        {isPayable && (
                          <Button
                            type="button"
                            size="sm"
                            onClick={() => setPayingObligation(item)}
                            className="bg-[#304638] hover:bg-[#25372C] text-white h-7 px-2.5 text-xs font-semibold"
                            title="Nộp thuế"
                          >
                            <Receipt className="w-3.5 h-3.5 mr-1" />
                            <span>Nộp</span>
                          </Button>
                        )}

                        {isCancelable && (
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            disabled={cancellingId === item.id}
                            onClick={() => handleCancelObligation(item.id, item.obligationCode)}
                            className="h-7 px-2 text-rose-600 hover:text-rose-700 hover:bg-rose-50 text-xs"
                            title="Hủy hồ sơ"
                          >
                            {cancellingId === item.id ? (
                              <Loader2 className="w-3.5 h-3.5 animate-spin" />
                            ) : (
                              <Trash2 className="w-3.5 h-3.5" />
                            )}
                          </Button>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination controls */}
        {totalItems > 0 && (
          <div className="px-4 py-3 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs text-slate-600">
            <div>
              Hiển thị <span className="font-semibold text-slate-900">{obligations.length}</span> /{' '}
              <span className="font-semibold text-slate-900">{totalItems}</span> hồ sơ thuế
            </div>
            <div className="flex items-center gap-2">
              <span className="text-[11px] text-slate-500 font-mono">
                Trang {currentPage + 1} / {totalPages}
              </span>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={currentPage === 0 || isLoading}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                className="h-7 w-7 p-0"
              >
                <ChevronLeft className="w-3.5 h-3.5" />
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={currentPage >= totalPages - 1 || isLoading}
                onClick={() => setCurrentPage((p) => p + 1)}
                className="h-7 w-7 p-0"
              >
                <ChevronRight className="w-3.5 h-3.5" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* 4. Modals */}
      <CreateTaxObligationModal
        isOpen={isCreateModalOpen}
        branchId={branchId}
        branchName={branchName}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={handleCreateSuccess}
      />

      <PayTaxModal
        isOpen={!!payingObligation}
        branchId={branchId}
        obligation={payingObligation}
        accounts={accounts}
        onClose={() => setPayingObligation(null)}
        onSuccess={handlePaySuccess}
      />

      <TaxObligationDetailModal
        isOpen={!!selectedDetailId}
        branchId={branchId}
        obligationId={selectedDetailId}
        onClose={() => setSelectedDetailId(null)}
        onOpenPayModal={(obl) => setPayingObligation(obl)}
        onObligationUpdated={fetchObligations}
      />
    </div>
  )
}
