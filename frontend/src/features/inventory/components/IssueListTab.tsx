import React, { useState, useEffect, useCallback } from 'react'
import {
  Search,
  Plus,
  RefreshCw,
  AlertCircle,
  Eye,
  CheckCircle2,
  Clock,
  ChevronLeft,
  ChevronRight,
  Loader2,
  Package,
  Coins,
  FileText,
  X,
  UserCheck,
} from 'lucide-react'
import { Button } from '@/shared/components/ui/button'
import { Input } from '@/shared/components/ui/input'
import { Badge } from '@/shared/components/ui/badge'
import { issueApi } from '../api/issueApi'
import { CreateIssueModal } from './CreateIssueModal'
import { IssueDetailModal } from './IssueDetailModal'
import type {
  StockDocumentDto,
  StockDocumentType,
  StockDocumentStatus,
  IssueFilterParams,
} from '../types/issue.types'
import type { CatalogItemDto } from '../types/catalog.types'

interface IssueListTabProps {
  entityId: string
  branchId: string
  branchName?: string
  items: CatalogItemDto[]
}

function formatVnd(value: string | number): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 2,
  }).format(num)
}

function formatQuantity(value: string | number): string {
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (isNaN(num)) return '0'
  return new Intl.NumberFormat('vi-VN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 3,
  }).format(num)
}

export const IssueListTab: React.FC<IssueListTabProps> = ({
  entityId,
  branchId,
  branchName,
  items,
}) => {
  const [documents, setDocuments] = useState<StockDocumentDto[]>([])
  const [totalItems, setTotalItems] = useState(0)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Filters
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [typeFilter, setTypeFilter] = useState<string>('ALL')
  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')
  const [page, setPage] = useState(0)
  const pageSize = 15

  // Modals
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [selectedDocIdForDetail, setSelectedDocIdForDetail] = useState<string | null>(null)

  // Quick post action state
  const [postingId, setPostingId] = useState<string | null>(null)

  const fetchDocuments = useCallback(async () => {
    if (!branchId) return
    setIsLoading(true)
    setError(null)
    try {
      const params: IssueFilterParams = {
        status: statusFilter !== 'ALL' ? (statusFilter as StockDocumentStatus) : undefined,
        documentType: typeFilter !== 'ALL' ? (typeFilter as StockDocumentType) : undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        search: searchTerm.trim() || undefined,
        page,
        size: pageSize,
      }
      const res = await issueApi.getIssueDocuments(branchId, params)
      setDocuments(res.items || [])
      setTotalItems(res.totalItems || 0)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { detail?: string; message?: string } } }
      setError(
        apiErr.response?.data?.detail ||
          apiErr.response?.data?.message ||
          'Không thể tải danh sách phiếu xuất kho.'
      )
    } finally {
      setIsLoading(false)
    }
  }, [branchId, statusFilter, typeFilter, startDate, endDate, searchTerm, page, pageSize])

  useEffect(() => {
    fetchDocuments()
  }, [fetchDocuments])

  // Handle Quick Post
  const handleQuickPost = async (doc: StockDocumentDto) => {
    if (!branchId || doc.status !== 'DRAFT') return
    const confirmed = window.confirm(
      `Bạn có chắc chắn muốn chốt ghi sổ phiếu xuất "${doc.documentNumber}"?\nHành động này sẽ trừ tồn kho theo Đơn giá vốn bình quân di động (Moving WAC).`
    )
    if (!confirmed) return

    setPostingId(doc.id)
    try {
      const updated = await issueApi.postIssueDocument(branchId, doc.id)
      setDocuments((prev) => prev.map((d) => (d.id === doc.id ? updated : d)))
    } catch (err: unknown) {
      const apiErr = err as {
        response?: { data?: { detail?: string; message?: string; type?: string } }
      }
      const problemType = apiErr.response?.data?.type
      const detail = apiErr.response?.data?.detail
      if (problemType === 'urn:mtms:error:insufficient-stock') {
        alert(
          detail ||
            'Tồn kho hiện tại không đủ để xuất mặt hàng trong phiếu này. Hệ thống không cho phép xuất âm (TC-021)!'
        )
      } else {
        alert(detail || 'Có lỗi xảy ra khi chốt ghi sổ phiếu xuất.')
      }
    } finally {
      setPostingId(null)
    }
  }

  const renderStatusBadge = (status: StockDocumentStatus) => {
    switch (status) {
      case 'POSTED':
        return (
          <Badge className="bg-emerald-100 text-emerald-800 border-emerald-300 font-semibold gap-1 text-[11px]">
            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
            Đã ghi sổ kho
          </Badge>
        )
      case 'DRAFT':
        return (
          <Badge className="bg-amber-100 text-amber-800 border-amber-300 font-semibold gap-1 text-[11px]">
            <Clock className="w-3 h-3 text-amber-600" />
            Bản nháp
          </Badge>
        )
      case 'SUBMITTED':
        return (
          <Badge className="bg-blue-100 text-blue-800 border-blue-300 font-semibold gap-1 text-[11px]">
            <UserCheck className="w-3 h-3 text-blue-600" />
            Đã trình duyệt
          </Badge>
        )
      case 'CANCELLED':
        return (
          <Badge className="bg-slate-100 text-slate-700 border-slate-300 font-semibold gap-1 text-[11px]">
            <X className="w-3 h-3 text-slate-500" />
            Đã hủy
          </Badge>
        )
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  const renderTypeBadge = (docType: StockDocumentType) => {
    switch (docType) {
      case 'CONSUMPTION_ISSUE':
        return (
          <Badge variant="outline" className="bg-emerald-50 text-emerald-800 border-emerald-200 text-[11px]">
            Xuất quầy bar pha chế
          </Badge>
        )
      case 'SCRAP_DISPOSAL':
        return (
          <Badge variant="outline" className="bg-rose-50 text-rose-800 border-rose-200 text-[11px]">
            Tiêu hủy hao hụt
          </Badge>
        )
      case 'ADJUSTMENT':
        return (
          <Badge variant="outline" className="bg-slate-100 text-slate-800 border-slate-200 text-[11px]">
            Điều chỉnh kiểm kê
          </Badge>
        )
      default:
        return <Badge variant="outline">{docType}</Badge>
    }
  }

  const totalPages = Math.ceil(totalItems / pageSize) || 1

  return (
    <div className="space-y-4">
      {/* Top Banner Notice (TC-020 & TC-022) */}
      <div className="bg-emerald-50/70 border border-emerald-200 rounded-xl p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 text-xs text-emerald-950">
        <div className="flex items-center gap-2.5">
          <div className="p-2 bg-emerald-100 text-emerald-800 rounded-lg">
            <Coins className="w-4 h-4" />
          </div>
          <div>
            <div className="font-semibold text-emerald-950 flex items-center gap-2">
              <span>Cơ chế xuất kho thủ công & Định giá vốn di động Moving WAC (TC-020, TC-021, TC-022)</span>
            </div>
            <p className="text-emerald-800 text-[11px]">
              Giá vốn ghi nhận được chốt tự động theo đơn giá bình quân di động hiện hành tại chi nhánh.
              Hệ thống khóa chặt với cơ chế kiểm soát số dư không cho phép xuất âm.
            </p>
          </div>
        </div>

        <Button
          onClick={() => setIsCreateOpen(true)}
          className="bg-[#304638] hover:bg-[#25372C] text-white text-xs font-semibold gap-1.5 shadow-sm shrink-0"
        >
          <Plus className="w-4 h-4" />
          <span>+ Xuất kho mới</span>
        </Button>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs space-y-3">
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-3">
          {/* Search */}
          <div className="relative md:col-span-2">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <Input
              type="text"
              placeholder="Tìm theo số phiếu, lý do xuất..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value)
                setPage(0)
              }}
              className="pl-8 text-xs h-9 bg-white"
            />
          </div>

          {/* Type Filter */}
          <div>
            <select
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value)
                setPage(0)
              }}
              className="w-full text-xs h-9 px-2.5 rounded-lg border border-slate-300 bg-white text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-600"
            >
              <option value="ALL">Tất cả loại phiếu</option>
              <option value="CONSUMPTION_ISSUE">Xuất quầy bar pha chế</option>
              <option value="SCRAP_DISPOSAL">Tiêu hủy hao hụt</option>
              <option value="ADJUSTMENT">Điều chỉnh kiểm kê âm</option>
            </select>
          </div>

          {/* Status Filter */}
          <div>
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value)
                setPage(0)
              }}
              className="w-full text-xs h-9 px-2.5 rounded-lg border border-slate-300 bg-white text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-600"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="POSTED">Đã ghi sổ kho (POSTED)</option>
              <option value="DRAFT">Bản nháp (DRAFT)</option>
              <option value="SUBMITTED">Đã trình duyệt (SUBMITTED)</option>
              <option value="CANCELLED">Đã hủy (CANCELLED)</option>
            </select>
          </div>

          {/* Date Range / Refresh */}
          <div className="flex items-center gap-2">
            <Input
              type="date"
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value)
                setPage(0)
              }}
              className="text-xs h-9 bg-white"
              title="Từ ngày"
            />
            <Input
              type="date"
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value)
                setPage(0)
              }}
              className="text-xs h-9 bg-white"
              title="Đến ngày"
            />
            <Button
              variant="outline"
              size="icon"
              onClick={fetchDocuments}
              className="h-9 w-9 shrink-0 text-slate-600 hover:text-slate-900 border-slate-300"
              title="Tải lại danh sách"
            >
              <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
            </Button>
          </div>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-center gap-3 text-rose-800 text-xs">
          <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
          <span>{error}</span>
          <Button
            size="sm"
            variant="outline"
            onClick={fetchDocuments}
            className="ml-auto text-xs text-rose-800 border-rose-300 hover:bg-rose-100"
          >
            Thử lại
          </Button>
        </div>
      )}

      {/* Documents Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
              <tr>
                <th className="py-3 px-4">Số phiếu / Ngày xuất</th>
                <th className="py-3 px-4">Loại phiếu</th>
                <th className="py-3 px-4">Lý do xuất</th>
                <th className="py-3 px-4 text-center">Số mặt hàng</th>
                <th className="py-3 px-4 text-right">Tổng lượng xuất</th>
                <th className="py-3 px-4 text-right">Tổng giá vốn (WAC)</th>
                <th className="py-3 px-4">Người lập / Ngày chốt</th>
                <th className="py-3 px-4 text-center">Trạng thái</th>
                <th className="py-3 px-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-slate-400">
                    <Loader2 className="w-6 h-6 animate-spin mx-auto text-emerald-700 mb-2" />
                    <span>Đang tải danh sách phiếu xuất kho...</span>
                  </td>
                </tr>
              ) : documents.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-12 text-center text-slate-400">
                    <Package className="w-8 h-8 mx-auto text-slate-300 mb-2" />
                    <p className="font-medium text-slate-600">Chưa có phiếu xuất kho nào</p>
                    <p className="text-[11px] text-slate-400 mt-1">
                      Nhấn nút &quot;+ Xuất kho mới&quot; để tạo phiếu xuất quầy bar pha chế đầu tiên.
                    </p>
                  </td>
                </tr>
              ) : (
                documents.map((doc) => (
                  <tr key={doc.id} className="hover:bg-slate-50/70 transition-colors">
                    {/* Doc number & date */}
                    <td className="py-3 px-4">
                      <div className="font-bold text-slate-900 font-mono flex items-center gap-1.5">
                        <FileText className="w-3.5 h-3.5 text-slate-400" />
                        <span>{doc.documentNumber}</span>
                      </div>
                      <div className="text-[11px] text-slate-500 font-mono mt-0.5">
                        {doc.documentDate}
                      </div>
                    </td>

                    {/* Document Type */}
                    <td className="py-3 px-4">{renderTypeBadge(doc.documentType)}</td>

                    {/* Reason */}
                    <td className="py-3 px-4">
                      <span className="text-slate-800 line-clamp-1">
                        {doc.reason || '—'}
                      </span>
                    </td>

                    {/* Total lines */}
                    <td className="py-3 px-4 text-center">
                      <Badge variant="outline" className="text-[11px] bg-slate-100 text-slate-700">
                        {doc.totalLines} dòng
                      </Badge>
                    </td>

                    {/* Total quantity */}
                    <td className="py-3 px-4 text-right font-mono font-semibold text-slate-800">
                      {formatQuantity(doc.totalQuantity)}
                    </td>

                    {/* Total booked cost (WAC) */}
                    <td className="py-3 px-4 text-right font-mono font-bold text-emerald-900">
                      {formatVnd(doc.totalBookedCost)}
                    </td>

                    {/* Creator / Post info */}
                    <td className="py-3 px-4">
                      <div className="text-slate-800 font-medium">
                        {doc.createdByName || '—'}
                      </div>
                      {doc.postedAt && (
                        <div className="text-[11px] text-emerald-700 flex items-center gap-1">
                          <CheckCircle2 className="w-3 h-3" />
                          <span>Đã ghi sổ</span>
                        </div>
                      )}
                    </td>

                    {/* Status */}
                    <td className="py-3 px-4 text-center">{renderStatusBadge(doc.status)}</td>

                    {/* Actions */}
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => setSelectedDocIdForDetail(doc.id)}
                          className="h-8 px-2 text-slate-600 hover:text-emerald-800 hover:bg-emerald-50 text-xs"
                          title="Xem chi tiết phiếu xuất"
                        >
                          <Eye className="w-3.5 h-3.5 mr-1" />
                          <span>Chi tiết</span>
                        </Button>

                        {doc.status === 'DRAFT' && (
                          <Button
                            size="sm"
                            variant="outline"
                            disabled={postingId === doc.id}
                            onClick={() => handleQuickPost(doc)}
                            className="h-8 px-2.5 text-xs font-semibold text-emerald-800 border-emerald-300 hover:bg-emerald-50 hover:text-emerald-900 gap-1"
                            title="Chốt ghi sổ phiếu này ngay"
                          >
                            {postingId === doc.id ? (
                              <Loader2 className="w-3.5 h-3.5 animate-spin" />
                            ) : (
                              <CheckCircle2 className="w-3.5 h-3.5" />
                            )}
                            <span>Ghi sổ</span>
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        {totalItems > 0 && (
          <div className="bg-slate-50 border-t border-slate-200 px-4 py-3 flex items-center justify-between text-xs text-slate-600">
            <div>
              Hiển thị{' '}
              <strong>
                {Math.min(page * pageSize + 1, totalItems)} - {Math.min((page + 1) * pageSize, totalItems)}
              </strong>{' '}
              trên tổng số <strong>{totalItems}</strong> phiếu xuất
            </div>

            <div className="flex items-center gap-2">
              <Button
                size="sm"
                variant="outline"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0 || isLoading}
                className="h-7 text-xs px-2"
              >
                <ChevronLeft className="w-3.5 h-3.5 mr-1" /> Trước
              </Button>
              <span className="font-medium text-slate-700">
                Trang {page + 1} / {totalPages}
              </span>
              <Button
                size="sm"
                variant="outline"
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= totalPages - 1 || isLoading}
                className="h-7 text-xs px-2"
              >
                Sau <ChevronRight className="w-3.5 h-3.5 ml-1" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Create Issue Modal */}
      <CreateIssueModal
        isOpen={isCreateOpen}
        entityId={entityId}
        branchId={branchId}
        branchName={branchName}
        items={items}
        onClose={() => setIsCreateOpen(false)}
        onSuccess={(created) => {
          fetchDocuments()
          setSelectedDocIdForDetail(created.id)
        }}
      />

      {/* Issue Detail Modal */}
      <IssueDetailModal
        isOpen={Boolean(selectedDocIdForDetail)}
        branchId={branchId}
        documentId={selectedDocIdForDetail}
        onClose={() => setSelectedDocIdForDetail(null)}
        onDocumentPosted={(updated) => {
          setDocuments((prev) => prev.map((d) => (d.id === updated.id ? updated : d)))
        }}
      />
    </div>
  )
}
